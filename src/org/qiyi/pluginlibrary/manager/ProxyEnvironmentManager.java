package org.qiyi.pluginlibrary.manager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.qiyi.pluginlibrary.ApkTargetMappingNew;
import org.qiyi.pluginlibrary.PActivityStackSupervisor;
import org.qiyi.pluginlibrary.PServiceSupervisor;
import org.qiyi.pluginlibrary.ProxyComponentMappingByProcess;
import org.qiyi.pluginlibrary.ErrorType.ErrorType;
import org.qiyi.pluginlibrary.api.ITargetLoadListener;
import org.qiyi.pluginlibrary.exception.PluginStartupException;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.pm.CMPackageInfo;
import org.qiyi.pluginlibrary.pm.CMPackageManagerImpl;
import org.qiyi.pluginlibrary.pm.PluginPackageInfoExt;
import org.qiyi.pluginlibrary.utils.ContextUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;

/**
 * 管理插件运行环境
 */
public class ProxyEnvironmentManager {
    private static final String TAG = ProxyEnvironmentManager.class.getSimpleName();

    // 控制插件启动时的progress dialog
    public static final String EXTRA_SHOW_LOADING = "plugin_show_loading";

    public static final String ACTION_QUIT = "org.qiyi.pluginapp.action.QUIT";

    static Handler sHandler = new Handler(Looper.getMainLooper());

    private static IDeliverPlug sDeliverPlug;

    private static IPluginEnvironmentStatusListener sPluginEnvStatusListener;

    // 插件包名对应Environment的Hash
    private static ConcurrentHashMap<String, ProxyEnvironment> sPluginsMap =
            new ConcurrentHashMap<String, ProxyEnvironment>();

