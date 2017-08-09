package org.qiyi.pluginlibrary.pm;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.ErrorType.ErrorType;
import org.qiyi.pluginlibrary.install.IActionFinishCallback;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.install.PluginInstaller;
import org.qiyi.pluginlibrary.utils.ContextUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 此类的功能和{@link PluginPackageManager}基本一致<br/>
 * 只不过同一个功能这个类可以在任何进程使用<br>
 * {@link PluginPackageManager}只能在主进程使用
 */
public class PluginPackageManagerNative {
    private static final String TAG = PluginInstaller.TAG+"_Native";

    private static final int STATUS_PACKAGE_INSTALLED = 0x00;
    private static final int STATUS_PACKAGE_INSTALLING = 0x01;
    private static final int STATUS_PACAKGE_UPDATING = 0x02;
    private static final int STATUS_PACKAGE_DELETING = 0x03;
    private static final int STATUS_PACKAGE_NOT_INSTALLED = 0x04;


    private interface Action {
        String getPackageName();

        boolean meetCondition();

        void doAction();

        int getStatus();
    }

    /**
     * Action执行完毕回调
     */
    private static class ActionFinishCallback extends IActionFinishCallback.Stub {

        private String mProcessName;

        public ActionFinishCallback(String processName) {
            mProcessName = processName;
        }

        @Override
        public void onActionComplete(String packageName, int errorCode) throws RemoteException {
            PluginDebugLog.installFormatLog(TAG, "onActionComplete with %s,errorCode:%d",packageName ,  errorCode);
            if (mActionMap.containsKey(packageName)) {
                CopyOnWriteArrayList<Action> list = mActionMap.get(packageName);
                if (null == list) {
                    return;
                }
                synchronized (list) {
                    PluginDebugLog.installFormatLog(TAG, "%s has %d action in list!",packageName,list.size());
                    if (list.size() > 0) {
                        if (PluginDebugLog.isDebug()) {
                            for (int index = 0; index < list.size(); index++) {
                                Action action = list.get(index);
                                if (action != null) {
                                    PluginDebugLog.installFormatLog(TAG,
                                            "index %d action :%s",index , action.toString());
                                }
                            }
                        }

                        Action finishedAction = list.remove(0);
                        if (finishedAction != null) {
                            PluginDebugLog.installFormatLog(TAG,
                                    "get and remove first action:%s ",finishedAction.toString());
                        }

                        if (finishedAction != null && finishedAction instanceof PluginUninstallAction) {
                            PluginDebugLog.installFormatLog(TAG,
                                    "this is PluginUninstallAction  for :%s",packageName);
                            PluginUninstallAction uninstallAction = (PluginUninstallAction) finishedAction;
                            if (uninstallAction != null && uninstallAction.observer != null && uninstallAction.info != null
                                    && !TextUtils.isEmpty(uninstallAction.info.packageName)) {
                                PluginDebugLog.installFormatLog(TAG, "PluginUninstallAction packageDeleted for %s",packageName);
                                uninstallAction.observer.onPluginUnintall(uninstallAction.info.packageName, errorCode);
                            }
                        }

                        int index = 0;
                        PluginDebugLog.installFormatLog(TAG,"start find can execute action ...");
                        while (index < list.size()) {
                            Action action = list.get(index);
                            if (action != null) {
                                if (action.meetCondition()) {
                                    PluginDebugLog.installFormatLog(TAG,
                                            "doAction for %s and action is %s" ,packageName,
                                            action.toString());
                                    action.doAction();
                                    break;
                                } else {
                                    PluginDebugLog.installFormatLog(TAG,
                                            "remove deprecate action of %s,and action:%s "
                                            ,packageName, action.toString());
                                    list.remove(index);
                                }
                            }
                        }

                        if (list.size() == 0) {
                            PluginDebugLog.installFormatLog(TAG,
                                    "remove empty action list of %s",packageName);
                            mActionMap.remove(packageName);
                        }
                    }
                }
            }
        }

