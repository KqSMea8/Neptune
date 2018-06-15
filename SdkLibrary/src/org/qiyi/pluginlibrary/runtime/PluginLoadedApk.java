package org.qiyi.pluginlibrary.runtime;

import android.app.Application;
import android.app.Instrumentation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.HybirdPlugin;
import org.qiyi.pluginlibrary.install.PluginInstaller;
import org.qiyi.pluginlibrary.loader.PluginClassLoader;
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
import org.qiyi.pluginlibrary.utils.ErrorUtil;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.ReflectionUtils;
import org.qiyi.pluginlibrary.utils.ResourcesToolForPlugin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;

/**
 * 插件在内存中的表现形式：
 * 每一个{@link PluginLoadedApk}代表了一个插件实例，
 * 保存当前插件的{@link android.content.res.Resources}<br/>
 * {@link ClassLoader}, {@link PackageInfo}等信息
 *
 * Author:yuanzeyao
 * Date:2017/7/3 17:01
 * Email:yuanzeyao@qiyi.com
 */

public class PluginLoadedApk implements IIntentConstant {
    private static final String TAG = "PluginLoadedApk";
    /**
     * 保存注入到宿主ClassLoader的插件
     */
    private static Set<String> sInjectedPlugins = Collections.synchronizedSet(new HashSet<String>());
    /**
     * 保存所有的插件ClassLoader
     */
    private static Map<String, DexClassLoader> sAllPluginClassLoader = new ConcurrentHashMap<>();

    /** 宿主的Context */
    private final Context mHostContext;
    /** 宿主的ClassLoader */
    private final ClassLoader mHostClassLoader;
    /** 宿主的Resource对象 */
    private final Resources mHostResource;
    /** 宿主的包名 */
    private final String mHostPackageName;

    /** 插件ClassLoader的parent */
    private ClassLoader mParent;
    /** 插件的路径 */
    private final String mPluginPath;
    /** 插件运行的进程名 */
    private final String mProcessName;
    /** 插件的类加载器 */
    private DexClassLoader mPluginClassLoader;
    /** 插件的Resource对象 */
    private Resources mPluginResource;
    /** 插件的AssetManager对象 */
    private AssetManager mPluginAssetManager;
    /** 插件的全局默认主题 */
    private Resources.Theme mPluginTheme;
    /** 插件的详细信息，主要通过解析AndroidManifest.xml获得 */
    private PluginPackageInfo mPluginPackageInfo;
    /** 插件工程的包名 */
    private String mPluginPackageName;
    /** 插件的Application */
    private Application mPluginApplication;
    /** 自定义插件Context,主要用来改写其中的一些方法从而改变插件行为*/
    private PluginContextWrapper mPluginAppContext;
    /** 自定义Instrumentation，对Activity跳转进行拦截 */
    private PluginInstrument mPluginInstrument;

    /** 动态通过资源名称获取资源id的工具类 */
    @Deprecated
    private ResourcesToolForPlugin mResourceTool;

    /** 当前插件的Activity栈 */
    private PActivityStackSupervisor mActivityStackSupervisor;
    /** 插件Application是否已经初始化 */
    private volatile boolean isPluginInit = false;
    /** 当前是否有正在启动的Intent */
    private volatile boolean isLaunchingIntent = false;