    /**
     * 获取插件运行环境实例
     *
     * @param pkgName 插件包名
     * @return 插件环境对象
     */
    public static ProxyEnvironment getEnvByPkgName(String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            return null;
        }
        return sPluginsMap.get(pkgName);
    }

    /**
     * 是否已经建立对应插件的environment
     *
     * @param packageName 包名，已经做非空判断
     * @return true表示已经建立
     */
    public static boolean hasEnvInstance(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        return sPluginsMap.containsKey(packageName);
    }

    static void addEnvInstance(String pkgName, ProxyEnvironment env) {
        if (TextUtils.isEmpty(pkgName) || null == env) {
            return;
        }
        sPluginsMap.put(pkgName, env);
    }

    static ProxyEnvironment removeEnvInstance(String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            return null;
        }
        return sPluginsMap.remove(pkgName);
    }

    /**
     * 运行插件代理
     *
     * @param context host 的application context
     * @param intent 加载插件运行的intent
     */
    static void enterProxy(final Context context, final ServiceConnection conn, final Intent intent) {
        enterProxy(context, conn, intent, ProxyComponentMappingByProcess.getDefaultPlugProcessName());
    }

    static void enterProxy(final Context context, final ServiceConnection conn, final Intent intent,
            final String processName) {
        PluginDebugLog.runtimeLog(TAG, "enterProxy: " + intent);
        final String packageName = tryParsePkgName(context, intent);
        if (TextUtils.isEmpty(packageName)) {
            deliverPlug(context, false, context.getPackageName(), ErrorType.ERROR_CLIENT_LOAD_NO_PAKNAME);
            PluginDebugLog.runtimeLog(TAG, "enterProxy packageName is null return! packageName: " + packageName);
            return;
        }

        LinkedBlockingQueue<Intent> cacheIntents = PActivityStackSupervisor.getCachedIntent(packageName);
        if (cacheIntents != null && cacheIntents.size() > 0) {
            // 说明插件正在loading,正在loading，直接返回吧，等着loading完调起
            // 把intent都缓存起来
            cacheIntents.add(intent);
            PluginDebugLog.runtimeLog(TAG, "LoadingMap is not empty, Cache current intent, intent: " + intent);
            return;
        }

        // 判断是否已经进入到代理
        boolean isEnterProxy = isEnterProxy(packageName);
        if (!isEnterProxy) {
            if (null == cacheIntents) {
                cacheIntents = new LinkedBlockingQueue<Intent>();
                PActivityStackSupervisor.addCachedIntent(packageName, cacheIntents);
            }
            // 正在加载的插件队列
            cacheIntents.add(intent);
            PluginDebugLog.runtimeLog(TAG, "Environment is loading cache current intent, intent: " + intent);
        } else {
            // 已经初始化，直接起Intent
            ProxyEnvironment.launchIntent(context, conn, intent);
            return;
        }

        // Handle plugin dependences
        final CMPackageInfo info = CMPackageManagerImpl.getInstance(context.getApplicationContext())
                .getPackageInfo(packageName);
        if (info != null && info.pluginInfo != null && info.pluginInfo.getPluginResfs() != null
                && info.pluginInfo.getPluginResfs().size() > 0) {
            PluginDebugLog.runtimeLog(TAG,
                    "Start to check dependence installation size: " + info.pluginInfo.getPluginResfs().size());
            final AtomicInteger count = new AtomicInteger(info.pluginInfo.getPluginResfs().size());
            for (String pkgName : info.pluginInfo.getPluginResfs()) {
                PluginDebugLog.runtimeLog(TAG, "Start to check installation pkgName: " + pkgName);
                final CMPackageInfo refInfo = CMPackageManagerImpl.getInstance(context.getApplicationContext())
                        .getPackageInfo(pkgName);
                CMPackageManagerImpl.getInstance(context.getApplicationContext()).packageAction(refInfo,
                        new IInstallCallBack.Stub() {
                            @Override
                            public void onPacakgeInstalled(CMPackageInfo packageInfo) {
                                count.getAndDecrement();
                                PluginDebugLog.runtimeLog(TAG, "Check installation success pkgName: " + refInfo.packageName);
                                if (count.get() == 0) {
                                    PluginDebugLog.runtimeLog(TAG,
                                            "Start Check installation after check dependence packageName: "
                                                    + packageName);
                                    checkPkgInstallationAndLaunch(context, info, processName, conn, intent);
                                }
                            }

                            @Override
                            public void onPackageInstallFail(String pName, int failReason) throws RemoteException {
                                PluginDebugLog.runtimeLog(TAG,
                                        "Check installation failed pkgName: " + pName + " failReason: " + failReason);
                                count.set(-1);
                            }
                        });
            }
        } else {
            PluginDebugLog.runtimeLog(TAG, "Start Check installation without dependences packageName: " + packageName);
            checkPkgInstallationAndLaunch(context, info, processName, conn, intent);
        }
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
        ProxyEnvironment env = sPluginsMap.get(packageName);
        if (env != null && env.hasInit()) {
            return true;
        }

        return false;
    }

    public static Map<String, ProxyEnvironment> getAllEnv() {
        return sPluginsMap;
    }

    private static void checkPkgInstallationAndLaunch(final Context context, final CMPackageInfo packageInfo,
            final String processName, final ServiceConnection conn, final Intent intent) {
        CMPackageManagerImpl.getInstance(context.getApplicationContext()).packageAction(packageInfo,
                new IInstallCallBack.Stub() {

                    @Override
                    public void onPackageInstallFail(String packageName, int failReason) {
                        PluginDebugLog.runtimeLog(TAG, "checkPkgInstallationAndLaunch failed packageName: " + packageName
                                + " failReason: " + failReason);
                        PActivityStackSupervisor.clearLoadingIntent(packageName);
                        deliverPlug(context, false, packageName, failReason);
                    }

                    @Override
                    public void onPacakgeInstalled(CMPackageInfo info) {
                        initTarget(context.getApplicationContext(), packageInfo.packageName, processName,
                                new ITargetLoadListener() {
                                    @Override
                                    public void onLoadFinished(String packageName) {
                                        try {
                                            ProxyEnvironment.launchIntent(context, conn, intent, false);
                                            if (sPluginEnvStatusListener != null) {
                                                sPluginEnvStatusListener.onPluginEnvironmentIsReady(packageName);
                                            }
                                        } catch (Exception e) {
                                            PActivityStackSupervisor.clearLoadingIntent(packageName);
                                            ProxyEnvironment env = sPluginsMap.get(packageName);
                                            if (null != env) {
                                                env.updateLaunchingIntentFlag(false);
                                            }
                                            e.printStackTrace();
                                        }
                                    }
                                });
                    }
                });
    }

    /**
     * 加载插件
     *
     * @param context application Context
     * @param packageName 插件包名
     * @param listenner 插件加载后的回调
     */
    static void initTarget(final Context context, String packageName, String processName,
            final ITargetLoadListener listenner) {
        PluginDebugLog.runtimeLog(TAG, "initTarget");
        try {
            new InitProxyEnvironment(context, packageName, processName, listenner).start();
        } catch (Exception e) {
            PActivityStackSupervisor.clearLoadingIntent(packageName);
            deliverPlug(context, false, packageName, ErrorType.ERROR_CLIENT_LOAD_INIT_TARGET);
            e.printStackTrace();
        }
    }

    public static void updateConfiguration(Configuration config) {
        for (Map.Entry<String, ProxyEnvironment> entry : sPluginsMap.entrySet()) {
            ProxyEnvironment env = entry.getValue();
            env.updateConfiguration(config);
        }
    }

    // TODO check!!!
    public static void stopService(Intent intent) {
        if (intent == null || intent.getComponent() == null
                || TextUtils.isEmpty(intent.getComponent().getPackageName())) {
            return;
        }
        final String packageName = intent.getComponent().getPackageName();
        ProxyEnvironment env = sPluginsMap.get(packageName);
        if (env == null) {
            return;
        }

        if (env.mAppWrapper != null) {
            env.mAppWrapper.stopService(intent);
        }
    }

    public static void quit(Context context, String processName) {

        CMPackageManagerImpl.getInstance(context).exit();

        for (Map.Entry<String, ProxyEnvironment> entry : ProxyEnvironmentManager.getAllEnv().entrySet()) {
            ProxyEnvironment plugin = entry.getValue();
            if (plugin != null) {
                plugin.quitApp(true, false);
            }
        }
        PServiceSupervisor.clearConnections();
        // sAliveServices will be cleared, when on ServiceProxy destroy.

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
                PluginDebugLog.runtimeLog(TAG, "try to stop service " + proxyServiceName);
                intent.setClass(context, proxyServiceNameClass);
                intent.setAction(ACTION_QUIT);
                context.startService(intent);
            }
        }
    }

    /**
     * Helper method to get package name from intent
     *
     * @return
     */
    static String tryParsePkgName(Context context, Intent intent) {
        if (intent == null) {
            return "";
        }
        ComponentName cpn = intent.getComponent();
        if (cpn == null || TextUtils.isEmpty(cpn.getPackageName())) {
            if (context == null) {
                return "";
            }

            List<CMPackageInfo> packageList = CMPackageManagerImpl.getInstance(context).getInstalledApps();
            if (packageList != null) {
                // Here, loop all installed packages to get pkgName.
                for (CMPackageInfo info : packageList) {
                    if (info != null) {
                        ApkTargetMappingNew target = info.getTargetMapping(context);
                        if (null != target) {
                            if (target.resolveActivity(intent) != null) {
                                return info.packageName;
                            }
                        }
                    }
                }
            }
        } else {
            return cpn.getPackageName();
        }
        return "";
    }

    /**
     * @param success
     * @param pakName
     * @param errorCode 用于插件qos 投递
     */
    public static void deliverPlug(final Context context, final boolean success, final String pakName,
            final int errorCode) {
        if (Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    deliverPlugInner(context, success, pakName, errorCode);
                    return null;
                }

            }.execute();
        } else {
            deliverPlugInner(context, success, pakName, errorCode);
        }
    }

    private static void deliverPlugInner(Context context, boolean success, String pakName, int errorCode) {
        if (null != context && sDeliverPlug != null && !TextUtils.isEmpty(pakName)) {
            CMPackageInfo info = CMPackageManagerImpl.getInstance(ContextUtils.getOriginalContext(context))
                    .getPackageInfo(pakName);
            if (info != null && info.pluginInfo != null) {
                sDeliverPlug.deliverPlug(success, info.pluginInfo, errorCode);
            }
        }
    }

    public static void setiDeliverPlug(IDeliverPlug deliverPlug) {
        sDeliverPlug = deliverPlug;
    }

    public static void setPluginEnvironmentStatusListener(IPluginEnvironmentStatusListener listener) {
        sPluginEnvStatusListener = listener;
    }

    public interface IDeliverPlug {
        void deliverPlug(boolean success, PluginPackageInfoExt pkgInfo, int errorCode);
    }

    public interface IPluginEnvironmentStatusListener {
        void onPluginEnvironmentIsReady(String packageName);
    }

    static class InitProxyEnvironment extends Thread {
        public String pakName;
        public Context pContext;
        ITargetLoadListener listener;
        String mProcessName;

        public InitProxyEnvironment(Context context, String pakName, String processName, ITargetLoadListener listener) {
            this.pakName = pakName;
            this.pContext = context;
            this.listener = listener;
            mProcessName = processName;
        }

        @Override
        public void run() {
            super.run();
            try {
                CMPackageInfo packageInfo =
                        CMPackageManagerImpl.getInstance(pContext).getPackageInfo(pakName);
                if (packageInfo != null && packageInfo.pluginInfo != null) {
                    String installMethod = packageInfo.pluginInfo.mPluginInstallMethod;
                    PluginDebugLog.runtimeLog("plugin",
                            "doInBackground:" + pakName + ", installMethod: " +
                                    installMethod + " ProcessName: " + mProcessName);
                    ProxyEnvironment.initProxyEnvironment(
                            pContext, packageInfo, installMethod, mProcessName);
                    new InitHandler(listener, pakName, Looper.getMainLooper()).sendEmptyMessage(1);
                } else {
                    PluginDebugLog.runtimeLog("plugin", "packageInfo is null before initProxyEnvironment");
                }
            } catch (Exception e) {
                if (e instanceof PluginStartupException) {
                    PluginStartupException ex = (PluginStartupException) e;
                    ProxyEnvironmentManager.deliverPlug(pContext, false, pakName, ex.getCode());
                } else {
                    ProxyEnvironmentManager.deliverPlug(pContext, false, pakName,
                            ErrorType.ERROR_CLIENT_LOAD_INIT_ENVIRONMENT_FAIL);
                }
            }
        }
    }

    static class InitHandler extends Handler {
        ITargetLoadListener listener;
        String pakName;

        public InitHandler(ITargetLoadListener listenner, String pakName, Looper looper) {
            super(looper);
            this.listener = listenner;
            this.pakName = pakName;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case 1:
                listener.onLoadFinished(pakName);
                break;

            default:
                break;
            }
            super.handleMessage(msg);
        }
    }
}
