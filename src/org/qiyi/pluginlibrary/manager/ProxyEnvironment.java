package org.qiyi.pluginlibrary.manager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.Instrumentation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.ActivityJumpUtil;
import org.qiyi.pluginlibrary.ApkTargetMappingNew;
import org.qiyi.pluginlibrary.PActivityStackSupervisor;
import org.qiyi.pluginlibrary.PServiceSupervisor;
import org.qiyi.pluginlibrary.PluginActivityControl;
import org.qiyi.pluginlibrary.PluginInstrument;
import org.qiyi.pluginlibrary.PluginServiceWrapper;
import org.qiyi.pluginlibrary.ProxyComponentMappingByProcess;
import org.qiyi.pluginlibrary.ResourcesProxy;
import org.qiyi.pluginlibrary.ServiceJumpUtil;
import org.qiyi.pluginlibrary.ErrorType.ErrorType;
import org.qiyi.pluginlibrary.context.CMContextWrapperNew;
import org.qiyi.pluginlibrary.plugin.TargetMapping;
import org.qiyi.pluginlibrary.pm.CMPackageInfo;
import org.qiyi.pluginlibrary.pm.CMPackageManager;
import org.qiyi.pluginlibrary.pm.CMPackageManagerImpl;
import org.qiyi.pluginlibrary.pm.PluginPackageInfoExt;
import org.qiyi.pluginlibrary.utils.ClassLoaderInjectHelper;
import org.qiyi.pluginlibrary.utils.ClassLoaderInjectHelper.InjectResult;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.ReflectionUtils;
import org.qiyi.pluginlibrary.utils.ResourcesToolForPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import dalvik.system.DexClassLoader;

/**
 * 插件运行的环境
 */
public class ProxyEnvironment {

    public static final String TAG = ProxyEnvironment.class.getSimpleName();

    public static final String EXTRA_TARGET_RECEIVER = "pluginapp_extra_target_receiver";
    public static final String EXTRA_TARGET_PACKAGNAME = "pluginapp_extra_target_pacakgename";

    public static final String EXTRA_VALUE_LOADTARGET_STUB = "pluginapp_loadtarget_stub";

    /**
     * 插件加载成功的广播
     */
    public static final String ACTION_TARGET_LOADED = "org.qiyi.pluginapp.action.TARGET_LOADED";

    /**
     * 插件加载成功的广播(new)
     */
    public static final String ACTION_PLUGIN_LOADED = "org.qiyi.pluginapp.ACTION_PLUGIN_LOADED";

    /**
     * 运行在当前进程内所有插件依赖库
     **/
    private static ConcurrentMap<String, PluginPackageInfoExt> sPluginDependences =
            new ConcurrentHashMap<String, PluginPackageInfoExt>();

    /** 插件调试日志 **/
    private static ActivityLifecycleCallbacks sActivityLifecycleCallback = new ActivityLifecycleCallbacks() {

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            PluginDebugLog.runtimeLog(TAG, "onActivityCreated: " + activity);
        }

        @Override
        public void onActivityStarted(Activity activity) {
            PluginDebugLog.runtimeLog(TAG, "onActivityStarted: " + activity);
        }

        @Override
        public void onActivityResumed(Activity activity) {
            PluginDebugLog.runtimeLog(TAG, "onActivityResumed: " + activity);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            PluginDebugLog.runtimeLog(TAG, "onActivityPaused: " + activity);
        }