    /**
     * 在启动插件时，需要先将插件以{@link PluginLoadedApk}的形式加载到内存
     *
     * @param mHostContext       主工程的上下文
     * @param mPluginPath        需要加载的插件apk文件
     * @param mPluginPackageName 插件的包名
     * @param mProcessName       插件运行的进程名称
     * @throws Exception 当以上参数有一个为Null时，可能抛出{@link NullPointerException}
     *                   当创建ClassLoader失败时，会抛出异常
     */
    public PluginLoadedApk(Context mHostContext,
                           String mPluginPath,
                           String mPluginPackageName,
                           String mProcessName) {
        if (mHostContext == null
                || TextUtils.isEmpty(mPluginPath)
                || TextUtils.isEmpty(mPluginPackageName)) {
            throw new NullPointerException("PluginLoadedApk Constructor' parameter is null!");
        }

        this.mHostContext = mHostContext;
        this.mHostClassLoader = mHostContext.getClassLoader();
        this.mHostResource = mHostContext.getResources();
        this.mHostPackageName = mHostContext.getPackageName();

        this.mPluginPath = mPluginPath;
        this.mPluginPackageName = mPluginPackageName;
        this.mActivityStackSupervisor = new PActivityStackSupervisor(this);
        this.mProcessName = mProcessName;
        // 提取插件Apk的信息
        extraPluginPackageInfo(this.mPluginPackageName);
        // 创建插件ClassLoader
        if (HybirdPlugin.getConfig().shouldUseNewCLMode()) {
            if (!createNewClassLoader()) {
                throw new RuntimeException("ProxyEnvironmentNew init failed for createNewClassLoader failed:" + " apkFile: " + mPluginPath + " pluginPakName: " + mPluginPackageName);
            }
        } else {
            if (!createClassLoader()) {
                throw new RuntimeException("ProxyEnvironmentNew init failed for createClassLoader failed:" + " apkFile: " + mPluginPath + " pluginPakName: " + mPluginPackageName);
            }
        }
        PluginDebugLog.runtimeFormatLog(TAG, "plugin %s, class loader: %s", mPluginPackageName, mPluginClassLoader.toString());
        // 创建插件资源
        if (HybirdPlugin.getConfig().shouldUseNewResGen()) {
            createNewPluginResource();
        } else {
            createPluginResource();
        }
        // 插件Application的Base Context
        this.mPluginAppContext = new PluginContextWrapper(((Application) mHostContext)
                .getBaseContext(), mPluginPackageName, true);
        // 注册静态广播
        installStaticReceiver();
    }

    /**
     * 动态注册插件中的静态Receiver
     */
    private void installStaticReceiver() {
        if (mPluginPackageInfo == null || mHostContext == null) {
            return;
        }
        Map<String, PluginPackageInfo.ReceiverIntentInfo> mReceiverIntentInfos =
                mPluginPackageInfo.getReceiverIntentInfos();
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

        PluginDebugLog.runtimeLog(TAG, "createPluginResource for " + mPluginPackageName);
        try {
            // Android 5.0以下AssetManager不支持扩展资源表，始终新建一个，避免污染宿主的AssetManager
            AssetManager am = AssetManager.class.newInstance();
            // 添加插件的资源
            ReflectionUtils.on(am).call("addAssetPath", PluginActivityControl.sMethods, mPluginPath);
            boolean shouldAddHostRes = !mPluginPackageInfo.isIndividualMode() && mPluginPackageInfo.isResourceNeedMerge();
            if (shouldAddHostRes) {
                // 添加宿主的资源到插件的AssetManager
                ReflectionUtils.on(am).call("addAssetPath", PluginActivityControl.sMethods, mHostContext.getApplicationInfo().sourceDir);
                PluginDebugLog.runtimeLog(TAG, "--- Resource merging into plugin @ " + mPluginPackageInfo.getPackageName());
            }

            mPluginAssetManager = am;
        } catch (Exception e) {
            ErrorUtil.throwErrorIfNeed(e);
            PluginManager.deliver(mHostContext, false, mPluginPackageName, ErrorType.ERROR_CLIENT_LOAD_INIT_RESOURCE_FAILE);
        }

        Configuration config = new Configuration();
        config.setTo(mHostResource.getConfiguration());
        if (mPluginPackageInfo.isIndividualMode()) {
            // 独立插件包，不依赖宿主的Resource
            mPluginResource = new Resources(mPluginAssetManager, mHostResource.getDisplayMetrics(),
                    config);
        } else {
            mPluginResource = new ResourcesProxy(mPluginAssetManager, mHostResource.getDisplayMetrics(),
                    config, mHostResource, mPluginPackageName);
        }
        mPluginTheme = mPluginResource.newTheme();
        mPluginTheme.setTo(mHostContext.getTheme());
        mResourceTool = new ResourcesToolForPlugin(mHostContext);
    }

