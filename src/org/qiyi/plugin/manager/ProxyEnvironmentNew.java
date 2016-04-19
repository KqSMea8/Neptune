package org.qiyi.plugin.manager;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.qiyi.pluginlibrary.pm.CMPackageManagerImpl;
import org.qiyi.pluginlibrary.pm.PluginPackageInfoExt;
import org.qiyi.pluginlibrary.proxy.BroadcastReceiverProxy;
import org.qiyi.pluginlibrary.utils.ClassLoaderInjectHelper;
import org.qiyi.pluginlibrary.utils.ClassLoaderInjectHelper.InjectResult;
import org.qiyi.pluginlibrary.utils.JavaCalls;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.ReflectionUtils;
import org.qiyi.pluginlibrary.utils.ResourcesToolForPlugin;
import org.qiyi.pluginnew.ActivityJumpUtil;
import org.qiyi.pluginnew.ProxyComponentMappingByProcess;
import org.qiyi.pluginnew.classloader.PluginClassLoader;
import org.qiyi.pluginnew.context.PluginContextWrapper;
import org.qiyi.pluginnew.service.PluginServiceWrapper;

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
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
//import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;

/**
 * 插件运行的环境
 */
public class ProxyEnvironmentNew {

    public static final String TAG = ProxyEnvironmentNew.class.getSimpleName();

    public static final String EXTRA_TARGET_ACTIVITY = "pluginapp_extra_target_activity";
    public static final String EXTRA_TARGET_SERVICE = "pluginapp_extra_target_service";
    public static final String EXTRA_TARGET_RECEIVER = "pluginapp_extra_target_receiver";
    public static final String EXTRA_TARGET_PACKAGNAME = "pluginapp_extra_target_pacakgename";
    private static final String EXTRA_TARGET_CATEGORY = "pluginapp_service_category";
    public static final String EXTRA_TARGET_ISBASE = "pluginapp_extra_target_isbase";
    public static final String EXTRA_TARGET_REDIRECT_ACTIVITY = "pluginapp_extra_target_redirect_activity";
    public static final String EXTRA_VALUE_LOADTARGET_STUB = "pluginapp_loadtarget_stub";

    public static final String BIND_SERVICE_FLAGS = "bind_service_flags";

    public static final String META_KEY_CLASSINJECT = "pluginapp_class_inject";

    /**
     * 插件加载成功的广播
     */
    public static final String ACTION_TARGET_LOADED = "org.qiyi.pluginapp.action.TARGET_LOADED";

    public static final String ACTION_QUIT = "org.qiyi.pluginapp.action.QUIT";

    /**
     * pluginapp开关:data是否【去掉】包名前缀，默认加载包名前缀
     */
    public static final String META_KEY_DATA_WITH_PREFIX = "pluginapp_cfg_data_with_prefix";
    /**
     * 插件包名对应Environment的Hash
     */
    private static HashMap<String, ProxyEnvironmentNew> sPluginsMap = new HashMap<String, ProxyEnvironmentNew>();

    /**
     * 运行在当前进程内所有插件依赖库
     **/
    private static List<PluginPackageInfoExt> sPluginDependences = new ArrayList<PluginPackageInfoExt>();
    /** 插件调试日志 **/
//    private static ActivityLifecycleCallbacks sActivityLifecycleCallback = new ActivityLifecycleCallbacks() {
//
//        @Override
//        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
//            PluginDebugLog.log(TAG, "onActivityCreated: " + activity);
//        }
//
//        @Override
//        public void onActivityStarted(Activity activity) {
//            PluginDebugLog.log(TAG, "onActivityStarted: " + activity);
//        }
//
//        @Override
//        public void onActivityResumed(Activity activity) {
//            PluginDebugLog.log(TAG, "onActivityResumed: " + activity);
//        }
//
//        @Override
//        public void onActivityPaused(Activity activity) {
//            PluginDebugLog.log(TAG, "onActivityPaused: " + activity);
//        }
//
//        @Override
//        public void onActivityStopped(Activity activity) {
//            PluginDebugLog.log(TAG, "onActivityStopped: " + activity);
//        }
//
//        @Override
//        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
//            PluginDebugLog.log(TAG, "onActivitySaveInstanceState: " + activity);
//        }
//
//        @Override
//        public void onActivityDestroyed(Activity activity) {
//            PluginDebugLog.log(TAG, "onActivityDestroyed: " + activity);
//        }
//    };

    /**
     * 记录正在运行的service
     */
    public static ConcurrentMap<String, PluginServiceWrapper> sAliveServices = new ConcurrentHashMap<String, PluginServiceWrapper>(
            1);

    public static ConcurrentMap<String, ServiceConnection> sAliveServiceConnection = new ConcurrentHashMap<String, ServiceConnection>();

    // 等在加载的intent请求
    private static ConcurrentMap<String, LinkedBlockingQueue<Intent>> sIntentCacheMap = new ConcurrentHashMap<String, LinkedBlockingQueue<Intent>>();
    // 正在加载中的intent请求
    private static ConcurrentMap<String, List<Intent>> sIntentLoadingMap = new ConcurrentHashMap<String, List<Intent>>();