        @Override
        public String getProcessName() throws RemoteException {
            return mProcessName;
        }
    }

    private static class PluginInstallAction implements Action {

        public String filePath;
        public IInstallCallBack listener;
        public PluginLiteInfo info;
        public PluginPackageManagerNative callbackHost;

        @Override
        public String toString() {
            StringBuilder infoBuider = new StringBuilder();
            infoBuider.append("PluginInstallAction: ");
            infoBuider.append("filePath: ").append(filePath);
            infoBuider.append(" has IInstallCallBack: ").append(listener != null);
            if (info != null) {
                infoBuider.append(" packagename: ").append(info.packageName);
                infoBuider.append(" plugin_ver: ").append(info.mPluginVersion);
                infoBuider.append(" plugin_gray_version: ").append(info.mPluginGrayVersion);
            }
            return infoBuider.toString();
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
                // is not connected, so that the action can be added in action
                // list.
                canMeetCondition = true;
            }
            if (info != null) {
                PluginDebugLog.installFormatLog(TAG, "%s 's PluginInstallAction meetCondition:%s",
                        info.packageName,String.valueOf(canMeetCondition));
            }
            return canMeetCondition;
        }

        @Override
        public void doAction() {
            if (callbackHost != null) {
                callbackHost.installApkFileInternal(filePath, listener, info);
            }
        }

