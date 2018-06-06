package org.qiyi.pluginlibrary;

import android.app.ActivityThread;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;

import org.qiyi.pluginlibrary.component.wraper.PluginHookedInstrument;
import org.qiyi.pluginlibrary.component.wraper.PluginInstrument;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.pm.IPluginUninstallCallBack;
import org.qiyi.pluginlibrary.pm.PluginLiteInfo;
import org.qiyi.pluginlibrary.pm.PluginPackageManager;
import org.qiyi.pluginlibrary.pm.PluginPackageManagerNative;
import org.qiyi.pluginlibrary.runtime.PluginLoadedApk;
import org.qiyi.pluginlibrary.runtime.PluginManager;
import org.qiyi.pluginlibrary.utils.ContextUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.ReflectionUtils;

import java.io.File;

/**
 * HybirdPlugin对外暴露的统一调用类
 *
 * author: liuchun
 * date: 2018/6/4
 */
public class HybirdPlugin {
    private static final String TAG = "HybirdPlugin";

    private static Context sAppContext;

    private static HybirdPluginConfig sGlobalConfig;

    private static Instrumentation mHostInstr;
    /**
     * 初始化HybirdPlugin环境
     *
     * @param app
     * @param config
     */
    public static void init(Application app, HybirdPluginConfig config) {

        sAppContext = app;
        sGlobalConfig = config != null ? config
                : new HybirdPluginConfig.HybirdPluginConfigBuilder().build();

        boolean hookInstr = ContextUtils.isAndroidP() || sGlobalConfig.getSdkMode() == 1;
        if (hookInstr) {
            hookInstrumentation();
        }

        PluginLoadedApk.sUseNewClassLoader = sGlobalConfig.shouldUseNewCLMode();
        PluginLoadedApk.sUseNewResCreator = sGlobalConfig.shouldUseNewResGen();

        PluginPackageManager.setVerifyPluginInfoImpl(sGlobalConfig.getVerifyPluginInfo());
    }


    public static HybirdPluginConfig getConfig() {
        return sGlobalConfig;
    }


    /**
     * 反射替换ActivityThread的mInstrumentation
     */
    private static void hookInstrumentation() {

        PluginDebugLog.runtimeLog(TAG, "need to hook Instrumentation for plugin framework");
        ActivityThread activityThread = ActivityThread.currentActivityThread();
        Instrumentation hostInstr = getHostInstrumentation();

        if (hostInstr != null) {
            PluginInstrument pluginInstrument = new PluginHookedInstrument(hostInstr);
            ReflectionUtils.on(activityThread).set("mInstrumentation", pluginInstrument);
            PluginDebugLog.runtimeLog(TAG, "init hook ActivityThread Instrumentation success");
        } else {
            PluginDebugLog.runtimeLog(TAG, "init hook ActivityThread Instrumentation failed, hostInstr==null");
        }

//        Object activityThread = getActivityThread();
//        Instrumentation hostInstr;
//        try {
//            hostInstr = ReflectionUtils.on(activityThread).call("getInstrumentation").get();
//        } catch (ReflectException e) {
//            hostInstr = ReflectionUtils.on(activityThread).get("mInstrumentation");
//        }
//
//        if (hostInstr != null) {
//            PluginInstrument pluginInstrument = new PluginHookedInstrument(hostInstr);
//            ReflectionUtils.on(activityThread).set("mInstrumentation", pluginInstrument);
//            PluginDebugLog.runtimeLog(TAG, "init hook ActivityThread Instrumentation success");
//        } else {
//            PluginDebugLog.runtimeLog(TAG, "init hook ActivityThread Instrumentation failed");
//        }
    }

    /**
     * 获取ActivityThread的Instrumentation对象
     *
     * @return
     */
    public static Instrumentation getHostInstrumentation() {

        if (mHostInstr == null) {
            ActivityThread activityThread = ActivityThread.currentActivityThread();
            Instrumentation hostInstr = activityThread.getInstrumentation();
            mHostInstr = PluginInstrument.unwrap(hostInstr);
        }

        return mHostInstr;
    }

    /**
     * 反射获取ActivityThread对象
     */
    private static Object getActivityThread() {
        ReflectionUtils ref = ReflectionUtils.on("android.app.ActivityThread");
        Object obj = null;
        try {
            obj = ref.call("currentActivityThread").get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (obj == null) {
            obj = ref.get("sCurrentActivityThread");
        }
        if (obj == null) {
            obj = ((ThreadLocal<?>)ref.get("sThreadLocal")).get();
        }
        return obj;
    }

    /**
     * 安装sd卡上的插件
     *
     * @param apkPath
     */
    public static void install(String apkPath) {

        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            return;
        }

        PluginLiteInfo liteInfo = new PluginLiteInfo();
        PackageInfo packageInfo = sAppContext.getPackageManager()
                .getPackageArchiveInfo(apkPath, 0);
        if (packageInfo != null) {
            liteInfo.mPath = apkPath;
            liteInfo.packageName = packageInfo.packageName;
            liteInfo.pluginVersion = packageInfo.versionName;
            install(liteInfo, null);
        }
    }

    /**
     * 安装一个插件
     * @param info
     * @param callBack
     */
    public static void install(PluginLiteInfo info, IInstallCallBack callBack) {
        // install
        PluginPackageManagerNative.getInstance(sAppContext).install(info, callBack);
    }


    /**
     * 根据包名卸载一个插件
     * @param pkgName
     */
    public static void uninstall(String pkgName) {
        PluginLiteInfo info = PluginPackageManagerNative.getInstance(sAppContext).getPackageInfo(pkgName);
        if (info != null) {
            uninstall(info, null);
        }
    }

    /**
     * 卸载一个插件
     * @param info
     * @param callBack
     */
    public static void uninstall(PluginLiteInfo info, IPluginUninstallCallBack callBack) {
        // uninstall
        PluginPackageManagerNative.getInstance(sAppContext).uninstall(info, callBack);
    }

    /**
     * 启动一个插件的入口类
     *
     * @param mHostContext
     * @param pkgName
     */
    public static void launchPlugin(Context mHostContext, String pkgName) {
        // start plugin
        PluginManager.launchPlugin(mHostContext, pkgName);
    }

    /**
     * 根据Intent启动一个插件
     *
     * @param mHostContext
     * @param intent
     */
    public static void launchPlugin(Context mHostContext, Intent intent) {
        // start plugin, 默认主进程
        PluginManager.launchPlugin(mHostContext, intent, mHostContext.getPackageName());
    }

    /**
     * 根据Intent启动一个插件，指定运行进程的名称
     *
     * @param mHostContext
     * @param intent
     * @param processName
     */
    public static void launchPlugin(Context mHostContext, Intent intent, String processName) {
        // start plugin, 指定进程
        PluginManager.launchPlugin(mHostContext, intent, processName);
    }

    /**
     * 判断插件是否安装
     *
     * @param pkgName
     * @return
     */
    public static boolean isPackageInstalled(String pkgName) {

        return PluginPackageManagerNative.getInstance(sAppContext).isPackageInstalled(pkgName);
    }

    /**
     * 判断插件是否可用
     *
     * @param pkgName
     * @return
     */
    public static boolean isPackageAvailable(String pkgName) {

        return PluginPackageManagerNative.getInstance(sAppContext).isPackageAvailable(pkgName);
    }

    /**
     * 获取插件PluginLiteInfo
     *
     * @param pkgName
     * @return
     */
    public static PluginLiteInfo getPluginInfo(String pkgName) {

        return PluginPackageManagerNative.getInstance(sAppContext).getPackageInfo(pkgName);
    }
}
