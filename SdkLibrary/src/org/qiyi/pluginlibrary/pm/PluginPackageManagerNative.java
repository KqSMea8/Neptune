package org.qiyi.pluginlibrary.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.error.ErrorType;
import org.qiyi.pluginlibrary.install.IActionFinishCallback;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.runtime.NotifyCenter;
import org.qiyi.pluginlibrary.utils.ContextUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.Util;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 此类的功能和{@link PluginPackageManager}基本一致<br/>
 * 只不过同一个功能这个类可以在任何进程使用<br>
 * {@link PluginPackageManager}只能在主进程使用
 * <p>
 * 该类通过IPC与{@link PluginPackageManagerService}进行交互，
 * 实现插件的安装和卸载功能
 */
public class PluginPackageManagerNative {
    private static final String TAG = "PluginPackageManagerNative";

    private interface Action {
        String getPackageName();

        boolean meetCondition();

        void doAction();
    }

    /**
     * Action执行完毕回调
     */
    private static class ActionFinishCallback extends IActionFinishCallback.Stub {

        private String mProcessName;

        private Executor mActionExecutor;

        public ActionFinishCallback(String processName) {
            mProcessName = processName;
            mActionExecutor = Executors.newFixedThreadPool(2);
        }

        @Override
        public void onActionComplete(String packageName, int errorCode) throws RemoteException {
            PluginDebugLog.installFormatLog(TAG, "onActionComplete with %s, errorCode:%d", packageName, errorCode);
            if (mActionMap.containsKey(packageName)) {
                final CopyOnWriteArrayList<Action> list = mActionMap.get(packageName);
                if (null == list) {
                    return;
                }
                synchronized (list) {
                    PluginDebugLog.installFormatLog(TAG, "%s has %d action in list!", packageName, list.size());
                    if (list.size() > 0) {

                        Action finishedAction = list.remove(0);
                        if (finishedAction != null) {
                            PluginDebugLog.installFormatLog(TAG,
                                    "get and remove first action:%s ", finishedAction.toString());
                        }

                        if (finishedAction instanceof PluginUninstallAction) {
                            PluginDebugLog.installFormatLog(TAG,
                                    "this is PluginUninstallAction  for :%s", packageName);
                            PluginUninstallAction uninstallAction = (PluginUninstallAction) finishedAction;
                            if (uninstallAction.observer != null && uninstallAction.info != null
                                    && !TextUtils.isEmpty(uninstallAction.info.packageName)) {
                                PluginDebugLog.installFormatLog(TAG, "PluginUninstallAction packageDeleted for %s", packageName);
                                uninstallAction.observer.onPluginUninstall(uninstallAction.info.packageName, errorCode);
                            }
                        }
                        // 执行下一个卸载操作，不能同步，防止栈溢出
                        executeNextAction(list, packageName);

                        if (list.isEmpty()) {
                            PluginDebugLog.installFormatLog(TAG,
                                    "remove empty action list of %s", packageName);
                            mActionMap.remove(packageName);
                        }
                    }
                }
            }
        }

        /**
         * 异步执行下一个Action
         * @param actions
         */
        private void executeNextAction(final List<Action> actions, final String packageName) {
            mActionExecutor.execute(new Runnable() {
                @Override
                public void run() {

                    int index = 0;
                    PluginDebugLog.installFormatLog(TAG, "start find can execute action ...");
                    while (index < actions.size()) {
                        Action action = actions.get(index);
                        if (action != null) {
                            if (action.meetCondition()) {
                                PluginDebugLog.installFormatLog(TAG,
                                        "doAction for %s and action is %s", packageName,
                                        action.toString());
                                action.doAction();
                                break;
                            } else {
                                PluginDebugLog.installFormatLog(TAG,
                                        "remove deprecate action of %s,and action:%s "
                                        , packageName, action.toString());
                                actions.remove(index);
                            }
                        }
                    }

                    if (actions.isEmpty()) {
                        PluginDebugLog.installFormatLog(TAG,
                                "remove empty action list of %s", packageName);
                        mActionMap.remove(packageName);
                    }
                }
            });
        }

        @Override
        public String getProcessName() throws RemoteException {
            return mProcessName;
        }
    }


