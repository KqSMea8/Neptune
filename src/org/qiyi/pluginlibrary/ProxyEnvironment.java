package org.qiyi.pluginlibrary;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.qiyi.pluginlibrary.ErrorType.ErrorType;
import org.qiyi.pluginlibrary.adapter.ActivityProxyAdapter;
import org.qiyi.pluginlibrary.api.ILoadingViewCreator;
import org.qiyi.pluginlibrary.api.ITargetLoadListenner;
import org.qiyi.pluginlibrary.component.CMActivity;
import org.qiyi.pluginlibrary.component.CMApplication;
import org.qiyi.pluginlibrary.component.service.CMService;
import org.qiyi.pluginlibrary.exception.PluginStartupException;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.install.PluginInstaller;
import org.qiyi.pluginlibrary.plugin.ApkTargetMapping;
import org.qiyi.pluginlibrary.plugin.TargetMapping;
import org.qiyi.pluginlibrary.pm.CMPackageManager;
import org.qiyi.pluginlibrary.proxy.BroadcastReceiverProxy;
import org.qiyi.pluginlibrary.proxy.activity.ActivityProxy;
import org.qiyi.pluginlibrary.proxy.service.ServiceProxy;
import org.qiyi.pluginlibrary.utils.ClassLoaderInjectHelper;
import org.qiyi.pluginlibrary.utils.JavaCalls;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import dalvik.system.DexClassLoader;

/**
 * 插件运行的环境
 */
public class ProxyEnvironment {

    public static final String TAG = PluginDebugLog.TAG;

    public static final String EXTRA_TARGET_ACTIVITY = "pluginapp_extra_target_activity";
    public static final String EXTRA_TARGET_SERVICE = "pluginapp_extra_target_service";
    public static final String EXTRA_TARGET_RECEIVER= "pluginapp_extra_target_receiver";
    public static final String EXTRA_TARGET_PACKAGNAME = "pluginapp_extra_target_pacakgename";
    public static final String EXTRA_TARGET_ISBASE = "pluginapp_extra_target_isbase";
    public static final String EXTRA_TARGET_REDIRECT_ACTIVITY = "pluginapp_extra_target_redirect_activity";
    public static final String EXTRA_VALUE_LOADTARGET_STUB = "pluginapp_loadtarget_stub";
    
    public static final String BIND_SERVICE_FLAGS = "bind_service_flags";


    /** pluginapp开关:data是否和host的路径相同，默认独立路径 TODO 待实现 */
    public static final String META_KEY_DATAINHOST = "pluginapp_cfg_datainhost";
    /** pluginapp开关:data是否【去掉】包名前缀，默认加载包名前缀 */
    public static final String META_KEY_DATA_WITHOUT_PREFIX = "pluginapp_cfg_data_without_prefix";
    /** pluginapp开关：class是否注入到host，默认不注入 */
    public static final String META_KEY_CLASSINJECT = "pluginapp_class_inject";

    /** 插件加载成功的广播 */
    public static final String ACTION_TARGET_LOADED = "org.qiyi.pluginapp.action.TARGET_LOADED";

    /** 插件包名对应Environment的Hash */
    private static HashMap<String, ProxyEnvironment> sPluginsMap = new HashMap<String, ProxyEnvironment>();

    private final Context context;
    private final File apkFile;

    private ClassLoader dexClassLoader;
    private Resources targetResources;
    private AssetManager targetAssetManager;
    private Theme targetTheme;
    private TargetMapping targetMapping;
    /** data文件是否需要加前缀 */
    private boolean bIsDataNeedPrefix = true;
    private String parentPackagename;
    /** 插件的Activity栈 */
    private LinkedList<Activity> activityStack;
    /** 插件虚拟的Application实例 */
    private CMApplication application;
    /** 插件数据根目录 */
    private File targetDataRoot;
    /** 是否初始化了插件Application */
    private boolean bIsApplicationInit = false;
    
    /** Loading Map，正在loading中的插件 */
    private static Map<String, List<Intent>> gLoadingMap = new HashMap<String, List<Intent>>();
    /** 插件loading样式的创建器 */
    private static Map<String, ILoadingViewCreator> gLoadingViewCreators = new HashMap<String, ILoadingViewCreator>();
    
    private String pluginPakName;
    
