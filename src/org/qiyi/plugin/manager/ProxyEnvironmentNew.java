package org.qiyi.plugin.manager;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.qiyi.pluginlibrary.LPluginInstrument;
import org.qiyi.pluginlibrary.PluginActivityControl;
import org.qiyi.pluginlibrary.ResourcesProxy;
import org.qiyi.pluginlibrary.ErrorType.ErrorType;
import org.qiyi.pluginlibrary.api.ITargetLoadListenner;
import org.qiyi.pluginlibrary.component.InstrActivityProxy;
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
import org.qiyi.pluginlibrary.utils.ReflectionUtils;
import org.qiyi.pluginlibrary.utils.ResourcesToolForPlugin;
import org.qiyi.pluginnew.ActivityJumpUtil;
import org.qiyi.pluginnew.classloader.PluginClassLoader;
import org.qiyi.pluginnew.context.PluginContextWrapper;
import org.qiyi.pluginnew.service.PluginServiceWrapper;
import org.qiyi.pluginnew.service.ServiceProxyNew;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
//import android.app.Application.ActivityLifecycleCallbacks;
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

    public static final String EXTRA_TARGET_ACTIVITY = "pluginapp_extra_target_activity";
    public static final String EXTRA_TARGET_SERVICE = "pluginapp_extra_target_service";
    public static final String EXTRA_TARGET_RECEIVER= "pluginapp_extra_target_receiver";
    public static final String EXTRA_TARGET_PACKAGNAME = "pluginapp_extra_target_pacakgename";
    public static final String EXTRA_TARGET_ISBASE = "pluginapp_extra_target_isbase";
    public static final String EXTRA_TARGET_REDIRECT_ACTIVITY = "pluginapp_extra_target_redirect_activity";
    public static final String EXTRA_VALUE_LOADTARGET_STUB = "pluginapp_loadtarget_stub";
    
    public static final String BIND_SERVICE_FLAGS = "bind_service_flags";
    
    public static final String META_KEY_CLASSINJECT = "pluginapp_class_inject";
    
    /** 插件加载成功的广播 */
    public static final String ACTION_TARGET_LOADED = "org.qiyi.pluginapp.action.TARGET_LOADED";

	/** pluginapp开关:data是否【去掉】包名前缀，默认加载包名前缀 */
	public static final String META_KEY_DATA_WITH_PREFIX = "pluginapp_cfg_data_with_prefix";
	/** 插件包名对应Environment的Hash */
	private static HashMap<String, ProxyEnvironmentNew> sPluginsMap = new HashMap<String, ProxyEnvironmentNew>();

