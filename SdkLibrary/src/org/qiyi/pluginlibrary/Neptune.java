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
import org.qiyi.pluginlibrary.pm.PluginPackageManagerNative;
import org.qiyi.pluginlibrary.runtime.PluginManager;
import org.qiyi.pluginlibrary.utils.ContextUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.ReflectionUtils;
import org.qiyi.pluginlibrary.utils.VersionUtils;

import java.io.File;

/**
 * Neptune对外暴露的统一调用类
 *
 * author: liuchun
 * date: 2018/6/4
 */
public class Neptune {
    private static final String TAG = "Neptune";

    public static final boolean SEPARATED_CLASSLOADER = true;
    public static final boolean NEW_RESOURCE_CREATOR = true;
    public static final boolean NEW_COMPONENT_PARSER = true;

    private static Context sHostContext;

    private static NeptuneConfig sGlobalConfig;

    private static Instrumentation mHostInstr;
    /**
     * 初始化HybirdPlugin环境
     *
     * @param app
     * @param config
     */
    public static void init(Application app, NeptuneConfig config) {

        sHostContext = app;
        sGlobalConfig = config != null ? config
                : new NeptuneConfig.NeptuneConfigBuilder().build();

        boolean hookInstr = VersionUtils.hasPie() || sGlobalConfig.getSdkMode() != NeptuneConfig.LEGACY_MODE;
        if (hookInstr) {
            hookInstrumentation();
        }

        // 调用getInstance()方法会初始化bindService
        PluginPackageManagerNative.getInstance(app).setPackageInfoManager(sGlobalConfig.getPluginInfoProvider());
        // 注册卸载监听广播
        PluginManager.registerUninstallReceiver(app);
    }

    public static Context getHostContext() {
        return sHostContext;
    }

    public static NeptuneConfig getConfig() {
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
            String hostInstrName = hostInstr.getClass().getName();
            PluginDebugLog.runtimeLog(TAG, "host Instrument name: " + hostInstrName);

            if (hostInstrName.startsWith("com.chaozhuo.superme")
               || hostInstrName.startsWith("com.lody.virtual")) {
                // warning: 特殊case，VirtualApp环境，暂不Hook
                PluginDebugLog.runtimeLog(TAG, "reject hook instrument, run in VirtualApp Environment");
            } else if (hostInstr instanceof PluginHookedInstrument) {
                // already hooked
                PluginDebugLog.runtimeLog(TAG, "ActivityThread Instrumentation already hooked");
            } else {
                PluginInstrument pluginInstrument = new PluginHookedInstrument(hostInstr);
                ReflectionUtils.on(activityThread).set("mInstrumentation", pluginInstrument);
                PluginDebugLog.runtimeLog(TAG, "init hook ActivityThread Instrumentation success");
            }
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
     * @param context
     * @param apkPath
     */
    public static void install(Context context, String apkPath) {
        install(context, apkPath, null);
    }

    /**
     * 安装sd上的插件
     *
     * @param context
     * @param apkPath
     * @param callBack
     */
    public static void install(Context context, String apkPath, IInstallCallBack callBack) {

        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            return;
        }

        PluginLiteInfo liteInfo = new PluginLiteInfo();
        Context mContext = ensureContext(context);
        PackageInfo packageInfo = mContext.getPackageManager()
                .getPackageArchiveInfo(apkPath, 0);
        if (packageInfo != null) {
            liteInfo.mPath = apkPath;
            liteInfo.packageName = packageInfo.packageName;
            liteInfo.pluginVersion = packageInfo.versionName;
            install(mContext, liteInfo, callBack);
        }
    }

    /**
     * 安装一个插件
     * @param context
     * @param info
     * @param callBack
     */
    public static void install(Context context, PluginLiteInfo info, IInstallCallBack callBack) {
        // install
        Context mContext = ensureContext(context);
        PluginPackageManagerNative.getInstance(mContext).install(info, callBack);
    }


    /**
     * 根据包名卸载一个插件
     * @param context
     * @param pkgName
     */
    public static void uninstall(Context context, String pkgName) {
        uninstall(context, pkgName, null);
    }


    /**
     * 根据包名卸载一个插件
     * @param context
     * @param pkgName
     * @param callBack
     */
    public static void uninstall(Context context, String pkgName, IPluginUninstallCallBack callBack) {
        Context mContext = ensureContext(context);
        PluginLiteInfo info = PluginPackageManagerNative.getInstance(mContext).getPackageInfo(pkgName);
        if (info != null) {
            uninstall(mContext, info, callBack);
        }
    }

    /**
     * 卸载一个插件
     * @param context
     * @param info
     * @param callBack
     */
    public static void uninstall(Context context, PluginLiteInfo info, IPluginUninstallCallBack callBack) {
        // uninstall
        Context mContext = ensureContext(context);
        PluginPackageManagerNative.getInstance(mContext).uninstall(info, callBack);
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
        // start plugin, 默认选择进程
        PluginManager.launchPlugin(mHostContext, intent, null);
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
     * @param context
     * @param pkgName
     * @return
     */
    public static boolean isPackageInstalled(Context context, String pkgName) {

        Context mContext = ensureContext(context);
        return PluginPackageManagerNative.getInstance(mContext).isPackageInstalled(pkgName);
    }

    /**
     * 判断插件是否可用
     * @param context
     * @param pkgName
     * @return
     */
    public static boolean isPackageAvailable(Context context, String pkgName) {

        Context mContext = ensureContext(context);
        return PluginPackageManagerNative.getInstance(mContext).isPackageAvailable(pkgName);
    }

    /**
     * 获取插件PluginLiteInfo
     *
     * @param context
     * @param pkgName
     * @return
     */
    public static PluginLiteInfo getPluginInfo(Context context, String pkgName) {

        Context mContext = ensureContext(context);
        return PluginPackageManagerNative.getInstance(mContext).getPackageInfo(pkgName);
    }



    private static Context ensureContext(Context originContext) {
        if (originContext != null) {
            return originContext;
        }
        return sHostContext;
    }
}