    /**
     * 通过PackageManager的公开API创建插件的Resource对象
     */
    private void createNewPluginResource() {

        PluginDebugLog.runtimeLog(TAG, "createNewPluginResource for " + mPluginPackageName);
        PackageManager pm = mHostContext.getPackageManager();
        try {
            Resources resources = pm.getResourcesForApplication(mPluginPackageInfo.getApplicationInfo());
            if (mPluginPackageInfo.isIndividualMode()) {
                mPluginResource = resources;
                mPluginAssetManager = resources.getAssets();
            } else {
                // 对基线资源存在依赖
                mPluginAssetManager = resources.getAssets();
                if (mPluginPackageInfo.isResourceNeedMerge()) {
                    ReflectionUtils.on(mPluginAssetManager).call("addAssetPath", PluginActivityControl.sMethods,
                            mHostContext.getApplicationInfo().sourceDir);
                    PluginDebugLog.runtimeLog(TAG, "--- Resource merging into plugin @ " + mPluginPackageInfo.getPackageName());
                }

                Configuration config = new Configuration();
                config.setTo(mHostResource.getConfiguration());
                // 重新New一个Resource
                mPluginResource = new ResourcesProxy(mPluginAssetManager, mHostResource.getDisplayMetrics(),
                        config, mHostResource, mPluginPackageName);
            }

            mPluginTheme = mPluginResource.newTheme();
            mPluginTheme.setTo(mHostContext.getTheme());
            mResourceTool = new ResourcesToolForPlugin(mHostContext);
        } catch (PackageManager.NameNotFoundException e) {
            //使用旧的反射的方案创建Resource
            createPluginResource();
        } catch (Exception e) {
            createPluginResource();
        }
    }