        @Override
        public void onActivityStopped(Activity activity) {
            PluginDebugLog.log(TAG, "onActivityStopped: " + activity);
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            PluginDebugLog.runtimeLog(TAG, "onActivitySaveInstanceState: " + activity);
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            PluginDebugLog.runtimeLog(TAG, "onActivityDestroyed: " + activity);
        }
    };

    // 插件APP退出时要处理的事情
    private static IAppExitStuff sExitStuff;
    private static Resources sHostRes;

    static Handler sHandler = new Handler(Looper.getMainLooper());

    private final Context mContext;
    private final File mApkFile;
    private final String mInstallType;
    private final String mProcessName;

    private DexClassLoader mDexClassLoader;
    private Resources mTargetResources;
    private ResourcesToolForPlugin mResTool;
    private AssetManager mTargetAssetManager;
    private Theme mTargetTheme;
    private TargetMapping mTargetMapping;
    private PActivityStackSupervisor mActivityStackSupervisor;
    private String mParentPackagename;
    /**
     * 插件虚拟的Application实例
     */
    private Application mApplication;

    /**
     * 是否初始化了插件Application
     */
    private boolean mIsApplicationInit = false;

    private String mPluginPakName;

    public CMContextWrapperNew mAppWrapper;

    public PluginInstrument mPluginInstrument;

    // 是否正在执行intent启动
    private volatile boolean mIsLaunchingIntent = false;

    /**
     * 构造方法，解析apk文件，创建插件运行环境
     *
     * @param context host application context
     * @param apkFile 插件apk文件
     * @throws Exception
     */
    private ProxyEnvironment(Context context, File apkFile, String pluginPakName, String installType, String processName)
            throws Exception {
        if (context == null || apkFile == null || TextUtils.isEmpty(pluginPakName) || TextUtils.isEmpty(installType)) {
            throw new Exception("ProxyEnvironmentNew init failed for parameters has null: context: " + context + " apkFile: " + apkFile
                    + " pluginPakName: " + pluginPakName + " installType: " + installType);
        }
        mContext = context.getApplicationContext();
        mApkFile = apkFile;
        mParentPackagename = context.getPackageName();
        mPluginPakName = pluginPakName;
        mInstallType = installType;
        mProcessName = processName;
        createTargetMapping(pluginPakName);

        // 加载classloader
        boolean clsLoader = createClassLoader();
        if (!clsLoader) {
            ProxyEnvironmentManager.deliverPlug(context, false, pluginPakName, ErrorType.ERROR_CLIENT_CREATE_CLSlOADER);
            throw new Exception("ProxyEnvironmentNew init failed for createClassLoader failed: " + context + " apkFile: " + apkFile
                    + " pluginPakName: " + pluginPakName + " installType: " + installType);
        }
        // 加载资源
        createTargetResource();
        // 创建ActivityStackSupervisor
        mActivityStackSupervisor = new PActivityStackSupervisor(this);
        //注册静态广播
        installStaticReceiver();

    }

    /**
     * 动态注册插件定义的静态广播
     */
    private void installStaticReceiver() {
        if(mTargetMapping ==null){
            return;
        }
        Map<String,ApkTargetMappingNew.ReceiverIntentInfo> mReceiverIntentInfos =
                mTargetMapping.getReceiverIntentInfos();
        if(mReceiverIntentInfos != null){
            Set<Entry<String,ApkTargetMappingNew.ReceiverIntentInfo>> mEntrys = mReceiverIntentInfos.entrySet();
            Context mGlobalContext = mContext.getApplicationContext();
            if(mEntrys != null){
                for(Map.Entry<String,ApkTargetMappingNew.ReceiverIntentInfo> mEntry : mEntrys){
                    ApkTargetMappingNew.ReceiverIntentInfo mReceiverInfo = mEntry.getValue();
                    if(mReceiverInfo != null){
                        try{
                            BroadcastReceiver mReceiver =
                                    BroadcastReceiver.class.cast(mDexClassLoader.
                                            loadClass(mReceiverInfo.mInfo.name).newInstance());
                            List<IntentFilter> mFilters = mReceiverInfo.mFilter;
                            if(mFilters != null){
                                for(IntentFilter mItem :mFilters){
                                    mGlobalContext.registerReceiver(mReceiver,mItem);
                                }
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                    }
                }
            }

        }
    }


    void updateConfiguration(Configuration config) {
        if (null != mTargetResources) {
            mTargetResources.updateConfiguration(config, sHostRes != null ? sHostRes.getDisplayMetrics() : null);
        }
    }

    boolean hasInit() {
        return mIsApplicationInit;
    }

    void updateLaunchingIntentFlag(boolean status) {
        mIsLaunchingIntent = status;
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
     * 进入代理模式
     *
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
        PluginDebugLog.runtimeLog(TAG, "launchIntent: " + intent);
        String packageName = ProxyEnvironmentManager.tryParsePkgName(context, intent);
        ProxyEnvironment env = ProxyEnvironmentManager.getEnvByPkgName(packageName);
        if (env == null) {
            ProxyEnvironmentManager.deliverPlug(context, false, packageName, ErrorType.ERROR_CLIENT_NOT_LOAD);
            PluginDebugLog.runtimeLog(TAG, packageName + " launchIntent env is null! Just return!");
            PActivityStackSupervisor.clearLoadingIntent(packageName);
            return false;
            // throw new IllegalArgumentException(packageName
            // +" not loaded, Make sure you have call the init method!");
        }

        LinkedBlockingQueue<Intent> cacheIntents = null;
        if (!env.mIsApplicationInit && env.mApplication == null) {
            String className = env.mTargetMapping.getApplicationClassName();
            if (TextUtils.isEmpty(className) || Application.class.getName().equals(className)) {
                // 创建默认的虚拟Application
                env.mApplication = new Application();
            } else {
                try {
                    env.mApplication = ((Application) env.mDexClassLoader.loadClass(className).asSubclass(Application.class).newInstance());
                } catch (Exception e) {
                    ProxyEnvironmentManager.deliverPlug(context, false, packageName, ErrorType.ERROR_CLIENT_INIT_PLUG_APP);
                    e.printStackTrace();
                    return false;
                }
            }

            env.setApplicationBase(env, env.mApplication, packageName);
            try {
                env.mApplication.onCreate();
            } catch (Throwable t) {
                PActivityStackSupervisor.clearLoadingIntent(packageName);
                PluginDebugLog.runtimeLog(TAG, "launchIntent application oncreate failed!");
                t.printStackTrace();
                // catch exception when application oncreate
                System.exit(0);
                return false;
            }
            env.changeInstrumentation(context, packageName);
            env.mIsApplicationInit = true;
            ProxyEnvironmentManager.deliverPlug(context, true, packageName, ErrorType.SUCCESS);
            env.mApplication.registerActivityLifecycleCallbacks(sActivityLifecycleCallback);
            env.mIsLaunchingIntent = false;
        }
        cacheIntents = PActivityStackSupervisor.getCachedIntent(packageName);

        if (cacheIntents == null) {
            // 没有缓存的Intent，取当前的Intent;
            cacheIntents = new LinkedBlockingQueue<Intent>();
            PActivityStackSupervisor.addCachedIntent(packageName, cacheIntents);
        }
        // 避免重复添加到队列中，尤其是第一次初始化时在enterProxy中已经添加了一次
        if (!cacheIntents.contains(intent) && needAddCache) {
            PluginDebugLog.runtimeLog(TAG, "launchIntent add to cacheIntent....");
            cacheIntents.offer(intent);
        }else{
            PluginDebugLog.runtimeLog(TAG, "launchIntent not add to cacheIntent....needAddCache:"+needAddCache);
        }

        PluginDebugLog.runtimeLog(TAG, "launchIntent_cacheIntents: " + cacheIntents);
        if (!env.mIsLaunchingIntent) {
            Intent curIntent = cacheIntents.poll();
            if (null != curIntent) {
                doRealLaunch(curIntent, env, packageName, conn, context);
            }
            PluginDebugLog.runtimeLog(TAG,"launchIntent no launching intnet... and launch end!");
        }else{
            PluginDebugLog.runtimeLog(TAG, "launchIntent has launching intent.... so return directly!");
        }

        return false;
    }

    private static void doRealLaunch(Intent curIntent, final ProxyEnvironment env, final String packageName,
            final ServiceConnection conn, final Context context) {
        String targetClassName = "";
        if (curIntent.getComponent() != null) {
            // 获取目标class
            targetClassName = curIntent.getComponent().getClassName();
            PluginDebugLog.runtimeLog(TAG, "launchIntent_targetClassName:" + targetClassName);
            if (TextUtils.isEmpty(targetClassName)) {
                targetClassName = env.getTargetMapping().getDefaultActivityName();
            }
        }

        Class<?> targetClass = null;
        if (!TextUtils.isEmpty(targetClassName)) {
            try {
                targetClass = env.mDexClassLoader.loadClass(targetClassName);
            } catch (Exception e) {
                ProxyEnvironmentManager.deliverPlug(context, false, packageName, ErrorType.ERROR_CLIENT_LOAD_START);
                PluginDebugLog.runtimeLog(TAG, "launchIntent loadClass failed for targetClassName: " + targetClassName);
                executeNext(env, packageName, conn, context);
                return;
            }
        }

        String action = curIntent.getAction();
        if (TextUtils.equals(action, ACTION_TARGET_LOADED)
                || TextUtils.equals(targetClassName, EXTRA_VALUE_LOADTARGET_STUB)) {
            PluginDebugLog.runtimeLog(TAG, "launchIntent loadtarget stub!");
            // 发一个内部用的动态广播
            if (BroadcastReceiver.class.isAssignableFrom(targetClass)) {
                Intent newIntent = new Intent(curIntent);
                newIntent.setComponent(null);
                newIntent.putExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME, packageName);
                newIntent.setPackage(context.getPackageName());
                context.sendBroadcast(newIntent);
            }
            // 表示后台加载，不需要处理该Intent
            executeNext(env, packageName, conn, context);
            return;
        }

        env.mIsLaunchingIntent = true;
        PluginDebugLog.runtimeLog(TAG, "launchIntent_targetClass: " + targetClass);
        if (targetClass != null && Service.class.isAssignableFrom(targetClass)) {
            // 处理启动的是service
            ServiceJumpUtil.remapStartServiceIntent(env, curIntent, targetClassName);
            if (conn == null) {
                context.startService(curIntent);
            } else {
                context.bindService(curIntent, conn,
                        curIntent.getIntExtra(ServiceJumpUtil.BIND_SERVICE_FLAGS, Context.BIND_AUTO_CREATE));
            }
        } else {
            ActivityJumpUtil.handleStartActivityIntent(env.mPluginPakName, curIntent, -1, null, context);
            PActivityStackSupervisor.addLoadingIntent(packageName, curIntent);
            Context lastActivity = null;
            if (env.mActivityStackSupervisor != null && !env.mActivityStackSupervisor.getActivityStack().isEmpty()) {
                lastActivity = env.mActivityStackSupervisor.getActivityStack().getLast();
            }
            if (!(context instanceof Activity) && null != lastActivity) {
                int flag = curIntent.getFlags();
                flag = flag ^ Intent.FLAG_ACTIVITY_NEW_TASK;
                curIntent.setFlags(flag);
                lastActivity.startActivity(curIntent);
            } else {
                context.startActivity(curIntent);
            }
        }
//        sendPluginLoadedBroadcast(context);
        executeNext(env, packageName, conn, context);
    }

    /**
     * 发送插件加载成功的广播，目前主要用于从桌面快捷方式启动的插件的情况
     */
    public static void sendPluginLoadedBroadcast(Context context) {
        Intent intent = new Intent();
        intent.setAction(ACTION_PLUGIN_LOADED);
        context.sendBroadcast(intent);
    }

    private static void executeNext(final ProxyEnvironment env, final String packageName, final ServiceConnection conn,
            final Context context) {
        Message msg = Message.obtain(sHandler, new Runnable() {

            @Override
            public void run() {
                LinkedBlockingQueue<Intent> cacheIntents = PActivityStackSupervisor.getCachedIntent(packageName);
                PluginDebugLog.runtimeLog(TAG, "executeNext cacheIntents: " + cacheIntents);
                if (null != cacheIntents) {
                    Intent toBeStart = cacheIntents.poll();
                    if (null != toBeStart) {
                        doRealLaunch(toBeStart, env, packageName, conn, context);
                        return;
                    }
                }
                env.mIsLaunchingIntent = false;
            }
        });
        sHandler.sendMessage(msg);
    }

    private void setApplicationBase(ProxyEnvironment env, Application application, String packageName) {
        CMContextWrapperNew ctxWrapper = new CMContextWrapperNew(((Application) mContext).getBaseContext(), packageName);
        this.mAppWrapper = ctxWrapper;
        // attach
        Method attachMethod;
        try {
            attachMethod = Application.class.getDeclaredMethod("attach", Context.class);
            attachMethod.setAccessible(true);
            attachMethod.invoke(application, ctxWrapper);
        } catch (Exception e) {
            ProxyEnvironmentManager.deliverPlug(mContext, false, packageName,
                    ErrorType.ERROR_CLIENT_SET_APPLICATION_BASE_FAIL);
            e.printStackTrace();
        }
    }

    /**
     * 初始化插件的运行环境，如果已经初始化，则什么也不做
     *
     * @param context application context
     * @param packageInfo CMPackageInfo
     * @param pluginInstallMethod 插件安装方式
     */
    public static void initProxyEnvironment(Context context, CMPackageInfo packageInfo,
                                            String pluginInstallMethod, String processName) {

        if (packageInfo != null) {
            String packageName = packageInfo.packageName;
            if (!TextUtils.isEmpty(packageName)) {
                boolean hasInstance = ProxyEnvironmentManager.hasEnvInstance(packageName);
                PluginDebugLog.runtimeLog(TAG, "sPluginsMap.containsKey(" + packageName + "):" + hasInstance);
                if (hasInstance) {
                    return;
                }
                ProxyEnvironment newEnv = null;
                CMPackageInfo.updateSrcApkPath(context, packageInfo);
                if (!TextUtils.isEmpty(packageInfo.srcApkPath)) {
                    File apkFile = new File(packageInfo.srcApkPath);
                    if (!apkFile.exists()) {
                        PluginDebugLog.runtimeLog(TAG,
                                "Special case apkFile not exist, notify client! packageName: " + packageName);
                        CMPackageManager.notifyClientPluginException(context, packageName, "Apk file not exist!");
                        return;
                    }
                    try {
                        newEnv = new ProxyEnvironment(context, apkFile, packageName,
                                pluginInstallMethod, processName);
                    } catch (Exception e) {
                        PActivityStackSupervisor.clearLoadingIntent(packageName);
                        e.printStackTrace();
                        ProxyEnvironmentManager.deliverPlug(context, false, packageName,
                                ErrorType.ERROR_CLIENT_ENVIRONMENT_NULL);
                    }
                    if (newEnv != null) {
                        ProxyEnvironmentManager.addEnvInstance(packageName, newEnv);
                    }
                }
            }
        }
    }

    /**
     * 退出插件
     *
     * @param packageName 插件包名
     */
    public static void exitProxy(String packageName) {
        if (packageName != null) {
            ProxyEnvironment env = ProxyEnvironmentManager.removeEnvInstance(packageName);
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
        return mDexClassLoader;
    }

    /**
     * 获取插件资源
     *
     * @return 资源对象
     */
    public Resources getTargetResources() {
        return mTargetResources;
    }

    public AssetManager getTargetAssetManager() {
        return mTargetAssetManager;
    }

    public Theme getTargetTheme() {
        return mTargetTheme;
    }

    public TargetMapping getTargetMapping() {
        return mTargetMapping;
    }

    public String getTargetPackageName() {
        return mTargetMapping.getPackageName();
    }

    public PackageInfo getTargetPackageInfo() {
        return mTargetMapping.getPackageInfo();
    }

    public PActivityStackSupervisor getActivityStackSupervisor() {
        return mActivityStackSupervisor;
    }

    /**
     * 获取某个插件的data 根目录
     *
     * @param packageName
     * @return
     */
    public File getDataDir(Context context, String packageName) {
        PluginDebugLog.runtimeLog(TAG, "packageName:" + packageName + " context:" + context);
        File file = null;
        try {
            if (TextUtils.isEmpty(packageName)) {
                ProxyEnvironmentManager.deliverPlug(context, false, context.getPackageName(),
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
        CMPackageInfo pkgInfo = CMPackageManagerImpl.getInstance(mContext).getPackageInfo(mPluginPakName);
        List<String> dependencies = pkgInfo.pluginInfo.getPluginResfs();
        if (null != dependencies) {
            CMPackageInfo libraryInfo;
            InjectResult injectResult;
            for (int i = 0; i < dependencies.size(); i++) {
                libraryInfo = CMPackageManagerImpl.getInstance(mContext).getPackageInfo(dependencies.get(i));
                if (null != libraryInfo && !TextUtils.isEmpty(libraryInfo.packageName)) {
                    if (!sPluginDependences.containsKey(libraryInfo.packageName)
                            && !TextUtils.equals(libraryInfo.pluginInfo.mSuffixType, CMPackageManager.PLUGIN_FILE_SO)) {
                        PluginDebugLog.runtimeLog(TAG, "handleDependences inject " + libraryInfo.pluginInfo);
                        CMPackageInfo.updateSrcApkPath(mContext, libraryInfo);
                        File apkFile = new File(libraryInfo.srcApkPath);
                        if (!apkFile.exists()) {
                            PluginDebugLog.runtimeLog(TAG, "Special case apkFile not exist, notify client! packageName: "
                                    + libraryInfo.packageName);
                            CMPackageManager.notifyClientPluginException(mContext, libraryInfo.packageName,
                                    "Apk file not exist!");
                            return false;
                        }
                        injectResult = ClassLoaderInjectHelper.inject(mContext, libraryInfo.srcApkPath, null, null);
                        if (null != injectResult && injectResult.mIsSuccessful) {
                            PluginDebugLog.runtimeLog(TAG,
                                    "handleDependences injectResult success for " + libraryInfo.pluginInfo);
                            sPluginDependences.put(libraryInfo.packageName, libraryInfo.pluginInfo);
                        } else {
                            PluginDebugLog.runtimeLog(TAG,
                                    "handleDependences injectResult faild for " + libraryInfo.pluginInfo);
                            return false;
                        }
                    } else {
                        PluginDebugLog.runtimeLog(TAG, "handleDependences libraryInfo already handled!");
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
        PluginDebugLog.runtimeLog(TAG, "handleDependences: " + dependence);
        if (!dependence) {
            return dependence;
        }
        PluginDebugLog.runtimeLog(TAG, "createClassLoader");
        File optimizedDirectory = new File(getDataDir(mContext, mPluginPakName).getAbsolutePath());
        if (optimizedDirectory.exists() && optimizedDirectory.canRead() && optimizedDirectory.canWrite()) {
            mDexClassLoader = new DexClassLoader(mApkFile.getAbsolutePath(), optimizedDirectory.getAbsolutePath(),
                    getTargetMapping().getnativeLibraryDir(), mContext.getClassLoader());

            // 把插件 classloader 注入到host程序中，方便host app 能够找到 插件 中的class
            if (mTargetMapping.getMetaData() != null && mTargetMapping.isClassNeedInject()) {
                if (!sPluginDependences.containsKey(mPluginPakName)) {
                    ClassLoaderInjectHelper.inject(mContext.getClassLoader(), mDexClassLoader,
                            mTargetMapping.getPackageName() + ".R");
                    PluginDebugLog.runtimeLog(TAG, "--- Class injecting @ " + mTargetMapping.getPackageName());
                } else {
                    PluginDebugLog.runtimeLog(TAG,
                            "--- Class injecting @ " + mTargetMapping.getPackageName() + " already injected!");
                }
            }
            return true;
        } else {
            PluginDebugLog.runtimeLog(TAG,
                    "createClassLoader failed as " + optimizedDirectory.getAbsolutePath() + " exist: "
                            + optimizedDirectory.exists() + " can read: " + optimizedDirectory.canRead()
                            + " can write: " + optimizedDirectory.canWrite());
            return false;
        }
    }

    /**
     * 如果注入了classloader，执行反注入操作。用于卸载时
     */
    public void ejectClassLoader() {
        if (mDexClassLoader != null && mTargetMapping.getMetaData() != null && mTargetMapping.isClassNeedInject()) {
            PluginDebugLog.runtimeLog(TAG, "--- Class injecting @ " + mTargetMapping.getPackageName());
            ClassLoaderInjectHelper.eject(mContext.getClassLoader(), mDexClassLoader);
        }
    }

    @SuppressLint("NewApi")
    private void createTargetResource() {
        try {
            AssetManager am = AssetManager.class.newInstance();
            ReflectionUtils.on(am).call("addAssetPath", PluginActivityControl.sMethods, mApkFile.getAbsolutePath());
            mTargetAssetManager = am;
        } catch (Exception e) {
            ProxyEnvironmentManager.deliverPlug(mContext, false, mPluginPakName, ErrorType.ERROR_CLIENT_LOAD_INIT_RESOURCE_FAILE);
            e.printStackTrace();
        }

        // 解决一个HTC ONE X上横竖屏会黑屏的问题
        sHostRes = mContext.getResources();
        Configuration config = new Configuration();
        config.setTo(sHostRes.getConfiguration());
        mTargetResources = new ResourcesProxy(mTargetAssetManager, sHostRes.getDisplayMetrics(), config, sHostRes, mPluginPakName);
        mTargetTheme = mTargetResources.newTheme();
        mTargetTheme.setTo(mContext.getTheme());

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
            mTargetMapping = pkgInfo.getTargetMapping(mContext);
            if (null == mTargetMapping || null == mTargetMapping.getPackageInfo()) {
                throw new Exception("Exception case targetMapping init failed!");
            }
        } else {
            throw new Exception("Havn't install pkgName");
        }
    }

    /**
     * @return the parentPackagename
     */
    public String getParentPackagename() {
        return mParentPackagename;
    }

    public int getTargetActivityThemeResource(String activity) {
        return mTargetMapping.getThemeResource(activity);
    }

    /**
     * 获取插件Activity的屏幕方向
     *
     * @param activity activity类名
     * @return 屏幕方向
     */
    public int getTargetActivityOrientation(String activity) {
        return mTargetMapping.getActivityInfo(activity).screenOrientation;
    }

    /**
     * @return the application
     */
    public Application getApplication() {
        return mApplication;
    }

    public ActivityInfo findActivityByClassName(String activityClsName) {
        return mTargetMapping.getActivityInfo(activityClsName);
    }

    /**
     * 退出某个应用。不是卸载插件应用
     *
     * @param force indicate whether quit by user or by system, true for trigger
     * by user, false for trigger by plugin system.
     */
    public void quitApp(boolean force) {
        quitApp(force, true);
    }

    void quitApp(boolean force, boolean notifyHost) {
        if (force) {
            PluginDebugLog.runtimeLog(TAG, "quitapp with " + mPluginPakName);
            while (!mActivityStackSupervisor.getActivityStack().isEmpty()) {
                mActivityStackSupervisor.pollActivityStack().finish();
            }
            mActivityStackSupervisor.clearActivityStack();
            PActivityStackSupervisor.clearLoadingIntent(mPluginPakName);
            PActivityStackSupervisor.removeLoadingIntent(mPluginPakName);

            for (Entry<String, PluginServiceWrapper> entry : PServiceSupervisor.getAliveServices().entrySet()) {
                PluginServiceWrapper serviceWrapper = entry.getValue();
                if (serviceWrapper != null) {
                    if (!TextUtils.isEmpty(mPluginPakName) &&
                            TextUtils.equals(mPluginPakName, serviceWrapper.getPkgName())) {
                        String identity = PluginServiceWrapper.
                                getIndeitfy(mPluginPakName, serviceWrapper.getServiceClassName());
                        if (!TextUtils.isEmpty(identity)) {
                            PluginDebugLog.runtimeLog(TAG, mPluginPakName + " quitapp with service: " + identity);
                            ServiceConnection connection = PServiceSupervisor.getConnection(identity);
                            if (connection != null && mAppWrapper != null) {
                                try {
                                    PluginDebugLog.runtimeLog(TAG, "quitapp unbindService" + connection);
                                    mAppWrapper.unbindService(connection);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        Service service = entry.getValue().getCurrentService();
                        if (service != null) {
                            service.stopSelf();
                        }
                    }
                }
            }
        }
        if (notifyHost && (force || (isActivityStackEmpty() && PServiceSupervisor.getAliveServices().isEmpty()))) {

            if (null != sExitStuff) {
                PluginDebugLog.runtimeLog(TAG, "do exit stuff with " + mPluginPakName);
                sExitStuff.doExitStuff(getTargetPackageName());
            }
        }
    }

    private static boolean isActivityStackEmpty() {
        for (Entry<String, ProxyEnvironment> entry : ProxyEnvironmentManager.getAllEnv().entrySet()) {
            ProxyEnvironment environmentNew = entry.getValue();
            if (environmentNew != null && environmentNew.mActivityStackSupervisor.getActivityStack().size() > 0) {
                return false;
            }
        }
        return true;
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
                mPluginInstrument = new PluginInstrument((Instrumentation) instrumentationF.get(activityThread), pkgName);
            } else {
                // Default option
                mPluginInstrument = new PluginInstrument((Instrumentation) instrumentationF.get(activityThread), pkgName);
            }
            instrumentationF.setAccessible(false);
        } catch (Exception e) {
            ProxyEnvironmentManager.deliverPlug(context, false, pkgName,
                    ErrorType.ERROR_CLIENT_CHANGE_INSTRUMENTATION_FAIL);
            e.printStackTrace();
        }
    }

    public static void setPluginAppExitStuff(IAppExitStuff stuff) {
        sExitStuff = stuff;
    }

    public static void setPluginLifeCallBack(ActivityLifecycleCallbacks mActivityLifeCallBack){
        sActivityLifecycleCallback = mActivityLifeCallBack;
    }

    public interface IAppExitStuff {
        void doExitStuff(String pkgName);
    }
}