        @Override
        public int getStatus() {
            return STATUS_PACKAGE_INSTALLING;
        }
    }

    private static class BuildinPluginInstallAction extends PluginInstallAction {
        @Override
        public void doAction() {
            if (callbackHost != null) {
                callbackHost.installBuildinAppsInternal(info, listener);
            }
        }
    }

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
                infoBuilder.append(" packagename: ").append(info.packageName);
                infoBuilder.append(" plugin_ver: ").append(info.mPluginVersion);
                infoBuilder.append(" plugin_gray_ver: ").append(info.mPluginGrayVersion);
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
            PluginDebugLog.installFormatLog(TAG,
                    "%s 's PluginDeleteAction canMeetCondition %s",info.packageName,canMeetCondition);
            return canMeetCondition;
        }

        @Override
        public void doAction(){
            if (callbackHost != null) {
                callbackHost.uninstallInternal(info);
            }
        }

        @Override
        public int getStatus() {
            return STATUS_PACKAGE_DELETING;
        }
    }



    private static ConcurrentHashMap<String, CopyOnWriteArrayList<Action>> mActionMap =
            new ConcurrentHashMap<String, CopyOnWriteArrayList<Action>>();



    private static class PluginPackageManagerServiceConnection implements ServiceConnection {

        private Context mContext;

        public PluginPackageManagerServiceConnection(Context context) {
            mContext = context;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (null != service) {
                mService = IPluginPackageManager.Stub.asInterface(service);
            }
            if (mService != null) {
                try {
                    mService.setActionFinishCallback(new ActionFinishCallback(mProcessName));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                executePackageAction(mContext);
                executePendingAction();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    }
    private static IPluginPackageManager mService = null;
    private static PluginPackageManagerNative sInstance = null;
    private Context mContext;
    private ServiceConnection mServiceConnection = null;
    private static String mProcessName = null;

    /**
     * 安装包任务队列。
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
        mProcessName = getCurrentProcessName(mContext);
    }

    private void init() {
        onBindService(mContext);
    }


    public void setPackageInfoManager(IVerifyPluginInfo packageInfoManager) {
        PluginPackageManager.setVerifyPluginInfoImpl(packageInfoManager);
    }

    private void onBindService(Context context) {
        if (null == context) {
            PluginDebugLog.log(TAG, "onBindService context is null return!");
            return;
        }

        Intent intent = new Intent(context, PluginPackageManagerService.class);
        try {
            context.bindService(intent, getConnection(context), Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            // 灰度时出现binder，从系统代码查不可能出现这个异常，添加保护
            // Caused by: java.lang.NullPointerException
            // at android.os.Parcel.readException(Parcel.java:1437)
            // at android.os.Parcel.readException(Parcel.java:1385)
            // at
            // android.app.ActivityManagerProxy.bindService(ActivityManagerNative.java:2801)
            // at
            // android.app.ContextImpl.bindServiceAsUser(ContextImpl.java:1489)
            // at android.app.ContextImpl.bindService(ContextImpl.java:1464)
            // at
            // android.content.ContextWrapper.bindService(ContextWrapper.java:496)
            // at
            // org.qiyi.pluginlibrary.pm.PluginPackageManagerNative.onBindService(Unknown
            // Source)
            e.printStackTrace();
        }
    }

    private static void executePendingAction() {
        for (Map.Entry<String, CopyOnWriteArrayList<Action>> entry : mActionMap.entrySet()) {
            if (entry != null) {
                CopyOnWriteArrayList<Action> actions = entry.getValue();
                PluginDebugLog.installFormatLog(TAG, "execute %d pending actions!",actions.size());
                if (actions != null) {
                    synchronized (actions) {
                        int index = 0;
                        while (index < actions.size()) {
                            Action action = actions.get(index);
                            if (action != null) {
                                if (action.meetCondition()) {
                                    PluginDebugLog.installFormatLog(TAG, "start doAction for pending action %s" , action.toString());
                                    action.doAction();
                                    break;
                                } else {
                                    PluginDebugLog.installFormatLog(TAG, "remove deprecate pending action from action list for %s" , action.toString());
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
     * 执行之前为执行的操做
     */
    private static void executePackageAction(Context context) {
        if (context != null) {
            Iterator<ExecutionPackageAction> iterator = mPackageActions.iterator();
            while (iterator.hasNext()) {
                ExecutionPackageAction action = iterator.next();
                ActionType type = action.type;
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
     * 根据应用包名，获取插件信息，通过aidl到CMPackageManagerService中获取值，如果service不存在，
     * 直接在sharedPreference中读取值，并且启动service
     *
     * @param pkg 插件包名
     * @return 返回插件信息
     */
    public PluginLiteInfo getPackageInfo(String pkg) {
        if (mService != null) {
            try {
                return mService.getPackageInfo(pkg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        PluginLiteInfo info =
                PluginPackageManager.getInstance(mContext).getPackageInfoDirectly(pkg);
        onBindService(mContext);
        return info;

    }

    /**
     * 获取插件依赖关系
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
     * 判断某个插件是否已经安装，通过aidl到CMPackageManagerService中获取值，如果service不存在，
     * 直接在sharedPreference中读取值，并且启动service
     *
     * @param pkg 插件包名
     * @return 返回是否安装
     */
    public boolean isPackageInstalled(String pkg) {
        if (mService != null) {
            try {
                return mService.isPackageInstalled(pkg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        boolean isInstalled =
                PluginPackageManager.getInstance(mContext).isPackageInstalledDirectly(pkg);
        onBindService(mContext);
        return isInstalled;
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
     * 通知安装插件，如果service不存在，则将事件加入列表，启动service，待service连接之后再执行。
     *
     * @param filePath
     * @param listener
     * @param info
     */
    public void installApkFile(String filePath, IInstallCallBack listener, PluginLiteInfo info) {
        PluginInstallAction action = new PluginInstallAction();
        action.filePath = filePath;
        action.listener = listener;
        action.info = info;
        action.callbackHost = this;
        if (action.meetCondition() && addAction(action) && actionIsReady(action)) {
            action.doAction();
        }
    }

    void installApkFileInternal(String filePath, IInstallCallBack listener, PluginLiteInfo info) {
        if (mService != null) {
            try {
                mService.deletePackage(info, null);
                mService.installApkFile(filePath, listener, info);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        onBindService(mContext);
    }

    /**
     * 通知安装再asset中的插件，如果service不存在，则将事件加入列表，启动service，待service连接之后再执行。
     *
     * @param listener
     * @param info
     */
    public void installBuildinApps(PluginLiteInfo info, IInstallCallBack listener) {
        if(info == null){
            PluginDebugLog.installLog(TAG,"installBuildInApps but PluginLiteInfo is null!");
            return;
        }
        BuildinPluginInstallAction action = new BuildinPluginInstallAction();
        action.listener = listener;
        action.info = info;
        action.callbackHost = this;
        if (action.meetCondition() && addAction(action) && actionIsReady(action)) {
            action.doAction();
        }
    }

    private void installBuildinAppsInternal(PluginLiteInfo info,IInstallCallBack listener) {
        if (mService != null) {
            try {
                mService.deletePackage(getPackageInfo(info.packageName), null);
                mService.installBuildinApps(info, listener);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        onBindService(mContext);
    }

//    /**
//     * 删除某个插件，如果service不存在，则将事件加入列表，启动service，待service连接之后再执行。
//     *
//     * @param observer 删除成功回调监听
//     */
//    public void deletePackage(PluginLiteInfo info, IPackageDeleteObserver observer) {
//        PluginDeleteAction action = new PluginDeleteAction();
//        action.info = info;
//        action.callbackHost = this;
//        action.observer = observer;
//        if (action.meetCondition() && addAction(action) && actionIsReady(action)) {
//            action.doAction();
//        }
//    }

//    private void deletePackageInternal(PluginLiteInfo info, IPackageDeleteObserver observer) {
//        if (mService != null) {
//            try {
//                mService.deletePackage(info, observer);
//                return;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        onBindService(mContext);
//    }

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
     * 卸载插件，如果service不存在，则判断apk是否存在，如果存在，我们假设删除apk成功，暂时未考虑因内存不足或文件占用等原因导致的删除失败（
     * 此case概率较小）
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
                mService.packageAction(packageInfo, callBack);
                return;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
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
                    if (action != null && action.callBack != null) {
                        try {
                            action.callBack.onPackageInstallFail(action.packageInfo.packageName,
                                    ErrorType.ERROR_CLIENT_TIME_OUT);
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
        String filePath;
        IInstallCallBack callBack;// 安装回调
        PluginLiteInfo packageInfo;//包名
        IPluginUninstallCallBack observer;
    }

    enum ActionType {
        INSTALL_APK_FILE, // installApkFile
        INSTALL_BUILD_IN_APPS, // installBuildinApps
        DELETE_PACKAGE, // deletePackage
        PACKAGE_ACTION, // packageAction
        UNINSTALL_ACTION,// uninstall
    }


    private String getCurrentProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo process : manager.getRunningAppProcesses()) {
            if (process.pid == pid) {
                return process.processName;
            }
        }

        // try to read process name in /proc/pid/cmdline if no result from
        // activity manager
        String cmdline = null;
        try {
            BufferedReader processFileReader = new BufferedReader(new FileReader(String.format("/proc/%d/cmdline", Process.myPid())));
            cmdline = processFileReader.readLine().trim();
            processFileReader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return cmdline;
    }

    public ServiceConnection getConnection(Context context) {
        if (mServiceConnection == null) {
            mServiceConnection = new PluginPackageManagerServiceConnection(context);
        }
        return mServiceConnection;
    }

    public void exit() {
        if (mContext != null) {
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
    }

    public boolean isPackageAvailable(String packageName) {
        if (mActionMap.contains(packageName) && !TextUtils.isEmpty(packageName)) {
            List<Action> actions = mActionMap.get(packageName);
            if (actions != null && actions.size() > 0) {
                PluginDebugLog.log(TAG, actions.size() + " actions in action list for " + packageName + " isPackageAvailable : true");
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

        boolean available = isPackageInstalled(packageName);
        PluginDebugLog.log(TAG, packageName + " isPackageAvailable : " + available);
        return available;
    }



    /**
     * 获取插件的{@link android.content.pm.PackageInfo}
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
                PluginPackageManager.updateSrcApkPath(context,mPackageInfo);
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


}