    /**
     * 插件安装的Action
     */
    private static class PluginInstallAction implements Action {

        public IInstallCallBack listener;
        public PluginLiteInfo info;
        public PluginPackageManagerNative callbackHost;

        @Override
        public String toString() {
            StringBuilder infoBuilder = new StringBuilder();
            infoBuilder.append("PluginInstallAction: ");
            infoBuilder.append(" has IInstallCallBack: ").append(listener != null);
            if (info != null) {
                infoBuilder.append(" packageName: ").append(info.packageName);
                infoBuilder.append(" plugin_ver: ").append(info.pluginVersion);
                infoBuilder.append(" plugin_gray_version: ").append(info.pluginGrayVersion);
            }
            return infoBuilder.toString();
        }

        @Override
        public String getPackageName() {
            return info != null ? info.packageName : null;
        }

        @Override
        public boolean meetCondition() {
            boolean canMeetCondition = false;
            if (mService != null && info != null) {
                try {
                    canMeetCondition = mService.canInstallPackage(info);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (mService == null) {
                // set canMeetCondition to true in case of
                // PluginPackageManagerService
                // is not connected, so that the action can be added in action list.
                canMeetCondition = true;
            }
            if (info != null) {
                PluginDebugLog.installFormatLog(TAG, "%s 's PluginInstallAction meetCondition:%s",
                        info.packageName, String.valueOf(canMeetCondition));
            }
            return canMeetCondition;
        }

        @Override
        public void doAction() {
            if (callbackHost != null) {
                //callbackHost.installApkFileInternal(filePath, listener, info);
                callbackHost.installInternal(info, listener);
            }
        }
    }

//    private static class BuildinPluginInstallAction extends PluginInstallAction {
//        @Override
//        public void doAction() {
//            if (callbackHost != null) {
//                callbackHost.installBuildinAppsInternal(info, listener);
//            }
//        }
//    }

    /**
     * 插件卸载的Action
     */
    private static class PluginUninstallAction implements Action {

        IPluginUninstallCallBack observer;
        public PluginLiteInfo info;
        public PluginPackageManagerNative callbackHost;

        @Override
        public String getPackageName() {
            return info != null ? info.packageName : null;
        }

        @Override
        public String toString() {
            StringBuilder infoBuilder = new StringBuilder();
            infoBuilder.append("PluginDeleteAction: ");
            infoBuilder.append(
                    " has IPackageDeleteObserver: ").append(observer != null);
            if (info != null) {
                infoBuilder.append(" packageName: ").append(info.packageName);
                infoBuilder.append(" plugin_ver: ").append(info.pluginVersion);
                infoBuilder.append(" plugin_gray_ver: ").append(info.pluginGrayVersion);
            }

            return infoBuilder.toString();
        }

        @Override
        public boolean meetCondition() {
            boolean canMeetCondition = false;
            if (mService != null && info != null) {
                try {
                    canMeetCondition = mService.canUninstallPackage(info);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else if (mService == null) {
                // set canMeetCondition to true in case of
                // PluginPackageManagerService
                // is not connected, so that the action can be added in action
                // list.
                canMeetCondition = true;
            }
            if (null != info) {
                PluginDebugLog.installFormatLog(TAG,
                        "%s 's PluginDeleteAction canMeetCondition %s", info.packageName, canMeetCondition);
            }
            return canMeetCondition;
        }

        @Override
        public void doAction() {
            if (callbackHost != null) {
                callbackHost.uninstallInternal(info);
            }
        }
    }

    // 插件安装/卸载Action的mapping
    private static ConcurrentHashMap<String, CopyOnWriteArrayList<Action>> mActionMap =
            new ConcurrentHashMap<String, CopyOnWriteArrayList<Action>>();

    /**
     * 与{@link PluginPackageManagerService}交互的ServiceConnection
     */
    private static class PluginPackageManagerServiceConnection implements ServiceConnection {

        private Context mContext;

        PluginPackageManagerServiceConnection(Context context) {
            mContext = context;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (null != service) {
                mService = IPluginPackageManager.Stub.asInterface(service);
            }
            PluginDebugLog.runtimeLog(TAG, "onServiceConnected called");
            if (mService != null) {
                NotifyCenter.notifyServiceConnected(mContext, PluginPackageManagerService.class);
                try {
                    mService.setActionFinishCallback(new ActionFinishCallback(mProcessName));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                executePackageAction(mContext);
                executePendingAction();
            } else {
                PluginDebugLog.runtimeLog(TAG, "onServiceConnected, mService is null");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            PluginDebugLog.runtimeLog(TAG, "onServiceDisconnected called");
        }
    }

    private static IPluginPackageManager mService = null;
    private static PluginPackageManagerNative sInstance = null;
    private Context mContext;
    private ServiceConnection mServiceConnection = null;
    private static String mProcessName = null;

    /**
     * 安装包任务队列，目前仅处理插件依赖时使用
     */
    private static ConcurrentLinkedQueue<ExecutionPackageAction> mPackageActions =
            new ConcurrentLinkedQueue<ExecutionPackageAction>();

    public static PluginPackageManagerNative getInstance(Context context) {
        if (sInstance == null) {
            synchronized (PluginPackageManagerNative.class) {
                if (sInstance == null) {
                    sInstance = new PluginPackageManagerNative(context);
                    sInstance.init();
                }
            }
        }
        return sInstance;
    }

    private PluginPackageManagerNative(Context context) {
        mContext = context.getApplicationContext();
        mProcessName = Util.getCurrentProcessName(context);
    }

    private void init() {
        onBindService(mContext);
    }


    public void setPackageInfoManager(IVerifyPluginInfo packageInfoManager) {
        PluginPackageManager.setVerifyPluginInfoImpl(packageInfoManager);
    }

    public boolean isConnected() {
        return mService != null;
    }


    private void onBindService(Context context) {
        Intent intent = new Intent(context, PluginPackageManagerService.class);
        try {
            context.startService(intent);
            context.bindService(intent, getConnection(context), Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 执行等待中的Action
     */
    private static void executePendingAction() {
        PluginDebugLog.runtimeLog(TAG, "executePendingAction start....");
        for (Map.Entry<String, CopyOnWriteArrayList<Action>> entry : mActionMap.entrySet()) {
            if (entry != null) {
                CopyOnWriteArrayList<Action> actions = entry.getValue();
                PluginDebugLog.installFormatLog(TAG, "execute %d pending actions!", actions.size());
                if (actions != null) {
                    synchronized (actions) {
                        int index = 0;
                        while (index < actions.size()) {
                            Action action = actions.get(index);
                            if (action != null) {
                                if (action.meetCondition()) {
                                    PluginDebugLog.installFormatLog(TAG, "start doAction for pending action %s", action.toString());
                                    action.doAction();
                                    break;
                                } else {
                                    PluginDebugLog.installFormatLog(TAG, "remove deprecate pending action from action list for %s", action.toString());
                                    actions.remove(index);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 执行之前未执行的PacakgeAction操作
     */
    private static void executePackageAction(Context context) {
        if (context != null) {
            PluginDebugLog.runtimeLog(TAG, "executePackageAction start....");
            Iterator<ExecutionPackageAction> iterator = mPackageActions.iterator();
            while (iterator.hasNext()) {
                ExecutionPackageAction action = iterator.next();
                ActionType type = action.type;
                PluginDebugLog.runtimeLog(TAG, "executePackageAction iterator, actionType: " + type);
                switch (type) {
                    case PACKAGE_ACTION:
                        PluginPackageManagerNative.getInstance(context).
                                packageAction(action.packageInfo, action.callBack);
                        break;
                    default:
                        break;
                }
                iterator.remove();
            }
        }
    }


    private static boolean actionIsReady(Action action) {
        if (action != null) {
            String packageName = action.getPackageName();
            if (!TextUtils.isEmpty(packageName)) {
                if (mActionMap.containsKey(packageName)) {
                    List<Action> actionList = mActionMap.get(packageName);
                    if (actionList != null && actionList.indexOf(action) == 0) {
                        PluginDebugLog.log(TAG, "action is ready for " + action.toString());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean addAction(Action action) {
        if (action != null) {
            String packageName = action.getPackageName();
            if (!TextUtils.isEmpty(packageName)) {
                synchronized (mActionMap) {
                    CopyOnWriteArrayList<Action> actionList = mActionMap.get(packageName);
                    if (actionList == null) {
                        actionList = new CopyOnWriteArrayList<Action>();
                        mActionMap.put(packageName, actionList);
                    }
                    PluginDebugLog.log(TAG, "add action in action list for " + action.toString());
                    actionList.add(action);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 提交一个PluginInstallAction安装插件任务
     *
     * @param info
     * @param listener
     */
    public void install(PluginLiteInfo info, IInstallCallBack listener) {
        PluginInstallAction action = new PluginInstallAction();
        action.listener = listener;
        action.info = info;
        action.callbackHost = this;
        if (action.meetCondition() && addAction(action) && actionIsReady(action)) {
            action.doAction();
        }
    }

    /**
     * 通过aidl调用{@link PluginPackageManagerService}进行安装
     *
     * @param info
     * @param listener
     */
    private void installInternal(PluginLiteInfo info, IInstallCallBack listener) {
        if (mService != null) {
            try {
                //mService.deletePackage(info, null);
                mService.install(info, listener);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        onBindService(mContext);
    }


    /**
     * 通知安装插件，如果service不存在，则将事件加入列表，启动service，待service连接之后再执行。
     *
     * @param filePath
     * @param listener
     * @param info
     */
//    @Deprecated
//    public void installApkFile(String filePath, IInstallCallBack listener, PluginLiteInfo info) {
//        PluginInstallAction action = new PluginInstallAction();
//        action.listener = listener;
//        action.info = info;
//        action.callbackHost = this;
//        if (action.meetCondition() && addAction(action) && actionIsReady(action)) {
//            action.doAction();
//        }
//    }

//    @Deprecated
//    void installApkFileInternal(String filePath, IInstallCallBack listener, PluginLiteInfo info) {
//        if (mService != null) {
//            try {
//                mService.deletePackage(info, null);
//                mService.installApkFile(filePath, listener, info);
//                return;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        onBindService(mContext);
//    }

    /**
     * 通知安装再asset中的插件，如果service不存在，则将事件加入列表，启动service，待service连接之后再执行。
     *
     * @param listener
     * @param info
     */
//    @Deprecated
//    public void installBuildinApps(PluginLiteInfo info, IInstallCallBack listener) {
//        if (info == null) {
//            PluginDebugLog.installLog(TAG, "installBuildInApps but PluginLiteInfo is null!");
//            return;
//        }
//        BuildinPluginInstallAction action = new BuildinPluginInstallAction();
//        action.listener = listener;
//        action.info = info;
//        action.callbackHost = this;
//        if (action.meetCondition() && addAction(action) && actionIsReady(action)) {
//            action.doAction();
//        }
//    }

//    @Deprecated
//    private void installBuildinAppsInternal(PluginLiteInfo info, IInstallCallBack listener) {
//        if (mService != null) {
//            try {
//                mService.deletePackage(getPackageInfo(info.packageName), null);
//                mService.installBuildinApps(info, listener);
//                return;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        onBindService(mContext);
//    }

    /**
     * 提交一个PluginUninstallAction卸载插件的Action
     *
     * @param info
     * @param observer
     */
    public void uninstall(PluginLiteInfo info, IPluginUninstallCallBack observer) {
        PluginUninstallAction action = new PluginUninstallAction();
        action.info = info;
        action.callbackHost = this;
        action.observer = observer;
        if (action.meetCondition() && addAction(action) && actionIsReady(action)) {
            action.doAction();
        }
    }

    /**
     * 通过aidl调用{@link PluginPackageManagerService}进行卸载
     *
     * @param info PluginLiteInfo
     * @return
     */
    public void uninstallInternal(PluginLiteInfo info) {
        if (mService != null) {
            try {
                mService.uninstall(info);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        onBindService(mContext);
    }

    /**
     * 执行action操作，异步执行，如果service不存在，待连接之后执行。
     *
     * @param packageInfo
     * @param callBack
     */
    public void packageAction(PluginLiteInfo packageInfo, IInstallCallBack callBack) {
        if (mService != null) {
            try {
                PluginDebugLog.runtimeLog(TAG, "packageAction service is connected and not null, call remote service");
                mService.packageAction(packageInfo, callBack);
                return;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        PluginDebugLog.runtimeLog(TAG, "packageAction service is disconnected, need to rebind");
        ExecutionPackageAction action = new ExecutionPackageAction();
        action.type = ActionType.PACKAGE_ACTION;
        action.time = System.currentTimeMillis();
        action.packageInfo = packageInfo;
        action.callBack = callBack;
        packageActionModified(action);
        onBindService(mContext);
    }

    private void packageActionModified(ExecutionPackageAction action) {
        mPackageActions.add(action);
        clearExpiredPkgAction();
    }

    private void clearExpiredPkgAction() {
        long currentTime = System.currentTimeMillis();
        synchronized (this) {
            Iterator<ExecutionPackageAction> iterator = mPackageActions.iterator();
            while (iterator.hasNext()) {
                ExecutionPackageAction action = iterator.next();
                if (currentTime - action.time >= 60 * 1000) {// 1分钟
                    PluginDebugLog.runtimeLog(TAG, "packageAction is expired, remove it");
                    if (action != null && action.callBack != null) {
                        try {
                            action.callBack.onPackageInstallFail(action.packageInfo,
                                    ErrorType.INSTALL_ERROR_CLIENT_TIME_OUT);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    iterator.remove();
                }
            }
        }
    }

    /**
     * 包依赖任务队列对象。
     */
    private class ExecutionPackageAction {

        ActionType type;// 类型：
        long time;// 时间；
        IInstallCallBack callBack;// 安装回调
        PluginLiteInfo packageInfo;//包名
    }

    enum ActionType {
        INSTALL_APK_FILE, // installApkFile
        INSTALL_BUILD_IN_APPS, // installBuiltinApps
        DELETE_PACKAGE, // deletePackage
        PACKAGE_ACTION, // packageAction
        UNINSTALL_ACTION,// uninstall
    }

    public ServiceConnection getConnection(Context context) {
        if (mServiceConnection == null) {
            mServiceConnection = new PluginPackageManagerServiceConnection(context);
        }
        return mServiceConnection;
    }

    public void release() {
        Context applicationContext = mContext.getApplicationContext();
        if (applicationContext != null) {
            if (mServiceConnection != null) {
                try {
                    applicationContext.unbindService(mServiceConnection);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mService = null;
            Intent intent = new Intent(applicationContext, PluginPackageManagerService.class);
            applicationContext.stopService(intent);
        }
    }


    /**
     * 获取已经安装的插件列表，通过aidl到{@link PluginPackageManager}中获取值，
     * 如果service不存在，直接在sharedPreference中读取值，并且启动service
     *
     * @return 返回所有安装插件信息
     */
    public List<PluginLiteInfo> getInstalledApps() {
        if (mService != null) {
            try {
                return mService.getInstalledApps();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        List<PluginLiteInfo> installedList =
                PluginPackageManager.getInstance(mContext).getInstalledPackagesDirectly();
        onBindService(mContext);
        return installedList;
    }


    /**
     * 获取插件依赖关系
     *
     * @param pkgName
     * @return
     */
    public List<String> getPluginRefs(String pkgName) {
        if (mService != null) {
            try {
                return mService.getPluginRefs(pkgName);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        onBindService(mContext);
        return PluginPackageManager.getInstance(mContext).getPluginRefsDirectly(pkgName);
    }


    /**
     * 判断某个插件是否已经安装，通过aidl到{@link PluginPackageManagerService}中获取值，如果service不存在，
     * 直接在sharedPreference中读取值，并且启动service
     *
     * @param pkgName 插件包名
     * @return 返回是否安装
     */
    public boolean isPackageInstalled(String pkgName) {
        if (mService != null) {
            try {
                return mService.isPackageInstalled(pkgName);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        boolean isInstalled = PluginPackageManager.getInstance(mContext).isPackageInstalledDirectly(pkgName);
        onBindService(mContext);
        return isInstalled;
    }

    /**
     * 判断某个插件是否可用，如果插件正在执行安装/卸载操作，则认为不可用
     *
     * @param pkgName
     * @return
     */
    public boolean isPackageAvailable(String pkgName) {

        if (mActionMap.containsKey(pkgName) && !TextUtils.isEmpty(pkgName)) {
            List<Action> actions = mActionMap.get(pkgName);
            if (actions != null && actions.size() > 0) {
                PluginDebugLog.log(TAG, actions.size() + " actions in action list for " + pkgName + " isPackageAvailable : true");
                if (PluginDebugLog.isDebug()) {
                    for (int index = 0; index < actions.size(); index++) {
                        Action action = actions.get(index);
                        if (action != null) {
                            PluginDebugLog.log(TAG, index + " action in action list: " + action.toString());
                        }
                    }
                }
                return false;
            }
        }

        boolean available = isPackageInstalled(pkgName);
        PluginDebugLog.log(TAG, pkgName + " isPackageAvailable : " + available);
        return available;
    }


    /**
     * 根据应用包名，获取插件信息，通过aidl到PackageManagerService中获取值，如果service不存在，
     * 直接在sharedPreference中读取值，并且启动service
     *
     * @param pkg 插件包名
     * @return 返回插件信息
     */
    public PluginLiteInfo getPackageInfo(String pkg) {
        if (mService != null) {
            try {
                PluginDebugLog.runtimeLog(TAG, "getPackageInfo service is connected and not null, call remote service");
                return mService.getPackageInfo(pkg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        PluginDebugLog.runtimeLog(TAG, "getPackageInfo, service is disconnected, need rebind");
        PluginLiteInfo info =
                PluginPackageManager.getInstance(mContext).getPackageInfoDirectly(pkg);
        onBindService(mContext);
        return info;

    }


    /**
     * 获取插件的{@link android.content.pm.PackageInfo}
     *
     * @param packageName
     * @return
     */
    public PluginPackageInfo getPluginPackageInfo(String packageName) {
        PluginLiteInfo pluginLiteInfo = getPackageInfo(packageName);
        PluginPackageInfo target = null;
        if (pluginLiteInfo != null) {
            target = getPluginPackageInfo(mContext, pluginLiteInfo);
        }

        return target;
    }


    /**
     * 获取插件的{@link android.content.pm.PackageInfo}
     *
     * @param context
     * @param mPackageInfo
     * @return
     */
    public PluginPackageInfo getPluginPackageInfo(Context context, PluginLiteInfo mPackageInfo) {
        PluginPackageInfo target = null;
        if (mPackageInfo != null && !TextUtils.isEmpty(mPackageInfo.packageName)) {
            if (mService != null) {
                try {
                    target = mService.getPluginPackageInfo(mPackageInfo.packageName);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                PluginPackageManager.updateSrcApkPath(context, mPackageInfo);
                if (null != context && !TextUtils.isEmpty(mPackageInfo.srcApkPath)) {
                    File file = new File(mPackageInfo.srcApkPath);
                    if (file.exists()) {
                        target = new PluginPackageInfo(ContextUtils.getOriginalContext(context), file);
                    }
                }
                onBindService(context);
            }
        }
        return target;
    }

    /**
     * 用于安装一个没有后台配置的apk
     *
     * @param mContext
     * @param mApkFile
     */
//    public void installStrangeApkFile(Context mContext, File mApkFile, IInstallCallBack mInstallCallback) {
//        if (mContext == null || mApkFile == null) {
//            PluginDebugLog.installLog(TAG, "installStrangeApkFile mContext == null or mApkFile ==null");
//            return;
//        }
//
//        PluginLiteInfo mPluginLiteInfo = new PluginLiteInfo();
//        PackageInfo mPackageInfo = mContext.getPackageManager()
//                .getPackageArchiveInfo(mApkFile.getAbsolutePath(), 0);
//        if (mPackageInfo != null) {
//            mPluginLiteInfo.packageName = mPackageInfo.packageName;
//            mPluginLiteInfo.pluginVersion = mPackageInfo.versionName;
//            installApkFile(mApkFile.getAbsolutePath(), mInstallCallback, mPluginLiteInfo);
//        }
//    }
}
