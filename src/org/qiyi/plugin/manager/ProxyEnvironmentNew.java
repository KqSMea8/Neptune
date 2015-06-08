package org.qiyi.plugin.manager;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.qiyi.pluginlibrary.ProxyEnvironment;
import org.qiyi.pluginlibrary.ResourcesProxy;
import org.qiyi.pluginlibrary.ErrorType.ErrorType;
import org.qiyi.pluginlibrary.api.ITargetLoadListenner;
import org.qiyi.pluginlibrary.exception.PluginStartupException;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.install.PluginInstaller;
import org.qiyi.pluginlibrary.plugin.TargetMapping;
import org.qiyi.pluginlibrary.pm.CMPackageInfo;
import org.qiyi.pluginlibrary.pm.CMPackageManager;
import org.qiyi.pluginlibrary.proxy.BroadcastReceiverProxy;
import org.qiyi.pluginlibrary.utils.ClassLoaderInjectHelper;
import org.qiyi.pluginlibrary.utils.JavaCalls;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginnew.ActivityJumpUtil;
import org.qiyi.pluginnew.ActivityOverider;
import org.qiyi.pluginnew.ReflectionUtils;
import org.qiyi.pluginnew.classloader.PluginClassLoader;
import org.qiyi.pluginnew.context.PluginContextWrapper;
import org.qiyi.pluginnew.service.PluginServiceWrapper;
import org.qiyi.pluginnew.service.ServiceProxyNew;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

/**
 * 插件运行的环境
 */
public class ProxyEnvironmentNew {

	public static final String TAG = ProxyEnvironmentNew.class.getSimpleName();

	/** pluginapp开关:data是否【去掉】包名前缀，默认加载包名前缀 */
	public static final String META_KEY_DATA_WITH_PREFIX = "pluginapp_cfg_data_with_prefix";
	/** 插件包名对应Environment的Hash */
	private static HashMap<String, ProxyEnvironmentNew> sPluginsMap = new HashMap<String, ProxyEnvironmentNew>();

	private final Context context;
	private final File apkFile;

	private PluginClassLoader dexClassLoader;
	private Resources targetResources;
	private AssetManager targetAssetManager;
	private Theme targetTheme;
	private TargetMapping targetMapping;
	// /** data文件是否需要加前缀 */
	// private boolean bIsDataNeedPrefix = true;
	private String parentPackagename;
	/** 插件的Activity栈 */
	private LinkedList<Activity> activityStack;
	/** 插件虚拟的Application实例 */
	private Application application;
	// /** 插件数据根目录 */
	// private File targetDataRoot;
	/** 是否初始化了插件Application */
	private boolean bIsApplicationInit = false;

	/** Loading Map，正在loading中的插件 */
	private static Map<String, List<Intent>> gLoadingMap = new HashMap<String, List<Intent>>();

	private String pluginPakName;

	/**
	 * 记录正在运行的service
	 */
	public static ConcurrentMap<String, PluginServiceWrapper> sAliveServices = new ConcurrentHashMap<String, PluginServiceWrapper>(
			1);

	public static Activity mParent;// 保存宿主activity

	public PluginContextWrapper appWrapper;

	/**
	 * 构造方法，解析apk文件，创建插件运行环境
	 * 
	 * @param context
	 *            host application context
	 * @param apkFile
	 *            插件apk文件
	 */
	private ProxyEnvironmentNew(Context context, File apkFile, String pluginPakName) {
		this.context = context.getApplicationContext();
		this.apkFile = apkFile;
		activityStack = new LinkedList<Activity>();
		parentPackagename = context.getPackageName();
		this.pluginPakName = pluginPakName;
		createTargetMapping(pluginPakName);
		// 加载classloader
		createClassLoader();
		// 加载资源
		createTargetResource();
	}

	/**
	 * 获取插件apk路径
	 * 
	 * @return 绝对路径
	 */
	public String getTargetPath() {
		return this.apkFile.getAbsolutePath();
	}

