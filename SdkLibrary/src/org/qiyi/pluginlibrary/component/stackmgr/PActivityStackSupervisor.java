package org.qiyi.pluginlibrary.component.stackmgr;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.component.InstrActivityProxy1;
import org.qiyi.pluginlibrary.constant.IIntentConstant;
import org.qiyi.pluginlibrary.runtime.PluginLoadedApk;
import org.qiyi.pluginlibrary.runtime.PluginManager;
import org.qiyi.pluginlibrary.utils.ComponetFinder;
import org.qiyi.pluginlibrary.utils.IntentUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class PActivityStackSupervisor {
    private static final String TAG = PActivityStackSupervisor.class.getSimpleName();

    // 等在加载的intent请求
    private static ConcurrentMap<String, LinkedBlockingQueue<Intent>> sIntentCacheMap = new ConcurrentHashMap<String, LinkedBlockingQueue<Intent>>();
    // 正在加载中的intent请求
    private static ConcurrentMap<String, List<Intent>> sIntentLoadingMap = new ConcurrentHashMap<String, List<Intent>>();

    // 当前进程所有插件的Activity栈
    private static CopyOnWriteArrayList<Activity> sAllActivityStack = new CopyOnWriteArrayList<Activity>();

    // 插件的Activity栈
    private final LinkedList<Activity> mActivityStack;
    private PluginLoadedApk mLoadedApk;

    public PActivityStackSupervisor(PluginLoadedApk mLoadedApk) {
        this.mLoadedApk = mLoadedApk;
        mActivityStack = new LinkedList<Activity>();
    }

    public LinkedList<Activity> getActivityStack() {
        return mActivityStack;
    }

    public void clearActivityStack() {
        mActivityStack.clear();
    }

    public Activity pollActivityStack() {
        return mActivityStack.poll();
    }

    public void dealLaunchMode(Intent intent) {
        if (null == intent) {
            return;
        }
        PluginDebugLog.runtimeLog(TAG, "dealLaunchMode target activity: " + intent + " source: "
                + intent.getStringExtra(IIntentConstant.EXTRA_TARGET_CLASS_KEY));
        if (PluginDebugLog.isDebug()) {
            if (null != mActivityStack && mActivityStack.size() > 0) {
                for (Activity ac : mActivityStack) {
                    PluginDebugLog.runtimeLog(TAG, "dealLaunchMode stack: " + ac + " source: "
                            + IntentUtils.dump(ac));
                }
            } else {
                PluginDebugLog.runtimeLog(TAG, "dealLaunchMode stack is empty");
            }
        }
        String targetActivity = intent.getStringExtra(IIntentConstant.EXTRA_TARGET_CLASS_KEY);
        if (TextUtils.isEmpty(targetActivity)) {
            return;
        }

        // 不支持LAUNCH_SINGLE_INSTANCE
        ActivityInfo info = mLoadedApk.getPluginPackageInfo().getActivityInfo(targetActivity);
        if (info == null || info.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
            return;
        }
        boolean isSingleTop = info.launchMode == ActivityInfo.LAUNCH_SINGLE_TOP
                || (intent.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0;
        boolean isSingleTask = info.launchMode == ActivityInfo.LAUNCH_SINGLE_TASK;
        boolean isClearTop = (intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0;
        PluginDebugLog.runtimeLog(TAG, "dealLaunchMode isSingleTop " + isSingleTop + " isSingleTask "
                + isSingleTask + " isClearTop " + isClearTop);
        int flag = intent.getFlags();
        PluginDebugLog.runtimeLog(TAG, "before flag: " + Integer.toHexString(intent.getFlags()));
        if ((isSingleTop || isSingleTask) && (flag & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0) {
            flag = flag ^ Intent.FLAG_ACTIVITY_SINGLE_TOP;
        }
        if ((isSingleTask || isClearTop) && (flag & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0) {
            flag = flag ^ Intent.FLAG_ACTIVITY_CLEAR_TOP;
        }
        intent.setFlags(flag);
        PluginDebugLog.runtimeLog(TAG, "after flag: " + Integer.toHexString(intent.getFlags()));

        if (isSingleTop && !isClearTop) {
            // 判断栈顶是否为需要启动的Activity
            Activity activity = null;
            if (!mActivityStack.isEmpty()) {
                activity = mActivityStack.getFirst();
            }
            boolean hasSameActivity = false;
            String proxyClsName = ComponetFinder.findActivityProxy(mLoadedApk, info);
            if (activity != null) {
                // 栈内有实例, 可能是ProxyActivity，也可能是插件真实的Activity
                if (TextUtils.equals(proxyClsName, activity.getClass().getName())
                        || TextUtils.equals(targetActivity, activity.getClass().getName())) {
                    String key = getActivityStackKey(activity);

                    if (!TextUtils.isEmpty(key) && TextUtils.equals(targetActivity, key)) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        hasSameActivity = true;
                    }
                }
            }
            if (hasSameActivity) {
                handleOtherPluginActivityStack(activity);
            }
            /*else {
                handleOtherPluginActivityStack(null);
            }*/
        } else if (isSingleTask || isClearTop) {

            Activity found = null;
            synchronized (mActivityStack) {
                // 遍历已经起过的activity
                for (Activity activity : mActivityStack) {
                    String proxyClsName = ComponetFinder.findActivityProxy(mLoadedApk, info);
                    if (activity != null) {
                        if (TextUtils.equals(proxyClsName, activity.getClass().getName())
                                || TextUtils.equals(targetActivity, activity.getClass().getName())) {
                            String key = getActivityStackKey(activity);
                            if (!TextUtils.isEmpty(key) && TextUtils.equals(targetActivity, key)) {
                                PluginDebugLog.runtimeLog(TAG, "dealLaunchMode found:" + IntentUtils.dump(activity));
                                found = activity;
                                break;
                            }
                        }
                    }
                }
            }

            // 栈中已经有当前activity
            if (found != null) {
                // 处理其他插件的逻辑
                // 在以这两种SingleTask， ClearTop flag启动情况下，在同一个栈的情况下
                handleOtherPluginActivityStack(found);
                List<Activity> popActivities = new ArrayList<Activity>(5);
                synchronized (mActivityStack) {
                    for (Activity activity : mActivityStack) {
                        if (activity == found) {
                            if (isSingleTask || isSingleTop) {
                                PluginDebugLog.runtimeLog(TAG, "dealLaunchMode add single top flag!");
                                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            }
                            PluginDebugLog.runtimeLog(TAG, "dealLaunchMode add clear top flag!");
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            break;
                        }
                        popActivities.add(activity);
                    }
                    for (Activity act : popActivities) {
                        if (!mActivityStack.isEmpty()) {
                            PluginDebugLog.runtimeLog(TAG, "dealLaunchMode mActivityStack remove " + IntentUtils.dump(act));
                            mActivityStack.remove(act);
                        }
                    }
                }
                for (Activity act : popActivities) {
                    PluginDebugLog.runtimeLog(TAG, "dealLaunchMode popActivities finish " + IntentUtils.dump(act));
                    act.finish();
                }
                mLoadedApk.quitApp(false);
            } else {
                // 遍历还未启动cache中的activity记录
                LinkedBlockingQueue<Intent> recoreds = sIntentCacheMap
                        .get(mLoadedApk.getPluginPackageName());
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
                                PluginDebugLog.runtimeLog(TAG, "sIntentCacheMap found: " + targetActivity);
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
                List<Intent> loadingIntents = sIntentLoadingMap.get(mLoadedApk.getPluginPackageName());
                if (null != loadingIntents) {
                    Iterator<Intent> loadingRecordIterator = loadingIntents.iterator();
                    String notLaunchTargetClassName = null;
                    while (loadingRecordIterator.hasNext()) {
                        Intent record = loadingRecordIterator.next();
                        if (null != record) {
                            notLaunchTargetClassName = record
                                    .getStringExtra(IIntentConstant.EXTRA_TARGET_CLASS_KEY);
                            if (TextUtils.equals(notLaunchTargetClassName, targetActivity)) {
                                PluginDebugLog.runtimeLog(TAG,
                                        "sIntentLoadingMap found: " + targetActivity);
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
//                handleOtherPluginActivityStack(null);
            }
        }
        PluginDebugLog.runtimeLog(TAG, "dealLaunchMode end: " + intent + " "
                + intent.getStringExtra(IIntentConstant.EXTRA_TARGET_CLASS_KEY));
    }

    private void handleOtherPluginActivityStack(Activity act) {
        // 假如栈中存在之前的Activity，并且在该Activity之上存在其他插件的activity，则finish掉其之上的activity
        // 例如场景桌面有多个插件的图标，点击一个业务的进入，然后home键，然后再点击另外一个循环。
        if (null == act) {
//            for (ProxyEnvironment env : ProxyEnvironmentManager.getAllEnv().values()) {
//                synchronized (env.getActivityStackSupervisor().getActivityStack()) {
//                    LinkedList<Activity> activityStack = env.getActivityStackSupervisor()
//                            .getActivityStack();
//                    if (!TextUtils.equals(mEnv.getTargetPackageName(), env.getTargetPackageName())
//                            && null != activityStack && activityStack.size() > 0) {
//                        Iterator<Activity> it = activityStack.iterator();
//                        while (it.hasNext()) {
//                            Activity activity = it.next();
//                            if (null != activity) {
//                                PluginDebugLog.log(TAG,
//                                        "dealLaunchMode clear other plugin's stack " + activity
//                                                + " source: "
//                                                + ((InstrActivityProxy1) activity).dump());
//                                try {
//                                    PluginDebugLog.log(TAG,
//                                            "finish: " + ((InstrActivityProxy1) activity).dump());
//                                    activity.finish();
//                                } catch (Exception ex) {
//                                    ex.printStackTrace();
//                                }
//                            }
//                        }
//                        activityStack.clear();
//                    }
//                }
//            }
        } else {
            Activity temp = null;
            List<Activity> needRemove = new ArrayList<Activity>();
            for (int i = sAllActivityStack.size() - 1; i > -1; i--) {
                temp = sAllActivityStack.get(i);
                if (null != temp && act == temp) {
                    break;
                }
                String pkgName = IntentUtils.parsePkgNameFromActivity(temp);
                if (temp != null && !TextUtils.equals(mLoadedApk.getPluginPackageName(),
                        pkgName)) {
                    needRemove.add(temp);
                }
            }
            PluginLoadedApk mLoadedApk = null;
            for (Activity removeItem : needRemove) {
                if (null != removeItem) {
                    String pkgName = IntentUtils.parsePkgNameFromActivity(removeItem);
                    mLoadedApk = PluginManager.getPluginLoadedApkByPkgName(pkgName);
                    if (null != mLoadedApk
                            && null != mLoadedApk.getActivityStackSupervisor().getActivityStack()) {
                        synchronized (mLoadedApk.getActivityStackSupervisor().getActivityStack()) {
                            try {
                                PluginDebugLog.runtimeLog(TAG,
                                        "finish: " + IntentUtils.dump(act));
                                removeItem.finish();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            mLoadedApk.getActivityStackSupervisor().mActivityStack.remove(removeItem);
                        }
                    }
                }
            }
        }
    }

    /**
     * 把插件Activity压入堆栈
     * 旧的插件方案：压入的是代理的Activity
     * hook Instr方案：压入的是插件真实的Activity
     *
     * @param activity
     */
    public void pushActivityToStack(Activity activity) {
        PluginDebugLog.runtimeLog(TAG, "pushActivityToStack activity: " + activity + " "
                + IntentUtils.dump(activity));
        sAllActivityStack.add(activity);
        removeLoadingIntent(mLoadedApk.getPluginPackageName(), activity.getIntent());
        synchronized (mActivityStack) {
            mActivityStack.addFirst(activity);
        }
    }

    public boolean popActivityFromStack(Activity activity) {
        sAllActivityStack.remove(activity);
        boolean result = false;
        synchronized (mActivityStack) {
            if (!mActivityStack.isEmpty()) {
                PluginDebugLog.runtimeLog(TAG, "popActivityFromStack activity: " + activity + " "
                        + IntentUtils.dump(activity));
                result = mActivityStack.remove(activity);
            }
        }
        // mEnv.quitApp(false);
        return result;
    }

    public static void addCachedIntent(String pkgName, LinkedBlockingQueue<Intent> cachedIntents) {
        if (TextUtils.isEmpty(pkgName) || null == cachedIntents) {
            return;
        }
        sIntentCacheMap.put(pkgName, cachedIntents);
    }

    /**
     * 获取对应插件缓存还未执行加载的Intent
     *
     * @param pkgName
     * @return
     */
    public static LinkedBlockingQueue<Intent> getCachedIntent(String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            return null;
        }
        return sIntentCacheMap.get(pkgName);
    }

    /**
     * 清除等待队列，防止异常情况，导致所有Intent都阻塞在等待队列，导致插件无法启动
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

    public static void addLoadingIntent(String pkgName, Intent intent) {
        if (null == intent || TextUtils.isEmpty(pkgName)) {
            return;
        }
        List<Intent> intents = sIntentLoadingMap.get(pkgName);
        if (null == intents) {
            intents = Collections.synchronizedList(new ArrayList<Intent>());
            sIntentLoadingMap.put(pkgName, intents);
        }
        PluginDebugLog.runtimeLog(TAG, "addLoadingIntent pkgName: " + pkgName + " intent: " + intent);
        intents.add(intent);
    }

    public static void removeLoadingIntent(String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            return;
        }
        sIntentLoadingMap.remove(pkgName);
    }

    public static void removeLoadingIntent(String pkgName, Intent intent) {
        if (null == intent || TextUtils.isEmpty(pkgName)) {
            return;
        }
        List<Intent> intents = sIntentLoadingMap.get(pkgName);
        Intent toBeRemoved = null;
        if (null != intents) {
            for (Intent temp : intents) {
                if (TextUtils.equals(temp.getStringExtra(IIntentConstant.EXTRA_TARGET_CLASS_KEY),
                        intent.getStringExtra(IIntentConstant.EXTRA_TARGET_CLASS_KEY))) {
                    toBeRemoved = temp;
                    break;
                }
            }
        }
        boolean result = false;
        if (null != toBeRemoved) {
            result = intents.remove(toBeRemoved);
        }
        PluginDebugLog.runtimeLog(TAG, "removeLoadingIntent pkgName: " + pkgName + " toBeRemoved: "
                + toBeRemoved + " result: " + result);
    }

    private static String getActivityStackKey(Activity activity) {
        String key = "";
        if (activity instanceof InstrActivityProxy1) {
            InstrActivityProxy1 lActivityProxy = (InstrActivityProxy1) activity;
            PluginActivityControl ctl = lActivityProxy.getController();
            if (ctl != null && ctl.getPlugin() != null) {
                key = ctl.getPlugin().getClass().getName();
            }
        } else {
            key = activity.getClass().getName();
        }
        return key;
    }
}
