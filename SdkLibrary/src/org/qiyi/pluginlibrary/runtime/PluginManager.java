package org.qiyi.pluginlibrary.runtime;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.pm.PluginPackageInfo;
import org.qiyi.pluginlibrary.error.ErrorType;
import org.qiyi.pluginlibrary.component.stackmgr.PActivityStackSupervisor;
import org.qiyi.pluginlibrary.component.stackmgr.PServiceSupervisor;
import org.qiyi.pluginlibrary.context.PluginContextWrapper;
import org.qiyi.pluginlibrary.listenter.IPluginInitListener;
import org.qiyi.pluginlibrary.listenter.IPluginLoadListener;
import org.qiyi.pluginlibrary.constant.IIntentConstant;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.pm.PluginLiteInfo;
import org.qiyi.pluginlibrary.pm.PluginPackageManager;
import org.qiyi.pluginlibrary.pm.PluginPackageManagerNative;
import org.qiyi.pluginlibrary.utils.ComponetFinder;
import org.qiyi.pluginlibrary.utils.ContextUtils;
import org.qiyi.pluginlibrary.utils.ErrorUtil;
import org.qiyi.pluginlibrary.utils.IntentUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 管理所有插件的运行状态
 * Author:yuanzeyao
 * Date:2017/7/3 18:30
 * Email:yuanzeyao@qiyi.com
 */

public class PluginManager implements IIntentConstant {
    public static final String TAG = "PluginManager";
    /**
     * 已经加载到内存了的插件集合
     */
    private static ConcurrentHashMap<String, PluginLoadedApk> sPluginsMap =
            new ConcurrentHashMap<>();
    /**
     * 异步加载插件线程池
     */
    private static Executor mExecutor = Executors.newCachedThreadPool();
    /**
     * 插件加载线程和主线程通信
     */
    private static Handler mHandler = new Handler(Looper.getMainLooper());
    /**
     * 插件状态投递
     */
    private static IDeliverInterface mDeliver;
    /**
     * 插件状态监听器
     */
    private static IPluginStatusListener sPluginStatusListener;
    /**
     * 处理插件退出时的善后逻辑
     */
    private static IAppExitStuff sExitStuff;