	/**
	 * 获取插件运行环境实例，调用前保证已经初始化，否则会抛出异常
	 * 
	 * @param packageName
	 *            插件包名
	 * @return 插件环境对象
	 */
	public static ProxyEnvironmentNew getInstance(String packageName) {
		ProxyEnvironmentNew env = null;
		if (packageName != null) {
			env = sPluginsMap.get(packageName);
		}
		if (env == null) {
			deliverPlug(false, packageName, ErrorType.ERROR_CLIENT_ENVIRONMENT_NULL);
			// throw new IllegalArgumentException(packageName
			// +" not loaded, Make sure you have call the init method!");

		}
		return env;
	}

	/**
	 * 是否已经建立对应插件的environment
	 * 
	 * @param packageName
	 *            包名，已经做非空判断
	 * @return true表示已经建立
	 */
	public static boolean hasInstance(String packageName) {
		if (packageName == null) {
			return false;
		}
		return sPluginsMap.containsKey(packageName);
	}

	/**
	 * 插件是否已经进入了代理模式
	 * 
	 * @param packageName
	 *            插件包名
	 * @return true or false
	 */
	public static boolean isEnterProxy(String packageName) {
		if (packageName == null) {
			return false;
		}
		ProxyEnvironmentNew env = sPluginsMap.get(packageName);
		if (env != null && env.bIsApplicationInit) {
			return true;
		}

		return false;
	}

	/**
	 * 清除等待队列，防止异常情况，导致所有Intent都阻塞在等待队列，插件再也起不来就杯具了
	 * 
	 * @param packageName
	 *            包名
	 */
	public static void clearLoadingIntent(String packageName) {
		if (packageName == null) {
			return;
		}

		synchronized (gLoadingMap) {
			gLoadingMap.remove(packageName);
		}
	}

	/**
	 * 插件是否正在loading中
	 * 
	 * @param packageName
	 *            插件包名
	 * @return true or false
	 */
	public static boolean isLoading(String packageName) {
		if (packageName == null) {
			return false;
		}

		boolean ret = false;
		synchronized (gLoadingMap) {
			ret = gLoadingMap.containsKey(packageName);
		}
		return ret;
	}

	/**
	 * 运行插件代理
	 * 
	 * @param context
	 *            host 的application context
	 * @param intent
	 *            加载插件运行的intent
	 */
	public static void enterProxy(final Context context, final ServiceConnection conn,
			final Intent intent) {
		if (context instanceof Activity) {
			mParent = (Activity) context;
		}
		final String packageName = intent.getComponent().getPackageName();
		if (TextUtils.isEmpty(packageName)) {
			deliverPlug(false, context.getPackageName(), ErrorType.ERROR_CLIENT_LOAD_NO_PAKNAME);// 添加投递
			// throw new
			// RuntimeException("*** loadTarget with null packagename!");
			return;
		}

		boolean isEnterProxy = false;
		synchronized (gLoadingMap) {
			List<Intent> cacheIntents = gLoadingMap.get(packageName);
			if (cacheIntents != null) {// 说明插件正在loading
				// 正在loading，直接返回吧，等着loading完调起
				// 把intent都缓存起来
				cacheIntents.add(intent);
				return;
			}

			isEnterProxy = isEnterProxy(packageName);// 判断是否已经进入到代理
			if (!isEnterProxy) {
				List<Intent> intents = new ArrayList<Intent>();
				intents.add(intent);
				gLoadingMap.put(packageName, intents);// 正在加载的插件队列
			}
		}

		if (isEnterProxy) {
			// 已经初始化，直接起Intent
			launchIntent(context, conn, intent);
			return;
		}

		CMPackageManager.getInstance(context.getApplicationContext()).packageAction(packageName,
				new IInstallCallBack() {

					@Override
					public void onPackageInstallFail(String packageName, int failReason) {
						clearLoadingIntent(packageName);
						ProxyEnvironmentNew.deliverPlug(false, packageName, failReason);
					}

					@Override
					public void onPacakgeInstalled(String packageName) {
						PluginDebugLog.log(TAG, "安装完成：开始初始化initTarget");

						initTarget(context.getApplicationContext(), packageName,
								new ITargetLoadListenner() {

									@Override
									public void onLoadFinished(String packageName) {
										try {
											launchIntent(context, conn, intent);
										} catch (Exception e) {

										}
									}
								});
					}
				});
	}