    /**
     * 记录正在运行的service
     */
    public static Map<String,CMService> aliveServiceMap = new HashMap<String, CMService>();

    public  static Activity mParent;//保存宿主activity
    /**
     * 构造方法，解析apk文件，创建插件运行环境
     * 
     * @param context
     *            host application context
     * @param apkFile
     *            插件apk文件
     */
    private ProxyEnvironment(Context context, File apkFile,String pluginPakName) {
        this.context = context.getApplicationContext();
        this.apkFile = apkFile;
        activityStack = new LinkedList<Activity>();
        parentPackagename = context.getPackageName();
        this.pluginPakName = pluginPakName;
        assertApkFile();
        createTargetMapping();
        //准备数据路径
        createDataRoot();
        //加载classloader
        createClassLoader();
        //加载资源
        createTargetResource();
        addPermissions();

    }
    
    /**
     * data文件是否需要加前缀
     * 
     * @return true or false
     */
    public boolean isDataNeedPrefix() {
        return bIsDataNeedPrefix;
    }

    /**
     * 获取插件数据根路径
     * 
     * @return 根路径文件
     */
    public File getTargetDataRoot() {
        return targetDataRoot;
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
     * 获取插件lib的绝对路径
     * 
     * @return 绝对路径
     */
    public String getTargetLibPath() {
        return new File(targetDataRoot, PluginInstaller.NATIVE_LIB_PATH).getAbsolutePath();
    }

    /**
     * 获取插件运行环境实例，调用前保证已经初始化，否则会抛出异常
     * 
     * @param packageName
     *            插件包名
     * @return 插件环境对象
     */
    public static ProxyEnvironment getInstance(String packageName) {
        ProxyEnvironment env = null;
        if (packageName != null) {
            env = sPluginsMap.get(packageName);
        }
        if (env == null) {
        	deliverPlug(false, packageName, ErrorType.ERROR_CLIENT_ENVIRONMENT_NULL);
//          throw new IllegalArgumentException(packageName +" not loaded, Make sure you have call the init method!");
        	
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
    public static  boolean hasInstance(String packageName) {
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
        ProxyEnvironment env = sPluginsMap.get(packageName);
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
     * 设置插件加载的loadingView creator
     * 
     * @param packageName
     *            插件包名
     * @param creator
     *            loadingview creator
     */
    public static void putLoadingViewCreator(String packageName, ILoadingViewCreator creator) {
        if (packageName == null) {
            return;
        }
        gLoadingViewCreators.put(packageName, creator);
    }

    /**
     * 获取插件加载的loadingview creator
     * 
     * @param packageName
     *            插件包名
     * @return creator
     */
    public static ILoadingViewCreator getLoadingViewCreator(String packageName) {
        if (packageName == null) {
            return null;
        }
        return gLoadingViewCreators.get(packageName);
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
    public static void enterProxy(final Context context, final ServiceConnection conn ,final Intent intent) {
    	if(context instanceof Activity){
    		mParent = (Activity)context;
    	}
        final String packageName = intent.getComponent().getPackageName();
        if (TextUtils.isEmpty(packageName)) {
        	deliverPlug(false, context.getPackageName(), ErrorType.ERROR_CLIENT_LOAD_NO_PAKNAME);//添加投递
//            throw new RuntimeException("*** loadTarget with null packagename!");
        	return;
        }
        
        boolean isEnterProxy = false;
        synchronized (gLoadingMap) {
            List<Intent> cacheIntents = gLoadingMap.get(packageName);
            if (cacheIntents != null) {//说明插件正在loading
                // 正在loading，直接返回吧，等着loading完调起
                // 把intent都缓存起来
//                cacheIntents.add(intent);
            	PluginDebugLog.log("plugin", "cacheIntents:"+cacheIntents.size());
                return;
            }

            isEnterProxy = isEnterProxy(packageName);//判断是否已经进入到代理
            if (!isEnterProxy) {
                List<Intent> intents = new ArrayList<Intent>();
                intents.add(intent);
                gLoadingMap.put(packageName, intents);//正在加载的插件队列
            }
        }
        
        if (isEnterProxy) {
            // 已经初始化，直接起Intent
        	try{
        		launchIntent(context,conn, intent);
        	}catch(Exception e){
        		PluginDebugLog.log("plugin", "launchIntent:异常");
        		clearLoadingIntent(packageName);
        	}
            return;
        }

        CMPackageManager.getInstance(context.getApplicationContext()).packageAction(packageName,
                new IInstallCallBack() {

                    @Override
                    public void onPackageInstallFail(String packageName, int failReason) {
                        clearLoadingIntent(packageName);
                    }

                    @Override
                    public void onPacakgeInstalled(String packageName) {
                    	PluginDebugLog.log(TAG, "onPacakgeInstalled 安装完成_开始初始化initTarget");

                        initTarget(context.getApplicationContext(), packageName, new ITargetLoadListenner() {

                            @Override
                            public void onLoadFinished(String packageName) {
                            	try{
                            		launchIntent(context,conn, intent);
                            	}catch(Exception e){
                            		PluginDebugLog.log("plugin", "initTarget_launchIntent:异常");
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
     * 用于插件qos 投递
     */
    public static void deliverPlug(boolean success,String pakName,int errorCode){
    	try {
    		JavaCalls.callStaticMethod("org.qiyi.android.plugin.utils.PluginDeliverUtils","deliverStartUp",success,pakName,errorCode);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			PluginDebugLog.log("plugin", "deliverPlug:类未加载");
			e.printStackTrace();
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
    public static boolean launchIntent(Context context, ServiceConnection conn,Intent intent) {
    	PluginDebugLog.log("plugin", "launchIntent");
        String packageName = intent.getComponent().getPackageName();
        ProxyEnvironment env = sPluginsMap.get(packageName);
        if (env == null) {
        	deliverPlug(false, packageName, ErrorType.ERROR_CLIENT_NOT_LOAD);//增加投递
            throw new IllegalArgumentException(packageName +" not loaded, Make sure you have call the init method!");
//            return false;
        }

        List<Intent> cacheIntents = null;
        if (!env.bIsApplicationInit && env.application == null) {

            // Application 创建
            String className = env.targetMapping.getApplicationClassName();
            if (className == null || "".equals(className) || Application.class.getName().equals(className)) {
                // 创建默认的虚拟Application
                env.application = new CMApplication();
            } else {
                try {
                    env.application = ((CMApplication) env.dexClassLoader.loadClass(className).asSubclass(CMApplication.class).newInstance());
                } catch (Exception e) {
                	deliverPlug(false, packageName, ErrorType.ERROR_CLIENT_INIT_PLUG_APP);//添加投递
//                	return false;
                    throw new RuntimeException(e.getMessage(), e);
                }
            }

            env.application.setApplicationProxy((Application) env.context);
            env.application.setTargetPackageName(packageName);
            env.application.onCreate();
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

        PluginDebugLog.log(TAG, "launchIntent_cacheIntents:"+cacheIntents.size()+";cacheIntents:"+cacheIntents);
        boolean haveLaunchActivity = false;
        for (Intent curIntent : cacheIntents) {

            // 获取目标class
            String targetClassName = curIntent.getComponent().getClassName();
            PluginDebugLog.log(TAG, "launchIntent_targetClassName:"+targetClassName);
            if (TextUtils.equals(targetClassName, EXTRA_VALUE_LOADTARGET_STUB)) {

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
            	deliverPlug(false, packageName, ErrorType.ERROR_CLIENT_LOAD_START);//添加投递
                targetClass = CMActivity.class;
            }
            PluginDebugLog.log(TAG, "launchIntent_targetClass:"+targetClass);
//            deliverPlug(true, packageName, ErrorType.ERROR_CLIENT_LOAD_START);//添加投递
            if (CMService.class.isAssignableFrom(targetClass)) {
                env.remapStartServiceIntent(curIntent, targetClassName);
                if(conn == null){
                	context.startService(curIntent);
                }else{
                	context.bindService(curIntent, conn, curIntent.getIntExtra(ProxyEnvironment.BIND_SERVICE_FLAGS, Context.BIND_AUTO_CREATE));
                }

            } else if (BroadcastReceiver.class.isAssignableFrom(targetClass)) { // 发一个内部用的动态广播
                Intent newIntent = new Intent(curIntent);
                newIntent.setComponent(null);
                newIntent.putExtra(EXTRA_TARGET_PACKAGNAME, packageName);
                newIntent.setPackage(context.getPackageName());
                context.sendBroadcast(newIntent);
            } else {
                Intent newIntent = new Intent(curIntent);
                newIntent.setClass(context, ActivityProxy.class);
                if (!(context instanceof Activity)) {
                    newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                env.remapStartActivityIntent(newIntent, targetClassName);
                int requestCode = curIntent.getIntExtra("requestCode", -1) ;
                if(requestCode != -1){
                	if(mParent != null){
                		PluginDebugLog.log(TAG, "requestCode :"+requestCode+";targetClassName:"+targetClassName+";mParent:"+mParent.getClass().getSimpleName());
                		mParent.startActivityForResult(newIntent, requestCode);
                	}
                }else{
                	context.startActivity(newIntent);
                }
                haveLaunchActivity = true;
            }
        }
        PluginDebugLog.log(TAG, "haveLaunchActivity :"+haveLaunchActivity);
        return haveLaunchActivity;
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
    	PluginDebugLog.log(TAG, "sPluginsMap.containsKey(packageName):"+sPluginsMap.containsKey(packageName));
        if (sPluginsMap.containsKey(packageName)) {
            return;
        }
        ProxyEnvironment newEnv = new ProxyEnvironment(context, PluginInstaller.getInstalledApkFile(context, packageName), packageName);
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
            ProxyEnvironment env = sPluginsMap.remove(packageName);
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
     * 当前是否运行在插件代理模式
     * 
     * @return true or false
     */
    public static boolean isProxyMode() {
        return sPluginsMap.size() > 0;
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

    public void remapStartActivityIntent(Intent originIntent) {
        // 启动系统的Activity，例如卸载、安装，getComponent 为null。这样的Intent，不需要remap
        if (originIntent.getComponent() != null) {
            String targetActivity = originIntent.getComponent().getClassName();
            remapStartActivityIntent(originIntent, targetActivity);
        }
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
        String targetActivity = intent.getStringExtra(EXTRA_TARGET_ACTIVITY);
        if (targetActivity == null) {
            return;
        }

        // 其他模式不支持，只支持single top和 single task
        ActivityInfo info = targetMapping.getActivityInfo(targetActivity);
        if (info.launchMode != ActivityInfo.LAUNCH_SINGLE_TOP && info.launchMode != ActivityInfo.LAUNCH_SINGLE_TASK) {
            return;
        }
        if (info.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP) {

            // 判断栈顶是否为需要启动的Activity
            Activity activity = null;
            if (!activityStack.isEmpty()) {
                activity = activityStack.getFirst();
            }
            if (activity instanceof ActivityProxyAdapter) {
                ActivityProxyAdapter adp = (ActivityProxyAdapter) activity;
                Object ma = adp.getTarget();
                if (ma != null && TextUtils.equals(targetActivity, ma.getClass().getName())) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                }
            }
        } else if (info.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK) {

            Activity found = null;
            Iterator<Activity> it = activityStack.iterator();
            while (it.hasNext()) {
                Activity activity = it.next();
                if (activity instanceof ActivityProxyAdapter) {
                    ActivityProxyAdapter adp = (ActivityProxyAdapter) activity;
                    Object ma = adp.getTarget();
                    if (ma != null && TextUtils.equals(targetActivity, ma.getClass().getName())) {
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
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        break;
                    }

                    activity.finish();
                }
            }
        }

    }

    public void remapStartActivityIntent(Intent intent, String targetActivity) {

        // 获取不到activity info，说明不是插件的Activity，不需要重映射
        if (targetMapping.getActivityInfo(targetActivity) == null) {
            return;
        }
        if(targetMapping.getActivityInfo(targetActivity).metaData != null 
        		&& targetMapping.getActivityInfo(targetActivity).metaData.getBoolean("pluginapp_is_proxy")){
        	return;
        }

        intent.putExtra(EXTRA_TARGET_ACTIVITY, targetActivity);
        intent.putExtra(EXTRA_TARGET_PACKAGNAME, targetMapping.getPackageName());
        Class<?> clazz = getRemapedActivityClass(targetActivity);
        if (clazz != null) {
            intent.setClass(context, clazz);
        }

        // 实现launch mode，目前支持singletop和
        dealLaunchMode(intent);
    }

    /**
     * 获取重映射之后的Activity类
     * 
     * @param targetActivity
     *            插件Activity类
     * @return 返回代理Activity类
     */
    public Class<?> getRemapedActivityClass(String targetActivity) {
        Class<?> targetClass;
        try {
            targetClass = dexClassLoader.loadClass(targetActivity);
        } catch (Exception e) {
            targetClass = CMActivity.class;
        }

        int theme = targetMapping.getActivityInfo(targetActivity).theme;
        Class<?> clazz = ProxyActivityCounter.getInstance().getNextAvailableActivityClass(targetClass, theme);

        return clazz;
    }

    public void remapStartServiceIntent(Intent intent, String targetService) {
        if (targetMapping.getServiceInfo(targetService) == null) {
            return;
        }
        intent.putExtra(EXTRA_TARGET_SERVICE, targetService);
        intent.putExtra(EXTRA_TARGET_PACKAGNAME, targetMapping.getPackageName());
        
        Class<?> clazz = getRemapedServiceClass(targetService);
        
        if (clazz != null) {
            intent.setClass(context, clazz);
        }
    }
    
    /**
     * @param targetService
     * @return
     */
    public Class<?> getRemapedServiceClass(String targetService) {
        Class<?> targetClass;
        try {
            targetClass = dexClassLoader.loadClass(targetService);
        } catch (Exception e) {
            targetClass = CMService.class;
        }

        Class<?> clazz;
		try {
			clazz = ProxyServiceCounter.getInstance().getAvailableService(targetClass);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			clazz = ServiceProxy.class;
		}
        return clazz;
    }


    /**
     * @param originIntent
     */
    public void remapReceiverIntent(Intent originIntent) {
        if (originIntent.getComponent() != null) {
            String targetReceiver = originIntent.getComponent().getClassName();
            originIntent.putExtra(EXTRA_TARGET_RECEIVER, targetReceiver);
            originIntent.putExtra(EXTRA_TARGET_PACKAGNAME, targetMapping.getPackageName());
            originIntent.setClass(context, BroadcastReceiverProxy.class);
        }
    }

    
    /**
     * 判断是否是apk文件
     */
    private void assertApkFile() {
        boolean isApk = apkFile.isFile() && apkFile.getName().endsWith(PluginInstaller.APK_SUFFIX);
        if (!isApk) {
//        	deliverPlug(false, context.getPackageName(), ErrorType.ERROR_CLIENT_LOAD_NO_APK);//添加投递
            throw new PluginStartupException(ErrorType.ERROR_CLIENT_LOAD_NO_APK,"Target file is not an apk.");
        }
    }

    /**
     * 创建数据根路径
     */
    private void createDataRoot() {

    	try{
    		 // TODO 这里需要考虑插件升级后MetaData的配置改变，data路径随之改变，是否需要保存数据
            if (targetMapping.getMetaData() != null && targetMapping.getMetaData().getBoolean(META_KEY_DATAINHOST)) {
                targetDataRoot = new File(context.getFilesDir().getParent());
            } else {
                targetDataRoot = getDataDir(context, targetMapping.getPackageName());
            }
            targetDataRoot.mkdirs();
    	}catch(Exception e){
//    		deliverPlug(false, context.getPackageName(), ErrorType.ERROR_CLIENT_LOAD_CREATE_ROOT_DIR);//添加投递
    		throw new PluginStartupException(ErrorType.ERROR_CLIENT_LOAD_CREATE_ROOT_DIR,"createDataRoot error");
    	}
       
    }
    /**
     * 获取某个插件的 data 根目录。
     * @param packageName
     * @return
     */
    public static File getDataDir(Context context, String packageName) {
    	PluginDebugLog.log("plugin", "packageName:"+packageName+" context:"+context);
    	File file = null;
    	try{
    		if(packageName == null){
    			deliverPlug(false, context.getPackageName(), ErrorType.ERROR_CLIENT_LOAD_CREATE_FILE_NULL);//添加投递
    			return null;
    		}
    		file = new File(PluginInstaller.getPluginappRootPath(context), packageName);
    	}catch(Exception e){
//    		deliverPlug(false, context.getPackageName(), ErrorType.ERROR_CLIENT_LOAD_CREATE_FILE_NULL);//添加投递
    	}
       return file;
    }

    /**
     * 创建ClassLoader， 需要在 createDataRoot之后调用
     */
    private void createClassLoader() {

    	PluginDebugLog.log("plugin", "createClassLoader:"+context.getClassLoader());
        dexClassLoader = new DexClassLoader(apkFile.getAbsolutePath(), targetDataRoot.getAbsolutePath(),getTargetLibPath(),
        		super.getClass().getClassLoader());

        // 把 插件 classloader 注入到 host程序中，方便host app 能够找到 插件 中的class。
        if (targetMapping.getMetaData() != null && targetMapping.getMetaData().getBoolean(META_KEY_CLASSINJECT)) {
            ClassLoaderInjectHelper.inject(context.getClassLoader(), dexClassLoader, targetMapping.getPackageName()+ ".R");
            PluginDebugLog.log(TAG, "--- Class injecting @ " + targetMapping.getPackageName());
        }
    }
    
    /**
     * 如果注入了classloader，执行反注入操作。用于卸载时。
     */
    public void ejectClassLoader() {
        if (dexClassLoader != null
                && targetMapping.getMetaData() != null 
                && targetMapping.getMetaData().getBoolean(META_KEY_CLASSINJECT)) {
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
        	deliverPlug(false, pluginPakName,ErrorType.ERROR_CLIENT_LOAD_INIT_RESOURCE_FAILE );
            e.printStackTrace();
        }
        
        // 解决一个HTC ONE X上横竖屏会黑屏的问题
        Resources hostRes = context.getResources();
        Configuration config = new Configuration();
        config.setTo(hostRes.getConfiguration());
        config.orientation = Configuration.ORIENTATION_UNDEFINED;
        targetResources = new ResourcesProxy(targetAssetManager, hostRes.getDisplayMetrics(), config, hostRes);
        targetTheme = targetResources.newTheme();
        targetTheme.setTo(context.getTheme());
    }
    
    /**
     * 初始化插件资源
     */
    private void createTargetMapping() {
        targetMapping = new ApkTargetMapping(context, apkFile);

        bIsDataNeedPrefix = targetMapping.getMetaData() == null || !targetMapping.getMetaData().getBoolean(META_KEY_DATA_WITHOUT_PREFIX);
    }

    private void addPermissions() {
        // TODO add permissions
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
    public CMApplication getApplication() {
        return application;
    }

    public int getHostResourcesId(String resourcesName,String resourceType) {
        if (context != null) {
            return context.getResources().getIdentifier(resourcesName, resourceType, context.getPackageName());
        }
        return 0;
    }
   
    /**
     * 退出某个应用。不是卸载插件应用
     * @param packageName
     */
    public  void quitApp() {
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
    public static void initTarget(final Context context, String packageName, final ITargetLoadListenner listenner) {
    	PluginDebugLog.log("plugin", "initTarget");
    	try{
    		new InitProxyEnvireonment(context,packageName,listenner).start();
    	}catch(Exception e){
    		PluginDebugLog.log("plugin", "initTarget异常");
    		clearLoadingIntent(packageName);
    		deliverPlug(false,packageName , ErrorType.ERROR_CLIENT_LOAD_INIT_TARGET);
    		e.printStackTrace();
    	}
    	
    }
    
   static class InitProxyEnvireonment extends Thread{
    	public String pakName;
    	public Context pContext;
    	ITargetLoadListenner listenner;
    	public InitProxyEnvireonment(Context context,String pakName,ITargetLoadListenner listenner){
    		this.pakName = pakName;
    		this.pContext = context;
    		this.listenner = listenner;
    	}
    	@Override
    	public void run() {
    		super.run();
    		try{
    			PluginDebugLog.log("plugin", "doInBackground:"+pakName);
    	        ProxyEnvironment.initProxyEnvironment(pContext, pakName);
    	        new InitHandler(listenner, pakName,Looper.getMainLooper()).sendEmptyMessage(1);
    		}catch(Exception e){
    			clearLoadingIntent(pakName);
    			if(e instanceof PluginStartupException){
    				PluginStartupException ex = (PluginStartupException)e;
    				deliverPlug(false, pakName, ex.getCode());//添加投递
    			}else{
    				deliverPlug(false, pakName, ErrorType.ERROR_CLIENT_LOAD_INIT_ENVIRONMENT_FAIL);//添加投递
    			}
    		}
    	}
    }
    
	static class InitHandler extends Handler {
		ITargetLoadListenner listenner;
		String pakName;

		public InitHandler(ITargetLoadListenner listenner, String pakName,
				Looper looper) {
			super(looper);
			this.listenner = listenner;
			this.pakName = pakName;
		}

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
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

}