    /**
     * 通过包名获取{@link PluginLoadedApk}对象
     * 注意:此方法仅仅到{@link #sPluginsMap}中查找，并不会去加载插件
     *
     * @param pkgName 插件的包名
     * @return 返回包名为mPluginPackage 指定的插件的PluginLoadedApk对象，如果插件没有加载，则返回Null
     */
    public static PluginLoadedApk getPluginLoadedApkByPkgName(String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            return null;
        }
        return sPluginsMap.get(pkgName);
    }

    /**
     * 通过ClassLoader查找{@link PluginLoadedApk}对象
     *
     * @param classLoader 插件的ClassLoader
     * @return
     */
    public static PluginLoadedApk findPluginLoadedApkByClassLoader(ClassLoader classLoader) {
        for (PluginLoadedApk loadedApk : sPluginsMap.values()) {
            if (loadedApk != null && loadedApk.getPluginClassLoader() == classLoader) {
                return loadedApk;
            }
        }
        return null;
    }

    /**
     * 判断插件是否已经加载
     *
     * @param mPluginPackage 插件的包名
     * @return true :插件已经加载，false:插件没有加载
     */
    public static boolean isPluginLoaded(String mPluginPackage) {
        return getPluginLoadedApkByPkgName(mPluginPackage) != null;
    }


    /**
     * 保存已经加载成功的{@link PluginLoadedApk}
     *
     * @param mPluginPackage   插件包名
     * @param mPluginLoadedApk 插件的内存实例对象
     */
    private static void addPluginLoadedApk(String mPluginPackage, PluginLoadedApk mPluginLoadedApk) {
        if (TextUtils.isEmpty(mPluginPackage)
                || mPluginLoadedApk == null) {
            return;
        }

        sPluginsMap.put(mPluginPackage, mPluginLoadedApk);
    }

    /**
     * 移除已经加载成功的{@link PluginLoadedApk}
     *
     * @param mPluginPackage 需要被移除的插件包名
     * @return 被移除的插件内存对象
     */
    public static PluginLoadedApk removePluginLoadedApk(String mPluginPackage) {
        if (TextUtils.isEmpty(mPluginPackage)) {
            return null;
        }
        return sPluginsMap.remove(mPluginPackage);
    }

    /**
     * 获取所有已经加载的插件
     *
     * @return 以Map的形式返回加载成功的插件，此Map是不可变的
     */
    public static Map<String, PluginLoadedApk> getAllPluginLoadedApk() {
        return Collections.unmodifiableMap(sPluginsMap);
    }

    /**
     * 启动插件
     *
     * @param mHostContext
     * @param packageName  插件包名
     */
    public static void launchPlugin(Context mHostContext, String packageName) {
        if (mHostContext == null && TextUtils.isEmpty(packageName)) {
            PluginDebugLog.runtimeLog(TAG, "launchPlugin mHostContext is null or packageName is null!");
            return;
        }

        ComponentName mComponentName = new ComponentName(packageName, "");
        Intent mIntent = new Intent();
        mIntent.setComponent(mComponentName);
        launchPlugin(mHostContext, mIntent, null);
    }

    /**
     * 启动插件
     *
     * @param mHostContext 主工程的上下文
     * @param mIntent      需要启动的组件的Intent
     * @param mProcessName 需要启动的插件运行的进程名称,插件方可以在Application的android:process指定
     *                     如果没有指定，则有插件中心分配
     */
    public static void launchPlugin(final Context mHostContext,
                                    Intent mIntent,
                                    String mProcessName) {
        launchPlugin(mHostContext, mIntent, null, mProcessName);
    }

    /**
     * 启动插件
     *
     * @param mHostContext       主工程的上下文
     * @param mIntent            需要启动的组件的Intent
     * @param mServiceConnection bindService时需要的ServiceConnection,如果不是bindService的方式启动组件，传入Null
     * @param mProcessName       需要启动的插件运行的进程名称,插件方可以在Application的android:process指定
     *                           如果没有指定，则有插件中心分配
     */
    public static void launchPlugin(final Context mHostContext,
                                    final Intent mIntent,
                                    final ServiceConnection mServiceConnection,
                                    final String mProcessName) {
        final String packageName = tryParsePkgName(mHostContext, mIntent);
        if (TextUtils.isEmpty(packageName)) {
            if (null != mHostContext) {
                deliver(mHostContext, false, mHostContext.getPackageName(), ErrorType.ERROR_CLIENT_LOAD_NO_PAKNAME);
            }
            PluginDebugLog.runtimeLog(TAG, "enterProxy packageName is null return! packageName: " + packageName);
            return;
        }

        LinkedBlockingQueue<Intent> cacheIntents = PActivityStackSupervisor.getCachedIntent(packageName);
        if (cacheIntents != null && cacheIntents.size() > 0) {
            cacheIntents.add(mIntent);
            PluginDebugLog.runtimeLog(TAG, "LoadingMap is not empty, Cache current intent, intent: " + mIntent + ", packageName: " + packageName);
            return;
        }

        boolean isLoadAndInit = isPluginLoadedAndInit(packageName);
        if (!isLoadAndInit) {
            if (null == cacheIntents) {
                cacheIntents = new LinkedBlockingQueue<Intent>();
                PActivityStackSupervisor.addCachedIntent(packageName, cacheIntents);
            }
            // 缓存这个intent，等待PluginLoadedApk加载到内存之后再启动这个Intent
            PluginDebugLog.runtimeLog(TAG, "Environment is initializing and loading, cache current intent first, intent: " + mIntent);
            cacheIntents.add(mIntent);
        } else {
            PluginDebugLog.runtimeLog(TAG, "Environment is already ready, launch current intent directly: " + mIntent);
            //可以直接启动组件
            readyToStartSpecifyPlugin(mHostContext, mServiceConnection, mIntent, true);
            return;
        }

        // 处理插件的依赖关系
        final PluginLiteInfo info = PluginPackageManagerNative.getInstance(mHostContext.getApplicationContext())
                .getPackageInfo(packageName);
        final List<String> mPluginRefs = PluginPackageManagerNative.getInstance(mHostContext)
                .getPluginRefs(packageName);
        if (info != null && mPluginRefs != null
                && mPluginRefs.size() > 0) {
            PluginDebugLog.runtimeLog(TAG,
                    "start to check dependence installation size: " + mPluginRefs.size());
            final AtomicInteger count = new AtomicInteger(mPluginRefs.size());
            for (String pkgName : mPluginRefs) {
                PluginDebugLog.runtimeLog(TAG, "start to check installation pkgName: " + pkgName);
                final PluginLiteInfo refInfo = PluginPackageManagerNative.getInstance(mHostContext.getApplicationContext())
                        .getPackageInfo(pkgName);
                PluginPackageManagerNative.getInstance(mHostContext.getApplicationContext()).packageAction(refInfo,
                        new IInstallCallBack.Stub() {
                            @Override
                            public void onPackageInstalled(PluginLiteInfo packageInfo) {
                                count.getAndDecrement();
                                PluginDebugLog.runtimeLog(TAG, "check installation success pkgName: " + refInfo.packageName);
                                if (count.get() == 0) {
                                    PluginDebugLog.runtimeLog(TAG,
                                            "start Check installation after check dependence packageName: "
                                                    + packageName);
                                    checkPkgInstallationAndLaunch(mHostContext, info, mServiceConnection, mIntent, mProcessName);
                                }
                            }

                            @Override
                            public void onPackageInstallFail(PluginLiteInfo info, int failReason) throws RemoteException {
                                PluginDebugLog.runtimeLog(TAG,
                                        "check installation failed pkgName: " + info.packageName + " failReason: " + failReason);
                                count.set(-1);
                            }
                        });
            }
        } else if (info != null) {
            PluginDebugLog.runtimeLog(TAG, "start Check installation without dependence packageName: " + packageName);
            checkPkgInstallationAndLaunch(mHostContext, info, mServiceConnection, mIntent, mProcessName);
        } else {
            PluginDebugLog.runtimeLog(TAG, "pluginLiteInfo is null packageName: " + packageName);
            PActivityStackSupervisor.clearLoadingIntent(packageName);
            if (PluginDebugLog.isDebug()) {
                throw new IllegalStateException("pluginLiteInfo is null when launchPlugin " + packageName);
            }
        }
    }


    /**
     * @param mHostContext
     * @param packageName
     * @param processName
     * @param mListener
     * @deprecated 异步初始化插件（宿主静默加载插件,遗留逻辑，不建议使用）
     */
    @Deprecated
    public static void initPluginAsync(final Context mHostContext,
                                       final String packageName,
                                       final String processName,
                                       final IPluginInitListener mListener) {
        // 插件已经加载
        if (PluginManager.isPluginLoadedAndInit(packageName)) {
            if (mListener != null) {
                mListener.onInitFinished(packageName);
            }
            return;
        }

        BroadcastReceiver recv = new BroadcastReceiver() {
            public void onReceive(Context ctx, Intent intent) {
                String curPkg = IntentUtils.getTargetPackage(intent);
                if (IIntentConstant.ACTION_PLUGIN_INIT.equals(intent.getAction()) && TextUtils.equals(packageName, curPkg)) {
                    PluginDebugLog.runtimeLog(TAG, "收到自定义的广播org.qiyi.pluginapp.action.TARGET_LOADED");
                    if (mListener != null) {
                        mListener.onInitFinished(packageName);
                    }
                    mHostContext.getApplicationContext().unregisterReceiver(this);
                }
            }
        };
        PluginDebugLog.runtimeLog(TAG, "注册自定义广播org.qiyi.pluginapp.action.TARGET_LOADED");
        IntentFilter filter = new IntentFilter();
        filter.addAction(IIntentConstant.ACTION_PLUGIN_INIT);
        mHostContext.getApplicationContext().registerReceiver(recv, filter);

        Intent intent = new Intent();
        intent.setAction(IIntentConstant.ACTION_PLUGIN_INIT);
        intent.setComponent(new ComponentName(packageName, recv.getClass().getName()));
        launchPlugin(mHostContext, intent, processName);
    }


    /**
     * 准备启动指定插件组件
     *
     * @param mContext      主工程Context
     * @param mConnection  bindService时需要的ServiceConnection,如果不是bindService的方式启动组件，传入Null
     * @param mIntent      需要启动组件的Intent
     * @param needAddCache 是否需要缓存Intnet,true:如果插件没有初始化，那么会缓存起来，等插件加载完毕再执行此Intent
     *                     false:如果插件没初始化，则直接抛弃此Intent
     */
    public static boolean readyToStartSpecifyPlugin(Context mContext,
                                                    ServiceConnection mConnection,
                                                    Intent mIntent,
                                                    boolean needAddCache) {
        PluginDebugLog.runtimeLog(TAG, "launchIntent: " + mIntent);
        String packageName = tryParsePkgName(mContext, mIntent);
        PluginLoadedApk mLoadedApk = getPluginLoadedApkByPkgName(packageName);
        if (mLoadedApk == null) {
            deliver(mContext, false, packageName, ErrorType.ERROR_CLIENT_NOT_LOAD);
            PluginDebugLog.runtimeLog(TAG, packageName + " launchIntent env is null! Just return!");
            PActivityStackSupervisor.clearLoadingIntent(packageName);
            return false;
        }

        if (!mLoadedApk.makeApplication()) {
            PluginDebugLog.log(TAG, "makeApplication fail:%s", packageName);
            return false;
        }

        LinkedBlockingQueue<Intent> cacheIntents =
                PActivityStackSupervisor.getCachedIntent(packageName);
        if (cacheIntents == null) {
            cacheIntents = new LinkedBlockingQueue<Intent>();
            PActivityStackSupervisor.addCachedIntent(packageName, cacheIntents);
        }
        // 避免重复添加到队列中，尤其是第一次初始化时在enterProxy中已经添加了一次
        if (!cacheIntents.contains(mIntent) && needAddCache) {
            PluginDebugLog.runtimeLog(TAG, "launchIntent add to cacheIntent....");
            cacheIntents.offer(mIntent);
        } else {
            PluginDebugLog.runtimeLog(TAG, "launchIntent not add to cacheIntent....needAddCache:" + needAddCache);
        }

        PluginDebugLog.runtimeLog(TAG, "launchIntent_cacheIntents: " + cacheIntents);
        Intent intent = cacheIntents.poll();
        if (!mLoadedApk.hasLaunchIngIntent() && intent != null) {
            doRealLaunch(mContext, mLoadedApk, intent, mConnection);
            PluginDebugLog.runtimeLog(TAG, "launchIntent no launching intnet... and launch end!");
        } else {
            PluginDebugLog.runtimeLog(TAG, "launchIntent has launching intent.... so return directly!");
        }
        return false;
    }

    /**
     * 更新所有插件的资源配置
     * 使用Application的callback实现
     *
     * @param config
     */
    @Deprecated
    public static void updateConfiguration(Configuration config) {
        for (Map.Entry<String, PluginLoadedApk> entry : sPluginsMap.entrySet()) {
            PluginLoadedApk mLoadedApk = entry.getValue();
            mLoadedApk.updateConfiguration(config);
        }
    }

    /**
     * 真正启动一个组件
     *
     * @param mHostContext 主工程Context
     * @param mLoadedApk   需要启动的插件的PluginLoadedApk
     * @param mIntent      需要启动组件的Intent
     * @param mConnection  bindService时需要的ServiceConnection,如果不是bindService的方式启动组件，传入Null
     */
    private static void doRealLaunch(Context mHostContext,
                                     PluginLoadedApk mLoadedApk,
                                     Intent mIntent,
                                     ServiceConnection mConnection) {

        String targetClassName = "";
        ComponentName mComponent = mIntent.getComponent();
        if (mComponent != null) {
            //显式启动
            targetClassName = mComponent.getClassName();
            PluginDebugLog.runtimeLog(TAG, "launchIntent_targetClassName:" + targetClassName);
            if (TextUtils.isEmpty(targetClassName)) {
                targetClassName = mLoadedApk.getPluginPackageInfo().getDefaultActivityName();
            }
        }

        Class<?> targetClass = null;
        if (!TextUtils.isEmpty(targetClassName)) {
            try {
                targetClass = mLoadedApk.getPluginClassLoader().loadClass(targetClassName);
            } catch (Exception e) {
                deliver(mHostContext, false,
                        mLoadedApk.getPluginPackageName(), ErrorType.ERROR_CLIENT_LOAD_START);
                PluginDebugLog.runtimeLog(TAG, "launchIntent loadClass failed for targetClassName: "
                        + targetClassName);
                executeNext(mLoadedApk, mConnection, mHostContext);
                return;
            }
        }

        String action = mIntent.getAction();
        if (TextUtils.equals(action, IIntentConstant.ACTION_PLUGIN_INIT)
                || TextUtils.equals(targetClassName, EXTRA_VALUE_LOADTARGET_STUB)) {
            PluginDebugLog.runtimeLog(TAG, "launchIntent load target stub!");
            //通知插件初始化完毕
            if (targetClass != null && BroadcastReceiver.class.isAssignableFrom(targetClass)) {
                Intent newIntent = new Intent(mIntent);
                newIntent.setComponent(null);
                newIntent.putExtra(IIntentConstant.EXTRA_TARGET_PACKAGE_KEY,
                        mLoadedApk.getPluginPackageName());
                newIntent.setPackage(mHostContext.getPackageName());
                mHostContext.sendBroadcast(newIntent);
            }
            // 表示后台加载，不需要处理该Intent
            executeNext(mLoadedApk, mConnection, mHostContext);
            return;
        }

        mLoadedApk.changeLaunchingIntentStatus(true);
        PluginDebugLog.runtimeLog(TAG, "launchIntent_targetClass: " + targetClass);
        if (targetClass != null && Service.class.isAssignableFrom(targetClass)) {
            //处理的是Service, 宿主启动插件Service只能通过显式启动
            ComponetFinder.switchToServiceProxy(mLoadedApk, mIntent, targetClassName);
            if (mConnection == null) {
                mHostContext.startService(mIntent);
            } else {
                mHostContext.bindService(mIntent, mConnection,
                        mIntent.getIntExtra(BIND_SERVICE_FLAGS, Context.BIND_AUTO_CREATE));
            }
        } else {
            //处理的是Activity
            ComponetFinder.switchToActivityProxy(mLoadedApk.getPluginPackageName(),
                    mIntent, -1, mHostContext);
            PActivityStackSupervisor.addLoadingIntent(mLoadedApk.getPluginPackageName(), mIntent);
            Context lastActivity = null;
            PActivityStackSupervisor mActivityStackSupervisor =
                    mLoadedApk.getActivityStackSupervisor();
            lastActivity = mActivityStackSupervisor.getAvailableActivity();
//            if (mActivityStackSupervisor != null && !mActivityStackSupervisor.getActivityStack().isEmpty()) {
//                lastActivity = mActivityStackSupervisor.getActivityStack().getLast();
//            }
            if (!(mHostContext instanceof Activity) && null != lastActivity) {
                int flag = mIntent.getFlags();
                flag = flag ^ Intent.FLAG_ACTIVITY_NEW_TASK;
                mIntent.setFlags(flag);
                lastActivity.startActivity(mIntent);
            } else {
                mHostContext.startActivity(mIntent);
            }
        }
        executeNext(mLoadedApk, mConnection, mHostContext);
    }

    /**
     * 启动队列中下一个Intent代表的组件
     *
     * @param mLoadedApk  当前要启动的组件的插件实例
     * @param mConnection bindService时需要的ServiceConnection,如果不是bindService的方式启动组件，传入Null
     * @param mContext    主进程的Context
     */
    private static void executeNext(final PluginLoadedApk mLoadedApk,
                                    final ServiceConnection mConnection,
                                    final Context mContext) {
        Message msg = Message.obtain(mHandler, new Runnable() {

            @Override
            public void run() {
                LinkedBlockingQueue<Intent> cacheIntents =
                        PActivityStackSupervisor.getCachedIntent(mLoadedApk.getPluginPackageName());
                PluginDebugLog.runtimeLog(TAG, "executeNext cacheIntents: " + cacheIntents);
                if (null != cacheIntents) {
                    Intent toBeStart = cacheIntents.poll();
                    if (toBeStart != null) {
                        doRealLaunch(mContext, mLoadedApk, toBeStart, mConnection);
                        return;
                    }
                }
                mLoadedApk.changeLaunchingIntentStatus(false);
            }
        });
        mHandler.sendMessage(msg);
    }

    /**
     * 判断插件是否已经初始化
     *
     * @param mPluginPackage 需要判断的插件的包名
     * @return true:已经初始化，false:没有初始化
     */
    public static boolean isPluginLoadedAndInit(String mPluginPackage) {

        PluginLoadedApk mPlugin = getPluginLoadedApkByPkgName(mPluginPackage);
        return mPlugin != null && mPlugin.hasPluginInit();
    }

    /**
     * 检查插件是否安装，如果安装则启动插件
     *
     * @param mHostContext       主进程Context
     * @param packageInfo        插件的详细信息
     * @param mServiceConnection bindService时需要的ServiceConnection,如果不是bindService的方式启动组件，传入Null
     * @param mIntent            启动组件的Intent
     */
    private static void checkPkgInstallationAndLaunch(final Context mHostContext,
                                                      final PluginLiteInfo packageInfo,
                                                      final ServiceConnection mServiceConnection,
                                                      final Intent mIntent,
                                                      final String mProcessName) {

        PluginPackageManagerNative.getInstance(mHostContext.getApplicationContext()).packageAction(packageInfo,
                new IInstallCallBack.Stub() {
                    @Override
                    public void onPackageInstalled(PluginLiteInfo info) {
                        //install done ,load async
                        PluginDebugLog.runtimeLog(TAG,
                                "checkPkgInstallationAndLaunch installed packageName: " + info.packageName);
                        loadPluginAsync(mHostContext.getApplicationContext(), packageInfo.packageName,
                                new IPluginLoadListener() {

                                    @Override
                                    public void onLoadSuccess(String packageName) {
                                        try {
                                            PluginDebugLog.runtimeLog(TAG,
                                                    "checkPkgInstallationAndLaunch loadPluginAsync callback onLoadSuccess pkgName: " + packageName);
                                            //load done,start plugin
                                            readyToStartSpecifyPlugin(mHostContext, mServiceConnection, mIntent, false);
                                            if (sPluginStatusListener != null) {
                                                sPluginStatusListener.onPluginReady(packageName);
                                            }
                                        } catch (Exception e) {
                                            PActivityStackSupervisor.clearLoadingIntent(packageName);
                                            PluginLoadedApk mPlugin = sPluginsMap.get(packageName);
                                            if (null != mPlugin) {
                                                mPlugin.changeLaunchingIntentStatus(false);
                                            }
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void onLoadFailed(String packageName) {
                                        PluginDebugLog.runtimeLog(TAG,
                                                "checkPkgInstallationAndLaunch loadPluginAsync callback onLoadFailed pkgName: " + packageName);
                                        //load failed, clear launching intent
                                        PActivityStackSupervisor.clearLoadingIntent(packageName);
                                        PluginLoadedApk mPlugin = sPluginsMap.get(packageName);
                                        if (null != mPlugin) {
                                            mPlugin.changeLaunchingIntentStatus(false);
                                        }
                                    }
                                }, mProcessName);
                    }


                    @Override
                    public void onPackageInstallFail(PluginLiteInfo info, int failReason) throws RemoteException {
                        String packageName = info.packageName;
                        PluginDebugLog.runtimeLog(TAG, "checkPkgInstallationAndLaunch failed packageName: " + packageName
                                + " failReason: " + failReason);
                        PActivityStackSupervisor.clearLoadingIntent(packageName);
                        deliver(mHostContext, false, packageName, failReason);
                    }
                });
    }


    /**
     * 异步加载插件
     *
     * @param mContext     主进程的Context
     * @param mPackageName 需要加载的插件包名
     * @param mListener    加载结果回调
     */
    private static void loadPluginAsync(final Context mContext, String mPackageName,
                                        final IPluginLoadListener mListener, String mProcessName) {
        try {
            mExecutor.execute(new LoadPluginTask(mContext, mPackageName, mListener, mProcessName));
        } catch (Exception e) {
            e.printStackTrace();
            PActivityStackSupervisor.clearLoadingIntent(mPackageName);
            deliver(mContext, false, mPackageName, ErrorType.ERROR_CLIENT_LOAD_INIT_TARGET);
        }
    }

    /**
     * 从mIntent里面解析插件包名
     * 1. 从Intent的package获取
     * 2. 从Intent的ComponentName获取
     * 3. 隐式Intent，从已安装插件列表中查找可以响应的插件
     *
     * @param mHostContext 主工程Context
     * @param mIntent      需要启动的组件
     * @return 返回需要启动插件的包名
     */
    private static String tryParsePkgName(Context mHostContext, Intent mIntent) {
        if (mIntent == null || mHostContext == null) {
            return "";
        }

        String pkgName = mIntent.getPackage();
        if (!TextUtils.isEmpty(pkgName) && !TextUtils.equals(pkgName, mHostContext.getPackageName())) {
            // 与宿主pkgName不同
            return pkgName;
        }

        ComponentName cpn = mIntent.getComponent();
        if (cpn != null && !TextUtils.isEmpty(cpn.getPackageName())) {
            // 显式启动插件
            return cpn.getPackageName();
        } else {
            // 隐式启动插件
            List<PluginLiteInfo> packageList =
                    PluginPackageManagerNative.getInstance(mHostContext).getInstalledApps();
            if (packageList != null) {
                // Here, loop all installed packages to get pkgName for this intent
                String packageName = "";
                ActivityInfo activityInfo = null;
                ServiceInfo serviceInfo = null;
                for (PluginLiteInfo info : packageList) {
                    if (info != null) {
                        PluginPackageInfo target = PluginPackageManagerNative.getInstance(mHostContext)
                                .getPluginPackageInfo(mHostContext, info);
                        if (target != null && (activityInfo = target.resolveActivity(mIntent)) != null) {
                            // 优先查找Activity, 这里转成显式Intent，后面不用二次resolve了
                            mIntent.setComponent(new ComponentName(info.packageName, activityInfo.name));
                            return info.packageName;
                        }
                        // resolve隐式Service
                        if (!TextUtils.isEmpty(packageName) && serviceInfo != null) {
                            continue;
                        }
                        if (target != null && (serviceInfo = target.resolveService(mIntent)) != null) {
                            packageName = info.packageName;
                        }
                    }
                }
                // Here, No Activity can handle this intent, we check service fallback
                if (!TextUtils.isEmpty(packageName)) {
                    if (serviceInfo != null) {
                        // 插件框架后面的逻辑只支持显式Service处理，这里需要更新Intent的信息
                        mIntent.setComponent(new ComponentName(packageName, serviceInfo.name));
                    }
                    return packageName;
                }
            }
        }

        return "";
    }

    /**
     * 插件状态投递
     *
     * @param mContext  插件进程Context
     * @param success   结果是否成功
     * @param pakName   插件包名
     * @param errorCode 错误码
     */
    public static void deliver(final Context mContext, final boolean success, final String pakName,
                               final int errorCode) {
        if (Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    deliverPlugInner(mContext, success, pakName, errorCode);
                    return null;
                }

            }.execute();
        } else {
            deliverPlugInner(mContext, success, pakName, errorCode);
        }
    }

    /**
     * 插件状态投递
     *
     * @param mContext
     * @param success
     * @param pakName
     * @param errorCode
     */
    private static void deliverPlugInner(Context mContext, boolean success, String pakName, int errorCode) {
        if (null != mContext && mDeliver != null && !TextUtils.isEmpty(pakName)) {
            PluginLiteInfo info = PluginPackageManagerNative.getInstance(ContextUtils.getOriginalContext(mContext))
                    .getPackageInfo(pakName);
            if (info != null) {
                mDeliver.deliver(success, info, errorCode);
            }
        }
    }

    /**
     * 加载插件的异步任务
     */
    private static class LoadPluginTask implements Runnable {

        private String mPackageName;
        private Context mHostContext;
        private String mProcessName;
        private PluginLoadedApkHandler mHandler;

        LoadPluginTask(Context mHostContext,
                       String mPackageName,
                       IPluginLoadListener mListener,
                       String mProcessName) {
            this.mHostContext = mHostContext;
            this.mPackageName = mPackageName;
            this.mProcessName = mProcessName;
            this.mHandler = new PluginLoadedApkHandler(mListener, mPackageName, Looper.getMainLooper());
        }

        @Override
        public void run() {
            boolean loaded = false;
            try {
                PluginLiteInfo packageInfo =
                        PluginPackageManagerNative.getInstance(mHostContext).getPackageInfo(mPackageName);
                if (packageInfo != null) {
                    PluginDebugLog.runtimeLog("plugin",
                            "doInBackground:" + mPackageName);
                    loaded = createPluginLoadedApkInstance(mHostContext, packageInfo, mProcessName);
                } else {
                    PluginDebugLog.runtimeLog("plugin", "packageInfo is null before initProxyEnvironment");
                }
            } catch (Exception e) {
                ErrorUtil.throwErrorIfNeed(e);
                deliver(mHostContext, false, mPackageName,
                        ErrorType.ERROR_CLIENT_LOAD_INIT_ENVIRONMENT_FAIL);
                loaded = false;
            }
            int what = loaded ? PluginLoadedApkHandler.PLUGIN_LOADED_APK_CREATE_SUCCESS : PluginLoadedApkHandler.PLUGIN_LOADED_APK_CREATE_FAILED;
            mHandler.sendEmptyMessage(what);
        }


        /**
         * 创建{@link PluginLoadedApk}
         *
         * @param context
         * @param packageInfo
         */
        private boolean createPluginLoadedApkInstance(Context context,
                                                      PluginLiteInfo packageInfo,
                                                      String mProcessName) {
            String packageName = packageInfo.packageName;
            if (!TextUtils.isEmpty(packageName)) {
                boolean loaded = isPluginLoaded(packageName);
                PluginDebugLog.runtimeLog(TAG, "sPluginsMap.containsKey(" + packageName + "):"
                        + loaded);
                if (loaded) {
                    return true;
                }
                PluginLoadedApk mLoadedApk = null;
                PluginPackageManager.updateSrcApkPath(context, packageInfo);
                if (!TextUtils.isEmpty(packageInfo.srcApkPath)) {
                    File apkFile = new File(packageInfo.srcApkPath);
                    if (!apkFile.exists()) {
                        PluginDebugLog.runtimeLog(TAG,
                                "Special case apkFile not exist, notify client! packageName: " + packageName);
                        PluginPackageManager.notifyClientPluginException(context, packageName, "Apk file not exist!");
                        return false;
                    }
                    try {
                        mLoadedApk = new PluginLoadedApk(context, packageInfo.srcApkPath, packageName, mProcessName);
                    } catch (Exception e) {
                        ErrorUtil.throwErrorIfNeed(e);
                        PActivityStackSupervisor.clearLoadingIntent(packageName);
                        deliver(context, false, packageName,
                                ErrorType.ERROR_CLIENT_ENVIRONMENT_NULL);
                    }
                    if (mLoadedApk != null) {
                        addPluginLoadedApk(packageName, mLoadedApk);
                        PluginDebugLog.runtimeLog(TAG, "plugin loaded success! packageName: " + packageName);
                        return true;
                    }
                }
            }
            PluginDebugLog.runtimeLog(TAG, "plugin loaded failed! packageName: " + packageName);

            return false;
        }
    }

    /**
     * 退出插件,将插件中的类从PathClassLoader中剔除
     *
     * @param mPackageName 需要退出的插件的包名
     */
    public static void exitPlugin(String mPackageName) {
        if (!TextUtils.isEmpty(mPackageName)) {
            PluginLoadedApk mLoadedApk = removePluginLoadedApk(mPackageName);
            if (mLoadedApk == null || mLoadedApk.getPluginApplication() == null) {
                return;
            }
            if (mLoadedApk.hasPluginInit()) {
                mLoadedApk.getPluginApplication().onTerminate();
            }
            mLoadedApk.ejectClassLoader();
        }
    }

    /**
     * 注册卸载广播，清理PluginLoadedApk内存引用
     *
     * @param context
     */
    public static void registerUninstallReceiver(Context context) {

        IntentFilter filter = new IntentFilter();
        filter.addAction(PluginPackageManager.ACTION_PACKAGE_UNINSTALL);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (PluginPackageManager.ACTION_PACKAGE_UNINSTALL.equals(intent.getAction())) {
                    // 卸载广播
                    String pkgName = intent.getStringExtra(IIntentConstant.EXTRA_PKG_NAME);
                    exitPlugin(pkgName);
                }
            }
        };
        context.registerReceiver(receiver, filter);
    }

    /**
     * 插件进程的Activity栈是否空
     *
     * @return true: Activity栈是空，false：Activity栈不是空
     */
    public static boolean isActivityStackEmpty() {
        for (Map.Entry<String, PluginLoadedApk> entry : PluginManager.getAllPluginLoadedApk().entrySet()) {
            PluginLoadedApk mLoadedApk = entry.getValue();
            if (mLoadedApk != null && !mLoadedApk.getActivityStackSupervisor().isStackEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 处理插件退出时的善后操作
     *
     * @param mPackageName 退出插件的包名
     * @param force
     */
    public static void doExitStuff(String mPackageName, boolean force) {
        if (TextUtils.isEmpty(mPackageName)) {
            return;
        }

        if (force || (isActivityStackEmpty() && PServiceSupervisor.getAliveServices().isEmpty())) {
            if (null != sExitStuff) {
                PluginDebugLog.runtimeLog(TAG, "do release stuff with " + mPackageName);
                sExitStuff.doExitStuff(mPackageName);
            }
        }
    }

    /**
     * 加载插件线程和主线程通信Handler
     */
    static class PluginLoadedApkHandler extends Handler {
        public static final int PLUGIN_LOADED_APK_CREATE_SUCCESS = 0x10;
        public static final int PLUGIN_LOADED_APK_CREATE_FAILED = 0x20;

        IPluginLoadListener mListener;
        String mPackageName;

        public PluginLoadedApkHandler(IPluginLoadListener listenner, String pakName, Looper looper) {
            super(looper);
            this.mListener = listenner;
            this.mPackageName = pakName;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PLUGIN_LOADED_APK_CREATE_SUCCESS:
                    if (mListener != null) {
                        mListener.onLoadSuccess(mPackageName);
                    }
                    break;
                case PLUGIN_LOADED_APK_CREATE_FAILED:
                    if (mListener != null) {
                        mListener.onLoadFailed(mPackageName);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 插件状态监听器
     */
    public interface IPluginStatusListener {
        /**
         * 插件初始化完毕
         *
         * @param packageName 初始化完毕的插件包名
         */
        void onPluginReady(String packageName);
    }


    /**
     * 设置投递逻辑的实现(宿主工程调用)
     *
     * @param mDeliverImpl
     */
    public static void setDeliverImpl(IDeliverInterface mDeliverImpl) {
        mDeliver = mDeliverImpl;
    }

    /**
     * 设置插件状态监听器(宿主工程调用)
     *
     * @param mListener
     */
    public static void setPluginStatusListener(IPluginStatusListener mListener) {
        sPluginStatusListener = mListener;
    }

    /**
     * 设置插件退出监听回调(宿主工程调用)
     *
     * @param mExitStuff
     */
    public static void setExitStuff(IAppExitStuff mExitStuff) {
        sExitStuff = mExitStuff;
    }

    /**
     * 插件状态投递逻辑接口，由外部实现并设置进来
     */
    public interface IDeliverInterface {
        void deliver(boolean success, PluginLiteInfo pkgInfo, int errorCode);
    }

    public interface IAppExitStuff {
        void doExitStuff(String pkgName);
    }

    /**
     * 停止指定的Service
     *
     * @param intent
     */
    public static void stopService(Intent intent) {
        if (intent == null || intent.getComponent() == null
                || TextUtils.isEmpty(intent.getComponent().getPackageName())) {
            return;
        }
        final String packageName = intent.getComponent().getPackageName();
        PluginLoadedApk mLoadedApk = sPluginsMap.get(packageName);
        if (mLoadedApk == null) {
            return;
        }
        PluginContextWrapper appWrapper = mLoadedApk.getAppWrapper();
        if (appWrapper != null) {
            appWrapper.stopService(intent);
        }
    }


    /**
     * 退出插件
     *
     * @param mContext     主进程Context
     * @param mProcessName 要退出进程
     */
    public static void quit(Context mContext, String mProcessName) {

        PluginPackageManagerNative.getInstance(mContext).release();

        for (Map.Entry<String, PluginLoadedApk> entry : getAllPluginLoadedApk().entrySet()) {
            PluginLoadedApk plugin = entry.getValue();
            if (plugin != null) {
                plugin.quitApp(true, false);
            }
        }
        PServiceSupervisor.clearConnections();
        // sAliveServices will be cleared, when on ServiceProxy1 destroy.

        if (mContext != null) {
            Intent intent = new Intent();
            String proxyServiceName = ComponetFinder.matchServiceProxyByFeature(mProcessName);

            Class<?> proxyServiceNameClass = null;
            try {
                proxyServiceNameClass = Class.forName(proxyServiceName);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            if (proxyServiceNameClass != null) {
                PluginDebugLog.runtimeLog(TAG, "try to stop service " + proxyServiceName);
                intent.setClass(mContext, proxyServiceNameClass);
                intent.setAction(ACTION_QUIT);
                mContext.startService(intent);
            }
        }
    }

    public static void dump(PrintWriter printWriter) {
        try {
            printWriter.print("================start dump plugin activity stack====================");
            Iterator<Map.Entry<String, PluginLoadedApk>> mIterator = sPluginsMap.entrySet().iterator();
            while (mIterator.hasNext()) {
                Map.Entry<String, PluginLoadedApk> tmp = mIterator.next();
                printWriter.print("packageName:" + tmp.getKey());
                printWriter.print("\n");
                tmp.getValue().getActivityStackSupervisor().dump(printWriter);
//                List<Activity> activities = tmp.getValue().getActivityStackSupervisor().getActivityStack();
//                for (Activity mActivity : activities) {
//                    ((InstrActivityProxy1) mActivity).dump(printWriter);
//                }
            }
            printWriter.print("================end dump plugin activity stack====================");
        } catch (Exception e) {
            e.printStackTrace();
            printWriter.print("error:" + e.getMessage());
        }

    }

    /**
     * 宿主注册到插件里的ActivityLifeCycle监听器
     * 插件重写了Application，需要注册到插件的Application类里去
     */
    final static ArrayList<PluginActivityLifeCycleCallback> sActivityLifecycleCallbacks =
            new ArrayList<PluginActivityLifeCycleCallback>();

    /**
     * 注册ActivityLifeCycle到插件的Application
     */
    public static void registerActivityLifecycleCallbacks(PluginActivityLifeCycleCallback callback) {
        synchronized (sActivityLifecycleCallbacks) {
            sActivityLifecycleCallbacks.add(callback);
        }
        // 对于已经运行的插件，需要注册到其Application类中
        for (Map.Entry<String, PluginLoadedApk> entry : sPluginsMap.entrySet()) {
            PluginLoadedApk loadedApk = entry.getValue();
            if (loadedApk != null && loadedApk.getPluginApplication() != null) {
                Application application = loadedApk.getPluginApplication();
                application.registerActivityLifecycleCallbacks(callback);
            }
        }
    }

    /**
     * 取消插件Application里的ActivityLifeCycle监听
     */
    public static void unregisterActivityLifecycleCallbacks(PluginActivityLifeCycleCallback callback) {
        synchronized (sActivityLifecycleCallbacks) {
            sActivityLifecycleCallbacks.remove(callback);
        }
        // 对于已经运行的插件，需要从其Application类中反注册
        for (Map.Entry<String, PluginLoadedApk> entry : sPluginsMap.entrySet()) {
            PluginLoadedApk loadedApk = entry.getValue();
            if (loadedApk != null && loadedApk.getPluginApplication() != null) {
                Application application = loadedApk.getPluginApplication();
                application.unregisterActivityLifecycleCallbacks(callback);
            }
        }
    }

    /**********************************************************
     *
     * 以下Dispatch相关方法只会在InstrActivityProxyN中调用
     * 新的Hook Instrumentation方案不会使用到
     *
     *********************************************************/

    public static void dispatchPluginActivityCreated(String pluginPkgName, Activity activity, Bundle savedInstanceState) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((PluginActivityLifeCycleCallback) callbacks[i]).onPluginActivityCreated(pluginPkgName, activity,
                        savedInstanceState);
            }
        }
    }

    public static void dispatchPluginActivityStarted(String pluginPkgName, Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((PluginActivityLifeCycleCallback) callbacks[i]).onPluginActivityStarted(pluginPkgName, activity);
            }
        }
    }

    public static void dispatchPluginActivityResumed(String pluginPkgName, Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((PluginActivityLifeCycleCallback) callbacks[i]).onPluginActivityResumed(pluginPkgName, activity);
            }
        }
    }

    public static void dispatchPluginActivityPaused(String pluginPkgName, Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((PluginActivityLifeCycleCallback) callbacks[i]).onPluginActivityPaused(pluginPkgName, activity);
            }
        }
    }

    public static void dispatchPluginActivityStopped(String pluginPkgName, Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((PluginActivityLifeCycleCallback) callbacks[i]).onPluginActivityStopped(pluginPkgName, activity);
            }
        }
    }

    public static void dispatchPluginActivitySaveInstanceState(String pluginPkgName, Activity activity, Bundle outState) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((PluginActivityLifeCycleCallback) callbacks[i]).onPluginActivitySaveInstanceState(pluginPkgName, activity,
                        outState);
            }
        }
    }

    public static void dispatchPluginActivityDestroyed(String pluginPkgName, Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i = 0; i < callbacks.length; i++) {
                ((PluginActivityLifeCycleCallback) callbacks[i]).onPluginActivityDestroyed(pluginPkgName, activity);
            }
        }
    }

    private static Object[] collectActivityLifecycleCallbacks() {
        Object[] callbacks = null;
        synchronized (sActivityLifecycleCallbacks) {
            if (sActivityLifecycleCallbacks.size() > 0) {
                callbacks = sActivityLifecycleCallbacks.toArray();
            }
        }
        return callbacks;
    }
}