    // 插件APP退出时要处理的事情
    private static IAppExitStuff sExitStuff;
    private static Resources sHostRes;
    private static IPluginEnvironmentStatusListener sPluginEnvSatusListener;

    static Handler sHandler = new Handler(Looper.getMainLooper());

    private final Context mContext;
    private final File mApkFile;
    private final String mInstallType;
    private final String mProcessName;

    private PluginClassLoader dexClassLoader;
    private Resources targetResources;
    private ResourcesToolForPlugin mResTool;
    private AssetManager targetAssetManager;
    private Theme targetTheme;
    private TargetMapping targetMapping;
    private String parentPackagename;
    /**
     * 插件的Activity栈
     */
    private LinkedList<Activity> mActivityStack;
    /**
     * 插件虚拟的Application实例
     */
    private Application mApplication;

    /**
     * 是否初始化了插件Application
     */
    private boolean mIsApplicationInit = false;

    private String mPluginPakName;

    public PluginContextWrapper mAppWrapper;

    public LPluginInstrument mPluginInstrument;

    // 是否正在启动Activity
    private volatile boolean mIsLaunchActivity = false;

    /**
     * 构造方法，解析apk文件，创建插件运行环境
     *
     * @param context host application context
     * @param apkFile 插件apk文件
     * @throws Exception
     */
    private ProxyEnvironmentNew(Context context, File apkFile, String pluginPakName,
            String installType, String processName) throws Exception {
        if (context == null || apkFile == null || TextUtils.isEmpty(pluginPakName)
                || TextUtils.isEmpty(installType)) {
            throw new Exception("ProxyEnvironmentNew init failed for parameters has null: context: "
                    + context + " apkFile: " + apkFile + " pluginPakName: " + pluginPakName
                    + " installType: " + installType);
        }
        mContext = context.getApplicationContext();
        mApkFile = apkFile;
        mActivityStack = new LinkedList<Activity>();
        parentPackagename = context.getPackageName();
        mPluginPakName = pluginPakName;
        mInstallType = installType;
        mProcessName = processName;
        createTargetMapping(pluginPakName);
        // 加载classloader
        boolean clsLoader = createClassLoader();
        if (!clsLoader) {
            deliverPlug(false, pluginPakName, ErrorType.ERROR_CLIENT_CREATE_CLSlOADER);
            throw new Exception("ProxyEnvironmentNew init failed for createClassLoader failed: "
                    + context + " apkFile: " + apkFile + " pluginPakName: " + pluginPakName
                    + " installType: " + installType);
        }
        // 加载资源
        createTargetResource();
    }

