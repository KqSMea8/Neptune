package org.qiyi.pluginlibrary.runtime;

import android.app.Application;
import android.app.Instrumentation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.pm.PluginLiteInfo;
import org.qiyi.pluginlibrary.pm.PluginPackageInfo;
import org.qiyi.pluginlibrary.error.ErrorType;
import org.qiyi.pluginlibrary.component.stackmgr.PActivityStackSupervisor;
import org.qiyi.pluginlibrary.component.stackmgr.PServiceSupervisor;
import org.qiyi.pluginlibrary.component.stackmgr.PluginActivityControl;
import org.qiyi.pluginlibrary.component.wraper.PluginInstrument;
import org.qiyi.pluginlibrary.component.stackmgr.PluginServiceWrapper;
import org.qiyi.pluginlibrary.component.wraper.ResourcesProxy;
import org.qiyi.pluginlibrary.constant.IIntentConstant;
import org.qiyi.pluginlibrary.context.PluginContextWrapper;
import org.qiyi.pluginlibrary.pm.PluginPackageManager;
import org.qiyi.pluginlibrary.pm.PluginPackageManagerNative;
import org.qiyi.pluginlibrary.utils.ClassLoaderInjectHelper;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.ReflectionUtils;
import org.qiyi.pluginlibrary.utils.ResourcesToolForPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;

/**
 * 插件在内存中的表现形式：
 * 每一个{@link PluginLoadedApk}代表了一个插件实例，保
 * 存当前插件的{@link android.content.res.Resources}<br/>{@link ClassLoader}等信息
 * Author:yuanzeyao
 * Date:2017/7/3 17:01
 * Email:yuanzeyao@qiyi.com
 */

public class PluginLoadedApk implements IIntentConstant {
    private static final String TAG = "PluginLoadedApk";

    /**
     * 保存插件的依赖关系
     */
    private static ConcurrentHashMap<String, PluginLiteInfo> sPluginDependences
            = new ConcurrentHashMap<>();
    /**
     * 主工程的Resource对象
     */
    private static Resources mHostResource;
    /**
     * 主工程Context
     */
    private final Context mHostContext;
    /**
     * 插件apk文件
     */
    private final File mPluginFile;
    /**
     * 插件运行的进程名
     */
    private String mProcessName;
    /**
     * 插件的类加载器
     */
    private DexClassLoader mPluginClassLoader;
    /**
     * 插件的Resource对象
     */
    private Resources mPluginResource;
    /**
     * 动态通过资源名称获取资源id的工具类
     */
    private ResourcesToolForPlugin mResourceTool;
    /**
     * 插件的AssetManager对象
     */
    private AssetManager mPluginAssetManager;
    /**
     * 插件的全局主题
     */
    private Resources.Theme mPluginTheme;
    /**
     * 插件的详细信息，主要通过解析AndroidManifet.xml获得
     */
    private PluginPackageInfo mPluginMapping;
    /**
     * 当前插件的Activity栈
     */
    private PActivityStackSupervisor mActivityStackSupervisor;
    /**
     * 主工程的包名
     */
    private String mHostPackageName;
    /**
     * 插件工程的包名
     */
    private String mPluginPackageName;
    /**
     * 插件的Application
     */
    private Application mPluginApplication;
    /**
     * 插件是否已经初始化
     */
    private boolean isPluginInit = false;
    /**
     * 自定义Context,主要用来改写其中的一些方法从而改变插件行为
     */
    private PluginContextWrapper mAppWrapper;
    /**
     * 自定义Instrumentation，对Activity跳转进行拦截
     */
    private PluginInstrument mPluginInstrument;

    /**
     * 当前是否有正在启动的Intent
     */
    private volatile boolean isLaunchingIntent = false;