	/**
	 * @param success
	 * @param pakName
	 * @param errorCode
	 *            用于插件qos 投递
	 */
	public static void deliverPlug(boolean success, String pakName, int errorCode) {
		if (iDeliverPlug != null) {
			iDeliverPlug.deliverPlug(success, pakName, errorCode);
		}
	}

	/**
	 * 进入代理模式
	 * 
	 * @param context
	 *            host 的 application context
	 * @throws Exception
	 *             调用逻辑异常
	 * 
	 * @return true表示启动了activity，false表示启动失败，或者启动非activity
	 */
	public static boolean launchIntent(Context context, ServiceConnection conn, Intent intent) {
		PluginDebugLog.log("plugin", "launchIntent");
		String packageName = intent.getComponent().getPackageName();
		ProxyEnvironmentNew env = sPluginsMap.get(packageName);
		if (env == null) {
			deliverPlug(false, packageName, ErrorType.ERROR_CLIENT_NOT_LOAD);// 增加投递
			return false;
			// throw new IllegalArgumentException(packageName
			// +" not loaded, Make sure you have call the init method!");
		}

		List<Intent> cacheIntents = null;
		if (!env.bIsApplicationInit && env.application == null) {

			// Application 创建
			String className = env.targetMapping.getApplicationClassName();
			if (className == null || "".equals(className)
					|| Application.class.getName().equals(className)) {
				// 创建默认的虚拟Application
				env.application = new Application();
			} else {
				try {
					env.application = ((Application) env.dexClassLoader.loadClass(className)
							.asSubclass(Application.class).newInstance());
				} catch (Exception e) {
					deliverPlug(false, packageName, ErrorType.ERROR_CLIENT_INIT_PLUG_APP);// 添加投递
					return false;
					// throw new RuntimeException(e.getMessage(), e);
				}
			}

			env.setApplicationBase(env, env.application, packageName);
			env.application.onCreate();
			env.changeInstrumentation(context, packageName);
			env.bIsApplicationInit = true;

			synchronized (gLoadingMap) {
				cacheIntents = gLoadingMap.remove(packageName);
			}
		}

		if (cacheIntents == null) {

			// 没有缓存的Intent，取当前的Intent;
			cacheIntents = new ArrayList<Intent>();
			cacheIntents.add(intent);
		}

		PluginDebugLog.log(TAG, "launchIntent_cacheIntents:" + cacheIntents.size()
				+ ";cacheIntents:" + cacheIntents);
		boolean haveLaunchActivity = false;
		for (Intent curIntent : cacheIntents) {

			// 获取目标class
			String targetClassName = curIntent.getComponent().getClassName();
			PluginDebugLog.log(TAG, "launchIntent_targetClassName:" + targetClassName);
			if (TextUtils.equals(targetClassName, ProxyEnvironment.EXTRA_VALUE_LOADTARGET_STUB)) {

				// 表示后台加载，不需要处理该Intent
				continue;
			}
			if (TextUtils.isEmpty(targetClassName)) {
				targetClassName = env.getTargetMapping().getDefaultActivityName();
			}

			// 处理启动的是service
			Class<?> targetClass;
			try {
				targetClass = env.dexClassLoader.loadClass(targetClassName);
			} catch (Exception e) {
				deliverPlug(false, packageName, ErrorType.ERROR_CLIENT_LOAD_START);// 添加投递
				// targetClass = CMActivity.class;
				return false;
			}
			PluginDebugLog.log(TAG, "launchIntent_targetClass:" + targetClass);
			// deliverPlug(true, packageName,
			// ErrorType.ERROR_CLIENT_LOAD_START);//添加投递
			if (Service.class.isAssignableFrom(targetClass)) {
				env.remapStartServiceIntent(curIntent, targetClassName);
				if (conn == null) {
					context.startService(curIntent);
				} else {
					context.bindService(curIntent, conn, curIntent.getIntExtra(
							ProxyEnvironment.BIND_SERVICE_FLAGS, Context.BIND_AUTO_CREATE));
				}

			} else if (BroadcastReceiver.class.isAssignableFrom(targetClass)) { // 发一个内部用的动态广播
				Intent newIntent = new Intent(curIntent);
				newIntent.setComponent(null);
				newIntent.putExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME, packageName);
				newIntent.setPackage(context.getPackageName());
				context.sendBroadcast(newIntent);
			} else {
				mappingActivity(context, curIntent);
				env.dealLaunchMode(intent);
				context.startActivity(curIntent);
				haveLaunchActivity = true;
			}
		}
		PluginDebugLog.log(TAG, "haveLaunchActivity :" + haveLaunchActivity);
		return haveLaunchActivity;
	}

	private void setApplicationBase(ProxyEnvironmentNew env, Application application,
			String packageName) {
		PluginContextWrapper ctxWrapper = new PluginContextWrapper(context, env);
		this.appWrapper = ctxWrapper;
		// attach
		Method attachMethod;
		try {
			attachMethod = android.app.Application.class.getDeclaredMethod("attach", Context.class);
			attachMethod.setAccessible(true);
			attachMethod.invoke(application, ctxWrapper);
		} catch (Exception e) {
			ProxyEnvironmentNew.deliverPlug(false, packageName,
					ErrorType.ERROR_CLIENT_SET_APPLICATION_BASE_FAIL);
			e.printStackTrace();
		}
	}

	private static void mappingActivity(Context context, Intent intent) {
		String plugIdOrPkg;
		String actName;
		ComponentName origComp = intent.getComponent();
		if (origComp != null) {
			plugIdOrPkg = origComp.getPackageName();
			actName = origComp.getClassName();
		} else {
			throw new IllegalArgumentException("plug intent must set the ComponentName!");
		}
		intent.putExtra(ActivityOverider.PLUGIN_ID, plugIdOrPkg);
		intent.putExtra(ActivityOverider.PLUGIN_ACTIVITY, actName);
		ComponentName comp = new ComponentName(context, ActivityJumpUtil.TARGET_CLASS_NAME);
		intent.setAction(null);
		intent.setComponent(comp);
	}

	/**
	 * 初始化插件的运行环境，如果已经初始化，则什么也不做
	 * 
	 * @param context
	 *            application context
	 * @param packageName
	 *            插件包名
	 */
	public static void initProxyEnvironment(Context context, String packageName) {
		PluginDebugLog.log(TAG,
				"sPluginsMap.containsKey(packageName):" + sPluginsMap.containsKey(packageName));
		if (sPluginsMap.containsKey(packageName)) {
			return;
		}
		ProxyEnvironmentNew newEnv = new ProxyEnvironmentNew(context,
				PluginInstaller.getInstalledApkFile(context, packageName), packageName);
		sPluginsMap.put(packageName, newEnv);
	}

	/**
	 * 退出插件
	 * 
	 * @param packageName
	 *            插件包名
	 */
	public static void exitProxy(String packageName) {
		if (packageName != null) {
			ProxyEnvironmentNew env = sPluginsMap.remove(packageName);
			if (env == null || env.application == null) {
				return;
			}
			env.ejectClassLoader();
			if (env.bIsApplicationInit) {
				env.application.onTerminate();
			}
		}
	}

	/**
	 * 获取插件的classloader
	 * 
	 * @return classloader
	 */
	public ClassLoader getDexClassLoader() {
		return dexClassLoader;
	}

	/**
	 * 获取插件资源
	 * 
	 * @return 资源对象
	 */
	public Resources getTargetResources() {
		return targetResources;
	}

	public AssetManager getTargetAssetManager() {
		return targetAssetManager;
	}

	public Theme getTargetTheme() {
		return targetTheme;
	}

	public TargetMapping getTargetMapping() {
		return targetMapping;
	}

	public String getTargetPackageName() {
		return targetMapping.getPackageName();
	}

	public void remapStartServiceIntent(Intent originIntent) {

		// 隐式启动Service不支持
		if (originIntent.getComponent() == null) {
			return;
		}

		String targetActivity = originIntent.getComponent().getClassName();
		remapStartServiceIntent(originIntent, targetActivity);
	}

	public void pushActivityToStack(Activity activity) {
		activityStack.addFirst(activity);
	}

	public boolean popActivityFromStack(Activity activity) {
		if (!activityStack.isEmpty()) {
			return activityStack.remove(activity);
		} else {
			return false;
		}
	}

	public void dealLaunchMode(Intent intent) {
		String targetActivity = intent.getStringExtra(ActivityOverider.PLUGIN_ACTIVITY);
		if (TextUtils.isEmpty(targetActivity) || intent == null) {
			return;
		}

		// 不支持LAUNCH_SINGLE_INSTANCE
		ActivityInfo info = targetMapping.getActivityInfo(targetActivity);
		if (info.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
			return;
		}
		boolean isSingleTop = info.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP
				|| (intent.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0;
		boolean isSingleTask = info.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK;
		boolean isClearTop = (intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0;
		if (isSingleTop && !isClearTop) {

			// 判断栈顶是否为需要启动的Activity
			Activity activity = null;
			if (!activityStack.isEmpty()) {
				activity = activityStack.getFirst();
			}
			if (activity != null
					&& TextUtils.equals(ActivityJumpUtil.TARGET_CLASS_NAME, activity.getClass()
							.getName())) {
				String key = activity.getClass().getSuperclass().getName();
				if (!TextUtils.isEmpty(key) && TextUtils.equals(targetActivity, key)) {
					intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				}
			}
		} else if (isSingleTask || isClearTop) {

			Activity found = null;
			Iterator<Activity> it = activityStack.iterator();
			while (it.hasNext()) {
				Activity activity = it.next();
				if (activity != null
						&& TextUtils.equals(ActivityJumpUtil.TARGET_CLASS_NAME, activity.getClass()
								.getName())) {
					String key = activity.getClass().getSuperclass().getName();
					if (!TextUtils.isEmpty(key) && TextUtils.equals(targetActivity, key)) {
						found = activity;
						break;
					}
				}
			}

			// 栈中已经有当前activity
			if (found != null) {
				Iterator<Activity> iterator = activityStack.iterator();
				while (iterator.hasNext()) {
					Activity activity = iterator.next();
					if (activity == found) {
						if (isSingleTask || isSingleTop) {
							intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
						}
						// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						break;
					}
					activity.finish();
				}
			}
		}
	}

	public void remapStartServiceIntent(Intent intent, String targetService) {
		if (targetMapping.getServiceInfo(targetService) == null) {
			return;
		}
		intent.putExtra(ProxyEnvironment.EXTRA_TARGET_SERVICE, targetService);
		intent.putExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME, targetMapping.getPackageName());

		intent.setClass(context, ServiceProxyNew.class);
	}

	/**
	 * @param originIntent
	 */
	public void remapReceiverIntent(Intent originIntent) {
		if (originIntent.getComponent() != null) {
			String targetReceiver = originIntent.getComponent().getClassName();
			originIntent.putExtra(ProxyEnvironment.EXTRA_TARGET_RECEIVER, targetReceiver);
			originIntent.putExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME,
					targetMapping.getPackageName());
			originIntent.setClass(context, BroadcastReceiverProxy.class);
		}
	}

	/**
	 * 获取某个插件的 data 根目录。
	 * 
	 * @param packageName
	 * @return
	 */
	public File getDataDir(Context context, String packageName) {
		PluginDebugLog.log("plugin", "packageName:" + packageName + " context:" + context);
		File file = null;
		try {
			if (TextUtils.isEmpty(packageName)) {
				deliverPlug(false, context.getPackageName(),
						ErrorType.ERROR_CLIENT_LOAD_CREATE_FILE_NULL);// 添加投递
				return null;
			}
			file = new File(getTargetMapping().getDataDir());
		} catch (Exception e) {
			// deliverPlug(false, context.getPackageName(),
			// ErrorType.ERROR_CLIENT_LOAD_CREATE_FILE_NULL);//添加投递
		}
		return file;
	}

	/**
	 * 创建ClassLoader， 需要在 createDataRoot之后调用
	 */
	private void createClassLoader() {

		PluginDebugLog.log("plugin", "createClassLoader");
		dexClassLoader = new PluginClassLoader(apkFile.getAbsolutePath(), getDataDir(context,
				pluginPakName).getAbsolutePath(), context.getClassLoader(), this);

		// 把 插件 classloader 注入到 host程序中，方便host app 能够找到 插件 中的class。
		if (targetMapping.getMetaData() != null
				&& targetMapping.getMetaData().getBoolean(ProxyEnvironment.META_KEY_CLASSINJECT)) {
			ClassLoaderInjectHelper.inject(context.getClassLoader(), dexClassLoader,
					targetMapping.getPackageName() + ".R");
			PluginDebugLog.log(TAG, "--- Class injecting @ " + targetMapping.getPackageName());
		}
	}

	/**
	 * 如果注入了classloader，执行反注入操作。用于卸载时。
	 */
	public void ejectClassLoader() {
		if (dexClassLoader != null && targetMapping.getMetaData() != null
				&& targetMapping.getMetaData().getBoolean(ProxyEnvironment.META_KEY_CLASSINJECT)) {
			ClassLoaderInjectHelper.eject(context.getClassLoader(), dexClassLoader);
		}
	}

	@SuppressLint("NewApi")
	private void createTargetResource() {
		try {
			AssetManager am = (AssetManager) AssetManager.class.newInstance();
			JavaCalls.callMethod(am, "addAssetPath", new Object[] { apkFile.getAbsolutePath() });
			targetAssetManager = am;
		} catch (Exception e) {
			deliverPlug(false, pluginPakName, ErrorType.ERROR_CLIENT_LOAD_INIT_RESOURCE_FAILE);
			e.printStackTrace();
		}

		// 解决一个HTC ONE X上横竖屏会黑屏的问题
		Resources hostRes = context.getResources();
		Configuration config = new Configuration();
		config.setTo(hostRes.getConfiguration());
		config.orientation = Configuration.ORIENTATION_UNDEFINED;
		targetResources = new ResourcesProxy(targetAssetManager, hostRes.getDisplayMetrics(),
				config, hostRes);
		targetTheme = targetResources.newTheme();
		targetTheme.setTo(context.getTheme());
	}

	/**
	 * 初始化插件资源
	 */
	private void createTargetMapping(String pkgName) {
		CMPackageInfo pkgInfo = CMPackageManager.getInstance(context).getPackageInfo(pkgName);
		if (pkgInfo != null) {
			targetMapping = pkgInfo.targetInfo;
		} else {
			// throw new Exception("Havn't install pkgName");
		}
		// targetMapping = new ApkTargetMappingNew(context, apkFile);
		//
		// bIsDataNeedPrefix = targetMapping.getMetaData() == null
		// || !targetMapping.getMetaData().getBoolean(
		// ProxyEnvironment.META_KEY_DATA_WITHOUT_PREFIX);
	}

	/**
	 * @return the parentPackagename
	 */
	public String getParentPackagename() {
		return parentPackagename;
	}

	public int getTargetActivityThemeResource(String activity) {
		return targetMapping.getThemeResource(activity);
	}

	/**
	 * 获取插件Activity的屏幕方向
	 * 
	 * @param activity
	 *            activity类名
	 * @return 屏幕方向
	 */
	public int getTargetActivityOrientation(String activity) {
		return targetMapping.getActivityInfo(activity).screenOrientation;
	}

	/**
	 * @return the application
	 */
	public Application getApplication() {
		return application;
	}

	public ActivityInfo findActivityByClassName(String activityClsName) {
		return targetMapping.getActivityInfo(activityClsName);
	}

	/**
	 * 退出某个应用。不是卸载插件应用
	 * 
	 * @param packageName
	 */
	public void quitApp() {
		while (!activityStack.isEmpty()) {
			activityStack.poll().finish();
		}
		activityStack.clear();
	}

	/**
	 * 加载插件
	 * 
	 * @param context
	 *            application Context
	 * @param packageName
	 *            插件包名
	 * @param listenner
	 *            插件加载后的回调
	 */
	public static void initTarget(final Context context, String packageName,
			final ITargetLoadListenner listenner) {
		PluginDebugLog.log("plugin", "initTarget");
		try {
			new InitProxyEnvireonment(context, packageName, listenner).start();
		} catch (Exception e) {
			deliverPlug(false, packageName, ErrorType.ERROR_CLIENT_LOAD_INIT_TARGET);
			e.printStackTrace();
		}
	}

	/**
	 * Get host's context
	 * 
	 * @return return host's context
	 */
	public Context getHostContext() {
		// return mParent first priority due to some 3rd SDK need host activity
		// like AliPay etc.
		if (mParent != null) {
			return mParent;
		}
		return context;
	}

	private void changeInstrumentation(Context context, String pkgName) {
		try {
			Context contextImpl = ((ContextWrapper) context).getBaseContext();
			Object activityThread = ReflectionUtils.getFieldValue(contextImpl, "mMainThread");
			Field instrumentationF = activityThread.getClass().getDeclaredField("mInstrumentation");
			instrumentationF.setAccessible(true);
			FrameworkInstrumentation instrumentation = new FrameworkInstrumentation(context);
			instrumentationF.set(activityThread, instrumentation);
			instrumentationF.setAccessible(false);
		} catch (Exception e) {
			ProxyEnvironmentNew.deliverPlug(false, pkgName,
					ErrorType.ERROR_CLIENT_CHANGE_INSTRUMENTATION_FAIL);
			e.printStackTrace();
		}
	}

	static class InitProxyEnvireonment extends Thread {
		public String pakName;
		public Context pContext;
		ITargetLoadListenner listenner;

		public InitProxyEnvireonment(Context context, String pakName, ITargetLoadListenner listenner) {
			this.pakName = pakName;
			this.pContext = context;
			this.listenner = listenner;
		}

		@Override
		public void run() {
			super.run();
			try {
				PluginDebugLog.log("plugin", "doInBackground:" + pakName);
				ProxyEnvironmentNew.initProxyEnvironment(pContext, pakName);
				new InitHandler(listenner, pakName, Looper.getMainLooper()).sendEmptyMessage(1);
			} catch (Exception e) {
				if (e instanceof PluginStartupException) {
					PluginStartupException ex = (PluginStartupException) e;
					deliverPlug(false, pakName, ex.getCode());// 添加投递
				} else {
					deliverPlug(false, pakName, ErrorType.ERROR_CLIENT_LOAD_INIT_ENVIRONMENT_FAIL);// 添加投递
				}
			}
		}
	}

	static class InitHandler extends Handler {
		ITargetLoadListenner listenner;
		String pakName;

		public InitHandler(ITargetLoadListenner listenner, String pakName, Looper looper) {
			super(looper);
			this.listenner = listenner;
			this.pakName = pakName;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				listenner.onLoadFinished(pakName);
				break;

			default:
				break;
			}
			super.handleMessage(msg);
		}
	}

	private static class FrameworkInstrumentation extends Instrumentation {
		private Context mContext;

		public FrameworkInstrumentation(Context context) {
			super();
			mContext = context;
		}

		@Override
		public Activity newActivity(ClassLoader cl, String className, Intent intent)
				throws InstantiationException, IllegalAccessException, ClassNotFoundException {

			if (className.equals(ActivityJumpUtil.TARGET_CLASS_NAME)) {
				String pluginId = intent.getStringExtra(ActivityOverider.PLUGIN_ID);
				String actClassName = intent.getStringExtra(ActivityOverider.PLUGIN_ACTIVITY);

				ProxyEnvironmentNew env = ProxyEnvironmentNew.getInstance(pluginId);
				if (env == null && mContext != null) {
					// Check whether pluginId has installed, then init the
					// installed apk
					CMPackageInfo info = CMPackageManager.getInstance(mContext).getPackageInfo(
							pluginId);
					if (null != info) {
						initProxyEnvironment(mContext, pluginId);
						env = ProxyEnvironmentNew.getInstance(pluginId);
					}
				}
				if (env != null && env.dexClassLoader != null) {
					return (Activity) ProxyEnvironmentNew.getInstance(pluginId).dexClassLoader
							.loadActivityClass(actClassName).newInstance();
				}
			}
			return super.newActivity(cl, className, intent);
		}
	}

	private static IDeliverPlug iDeliverPlug;

	public static void setiDeliverPlug(IDeliverPlug iDeliverPlug) {
		ProxyEnvironmentNew.iDeliverPlug = iDeliverPlug;
	}

	public interface IDeliverPlug {
		void deliverPlug(boolean success, String pakName, int errorCode);
	}
}