    public static String getTopActivity() {
        String topPackage = null;
        Iterator<Entry<String, ProxyEnvironmentNew>> iter = sPluginsMap.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, ProxyEnvironmentNew> entry = iter.next();
            String packageName = (String) entry.getKey();
            ProxyEnvironmentNew environmentNew = (ProxyEnvironmentNew) entry.getValue();
            if (environmentNew.mActivityStack.size() == 0) {
                continue;
            } else if (isResumed(environmentNew.mActivityStack.getFirst())) {
                topPackage = packageName;
            }
        }
        return topPackage;
    }

    public static void updateConfiguration(Configuration config) {
        Iterator<?> iter = sPluginsMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iter.next();
            ProxyEnvironmentNew env = (ProxyEnvironmentNew) entry.getValue();
            env.getTargetResources().updateConfiguration(config,
                    sHostRes != null ? sHostRes.getDisplayMetrics() : null);
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
     * For support multiple process.
     *
     * @return current running process name
     */
    public String getRunningProcessName() {
        if (TextUtils.isEmpty(mProcessName)) {
            return ProxyComponentMappingByProcess.getDefaultPlugProcessName();
        } else {
            return mProcessName;
        }
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
     * @param packageName 插件包名
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
     * @param packageName 包名，已经做非空判断
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
     * @param packageName 插件包名
     * @return true or false
     */
    public static synchronized boolean isEnterProxy(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        ProxyEnvironmentNew env = sPluginsMap.get(packageName);
        if (env != null && env.mIsApplicationInit) {
            return true;
        }

        return false;
    }

    /**
     * 清除等待队列，防止异常情况，导致所有Intent都阻塞在等待队列，插件再也起不来就杯具了
     *
     * @param packageName 包名
     */
    public static void clearLoadingIntent(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        sIntentCacheMap.remove(packageName);
    }

    /**
     * 插件是否正在loading中
     *
     * @param packageName 插件包名
     * @return true or false
     */
    public static boolean isLoading(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }

        return sIntentCacheMap.containsKey(packageName);
    }

    public static void enterProxy(final Context context, final ServiceConnection conn,
            final Intent intent, final String processName) {
        PluginDebugLog.log(TAG, "enterProxy: " + intent);
        final String packageName = tryParsePkgName(context, intent);
        if (TextUtils.isEmpty(packageName)) {
            deliverPlug(false, context.getPackageName(), ErrorType.ERROR_CLIENT_LOAD_NO_PAKNAME);
            PluginDebugLog.log(TAG,
                    "enterProxy packageName is null return! packageName: " + packageName);
            return;
        }

        LinkedBlockingQueue<Intent> cacheIntents = sIntentCacheMap.get(packageName);
        if (cacheIntents != null && cacheIntents.size() > 0) {
            // 说明插件正在loading,正在loading，直接返回吧，等着loading完调起
            // 把intent都缓存起来
            cacheIntents.add(intent);
            PluginDebugLog.log(TAG,
                    "LoadingMap is not empty, Cache current intent, intent: " + intent);
            return;
        }

        // 判断是否已经进入到代理
        boolean isEnterProxy = isEnterProxy(packageName);
        if (!isEnterProxy) {
            if (null == cacheIntents) {
                cacheIntents = new LinkedBlockingQueue<Intent>();
                sIntentCacheMap.put(packageName, cacheIntents);
            }
            // 正在加载的插件队列
            cacheIntents.add(intent);
            PluginDebugLog.log(TAG,
                    "Environment is loading cache current intent, intent: " + intent);
        } else {
            // 已经初始化，直接起Intent
            launchIntent(context, conn, intent);
            return;
        }

        // Handle plugin dependences
        CMPackageInfo info = CMPackageManagerImpl.getInstance(context.getApplicationContext())
                .getPackageInfo(packageName);
        if (info != null && info.pluginInfo != null && info.pluginInfo.getPluginResfs() != null
                && info.pluginInfo.getPluginResfs().size() > 0) {
            PluginDebugLog.log(TAG, "Start to check dependence installation size: "
                    + info.pluginInfo.getPluginResfs().size());
            final AtomicInteger count = new AtomicInteger(info.pluginInfo.getPluginResfs().size());
            for (String pkgName : info.pluginInfo.getPluginResfs()) {
                PluginDebugLog.log(TAG, "Start to check installation pkgName: " + pkgName);
                CMPackageManagerImpl.getInstance(context.getApplicationContext())
                .packageAction(pkgName, new IInstallCallBack.Stub() {
                    @Override
                    public void onPacakgeInstalled(String pName) {
                        count.getAndDecrement();
                        PluginDebugLog.log(TAG,
                                "Check installation success pkgName: " + pName);
                        if (count.get() == 0) {
                            PluginDebugLog.log(TAG,
                                    "Start Check installation after check dependence packageName: "
                                            + packageName);
                            checkPkgInstallationAndLaunch(context, packageName, processName,
                                    conn, intent);
                        }
                    }

                    @Override
                    public void onPackageInstallFail(String pName, int failReason)
                            throws RemoteException {
                        PluginDebugLog.log(TAG, "Check installation failed pkgName: "
                                + pName + " failReason: " + failReason);
                        count.set(-1);
                    }
                });
            }
        } else {
            PluginDebugLog.log(TAG,
                    "Start Check installation without dependences packageName: " + packageName);
            checkPkgInstallationAndLaunch(context, packageName, processName, conn, intent);
        }
    }

    private static void checkPkgInstallationAndLaunch(final Context context,
            final String packageName, final String processName, final ServiceConnection conn,
            final Intent intent) {
        CMPackageManagerImpl.getInstance(context.getApplicationContext()).packageAction(packageName,
                new IInstallCallBack.Stub() {

                    @Override
                    public void onPackageInstallFail(String packageName, int failReason) {
                        PluginDebugLog.log(TAG, "checkPkgInstallationAndLaunch failed packageName: "
                                + packageName + " failReason: " + failReason);
                        clearLoadingIntent(packageName);
                        ProxyEnvironmentNew.deliverPlug(false, packageName, failReason);
                    }

                    @Override
                    public void onPacakgeInstalled(String packageName) {
                        initTarget(context.getApplicationContext(), packageName, processName,
                                new ITargetLoadListenner() {
                                    @Override
                                    public void onLoadFinished(String packageName) {
                                        try {
                                            launchIntent(context, conn, intent, false);
                                            if (sPluginEnvSatusListener != null) {
                                                sPluginEnvSatusListener
                                                        .onPluginEnvironmentIsReady(packageName);
                                            }
                                        } catch (Exception e) {
                                            clearLoadingIntent(packageName);
                                            e.printStackTrace();
                                        }
                                    }
                                });
                    }
                });
    }

    /**
     * 运行插件代理
     *
     * @param context host 的application context
     * @param intent 加载插件运行的intent
     */
    public static void enterProxy(final Context context, final ServiceConnection conn,
            final Intent intent) {
        enterProxy(context, conn, intent,
                ProxyComponentMappingByProcess.getDefaultPlugProcessName());
    }

    /**
     * @param success
     * @param pakName
     * @param errorCode 用于插件qos 投递
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
            for (CMPackageInfo info : CMPackageManagerImpl.getInstance(context)
                    .getInstalledApps()) {
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
     * @param context host application context
     * @param conn bind Service时的connection
     * @param intent 启动组件的intent
     */
    public static boolean launchIntent(Context context, ServiceConnection conn, Intent intent) {
        return launchIntent(context, conn, intent, true);
    }

    /**
     * 进入代理模式
     *
     * @param context host application context
     * @param conn bind Service时的connection
     * @param intent 启动组件的intent
     * @param needAddCache 是否需要添加到未启动intent的记录中
     * @return 返回值无意义
     */
    public static boolean launchIntent(Context context, ServiceConnection conn, Intent intent, boolean needAddCache) {
        PluginDebugLog.log(TAG, "launchIntent: " + intent);
        String packageName = tryParsePkgName(context, intent);
        ProxyEnvironmentNew env = sPluginsMap.get(packageName);
        if (env == null) {
            deliverPlug(false, packageName, ErrorType.ERROR_CLIENT_NOT_LOAD);
            PluginDebugLog.log(TAG, "launchIntent env is null! Just return!");
            sIntentCacheMap.remove(packageName);
            return false;
            // throw new IllegalArgumentException(packageName
            // +" not loaded, Make sure you have call the init method!");
        }

        LinkedBlockingQueue<Intent> cacheIntents = null;
        if (!env.mIsApplicationInit && env.mApplication == null) {
            String className = env.targetMapping.getApplicationClassName();
            if (TextUtils.isEmpty(className) || Application.class.getName().equals(className)) {
                // 创建默认的虚拟Application
                env.mApplication = new Application();
            } else {
                try {
                    env.mApplication = ((Application) env.dexClassLoader.loadClass(className)
                            .asSubclass(Application.class).newInstance());
                } catch (Exception e) {
                    deliverPlug(false, packageName, ErrorType.ERROR_CLIENT_INIT_PLUG_APP);
                    e.printStackTrace();
                    return false;
                }
            }

            env.setApplicationBase(env, env.mApplication, packageName);
            try {
                env.mApplication.onCreate();
            } catch (Throwable t) {
                sIntentCacheMap.remove(packageName);
                PluginDebugLog.log(TAG, "launchIntent application oncreate failed!");
                t.printStackTrace();
                // catch exception when application oncreate
                System.exit(0);
                return false;
            }
            env.changeInstrumentation(context, packageName);
            env.mIsApplicationInit = true;
            deliverPlug(true, packageName, ErrorType.SUCCESS);
//            env.mApplication.registerActivityLifecycleCallbacks(sActivityLifecycleCallback);
            env.mIsLaunchActivity = false;
        }
        cacheIntents = sIntentCacheMap.get(packageName);

        if (cacheIntents == null) {
            // 没有缓存的Intent，取当前的Intent;
            cacheIntents = new LinkedBlockingQueue<Intent>();
            sIntentCacheMap.put(packageName, cacheIntents);
        }
        // 避免重复添加到队列中，尤其是第一次初始化时在enterProxy中已经添加了一次
        if (!cacheIntents.contains(intent) && needAddCache) {
            cacheIntents.offer(intent);
        }

        PluginDebugLog.log(TAG, "launchIntent_cacheIntents: " + cacheIntents);
        if (!env.mIsLaunchActivity) {
            Intent curIntent = cacheIntents.poll();
            if (null != curIntent) {
                doRealLaunch(curIntent, env, packageName, conn, context);
            }
        }
        PluginDebugLog.log(TAG, "haveLaunchActivity end!");
        return false;
    }

    private static void addLoadingIntent(String pkgName, Intent intent) {
        if (null == intent || TextUtils.isEmpty(pkgName)) {
            return;
        }
        List<Intent> intents = sIntentLoadingMap.get(pkgName);
        if (null == intents) {
            intents = Collections.synchronizedList(new ArrayList<Intent>());
            sIntentLoadingMap.put(pkgName, intents);
        }
        PluginDebugLog.log(TAG, "addLoadingIntent pkgName: " + pkgName + " intent: " + intent);
        intents.add(intent);
    }

    private static void removeLoadingIntent(String pkgName, Intent intent) {
        if (null == intent || TextUtils.isEmpty(pkgName)) {
            return;
        }
        List<Intent> intents = sIntentLoadingMap.get(pkgName);
        Intent toBeRemoved = null;
        if (null != intents) {
            for (Intent temp : intents) {
                if (TextUtils.equals(temp.getStringExtra(EXTRA_TARGET_ACTIVITY),
                        intent.getStringExtra(EXTRA_TARGET_ACTIVITY))) {
                    toBeRemoved = temp;
                    break;
                }
            }
        }
        boolean result = false;
        if (null != toBeRemoved) {
            result = intents.remove(toBeRemoved);
        }
        PluginDebugLog.log(TAG, "removeLoadingIntent pkgName: " + pkgName + " toBeRemoved: "
                + toBeRemoved + " result: " + result);
    }

    private static void doRealLaunch(Intent curIntent, final ProxyEnvironmentNew env,
            final String packageName, final ServiceConnection conn, final Context context) {
        env.mIsLaunchActivity = true;
        String targetClassName = "";
        if (curIntent.getComponent() != null) {
            // 获取目标class
            targetClassName = curIntent.getComponent().getClassName();
            PluginDebugLog.log(TAG, "launchIntent_targetClassName:" + targetClassName);
            if (TextUtils.equals(targetClassName,
                    ProxyEnvironmentNew.EXTRA_VALUE_LOADTARGET_STUB)) {
                // 表示后台加载，不需要处理该Intent
                executeNext(env, packageName, conn, context);
                return;
            }
            if (TextUtils.isEmpty(targetClassName)) {
                targetClassName = env.getTargetMapping().getDefaultActivityName();
            }
        }

        // 处理启动的是service
        Class<?> targetClass = null;
        if (!TextUtils.isEmpty(targetClassName)) {
            try {
                targetClass = env.dexClassLoader.loadClass(targetClassName);
            } catch (Exception e) {
                deliverPlug(false, packageName, ErrorType.ERROR_CLIENT_LOAD_START);
                PluginDebugLog.log(TAG,
                        "launchIntent loadClass failed for targetClassName: " + targetClassName);
                executeNext(env, packageName, conn, context);
                return;
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

        } else if (targetClass != null && BroadcastReceiver.class.isAssignableFrom(targetClass)) {
            // 发一个内部用的动态广播
            Intent newIntent = new Intent(curIntent);
            newIntent.setComponent(null);
            newIntent.putExtra(ProxyEnvironmentNew.EXTRA_TARGET_PACKAGNAME, packageName);
            newIntent.setPackage(context.getPackageName());
            context.sendBroadcast(newIntent);
        } else {
            ActivityJumpUtil.handleStartActivityIntent(env.mPluginPakName, curIntent, -1, null,
                    context);
            addLoadingIntent(packageName, curIntent);
            context.startActivity(curIntent);
        }
        executeNext(env, packageName, conn, context);
    }

    private static void executeNext(final ProxyEnvironmentNew env, final String packageName,
            final ServiceConnection conn, final Context context) {
        Message msg = Message.obtain(sHandler, new Runnable() {

            @Override
            public void run() {
                LinkedBlockingQueue<Intent> cacheIntents = sIntentCacheMap.get(packageName);
                PluginDebugLog.log(TAG, "executeNext cacheIntents: " + cacheIntents);
                if (null != cacheIntents) {
                    Intent toBeStart = cacheIntents.poll();
                    if (null != toBeStart) {
                        doRealLaunch(toBeStart, env, packageName, conn, context);
                        return;
                    }
                }
                env.mIsLaunchActivity = false;
            }
        });
        sHandler.sendMessage(msg);
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

        if (env.mAppWrapper != null) {
            env.mAppWrapper.stopService(intent);
        }
    }

    private void setApplicationBase(ProxyEnvironmentNew env, Application application,
            String packageName) {
        PluginContextWrapper ctxWrapper = new PluginContextWrapper(
                ((Application) mContext).getBaseContext(), env);
        this.mAppWrapper = ctxWrapper;
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
     * @param context application context
     * @param packageName 插件包名
     * @param pluginInstallMethod 插件安装方式
     */
    public static void initProxyEnvironment(Context context, String packageName,
            String pluginInstallMethod, String processName) {
        PluginDebugLog.log(TAG,
                "sPluginsMap.containsKey(packageName):" + sPluginsMap.containsKey(packageName));
        if (sPluginsMap.containsKey(packageName)) {
            return;
        }
        ProxyEnvironmentNew newEnv = null;
        try {
            newEnv = new ProxyEnvironmentNew(context,
                    PluginInstaller.getInstalledApkFile(context, packageName), packageName,
                    pluginInstallMethod, processName);
        } catch (Exception e) {
            clearLoadingIntent(packageName);
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
     * @param packageName 插件包名
     */
    public static void exitProxy(String packageName) {
        if (packageName != null) {
            ProxyEnvironmentNew env = sPluginsMap.remove(packageName);
            if (env == null || env.mApplication == null) {
                return;
            }
            env.ejectClassLoader();
            if (env.mIsApplicationInit) {
                env.mApplication.onTerminate();
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

    public PackageInfo getTargetPackageInfo() {
        return targetMapping.getPackageInfo();
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
        PluginDebugLog.log(TAG, "pushActivityToStack activity: " + activity + " "
                + ((InstrActivityProxy) activity).dump());
        removeLoadingIntent(mPluginPakName, activity.getIntent());
        mActivityStack.addFirst(activity);
    }

    public boolean popActivityFromStack(Activity activity) {
        boolean result = false;
        if (!mActivityStack.isEmpty()) {
            PluginDebugLog.log(TAG, "popActivityFromStack activity: " + activity + " "
                    + ((InstrActivityProxy) activity).dump());
            result = mActivityStack.remove(activity);
        }
        quitApp(false);
        return result;
    }

    public void dealLaunchMode(Intent intent) {
        if (null == intent) {
            return;
        }
        PluginDebugLog.log(TAG, "dealLaunchMode target activity: " + intent + " source: "
                + intent.getStringExtra(ProxyEnvironmentNew.EXTRA_TARGET_ACTIVITY));
        if (null != mActivityStack && mActivityStack.size() > 0) {
            for (Activity ac : mActivityStack) {
                PluginDebugLog.log(TAG, "dealLaunchMode stack: " + ac + " source: "
                        + ((InstrActivityProxy) ac).dump());
            }
        } else {
            PluginDebugLog.log(TAG, "dealLaunchMode stack is empty");
        }
        String targetActivity = intent.getStringExtra(ProxyEnvironmentNew.EXTRA_TARGET_ACTIVITY);
        if (TextUtils.isEmpty(targetActivity)) {
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
        PluginDebugLog.log(TAG, "dealLaunchMode isSingleTop " + isSingleTop + " isSingleTask "
                + isSingleTask + " isClearTop " + isClearTop);
        if (isSingleTop && !isClearTop) {
            // 判断栈顶是否为需要启动的Activity
            Activity activity = null;
            if (!mActivityStack.isEmpty()) {
                activity = mActivityStack.getFirst();
            }
            String proxyClsName = ActivityJumpUtil.getProxyActivityClsName(getInstallType(), info,
                    mProcessName);
            if (activity != null && TextUtils.equals(proxyClsName, activity.getClass().getName())) {
                String key = getActivityStackKey(activity, getInstallType());

                if (!TextUtils.isEmpty(key) && TextUtils.equals(targetActivity, key)) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                }
            }
        } else if (isSingleTask || isClearTop) {

            Activity found = null;
            // 遍历已经起过的activity
            Iterator<Activity> it = mActivityStack.iterator();
            while (it.hasNext()) {
                Activity activity = it.next();
                String proxyClsName = ActivityJumpUtil.getProxyActivityClsName(getInstallType(),
                        info, mProcessName);
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
                List<Activity> popActivities = new ArrayList<Activity>(5);
                Iterator<Activity> iterator = mActivityStack.iterator();
                while (iterator.hasNext()) {
                    Activity activity = iterator.next();
                    if (activity == found) {
                        if (isSingleTask || isSingleTop) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        break;
                    }
                    popActivities.add(activity);
                }
                for (Activity act : popActivities) {
                    if (!mActivityStack.isEmpty()) {
                        PluginDebugLog.log(TAG, "dealLaunchMode mActivityStack remove " + act);
                        mActivityStack.remove(act);
                    }
                }
                for (Activity act : popActivities) {
                    act.finish();
                }
                quitApp(false);
            } else {
                // 遍历还未启动cache中的activity记录
                LinkedBlockingQueue<Intent> recoreds = sIntentCacheMap.get(mPluginPakName);
                if (null != recoreds) {
                    Iterator<Intent> recordIterator = recoreds.iterator();
                    String notLaunchTargetClassName = null;
                    while (recordIterator.hasNext()) {
                        Intent record = recordIterator.next();
                        if (null != record) {
                            if (null != record.getComponent()) {
                                notLaunchTargetClassName = record.getComponent().getClassName();
                            }
                            if (TextUtils.equals(notLaunchTargetClassName, targetActivity)) {
                                PluginDebugLog.log(TAG, "sIntentCacheMap found: " + targetActivity);
                                if (isSingleTask || isSingleTop) {
                                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                }
                                // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                break;
                            }
                        }
                    }
                }
                // 遍历启动过程中的activity记录
                List<Intent> loadingIntents = sIntentLoadingMap.get(mPluginPakName);
                if (null != loadingIntents) {
                    Iterator<Intent> loadingRecordIterator = loadingIntents.iterator();
                    String notLaunchTargetClassName = null;
                    while (loadingRecordIterator.hasNext()) {
                        Intent record = loadingRecordIterator.next();
                        if (null != record) {
                            notLaunchTargetClassName = record.getStringExtra(EXTRA_TARGET_ACTIVITY);
                            if (TextUtils.equals(notLaunchTargetClassName, targetActivity)) {
                                PluginDebugLog.log(TAG, "sIntentLoadingMap found: " + targetActivity);
                                if (isSingleTask || isSingleTop) {
                                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                }
                                // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                break;
                            }
                        }
                    }
                }
            }
        }
        PluginDebugLog.log(TAG, "dealLaunchMode end: " + intent + " "
                + intent.getStringExtra(ProxyEnvironmentNew.EXTRA_TARGET_ACTIVITY));
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
        intent.setExtrasClassLoader(getDexClassLoader());
        intent.putExtra(ProxyEnvironmentNew.EXTRA_TARGET_SERVICE, targetService);
        intent.putExtra(ProxyEnvironmentNew.EXTRA_TARGET_PACKAGNAME,
                targetMapping.getPackageName());
        // 同一个进程内service的bind是否重新走onBind方法，以intent的参数匹配为准FilterComparison.filterHashCode)
        // 手动添加一个category 让系统认为不同的插件activity每次bind都会走service的onBind的方法
        intent.addCategory(EXTRA_TARGET_CATEGORY + System.currentTimeMillis());
        try {
            intent.setClass(mContext,
                    Class.forName(ProxyComponentMappingByProcess.mappingService(mProcessName)));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param originIntent
     */
    public void remapReceiverIntent(Intent originIntent) {
        if (originIntent.getComponent() != null) {
            String targetReceiver = originIntent.getComponent().getClassName();
            originIntent.setExtrasClassLoader(getDexClassLoader());
            originIntent.putExtra(ProxyEnvironmentNew.EXTRA_TARGET_RECEIVER, targetReceiver);
            originIntent.putExtra(ProxyEnvironmentNew.EXTRA_TARGET_PACKAGNAME,
                    targetMapping.getPackageName());
            originIntent.setClass(mContext, BroadcastReceiverProxy.class);
        }
    }

    /**
     * 获取某个插件的data 根目录
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
                        ErrorType.ERROR_CLIENT_LOAD_CREATE_FILE_NULL);
                return null;
            }
            file = new File(getTargetMapping().getDataDir());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    /**
     * Handle plugin dependences
     *
     * @return true for load dependences successfully otherwise false
     */
    private boolean handleDependences() {
        CMPackageInfo pkgInfo = CMPackageManagerImpl.getInstance(mContext)
                .getPackageInfo(mPluginPakName);
        List<String> dependencies = pkgInfo.pluginInfo.getPluginResfs();
        if (null != dependencies) {
            CMPackageInfo libraryInfo;
            InjectResult injectResult;
            for (int i = 0; i < dependencies.size(); i++) {
                libraryInfo = CMPackageManagerImpl.getInstance(mContext)
                        .getPackageInfo(dependencies.get(i));
                if (null != libraryInfo) {
                    if (!sPluginDependences.contains(libraryInfo)
                            && !TextUtils.equals(libraryInfo.pluginInfo.mSuffixType,
                                    CMPackageManager.PLUGIN_FILE_SO)) {
                        PluginDebugLog.log(TAG,
                                "handleDependences inject " + libraryInfo.pluginInfo);
                        injectResult = ClassLoaderInjectHelper.inject(mContext,
                                libraryInfo.srcApkPath, null, null);
                        if (null != injectResult && injectResult.mIsSuccessful) {
                            PluginDebugLog.log(TAG, "handleDependences injectResult success for "
                                    + libraryInfo.pluginInfo);
                            sPluginDependences.add(libraryInfo.pluginInfo);
                        } else {
                            PluginDebugLog.log(TAG, "handleDependences injectResult faild for "
                                    + libraryInfo.pluginInfo);
                            return false;
                        }
                    } else {
                        PluginDebugLog.log(TAG, "handleDependences libraryInfo already handled!");
                    }
                }
                libraryInfo = null;
                injectResult = null;
            }
        }
        return true;
    }

    /**
     * 创建ClassLoader, 需要在 createDataRoot之后调用
     */
    private boolean createClassLoader() {
        boolean dependence = handleDependences();
        PluginDebugLog.log(TAG, "handleDependences: " + dependence);
        if (!dependence) {
            return dependence;
        }
        PluginDebugLog.log(TAG, "createClassLoader");
        File optimizedDirectory = new File(getDataDir(mContext, mPluginPakName).getAbsolutePath());
        if (optimizedDirectory.exists() && optimizedDirectory.canRead()
                && optimizedDirectory.canWrite()) {
            dexClassLoader = new PluginClassLoader(mApkFile.getAbsolutePath(),
                    optimizedDirectory.getAbsolutePath(), mContext.getClassLoader(), this);

            // 把插件 classloader 注入到host程序中，方便host app 能够找到 插件 中的class
            if (targetMapping.getMetaData() != null && targetMapping.getMetaData()
                    .getBoolean(ProxyEnvironmentNew.META_KEY_CLASSINJECT)) {
                ClassLoaderInjectHelper.inject(mContext.getClassLoader(), dexClassLoader,
                        targetMapping.getPackageName() + ".R");
                PluginDebugLog.log(TAG, "--- Class injecting @ " + targetMapping.getPackageName());
            }
            return true;
        } else {
            PluginDebugLog.log(TAG,
                    "createClassLoader failed as " + optimizedDirectory.getAbsolutePath()
                            + " exist: " + optimizedDirectory.exists() + " can read: "
                            + optimizedDirectory.canRead() + " can write: "
                            + optimizedDirectory.canWrite());
            return false;
        }
    }

    /**
     * 如果注入了classloader，执行反注入操作。用于卸载时
     */
    public void ejectClassLoader() {
        if (dexClassLoader != null && targetMapping.getMetaData() != null && targetMapping
                .getMetaData().getBoolean(ProxyEnvironmentNew.META_KEY_CLASSINJECT)) {
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
        sHostRes = mContext.getResources();
        Configuration config = new Configuration();
        config.setTo(sHostRes.getConfiguration());
        targetResources = new ResourcesProxy(targetAssetManager, sHostRes.getDisplayMetrics(),
                config, sHostRes, mPluginPakName);
        targetTheme = targetResources.newTheme();
        targetTheme.setTo(mContext.getTheme());

        // Create Resource Tool for host
        mResTool = new ResourcesToolForPlugin(mContext);
    }

    /**
     * 初始化插件资源
     *
     * @throws Exception
     */
    private void createTargetMapping(String pkgName) throws Exception {
        CMPackageInfo pkgInfo = CMPackageManagerImpl.getInstance(mContext).getPackageInfo(pkgName);
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
     * @param activity activity类名
     * @return 屏幕方向
     */
    public int getTargetActivityOrientation(String activity) {
        return targetMapping.getActivityInfo(activity).screenOrientation;
    }

    /**
     * @return the application
     */
    public Application getApplication() {
        return mApplication;
    }

    public ActivityInfo findActivityByClassName(String activityClsName) {
        return targetMapping.getActivityInfo(activityClsName);
    }

    public static void quit(Context context, String processName) {

        CMPackageManagerImpl.getInstance(context).exit();

        for (Map.Entry<String, ProxyEnvironmentNew> entry : sPluginsMap.entrySet()) {
            ProxyEnvironmentNew plugin = entry.getValue();
            if (plugin != null) {
                plugin.quitApp(true, false);
            }
        }

        if (context != null) {
            Intent intent = new Intent();
            String proxyServiceName = ProxyComponentMappingByProcess.mappingService(processName);

            Class<?> proxyServiceNameClass = null;
            try {
                proxyServiceNameClass = Class.forName(proxyServiceName);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            if (proxyServiceNameClass != null) {
                PluginDebugLog.log(TAG, "try to stop service " + proxyServiceName);
                intent.setClass(context, proxyServiceNameClass);
                intent.setAction(ACTION_QUIT);
                context.startService(intent);
            }
        }
    }

    /**
     * 退出某个应用。不是卸载插件应用
     *
     * @param force
     *            indicate whether quit by user or by system, true for trigger
     *            by user, false for trigger by plugin system.
     */
    public void quitApp(boolean force) {
        quitApp(force, true);
    }

    private void quitApp(boolean force, boolean notifyHost) {
        if (force) {
            PluginDebugLog.log(TAG, "quitApp !");
            while (!mActivityStack.isEmpty()) {
                mActivityStack.poll().finish();
            }
            mActivityStack.clear();
            sIntentCacheMap.remove(mPluginPakName);
            sIntentLoadingMap.remove(mPluginPakName);

            for (Entry<String, ServiceConnection> entry : sAliveServiceConnection.entrySet()) {
                if (entry != null && mContext != null) {
                    try {
                        mContext.unbindService(entry.getValue());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            sAliveServiceConnection.clear();

            Service service;
            for (Entry<String, PluginServiceWrapper> entry : sAliveServices.entrySet()) {
                service = entry.getValue().getCurrentService();
                if (service != null) {
                    service.stopSelf();
                }
            }
            sAliveServices.clear();
            service = null;
        }
        if (notifyHost && (force || (mActivityStack.isEmpty() && sAliveServices.isEmpty()))) {
            if (null != sExitStuff) {
                sExitStuff.doExitStuff(getTargetPackageName());
            }
        }
    }

    /**
     * 加载插件
     *
     * @param context application Context
     * @param packageName 插件包名
     * @param listenner 插件加载后的回调
     */
    public static void initTarget(final Context context, String packageName, String processName,
            final ITargetLoadListenner listenner) {
        PluginDebugLog.log("plugin", "initTarget");
        try {
            new InitProxyEnvireonment(context, packageName, processName, listenner).start();
        } catch (Exception e) {
            clearLoadingIntent(packageName);
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
                // FrameworkInstrumentation instrumentation = new
                // FrameworkInstrumentation(context);
                // instrumentationF.set(activityThread, instrumentation);
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
        String mProcName;

        public InitProxyEnvireonment(Context context, String pakName, String processName,
                ITargetLoadListenner listenner) {
            this.pakName = pakName;
            this.pContext = context;
            this.listenner = listenner;
            mProcName = processName;
        }

        @Override
        public void run() {
            super.run();
            try {
                String installMethod = CMPackageManagerImpl.getInstance(pContext)
                        .getPackageInfo(pakName).pluginInfo.mPluginInstallMethod;
                PluginDebugLog.log("plugin", "doInBackground:" + pakName + ", installMethod: "
                        + installMethod + " mProcName: " + mProcName);
                ProxyEnvironmentNew.initProxyEnvironment(pContext, pakName, installMethod,
                        mProcName);
                new InitHandler(listenner, pakName, Looper.getMainLooper()).sendEmptyMessage(1);
            } catch (Exception e) {
                if (e instanceof PluginStartupException) {
                    PluginStartupException ex = (PluginStartupException) e;
                    deliverPlug(false, pakName, ex.getCode());
                } else {
                    deliverPlug(false, pakName, ErrorType.ERROR_CLIENT_LOAD_INIT_ENVIRONMENT_FAIL);
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

    private static IDeliverPlug iDeliverPlug;

    public static void setiDeliverPlug(IDeliverPlug iDeliverPlug) {
        ProxyEnvironmentNew.iDeliverPlug = iDeliverPlug;
    }

    public interface IDeliverPlug {
        void deliverPlug(boolean success, String pakName, int errorCode);
    }

    public static void setPluginAppExitStuff(IAppExitStuff stuff) {
        sExitStuff = stuff;
    }

    public interface IAppExitStuff {
        void doExitStuff(String pkgName);
    }

    public interface IPluginEnvironmentStatusListener {
        void onPluginEnvironmentIsReady(String packageName);
    }

    public static void setPluginEnvironmentStatusListener(
            IPluginEnvironmentStatusListener listener) {
        sPluginEnvSatusListener = listener;
    }
}