    /**
     * 在启动插件时，需要先将插件以{@link PluginLoadedApk}的形式加载到内存
     *
     * @param mHostContext       主工程的上下文
     * @param mPluginFile        需要加载的插件apk文件
     * @param mPluginPackageName 插件的包名
     * @param mProcessName 插件运行的进程名称
     * @throws Exception 当以上参数有一个为Null时，可能抛出{@link NullPointerException}
     *                   当创建ClassLoader失败时，会抛出异常
     */
    public PluginLoadedApk(Context mHostContext,
                           File mPluginFile,
                           String mPluginPackageName,
                           String mProcessName)
            throws Exception {
        if (mHostContext == null
                || mPluginFile == null
                || TextUtils.isEmpty(mPluginPackageName)) {
            throw new NullPointerException("PluginLoadedApk Constructer' parameter is null!");
        }
        this.mHostContext = mHostContext;
        this.mPluginFile = mPluginFile;
        this.mHostPackageName = mHostContext.getPackageName();
        this.mPluginPackageName = mPluginPackageName;
        this.mActivityStackSupervisor = new PActivityStackSupervisor(this);
        extraPluginPackageInfo(this.mPluginPackageName);
        this.mProcessName = mProcessName;
        if (!createClassLoader()) {
            throw new Exception("ProxyEnvironmentNew init failed for createClassLoader failed:" + " apkFile: " + mPluginFile.getAbsolutePath() + " pluginPakName: " + mPluginPackageName);
        }

        createPluginResource();
        installStaticReceiver();
    }