    /**
     * 创建插件的ClassLoader
     *
     * @return true:创建成功，false:创建失败
     */
    private boolean createClassLoader() {
        boolean dependence = handleDependencies();
        PluginDebugLog.runtimeLog(TAG, "handleDependencies: " + dependence);
        if (!dependence) {
            return false;
        }
        PluginDebugLog.runtimeLog(TAG, "createClassLoader");
        File optimizedDirectory = getDataDir(mHostContext, mPluginPackageName);
        if (optimizedDirectory.exists() && optimizedDirectory.canRead() && optimizedDirectory.canWrite()) {

            mPluginClassLoader = new DexClassLoader(mPluginPath, optimizedDirectory.getAbsolutePath(),
                    mPluginPackageInfo.getNativeLibraryDir(), mHostClassLoader);

            // 把插件 classloader 注入到host程序中，方便host app 能够找到 插件 中的class
            if (!isSharePluginInjectClassLoader()) {
                PluginDebugLog.runtimeLog(TAG, "share plugin: " + mPluginPackageInfo.getPackageName() + " no need to inject into host classloader");
            } else if (mPluginPackageInfo.isClassNeedInject()) {
                if (!sInjectedPlugins.contains(mPluginPackageName)) {
                    ClassLoaderInjectHelper.InjectResult injectResult = ClassLoaderInjectHelper.inject(mHostClassLoader,
                            mPluginClassLoader, mPluginPackageInfo.getPackageName() + ".R");
                    PluginDebugLog.runtimeLog(TAG, "--- Class injecting @ " + mPluginPackageInfo.getPackageName());
                    if (injectResult != null && injectResult.mIsSuccessful) {
                        sInjectedPlugins.add(mPluginPackageName);
                        PluginDebugLog.runtimeLog(TAG, "inject class result success for " + mPluginPackageName);
                    } else {
                        PluginDebugLog.runtimeLog(TAG, "inject class result failed for " + mPluginPackageName);
                    }
                } else {
                    PluginDebugLog.runtimeLog(TAG,
                            "--- Class injecting @ " + mPluginPackageInfo.getPackageName() + " already injected!");
                }
            } else {
                PluginDebugLog.runtimeLog(TAG, "plugin: " + mPluginPackageInfo.getPackageName() + " no need to inject to host classloader");
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
     * 创建插件新的ClassLoader，不使用注入Host ClassLoader方案
     */
    private boolean createNewClassLoader() {

        PluginDebugLog.runtimeLog(TAG, "createNewClassLoader");
        File optimizedDirectory = getDataDir(mHostContext, mPluginPackageName);
        mParent = mPluginPackageInfo.isIndividualMode() ? mHostClassLoader.getParent() : mHostClassLoader;
        if (optimizedDirectory.exists() && optimizedDirectory.canRead() && optimizedDirectory.canWrite()) {
            DexClassLoader classLoader = sAllPluginClassLoader.get(mPluginPackageName);
            if (classLoader == null) {
                mPluginClassLoader = new PluginClassLoader(mPluginPackageInfo, mPluginPath,
                        optimizedDirectory.getAbsolutePath(), mPluginPackageInfo.getNativeLibraryDir(), mParent);
                PluginDebugLog.runtimeLog(TAG, "createNewClassLoader success for plugin " + mPluginPackageName);
                sAllPluginClassLoader.put(mPluginPackageName, mPluginClassLoader);
            } else {
                PluginDebugLog.runtimeLog(TAG, "classloader find in cache, createNewClassLoader success for plugin " + mPluginPackageName);
                mPluginClassLoader = classLoader;
            }

            return handleNewDependencies();
        } else {
            PluginDebugLog.runtimeLog(TAG,
                    "createNewClassLoader failed as " + optimizedDirectory.getAbsolutePath() + " exist: "
                            + optimizedDirectory.exists() + " can read: " + optimizedDirectory.canRead()
                            + " can write: " + optimizedDirectory.canWrite());
            return false;
        }
    }

    /**
     * 将插件中的类从主工程中删除
     */
    void ejectClassLoader() {
        if (mPluginClassLoader != null && mPluginPackageInfo.isClassNeedInject()) {
            PluginDebugLog.runtimeLog(TAG, "--- Class eject @ " + mPluginPackageInfo.getPackageName());
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
            String className = mPluginPackageInfo.getApplicationClassName();
            if (TextUtils.isEmpty(className)) {
                className = "android.app.Application";
            }

            hookInstrumentation();
            try {
                // load plugin Application and call Application#attach()
                this.mPluginApplication = mPluginInstrument.newApplication(mPluginClassLoader, className, mPluginAppContext);
            } catch (Exception e) {
                ErrorUtil.throwErrorIfNeed(e);
                PluginManager.deliver(mHostContext, false, mPluginPackageName, ErrorType.ERROR_CLIENT_INIT_PLUG_APP);
                return false;
            }

//            if (TextUtils.isEmpty(className) || Application.class.getName().equals(className)) {
//                // 创建默认的虚拟Application
//                mPluginApplication = new Application();
//            } else {
//                try {
//                    mPluginApplication = ((Application) mPluginClassLoader.loadClass(className).asSubclass(Application.class).newInstance());
//                } catch (Exception e) {
//                    ErrorUtil.throwErrorIfNeed(e);
//                    PluginManager.deliver(mHostContext, false, mPluginPackageName, ErrorType.ERROR_CLIENT_INIT_PLUG_APP);
//                    return false;
//                }
//            }
//            invokeApplicationAttach();
            // 注册Application回调
            mHostContext.registerComponentCallbacks(new ComponentCallbacks2() {
                @Override
                public void onTrimMemory(int level) {
                    mPluginApplication.onTrimMemory(level);
                }

                @Override
                public void onConfigurationChanged(Configuration configuration) {
                    updateConfiguration(configuration);
                }

                @Override
                public void onLowMemory() {
                    mPluginApplication.onLowMemory();
                }
            });

            try {
                mPluginApplication.onCreate();
            } catch (Throwable t) {
                ErrorUtil.throwErrorIfNeed(t);
                PluginDebugLog.runtimeLog(TAG, "call plugin Application#onCreate() failed, pkgName=" + mPluginPackageName);
                PActivityStackSupervisor.clearLoadingIntent(mPluginPackageName);
                return false;
            }

            mPluginApplication.registerActivityLifecycleCallbacks(PluginManager.sActivityLifecycleCallback);
            isPluginInit = true;
            isLaunchingIntent = false;

            PluginManager.deliver(mHostContext, true, mPluginPackageName, ErrorType.SUCCESS);
        }
        return true;
    }


    /**
     * 反射获取ActivityThread中的Instrumentation对象
     * 从而拦截Activity跳转
     */
    private void hookInstrumentation() {
        try {
//            Context contextImpl = ((ContextWrapper) mHostContext).getBaseContext();
//            Object activityThread = ReflectionUtils.getFieldValue(contextImpl, "mMainThread");
//            Field instrumentationF = activityThread.getClass().getDeclaredField("mInstrumentation");
//            instrumentationF.setAccessible(true);
//            Instrumentation hostInstr = (Instrumentation) instrumentationF.get(activityThread);
            Instrumentation hostInstr = HybirdPlugin.getHostInstrumentation();
            mPluginInstrument = new PluginInstrument(hostInstr, mPluginPackageName);
        } catch (Exception e) {
            ErrorUtil.throwErrorIfNeed(e);
            PluginManager.deliver(mHostContext, false, mPluginPackageName,
                    ErrorType.ERROR_CLIENT_CHANGE_INSTRUMENTATION_FAIL);
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

        // attach
        Method attachMethod;
        try {
            attachMethod = Application.class.getDeclaredMethod("attach", Context.class);
            attachMethod.setAccessible(true);
            attachMethod.invoke(mPluginApplication, mPluginAppContext);
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
    private void extraPluginPackageInfo(String mPluginPackage) {
        PluginLiteInfo pkgInfo = PluginPackageManagerNative.getInstance(mHostContext)
                .getPackageInfo(mPluginPackage);
        if (pkgInfo != null) {
            mPluginPackageInfo = PluginPackageManagerNative.getInstance(mHostContext)
                    .getPluginPackageInfo(mHostContext, pkgInfo);
        }

        if (mPluginPackageInfo == null) {
            mPluginPackageInfo = new PluginPackageInfo(mHostContext, new File(mPluginPath));
        }
    }

    /**
     * 更新资源配置
     *
     * @param newConfig 新的资源配置信息
     */
    public void updateConfiguration(Configuration newConfig) {
        mPluginApplication.onConfigurationChanged(newConfig);
        mPluginResource.updateConfiguration(newConfig,
                mHostResource != null ? mHostResource.getDisplayMetrics() : mPluginResource.getDisplayMetrics());
    }

    /**
     * 处理当前插件的依赖关系
     *
     * @return true:处理成功，false：处理失败
     */
    private boolean handleDependencies() {
        List<String> dependencies = PluginPackageManagerNative
                .getInstance(mHostContext).getPluginRefs(mPluginPackageName); //pkgInfo.pluginInfo.getPluginResfs();
        if (null != dependencies) {
            PluginLiteInfo libraryInfo;
            ClassLoaderInjectHelper.InjectResult injectResult;
            for (int i = 0; i < dependencies.size(); i++) {
                libraryInfo = PluginPackageManagerNative.getInstance(mHostContext)
                        .getPackageInfo(dependencies.get(i));
                if (null != libraryInfo && !TextUtils.isEmpty(libraryInfo.packageName)) {
                    if (!sInjectedPlugins.contains(libraryInfo.packageName)) {
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
                        PluginDebugLog.runtimeLog(TAG,
                                "handleDependences src apk path : " + libraryInfo.srcApkPath);
                        File dataDir = new File(PluginInstaller.getPluginappRootPath(mHostContext), libraryInfo.packageName);
                        String nativeLibraryDir = new File(dataDir, PluginInstaller.NATIVE_LIB_PATH).getAbsolutePath();
                        injectResult = ClassLoaderInjectHelper.inject(mHostContext,
                                libraryInfo.srcApkPath, null, nativeLibraryDir);
                        if (null != injectResult && injectResult.mIsSuccessful) {
                            PluginDebugLog.runtimeLog(TAG,
                                    "handleDependences injectResult success for "
                                            + libraryInfo.packageName);
                            sInjectedPlugins.add(libraryInfo.packageName);
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
     * 处理当前插件的依赖关系
     *
     * @return true:处理成功，false：处理失败
     */
    private boolean handleNewDependencies() {
        List<String> dependencies = PluginPackageManagerNative
                .getInstance(mHostContext).getPluginRefs(mPluginPackageName); //pkgInfo.pluginInfo.getPluginResfs();
        if (null != dependencies) {
            PluginLiteInfo libraryInfo;
            PluginPackageInfo libraryPackageInfo;
            DexClassLoader dependency;
            ClassLoaderInjectHelper.InjectResult injectResult;
            for (int i = 0; i < dependencies.size(); i++) {
                libraryInfo = PluginPackageManagerNative.getInstance(mHostContext)
                        .getPackageInfo(dependencies.get(i));
                if (null != libraryInfo && !TextUtils.isEmpty(libraryInfo.packageName)) {
                    libraryPackageInfo = PluginPackageManagerNative.getInstance(mHostContext)
                            .getPluginPackageInfo(mHostContext, libraryInfo);

                    dependency = sAllPluginClassLoader.get(libraryInfo.packageName);
                    if (dependency == null) {
                        PluginDebugLog.runtimeLog(TAG, "handleNewDependencies not contain in cache " + libraryInfo.packageName);
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

                        PluginDebugLog.runtimeLog(TAG,
                                "handleNewDependencies src apk path : " + libraryInfo.srcApkPath);
                        File dataDir = libraryPackageInfo != null ? new File(libraryPackageInfo.getDataDir()) :
                                new File(PluginInstaller.getPluginappRootPath(mHostContext), libraryInfo.packageName);
                        String nativeLibraryDir = libraryPackageInfo != null ? libraryPackageInfo.getNativeLibraryDir() :
                                new File(dataDir, PluginInstaller.NATIVE_LIB_PATH).getAbsolutePath();

                        ClassLoader parent = (libraryPackageInfo != null && libraryPackageInfo.isIndividualMode())
                                ? mHostClassLoader.getParent() : mHostClassLoader;
                        dependency = new PluginClassLoader(libraryPackageInfo, libraryInfo.srcApkPath,
                                PluginInstaller.getPluginInjectRootPath(mHostContext).getAbsolutePath(), nativeLibraryDir, parent);
                        sAllPluginClassLoader.put(libraryInfo.packageName, dependency);
                    }
                    // 把依赖插件的ClassLoader添加到当前的ClassLoader
                    if (mPluginClassLoader instanceof PluginClassLoader) {
                        ((PluginClassLoader)mPluginClassLoader).addDependency(dependency);
                        PluginDebugLog.runtimeFormatLog(TAG, "handleNewDependencies addDependency %s into plugin %s success ",
                                libraryInfo.packageName, mPluginPackageName);
                    } else {
                        // 注入到PluginClassLoader
                        injectResult = ClassLoaderInjectHelper.inject(mPluginClassLoader, dependency, null);
                        if (injectResult != null && injectResult.mIsSuccessful) {
                            PluginDebugLog.runtimeFormatLog(TAG, "handleNewDependencies inject into %s success", mPluginPackageName);
                        } else {
                            PluginDebugLog.runtimeFormatLog(TAG, "handleNewDependencies inject into %s failed", mPluginPackageName);
                        }
                            return false;
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
        File dataDir = null;
        try {
            if (TextUtils.isEmpty(packageName)) {
                PluginManager.deliver(context, false, context.getPackageName(),
                        ErrorType.ERROR_CLIENT_LOAD_CREATE_FILE_NULL);
                return null;
            }
            dataDir = new File(mPluginPackageInfo.getDataDir());
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dataDir;
    }

    /**
     * 通过Activity的名字获取Theme id
     *
     * @param mActivityName 需要获取Theme id的Activity 类名
     * @return -1表示获取失败，>0表示成功获取
     */
    public int getActivityThemeResourceByClassName(String mActivityName) {
        if (mPluginPackageInfo != null) {
            return mPluginPackageInfo.getThemeResource(mActivityName);
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
        if (mPluginPackageInfo != null) {
            return mPluginPackageInfo.getActivityInfo(activityClsName);
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
                            if (connection != null && mPluginAppContext != null) {
                                try {
                                    PluginDebugLog.runtimeLog(TAG, "quitapp unbindService" + connection);
                                    mPluginAppContext.unbindService(connection);
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
     *
     * @return 返回插件中使用的Instrumentation
     */
    public PluginInstrument getPluginInstrument() {
        return mPluginInstrument;
    }

    /**
     * 返回插件的包的详细信息，这些信息主要是通过解析AndroidManifest.xml文件获得
     */
    public PluginPackageInfo getPluginPackageInfo() {
        return mPluginPackageInfo;
    }

    /**
     * 返回插件包的PackageInfo信息
     */
    public PackageInfo getPackageInfo() {
        if (mPluginPackageInfo != null) {
            return mPluginPackageInfo.getPackageInfo();
        }
        return null;
    }

    /**
     * 获取插件的包名
     */
    public String getPluginPackageName() {
        return mPluginPackageName;
    }

    /**
     * 获取插件的ClassLoader
     */
    public DexClassLoader getPluginClassLoader() {
        return mPluginClassLoader;
    }

    /**
     * 获取主工程的Context
     */
    public Context getHostContext() {
        return mHostContext;
    }

    /**
     * 返回基线资源工具
     */
    @Deprecated
    public ResourcesToolForPlugin getHostResourceTool() {
        return mResourceTool;
    }

    /**
     * 获取当前插件的Activity栈
     */
    public PActivityStackSupervisor getActivityStackSupervisor() {
        return mActivityStackSupervisor;
    }

    /**
     * 获取插件运行的进程名称(package:pluin or package:plugin1)
     *
     * @return 返回插件进程的名称，可以在Application的process中指定插件运行的进程
     */
    public String getProcessName() {
        return mProcessName;
    }

    /**
     * 获取当前插件的自定义Context
     *
     * @return
     */
    public PluginContextWrapper getAppWrapper() {
        return mPluginAppContext;
    }

    /**
     * 获取主工程的包名
     *
     * @return 主工程的包名
     */
    public String getHostPackageName() {
        return mHostPackageName;
    }

    /**
     * 获取插件的主题资源
     *
     * @return 插件的主题
     */
    public Resources.Theme getPluginTheme() {
        return mPluginTheme;
    }

    /**
     * 获取插件的Resource
     *
     * @return 插件的Resource, 通过此Resource插件可以访问主工程和插件的资源
     */
    public Resources getPluginResource() {
        return mPluginResource;
    }

    /**
     * 获取插件的AssetManager
     *
     * @return 插件的AssetManager
     */
    public AssetManager getPluginAssetManager() {
        if (mPluginAssetManager == null) {
            mPluginAssetManager = mPluginResource.getAssets();
        }
        return mPluginAssetManager;
    }


    /**
     * 更新插件是否有正在启动的页面 状态
     *
     * @param isLaunchingIntent true:有正在启动的intent,false:没有正在启动的intent
     */
    void changeLaunchingIntentStatus(boolean isLaunchingIntent) {
        this.isLaunchingIntent = isLaunchingIntent;
    }

    /**
     * 分享插件是否注入到主ClassLoader里
     * 临时方案，因为插件sdk无法访问基线的类
     *
     * @return false不注入，true维持线上逻辑
     */
    private boolean isSharePluginInjectClassLoader() {

        if (!TextUtils.equals("com.iqiyi.share", mPluginPackageName)) {
            // 不是分享插件，维持现状
            return true;
        }

        if (!mPluginPackageInfo.isClassNeedInject()) {
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