//	/** 插件调试日志 **/
//	private static ActivityLifecycleCallbacks sActivityLifecycleCallback = new ActivityLifecycleCallbacks() {
//
//		@Override
//		public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
//			PluginDebugLog.log(TAG, "onActivityCreated: " + activity);
//		}
//
//		@Override
//		public void onActivityStarted(Activity activity) {
//			PluginDebugLog.log(TAG, "onActivityStarted: " + activity);
//		}
//
//		@Override
//		public void onActivityResumed(Activity activity) {
//			PluginDebugLog.log(TAG, "onActivityResumed: " + activity);
//		}
//
//		@Override
//		public void onActivityPaused(Activity activity) {
//			PluginDebugLog.log(TAG, "onActivityPaused: " + activity);
//		}
//
//		@Override
//		public void onActivityStopped(Activity activity) {
//			PluginDebugLog.log(TAG, "onActivityStopped: " + activity);
//		}
//
//		@Override
//		public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
//			PluginDebugLog.log(TAG, "onActivitySaveInstanceState: " + activity);
//		}
//
//		@Override
//		public void onActivityDestroyed(Activity activity) {
//			PluginDebugLog.log(TAG, "onActivityDestroyed: " + activity);
//		}
//	};

	/** 插件安装方式 **/
	private final Context mContext;
	private final File mApkFile;
	private final String mInstallType;

	private PluginClassLoader dexClassLoader;
	private Resources targetResources;
	private ResourcesToolForPlugin mResTool;
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

	private String mPluginPakName;

	/**
	 * 记录正在运行的service
	 */
	public static ConcurrentMap<String, PluginServiceWrapper> sAliveServices = new ConcurrentHashMap<String, PluginServiceWrapper>(
			1);

	public static Activity mParent;// 保存宿主activity

    public PluginContextWrapper appWrapper;
    
    public LPluginInstrument mPluginInstrument;
    
    private static Resources hostRes;
    
	/**
	 * 构造方法，解析apk文件，创建插件运行环境
	 * 
	 * @param context
	 *            host application context
	 * @param apkFile
	 *            插件apk文件
	 * @throws Exception 
	 */
	private ProxyEnvironmentNew(Context context, File apkFile, String pluginPakName,
			String installType) throws Exception {
		if (context == null || apkFile == null || TextUtils.isEmpty(pluginPakName)
				|| TextUtils.isEmpty(installType)) {
			throw new Exception(
					"ProxyEnvironmentNew init failed for parameters has null: context: " + context
							+ " apkFile: " + apkFile + " pluginPakName: " + pluginPakName
							+ " installType: " + installType);
		}
		mContext = context.getApplicationContext();
		mApkFile = apkFile;
		activityStack = new LinkedList<Activity>();
		parentPackagename = context.getPackageName();
		mPluginPakName = pluginPakName;
		mInstallType = installType;
		createTargetMapping(pluginPakName);
		// 加载classloader
		createClassLoader();
		// 加载资源
		createTargetResource();
	}
	
	public static String getTopActivity() {
		String topPackage = null;
		Iterator<?> iter = sPluginsMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, ProxyEnvironmentNew> entry = (Entry<String, ProxyEnvironmentNew>) iter.next();
			String packageName = (String) entry.getKey();
			ProxyEnvironmentNew environmentNew = (ProxyEnvironmentNew) entry.getValue();
			if (environmentNew.activityStack.size() == 0) {
				continue;
			} else if (isResumed(environmentNew.activityStack.getFirst())) {
				topPackage = packageName;
			}
		}
		return topPackage;
	}

	public static void updateConfiguration(Configuration config){
		Iterator<?> iter = sPluginsMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<?,?> entry = (Map.Entry<?,?>) iter.next();
			ProxyEnvironmentNew env = (ProxyEnvironmentNew) entry.getValue();
			env.getTargetResources().updateConfiguration(config, hostRes != null?hostRes.getDisplayMetrics():null);
		}
	}
	
	private static boolean isResumed(Activity mActivity) {
		boolean result = true;
		try {
			Class<?> clazz = Class.forName("android.app.Activity");
			Method isResumed = clazz.getDeclaredMethod("isResumed");
			result = (Boolean) isResumed.invoke(mActivity);
		} catch (ClassNotFoundException e) {
			if (PluginDebugLog.isDebug()) {
				e.printStackTrace();
			}
		} catch (NoSuchMethodException e) {
			if (PluginDebugLog.isDebug()) {
				e.printStackTrace();
			}
		} catch (IllegalAccessException e) {
			if (PluginDebugLog.isDebug()) {
				e.printStackTrace();
			}
		} catch (IllegalArgumentException e) {
			if (PluginDebugLog.isDebug()) {
				e.printStackTrace();
			}
		} catch (InvocationTargetException e) {
			if (PluginDebugLog.isDebug()) {
				e.printStackTrace();
			}
		}

		return result;
	}

	/**
	 * Get install type {@link CMPackageManager#PLUGIN_METHOD_DEFAULT}
	 * {@link CMPackageManager#PLUGIN_METHOD_DEXMAKER}
	 * {@link CMPackageManager#PLUGIN_METHOD_INSTR}
	 */
	public String getInstallType() {
		return mInstallType;
	}

	/**
	 * 获取插件apk路径
	 * 
	 * @return 绝对路径
	 */
	public String getTargetPath() {
		return this.mApkFile.getAbsolutePath();
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
		if (!TextUtils.isEmpty(packageName)) {
			env = sPluginsMap.get(packageName);
		} else {
			PluginDebugLog.log(TAG, "getInstance packageName is empty!");
		}
		if (env == null) {
			PluginDebugLog.log(TAG, "getInstance env is null!");
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
		if (TextUtils.isEmpty(packageName)) {
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
		if (TextUtils.isEmpty(packageName)) {
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
		if (TextUtils.isEmpty(packageName)) {
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
		if (TextUtils.isEmpty(packageName)) {
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
		final String packageName = tryParsePkgName(context, intent);;
		if (TextUtils.isEmpty(packageName)) {
			deliverPlug(false, context.getPackageName(), ErrorType.ERROR_CLIENT_LOAD_NO_PAKNAME);// 添加投递
			PluginDebugLog.log(TAG, "enterProxy packageName is null return! packageName: "
					+ packageName);
			return;
		}

		boolean isEnterProxy = false;
		synchronized (gLoadingMap) {
			List<Intent> cacheIntents = gLoadingMap.get(packageName);
			if (cacheIntents != null) {// 说明插件正在loading
				// 正在loading，直接返回吧，等着loading完调起
				// 把intent都缓存起来
				cacheIntents.add(intent);
				PluginDebugLog.log(TAG, "LoadingMap is not empty, Cache current intent, intent: "
						+ intent);
				return;
			}

			isEnterProxy = isEnterProxy(packageName);// 判断是否已经进入到代理
			if (!isEnterProxy) {
				List<Intent> intents = new ArrayList<Intent>();
				intents.add(intent);
				PluginDebugLog.log(TAG, "Environment is loading cache current intent, intent: "
						+ intent);
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
											e.printStackTrace();
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
	 * Helper method to get package name from intent
	 * 
	 * @return
	 */
	private static String tryParsePkgName(Context context, Intent intent) {
		if (intent == null) {
			return "";
		}
		ComponentName cpn = intent.getComponent();
		if (cpn == null || TextUtils.isEmpty(cpn.getPackageName())) {
			if (context == null) {
				return "";
			}
			// Here, loop all installed packages to get pkgName.
			for (CMPackageInfo info : CMPackageManager.getInstance(context).getInstalledApps()) {
				if (info != null && info.targetInfo != null) {
					if (info.targetInfo.resolveActivity(intent) != null) {
						return info.packageName;
					}
				}
			}
		} else {
			return cpn.getPackageName();
		}
		return "";
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
		PluginDebugLog.log("plugin", "launchIntent: " + intent);
		String packageName = tryParsePkgName(context, intent);
		ProxyEnvironmentNew env = sPluginsMap.get(packageName);
		if (env == null) {
			deliverPlug(false, packageName, ErrorType.ERROR_CLIENT_NOT_LOAD);// 增加投递
			PluginDebugLog.log(TAG, "launchIntent env is null! Just return!");
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
					e.printStackTrace();
					return false;
					// throw new RuntimeException(e.getMessage(), e);
				}
			}

			env.setApplicationBase(env, env.application, packageName);
			env.application.onCreate();
			env.changeInstrumentation(context, packageName);
			env.bIsApplicationInit = true;
//			env.application.registerActivityLifecycleCallbacks(sActivityLifecycleCallback);

			synchronized (gLoadingMap) {
				cacheIntents = gLoadingMap.remove(packageName);
			}
		}

		if (cacheIntents == null) {

			// 没有缓存的Intent，取当前的Intent;
			cacheIntents = new ArrayList<Intent>();
			cacheIntents.add(intent);
		}

		PluginDebugLog.log(TAG, "launchIntent_cacheIntents: " + cacheIntents.size()
				+ "; cacheIntents: " + cacheIntents);
		boolean haveLaunchActivity = false;
		for (Intent curIntent : cacheIntents) {
			if (curIntent == null) {
				continue;
			}
			String targetClassName = "";
			if (curIntent.getComponent() != null) {
				// 获取目标class
				targetClassName = curIntent.getComponent().getClassName();
				PluginDebugLog.log(TAG, "launchIntent_targetClassName:" + targetClassName);
				if (TextUtils.equals(targetClassName, ProxyEnvironmentNew.EXTRA_VALUE_LOADTARGET_STUB)) {
					
					// 表示后台加载，不需要处理该Intent
					continue;
				}
				if (TextUtils.isEmpty(targetClassName)) {
					targetClassName = env.getTargetMapping().getDefaultActivityName();
				}
			}// else is for action invoke

			// 处理启动的是service
			Class<?> targetClass = null;
			if (!TextUtils.isEmpty(targetClassName)) {
				try {
					targetClass = env.dexClassLoader.loadClass(targetClassName);
				} catch (Exception e) {
					deliverPlug(false, packageName, ErrorType.ERROR_CLIENT_LOAD_START);// 添加投递
					PluginDebugLog.log(TAG, "launchIntent loadClass failed for targetClassName: "
							+ targetClassName);					
					return false;
				}
			}
			PluginDebugLog.log(TAG, "launchIntent_targetClass:" + targetClass);
			if (targetClass != null && Service.class.isAssignableFrom(targetClass)) {
				env.remapStartServiceIntent(curIntent, targetClassName);
				if (conn == null) {
					context.startService(curIntent);
				} else {
					context.bindService(curIntent, conn, curIntent.getIntExtra(
							ProxyEnvironmentNew.BIND_SERVICE_FLAGS, Context.BIND_AUTO_CREATE));
				}

			} else if (targetClass != null && BroadcastReceiver.class.isAssignableFrom(targetClass)) { // 发一个内部用的动态广播
				Intent newIntent = new Intent(curIntent);
				newIntent.setComponent(null);
				newIntent.putExtra(ProxyEnvironmentNew.EXTRA_TARGET_PACKAGNAME, packageName);
				newIntent.setPackage(context.getPackageName());
				context.sendBroadcast(newIntent);
			} else {
//				mappingActivity(context, curIntent);
				ActivityJumpUtil.handleStartActivityIntent(env.mPluginPakName, intent, -1, null, context);
				env.dealLaunchMode(intent);
				context.startActivity(curIntent);
				haveLaunchActivity = true;
			}
		}
		PluginDebugLog.log(TAG, "haveLaunchActivity :" + haveLaunchActivity);
		return haveLaunchActivity;
	}
	
	public static void stopService(Intent intent) {
		if (intent == null || intent.getComponent() == null
				|| TextUtils.isEmpty(intent.getComponent().getPackageName())) {
			return;
		}
		final String packageName = intent.getComponent().getPackageName();
		ProxyEnvironmentNew env = sPluginsMap.get(packageName);
		if (env == null) {
			return;
		}

		if (env.appWrapper != null) {
			env.appWrapper.stopService(intent);
		}
	}

	private void setApplicationBase(ProxyEnvironmentNew env, Application application,
			String packageName) {
		PluginContextWrapper ctxWrapper = new PluginContextWrapper(((Application)mContext).getBaseContext(), env);
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

	/**
	 * 初始化插件的运行环境，如果已经初始化，则什么也不做
	 * 
	 * @param context
	 *            application context
	 * @param packageName
	 *            插件包名
	 * @param pluginInstallMethod 插件安装方式
	 */
	public static void initProxyEnvironment(Context context, String packageName,
			String pluginInstallMethod) {
		PluginDebugLog.log(TAG,
				"sPluginsMap.containsKey(packageName):" + sPluginsMap.containsKey(packageName));
		if (sPluginsMap.containsKey(packageName)) {
			return;
		}
		ProxyEnvironmentNew newEnv = null;
		try {
			newEnv = new ProxyEnvironmentNew(context,
					PluginInstaller.getInstalledApkFile(context, packageName), packageName,
					pluginInstallMethod);
		} catch (Exception e) {
			e.printStackTrace();
			deliverPlug(false, packageName, ErrorType.ERROR_CLIENT_ENVIRONMENT_NULL);
		}
		if (newEnv != null) {
			sPluginsMap.put(packageName, newEnv);
		}
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
		String targetActivity = intent.getStringExtra(ProxyEnvironmentNew.EXTRA_TARGET_ACTIVITY);
		if (TextUtils.isEmpty(targetActivity) || intent == null) {
			return;
		}

		// 不支持LAUNCH_SINGLE_INSTANCE
		ActivityInfo info = targetMapping.getActivityInfo(targetActivity);
		if (info == null || info.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
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
			String proxyClsName = ActivityJumpUtil.getProxyActivityClsName(getInstallType(), info);
			if (activity != null && TextUtils.equals(proxyClsName, activity.getClass().getName())) {
				String key = getActivityStackKey(activity, getInstallType());
				
				if (!TextUtils.isEmpty(key) && TextUtils.equals(targetActivity, key)) {
					intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				}
			}
		} else if (isSingleTask || isClearTop) {

			Activity found = null;
			Iterator<Activity> it = activityStack.iterator();
			while (it.hasNext()) {
				Activity activity = it.next();
				String proxyClsName = ActivityJumpUtil.getProxyActivityClsName(getInstallType(), info);
				if (activity != null
						&& TextUtils.equals(proxyClsName, activity.getClass().getName())) {
					String key = getActivityStackKey(activity, getInstallType());
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

	private static String getActivityStackKey(Activity activity, String pluginInstallType) {
		String key = "";
		if (TextUtils.equals(CMPackageManager.PLUGIN_METHOD_INSTR, pluginInstallType)
				|| TextUtils.isEmpty(pluginInstallType)) {
			InstrActivityProxy lActivityProxy = null;
			try {
				lActivityProxy = (InstrActivityProxy) activity;
			} catch (Exception e) {
				e.printStackTrace();
				return key;
			}
			PluginActivityControl ctl = lActivityProxy.getController();
			if (ctl != null && ctl.getPlugin() != null) {
				key = ctl.getPlugin().getClass().getName();
			}
		} else if (TextUtils.equals(CMPackageManager.PLUGIN_METHOD_DEXMAKER, pluginInstallType)) {
			key = activity.getClass().getSuperclass().getName();
		}
		return key;
	}

	public void remapStartServiceIntent(Intent intent, String targetService) {
		if (targetMapping.getServiceInfo(targetService) == null) {
			return;
		}
		intent.putExtra(ProxyEnvironmentNew.EXTRA_TARGET_SERVICE, targetService);
		intent.putExtra(ProxyEnvironmentNew.EXTRA_TARGET_PACKAGNAME, targetMapping.getPackageName());

		intent.setClass(mContext, ServiceProxyNew.class);
	}

	/**
	 * @param originIntent
	 */
	public void remapReceiverIntent(Intent originIntent) {
		if (originIntent.getComponent() != null) {
			String targetReceiver = originIntent.getComponent().getClassName();
			originIntent.putExtra(ProxyEnvironmentNew.EXTRA_TARGET_RECEIVER, targetReceiver);
			originIntent.putExtra(ProxyEnvironmentNew.EXTRA_TARGET_PACKAGNAME,
					targetMapping.getPackageName());
			originIntent.setClass(mContext, BroadcastReceiverProxy.class);
		}
	}

	/**
	 * 获取某个插件的 data 根目录。
	 * 
	 * @param packageName
	 * @return
	 */
	public File getDataDir(Context context, String packageName) {
		PluginDebugLog.log(TAG, "packageName:" + packageName + " context:" + context);
		File file = null;
		try {
			if (TextUtils.isEmpty(packageName)) {
				deliverPlug(false, context.getPackageName(),
						ErrorType.ERROR_CLIENT_LOAD_CREATE_FILE_NULL);// 添加投递
				return null;
			}
			file = new File(getTargetMapping().getDataDir());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return file;
	}

	/**
	 * 创建ClassLoader， 需要在 createDataRoot之后调用
	 */
	private void createClassLoader() {

		PluginDebugLog.log(TAG, "createClassLoader");
		dexClassLoader = new PluginClassLoader(mApkFile.getAbsolutePath(), getDataDir(mContext,
				mPluginPakName).getAbsolutePath(), mContext.getClassLoader(), this);

		// 把 插件 classloader 注入到 host程序中，方便host app 能够找到 插件 中的class。
		if (targetMapping.getMetaData() != null
				&& targetMapping.getMetaData().getBoolean(ProxyEnvironmentNew.META_KEY_CLASSINJECT)) {
			ClassLoaderInjectHelper.inject(mContext.getClassLoader(), dexClassLoader,
					targetMapping.getPackageName() + ".R");
			PluginDebugLog.log(TAG, "--- Class injecting @ " + targetMapping.getPackageName());
		}
	}

	/**
	 * 如果注入了classloader，执行反注入操作。用于卸载时。
	 */
	public void ejectClassLoader() {
		if (dexClassLoader != null && targetMapping.getMetaData() != null
				&& targetMapping.getMetaData().getBoolean(ProxyEnvironmentNew.META_KEY_CLASSINJECT)) {
			ClassLoaderInjectHelper.eject(mContext.getClassLoader(), dexClassLoader);
		}
	}

	@SuppressLint("NewApi")
	private void createTargetResource() {
		try {
			AssetManager am = AssetManager.class.newInstance();
			JavaCalls.callMethod(am, "addAssetPath", new Object[] { mApkFile.getAbsolutePath() });
			targetAssetManager = am;
		} catch (Exception e) {
			deliverPlug(false, mPluginPakName, ErrorType.ERROR_CLIENT_LOAD_INIT_RESOURCE_FAILE);
			e.printStackTrace();
		}

		// 解决一个HTC ONE X上横竖屏会黑屏的问题
		hostRes = mContext.getResources();
		Configuration config = new Configuration();
		config.setTo(hostRes.getConfiguration());
		config.orientation = Configuration.ORIENTATION_UNDEFINED;
		targetResources = new ResourcesProxy(targetAssetManager, hostRes.getDisplayMetrics(),
				config, hostRes);
		targetTheme = targetResources.newTheme();
		targetTheme.setTo(mContext.getTheme());
		
		// Create Resource Tool for host
		mResTool = new ResourcesToolForPlugin(mContext);
	}

	/**
	 * 初始化插件资源
	 * @throws Exception 
	 */
	private void createTargetMapping(String pkgName) throws Exception {
		CMPackageInfo pkgInfo = CMPackageManager.getInstance(mContext).getPackageInfo(pkgName);
		if (pkgInfo != null) {
			targetMapping = pkgInfo.targetInfo;
		} else {
			throw new Exception("Havn't install pkgName");
		}
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
		return mContext;
	}

	/**
	 * Get host resource
	 * 
	 * @return host resource
	 */
	public ResourcesToolForPlugin getHostResourceTool() {
		if (mResTool != null) {
			return mResTool;
		}
		return null;
	}

	private void changeInstrumentation(Context context, String pkgName) {
		try {
			Context contextImpl = ((ContextWrapper) context).getBaseContext();
			Object activityThread = ReflectionUtils.getFieldValue(contextImpl, "mMainThread");
			Field instrumentationF = activityThread.getClass().getDeclaredField("mInstrumentation");
			instrumentationF.setAccessible(true);
			if (TextUtils.equals(CMPackageManager.PLUGIN_METHOD_DEXMAKER, getInstallType())) {
//				FrameworkInstrumentation instrumentation = new FrameworkInstrumentation(context);
//				instrumentationF.set(activityThread, instrumentation);
				throw new Exception("Unsupported install method");
			} else if (TextUtils.equals(CMPackageManager.PLUGIN_METHOD_INSTR, getInstallType())) {
				mPluginInstrument = new LPluginInstrument(
						(Instrumentation) instrumentationF.get(activityThread), pkgName);
			} else {
				// Default option
				mPluginInstrument = new LPluginInstrument(
						(Instrumentation) instrumentationF.get(activityThread), pkgName);
			}
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
				String installMethod = CMPackageManager.getInstance(pContext).getPackageInfo(
						pakName).pluginInfo.mPluginInstallMethod;
				PluginDebugLog.log("plugin", "doInBackground:" + pakName + ", installMethod: "
						+ installMethod);
				ProxyEnvironmentNew.initProxyEnvironment(pContext, pakName, installMethod);
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

//	private static class FrameworkInstrumentation extends Instrumentation {
//		private Context mContext;
//
//		public FrameworkInstrumentation(Context context) {
//			super();
//			mContext = context;
//		}
//
//		@Override
//		public Activity newActivity(ClassLoader cl, String className, Intent intent)
//				throws InstantiationException, IllegalAccessException, ClassNotFoundException {
//
//			if (className.equals(ActivityJumpUtil.TARGET_CLASS_NAME)) {
//				String pluginId = intent.getStringExtra(ProxyEnvironmentNew.EXTRA_TARGET_PACKAGNAME);
//				String actClassName = intent.getStringExtra(ProxyEnvironmentNew.EXTRA_TARGET_ACTIVITY);
//
//				ProxyEnvironmentNew env = ProxyEnvironmentNew.getInstance(pluginId);
//				if (env == null && mContext != null) {
//					// Check whether pluginId has installed, then init the
//					// installed apk
//					CMPackageInfo info = CMPackageManager.getInstance(mContext).getPackageInfo(
//							pluginId);
//					if (null != info && null != info.pluginInfo) {
//						initProxyEnvironment(mContext, pluginId, info.pluginInfo.mPluginInstallMethod);
//						env = ProxyEnvironmentNew.getInstance(pluginId);
//					}
//				}
//				if (env != null && env.dexClassLoader != null) {
//					return (Activity) ProxyEnvironmentNew.getInstance(pluginId).dexClassLoader
//							.loadActivityClass(actClassName).newInstance();
//				}
//			}
//			return super.newActivity(cl, className, intent);
//		}
//	}

	private static IDeliverPlug iDeliverPlug;

	public static void setiDeliverPlug(IDeliverPlug iDeliverPlug) {
		ProxyEnvironmentNew.iDeliverPlug = iDeliverPlug;
	}

	public interface IDeliverPlug {
		void deliverPlug(boolean success, String pakName, int errorCode);
	}
}