    /**
     * 动态注册插件中的静态Receiver
     */
    private void installStaticReceiver() {
        if (mPluginMapping == null || mHostContext == null) {
            return;
        }
        Map<String, PluginPackageInfo.ReceiverIntentInfo> mReceiverIntentInfos =
                mPluginMapping.getReceiverIntentInfos();
        if (mReceiverIntentInfos != null) {
            Set<Map.Entry<String, PluginPackageInfo.ReceiverIntentInfo>> mEntrys =
                    mReceiverIntentInfos.entrySet();
            Context mGlobalContext = mHostContext.getApplicationContext();
            for (Map.Entry<String, PluginPackageInfo.ReceiverIntentInfo> mEntry : mEntrys) {
                PluginPackageInfo.ReceiverIntentInfo mReceiverInfo = mEntry.getValue();
                if (mReceiverInfo != null) {
                    try {
                        BroadcastReceiver mReceiver =
                                BroadcastReceiver.class.cast(mPluginClassLoader.
                                        loadClass(mReceiverInfo.mInfo.name).newInstance());
                        List<IntentFilter> mFilters = mReceiverInfo.mFilter;
                        if (mFilters != null) {
                            for (IntentFilter mItem : mFilters) {
                                mGlobalContext.registerReceiver(mReceiver, mItem);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    /**
     * 创建插件的Resource {@link ResourcesProxy},通过此Resource对象
     * 插件可以访问主工程和插件的资源
     */
    private void createPluginResource() {
        try {
            AssetManager am = AssetManager.class.newInstance();
            ReflectionUtils.on(am)
                    .call("addAssetPath", PluginActivityControl.sMethods, mPluginFile.getAbsolutePath());
            mPluginAssetManager = am;
        } catch (Exception e) {
            PluginManager.deliver(mHostContext, false, mPluginPackageName, ErrorType.ERROR_CLIENT_LOAD_INIT_RESOURCE_FAILE);
            e.printStackTrace();
        }

        mHostResource = mHostContext.getResources();
        Configuration config = new Configuration();
        config.setTo(mHostResource.getConfiguration());
        mPluginResource = new ResourcesProxy(mPluginAssetManager, mHostResource.getDisplayMetrics(),
                config, mHostResource, mPluginPackageName);
        mPluginTheme = mPluginResource.newTheme();
        mPluginTheme.setTo(mHostContext.getTheme());
        mResourceTool = new ResourcesToolForPlugin(mHostContext);
    }

    /**
     * 创建插件的ClassLoader
     *
     * @return true:创建成功，false:创建失败
     */
    private boolean createClassLoader() {
        boolean dependence = handleDependences();
        PluginDebugLog.runtimeLog(TAG, "handleDependences: " + dependence);
        if (!dependence) {
            return dependence;
        }
        PluginDebugLog.runtimeLog(TAG, "createClassLoader");
        File optimizedDirectory = new File(getDataDir(mHostContext, mPluginPackageName).getAbsolutePath());
        if (optimizedDirectory.exists() && optimizedDirectory.canRead() && optimizedDirectory.canWrite()) {
            mPluginClassLoader = new DexClassLoader(mPluginFile.getAbsolutePath(), optimizedDirectory.getAbsolutePath(),
                    mPluginMapping.getnativeLibraryDir(), mHostContext.getClassLoader());

            // 把插件 classloader 注入到host程序中，方便host app 能够找到 插件 中的class
            if (!isSharePluginInjectClassLoader()) {
                PluginDebugLog.runtimeLog(TAG, "share plugin: " + mPluginMapping.getPackageName() + " no need to inject into host classloader");
            } else if (mPluginMapping.getMetaData() != null && mPluginMapping.isClassNeedInject()) {
                if (!sPluginDependences.containsKey(mPluginPackageName)) {
                    ClassLoaderInjectHelper.inject(mHostContext.getClassLoader(), mPluginClassLoader,
                            mPluginMapping.getPackageName() + ".R");
                    PluginDebugLog.runtimeLog(TAG, "--- Class injecting @ " + mPluginMapping.getPackageName());
                } else {
                    PluginDebugLog.runtimeLog(TAG,
                            "--- Class injecting @ " + mPluginMapping.getPackageName() + " already injected!");
                }
            } else {
                PluginDebugLog.runtimeLog(TAG, "plugin: " + mPluginMapping.getPackageName() + " no need to inject to host classloader");
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
     * 将插件中的类从主工程中删除
     */
    void ejectClassLoader() {
        if (mPluginClassLoader != null
                && mPluginMapping.getMetaData() != null
                && mPluginMapping.isClassNeedInject()) {
            PluginDebugLog.runtimeLog(TAG, "--- Class eject @ " + mPluginMapping.getPackageName());
            ClassLoaderInjectHelper.eject(mHostContext.getClassLoader(), mPluginClassLoader);
        }
    }


    /**
     * 创建插件的Application对象
     *
     * @return true：创建Application成功，false:创建失败
     */
    boolean makeApplication() {
        if (!isPluginInit || mPluginApplication == null) {
            String className = mPluginMapping.getApplicationClassName();
            if (TextUtils.isEmpty(className) || Application.class.getName().equals(className)) {
                // 创建默认的虚拟Application
                mPluginApplication = new Application();
            } else {
                try {
                    mPluginApplication = ((Application) mPluginClassLoader.loadClass(className).asSubclass(Application.class).newInstance());
                } catch (Exception e) {
                    PluginManager.deliver(mHostContext, false, mPluginPackageName, ErrorType.ERROR_CLIENT_INIT_PLUG_APP);
                    e.printStackTrace();
                    return false;
                }
            }

            invokeApplicationAttach();
            try {
                mPluginApplication.onCreate();
            } catch (Throwable t) {
                PActivityStackSupervisor.clearLoadingIntent(mPluginPackageName);
                PluginDebugLog.runtimeLog(TAG, "launchIntent application oncreate failed!");
                t.printStackTrace();
                System.exit(0);
                return false;
            }
            hookInstrumentation();
            isPluginInit = true;
            PluginManager.deliver(mHostContext, true, mPluginPackageName, ErrorType.SUCCESS);
            mPluginApplication.registerActivityLifecycleCallbacks(PluginManager.sActivityLifecycleCallback);
            isLaunchingIntent = false;
        }
        return true;
    }


    /**
     * 反射获取ActivityThread中的Instrumentation对象
     * 从而拦截Activity跳转
     */
    private void hookInstrumentation() {
        try {
            Context contextImpl = ((ContextWrapper) mHostContext).getBaseContext();
            Object activityThread = ReflectionUtils.getFieldValue(contextImpl, "mMainThread");
            Field instrumentationF = activityThread.getClass().getDeclaredField("mInstrumentation");
            instrumentationF.setAccessible(true);
            mPluginInstrument = new PluginInstrument((Instrumentation) instrumentationF.get(activityThread), mPluginPackageName);
        } catch (Exception e) {
            PluginManager.deliver(mHostContext, false, mPluginPackageName,
                    ErrorType.ERROR_CLIENT_CHANGE_INSTRUMENTATION_FAIL);
            e.printStackTrace();
        }
    }

    /**
     * 通过反射attach方法，让插件Application具备真正Application的能力
     */
    private void invokeApplicationAttach() {
        if (mPluginApplication == null) {
            PluginDebugLog.formatLog(TAG, "invokeApplicationAttach mPluginApplication is null! %s",
                    mPluginPackageName);
            return;
        }
        this.mAppWrapper = new PluginContextWrapper(((Application) mHostContext)
                .getBaseContext(), mPluginPackageName);
        // attach
        Method attachMethod;
        try {
            attachMethod = Application.class.getDeclaredMethod("attach", Context.class);
            attachMethod.setAccessible(true);
            attachMethod.invoke(mPluginApplication, mAppWrapper);
        } catch (Exception e) {
            PluginManager.deliver(mHostContext, false, mPluginPackageName,
                    ErrorType.ERROR_CLIENT_SET_APPLICATION_BASE_FAIL);
            e.printStackTrace();
        }
    }

    /**
     * 提取插件apk中的PackageInfo信息，主要就是解析AndroidManifest.xml文件
     *
     * @param mPluginPackage 需要提取信息的插件包名
     * @throws Exception 当提取信息失败时，会抛出一些异常
     */
    private void extraPluginPackageInfo(String mPluginPackage) throws Exception {
        PluginLiteInfo pkgInfo = PluginPackageManagerNative.getInstance(mHostContext)
                .getPackageInfo(mPluginPackage);
        if (pkgInfo != null) {
            mPluginMapping = PluginPackageManagerNative.getInstance(mHostContext)
                    .getPluginPackageInfo(mHostContext,pkgInfo); //pkgInfo.getTargetMapping(mHostContext);
            if (null == mPluginMapping || null == mPluginMapping.getPackageInfo()) {
                throw new Exception("Exception case targetMapping init failed!");
            }
        } else {
            throw new Exception("Havn't install pkgName");
        }
    }

    /**
     * 跟新资源配置
     *
     * @param mConfiguration 新的资源配置信息
     */
    public void updateConfiguration(Configuration mConfiguration) {
        if (mPluginResource != null) {
            mPluginResource.updateConfiguration(mConfiguration,
                    mHostResource != null ? mHostResource.getDisplayMetrics() : null);
        }
    }

    /**
     * 处理当前插件的依赖关系
     *
     * @return true:处理成功，false：处理失败
     */
    private boolean handleDependences() {
        List<String> dependencies = PluginPackageManagerNative
                .getInstance(mHostContext).getPluginRefs(mPluginPackageName); //pkgInfo.pluginInfo.getPluginResfs();
        if (null != dependencies) {
            PluginLiteInfo libraryInfo;
            ClassLoaderInjectHelper.InjectResult injectResult;
            for (int i = 0; i < dependencies.size(); i++) {
                libraryInfo = PluginPackageManagerNative.getInstance(mHostContext)
                        .getPackageInfo(dependencies.get(i));
                if (null != libraryInfo && !TextUtils.isEmpty(libraryInfo.packageName)) {
                    if (!sPluginDependences.containsKey(libraryInfo.packageName)) {
                        PluginDebugLog.runtimeLog(TAG, "handleDependences inject " + libraryInfo.packageName);
                        PluginPackageManager.updateSrcApkPath(mHostContext, libraryInfo);
                        File apkFile = new File(libraryInfo.srcApkPath);
                        if (!apkFile.exists()) {
                            PluginDebugLog.runtimeLog(TAG,
                                    "Special case apkFile not exist, notify client! packageName: "
                                            + libraryInfo.packageName);
                            PluginPackageManager.notifyClientPluginException(mHostContext,
                                    libraryInfo.packageName,
                                    "Apk file not exist!");
                            return false;
                        }
                        injectResult = ClassLoaderInjectHelper.inject(mHostContext,
                                libraryInfo.srcApkPath, null, null);
                        if (null != injectResult && injectResult.mIsSuccessful) {
                            PluginDebugLog.runtimeLog(TAG,
                                    "handleDependences injectResult success for "
                                            + libraryInfo.packageName);
                            sPluginDependences.put(libraryInfo.packageName, libraryInfo);
                        } else {
                            PluginDebugLog.runtimeLog(TAG,
                                    "handleDependences injectResult faild for "
                                            + libraryInfo.packageName);
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
     * 获取插件的数据目录
     *
     * @param context
     * @param packageName
     * @return
     */
    public File getDataDir(Context context, String packageName) {
        PluginDebugLog.runtimeLog(TAG, "packageName:" + packageName + " context:" + context);
        File file = null;
        try {
            if (TextUtils.isEmpty(packageName)) {
                PluginManager.deliver(context, false, context.getPackageName(),
                        ErrorType.ERROR_CLIENT_LOAD_CREATE_FILE_NULL);
                return null;
            }
            file = new File(mPluginMapping.getDataDir());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    /**
     * 通过Activity的名字获取Theme id
     *
     * @param mActivityName 需要获取Theme id的Activity 类名
     * @return -1表示获取失败，>0表示成功获取
     */
    public int getActivityThemeResourceByClassName(String mActivityName) {
        if (mPluginMapping != null) {
            return mPluginMapping.getThemeResource(mActivityName);
        }
        return -1;
    }

    /**
     * 通过类名获取ActivityInfo
     *
     * @param activityClsName 需要获取ActivityInfo的Activity的类名
     * @return
     */
    public ActivityInfo getActivityInfoByClassName(String activityClsName) {
        if (mPluginMapping != null) {
            return mPluginMapping.getActivityInfo(activityClsName);
        }
        return null;

    }

    public void quitApp(boolean force) {
        quitApp(force, true);
    }

    void quitApp(boolean force, boolean notifyHost) {
        if (force) {
            PluginDebugLog.runtimeLog(TAG, "quitapp with " + mPluginPackageName);
            while (!mActivityStackSupervisor.getActivityStack().isEmpty()) {
                mActivityStackSupervisor.pollActivityStack().finish();
            }
            mActivityStackSupervisor.clearActivityStack();
            PActivityStackSupervisor.clearLoadingIntent(mPluginPackageName);
            PActivityStackSupervisor.removeLoadingIntent(mPluginPackageName);

            for (Map.Entry<String, PluginServiceWrapper> entry : PServiceSupervisor.getAliveServices().entrySet()) {
                PluginServiceWrapper serviceWrapper = entry.getValue();
                if (serviceWrapper != null) {
                    if (!TextUtils.isEmpty(mPluginPackageName) &&
                            TextUtils.equals(mPluginPackageName, serviceWrapper.getPkgName())) {
                        String identity = PluginServiceWrapper.
                                getIndeitfy(mPluginPackageName, serviceWrapper.getServiceClassName());
                        if (!TextUtils.isEmpty(identity)) {
                            PluginDebugLog.runtimeLog(TAG, mPluginPackageName + " quitapp with service: " + identity);
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
        if (notifyHost) {
            PluginManager.doExitStuff(mPluginPackageName, force);
        }
    }


    /**
     * 插件是否初始化
     *
     * @return true :初始化，false:没有初始化
     */
    boolean hasPluginInit() {
        return isPluginInit;
    }

    /**
     * 是否有正在启动的Intent
     *
     * @return
     */
    public boolean hasLaunchIngIntent() {
        return isLaunchingIntent;
    }

    /**
     * 获取插件的Application
     *
     * @return 返回插件的Application, 如果插件有自定义的Application，那么返回自定义的Application，
     * 否则返回Application实例
     */
    public Application getPluginApplication() {
        return mPluginApplication;
    }

    /**
     * 获取插件的Instrumentation
     * @return
     *      返回插件中使用的Instrumentation
     */
    public PluginInstrument getPluginInstrument(){
        return mPluginInstrument;
    }

    /**
     * 返回插件的包的详细信息，这些信息主要是通过解析AndroidManifest.xml文件获得
     *
     * @return
     */
    public PluginPackageInfo getPluginPackageInfo() {
        return mPluginMapping;
    }

    public PackageInfo getPackageInfo(){
        if(mPluginMapping != null){
            return mPluginMapping.getPackageInfo();
        }
        return null;
    }

    /**
     * 获取插件的包名
     *
     * @return
     */
    public String getPluginPackageName() {
        return mPluginPackageName;
    }

    /**
     * 获取插件的ClassLoader
     *
     * @return
     */
    public DexClassLoader getPluginClassLoader() {
        return mPluginClassLoader;
    }

    /**
     * 获取主工程的Context
     *
     * @return
     */
    public Context getHostContext() {
        return mHostContext;
    }

    /**
     * 返回基线资源工具
     * @return
     */
    public ResourcesToolForPlugin getHostResourceTool(){
        return mResourceTool;
    }

    /**
     * 获取当前插件的Activity栈
     *
     * @return
     */
    public PActivityStackSupervisor getActivityStackSupervisor() {
        return mActivityStackSupervisor;
    }

    /**
     * 获取插件运行的进程名称(package:pluin or package:plugin1)
     *
     * @return
     *      返回插件进程的名称，可以在Application的process中指定插件运行的进程
     */
    public String getProcessName() {
        return mProcessName;
    }

    /**
     * 获取当前插件的自定义Context
     * @return
     */
    public PluginContextWrapper getAppWrapper(){
        return mAppWrapper;
    }

    /**
     * 获取主工程的包名
     *
     * @return
     *      主工程的包名
     */
    public String getHostPackageName() {
        return mHostPackageName;
    }

    /**
     * 获取插件的主题资源
     *
     * @return
     *      插件的主题
     */
    public Resources.Theme getPluginTheme() {
        return mPluginTheme;
    }

    /**
     * 获取插件的Resource
     *
     * @return
     *      插件的Resource,通过此Resource插件可以访问主工程和插件的资源
     */
    public Resources getPluginResource() {
        return mPluginResource;
    }

    /**
     * 获取插件的AssetManager
     *
     * @return
     *      插件的AssetManager
     */
    public AssetManager getPluginAssetManager() {
        return mPluginAssetManager;
    }


    /**
     * 跟新插件是否有正在启动的页面 状态
     *
     * @param isLaunchingIntent
     *      true:有正在启动的intent,false:没有正在启动的intent
     */
    void changeLaunchingIntentStatus(boolean isLaunchingIntent) {
        this.isLaunchingIntent = isLaunchingIntent;
    }

    /**
     * 分享插件是否注入到主ClassLoader里
     * 临时方案，因为插件sdk无法访问基线的类
     *
     * @return  false不注入，true维持线上逻辑
     */
    private boolean isSharePluginInjectClassLoader() {

        if (!TextUtils.equals("com.iqiyi.share", mPluginPackageName)) {
            // 不是分享插件，维持现状
            return true;
        }

        if (mPluginMapping.getMetaData() == null || !mPluginMapping.isClassNeedInject()) {
            // meta配置不需要处理
            return false;
        }

        SharedPreferences sp = mHostContext.getSharedPreferences("default_sharePreference", Context.MODE_MULTI_PROCESS);
        String pluginSwitch = sp.getString("WEIBO_SHARE_ENABLE", "0");  // 0维持线上逻辑，1去掉ClassLoader注入
        if ("1".equals(pluginSwitch)) {
            // 云控关闭ClassLoader注入
            PluginDebugLog.runtimeLog(TAG, "disable share plugin inject into host classloader by fusion switch");
            return false;
        }
        // 维持现状
        return true;
    }
}
