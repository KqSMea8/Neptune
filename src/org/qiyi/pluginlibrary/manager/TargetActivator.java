package org.qiyi.pluginlibrary.manager;

import org.qiyi.pluginlibrary.api.IGetClassLoaderCallback;
import org.qiyi.pluginlibrary.api.ITargetLoadedCallBack;
import org.qiyi.pluginlibrary.pm.CMPackageInfo;
import org.qiyi.pluginlibrary.pm.CMPackageManagerImpl;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.text.TextUtils;

/**
 * 插件控制器
 */
public class TargetActivator {

    /**
     * 加载并启动插件
     *
     * @param context
     * @param intent  包含目标Component
     * @param processName 插件运行的进程名
     */
    public static void loadTargetAndRun(final Context context, final Intent intent, final String processName) {
        ProxyEnvironmentManager.enterProxy(context, null, intent, processName);
    }

    /**
     * 加载并启动插件
     *
     * @param context
     * @param conn bind service时的connection
     * @param intent 包含目标Component
     * @param processName 插件运行的进程名
     */
    public static void loadTargetAndRun(final Context context, final ServiceConnection conn,
            final Intent intent, final String processName) {
        ProxyEnvironmentManager.enterProxy(context, null, intent, processName);
    }

    /**
     * 静默加载插件，异步加载，可以设置callback
     *
     * @param context application Context
     * @param packageName 插件包名
     * @param processName 插件运行的进程名
     * @param callback 加载成功的回调
     */
    public static void loadTarget(final Context context, final String packageName, final String processName,
            final ITargetLoadedCallBack callback) {

        // 插件已经加载
        if (ProxyEnvironmentManager.isEnterProxy(packageName)) {
            callback.onTargetLoaded(packageName);
            return;
        }

        if (callback == null) {
            return;
        }

        BroadcastReceiver recv = new BroadcastReceiver() {
            public void onReceive(Context ctx, Intent intent) {
                String curPkg = intent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME);
                if (ProxyEnvironment.ACTION_TARGET_LOADED.equals(intent.getAction()) && TextUtils.equals(packageName, curPkg)) {
                    PluginDebugLog.log("plugin", "收到自定义的广播org.qiyi.pluginapp.action.TARGET_LOADED");
                    callback.onTargetLoaded(packageName);
                    context.getApplicationContext().unregisterReceiver(this);
                }
            }
        };
        PluginDebugLog.log("plugin", "注册自定义广播org.qiyi.pluginapp.action.TARGET_LOADED");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ProxyEnvironment.ACTION_TARGET_LOADED);
        context.getApplicationContext().registerReceiver(recv, filter);

        Intent intent = new Intent();
        intent.setAction(ProxyEnvironment.ACTION_TARGET_LOADED);
        intent.setComponent(new ComponentName(packageName, recv.getClass().getName()));
        ProxyEnvironmentManager.enterProxy(context, null, intent, processName);
    }

    /**
     * 获取 package 对应的 classLoader。一般情况下不需要获得插件的classloader。 只有那种纯 jar
     * sdk形式的插件，需要获取classloader。 获取过程为异步回调的方式。此函数，存在消耗ui线程100ms-200ms级别。
     * <p/>
     * 插件已全部改为运行在独立进城，先注释掉没有使用的方法
     *
     * @param context application Context
     * @param packageName 插件包名
     * @param callback 回调，classloader 通过此异步回调返回给hostapp
     */
    public static void loadAndGetClassLoader(final Context context,
                                             final String packageName,
                                             final String processName,
                                             final IGetClassLoaderCallback callback) {

        loadTarget(context, packageName, processName, new ITargetLoadedCallBack() {

            @Override
            public void onTargetLoaded(String packageName) {
                if (!TextUtils.isEmpty(packageName)) {
                    CMPackageInfo packageInfo =
                            CMPackageManagerImpl.getInstance(context).getPackageInfo(packageName);
                    if (packageInfo != null && packageInfo.pluginInfo != null) {
                        try {
                            ProxyEnvironment.initProxyEnvironment(context, packageInfo,
                                    packageInfo.pluginInfo.mPluginInstallMethod,
                                    processName);
                            ProxyEnvironment targetEnv =
                                    ProxyEnvironmentManager.getEnvByPkgName(packageName);
                            ClassLoader classLoader = targetEnv.getDexClassLoader();

                            callback.getClassLoaderCallback(classLoader);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    /**
     * 注销插件App
     *
     * @param packageName 插件包名
     */
    public static void unLoadTarget(String packageName) {
        ProxyEnvironment.exitProxy(packageName);
    }
}
