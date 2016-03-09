package org.qiyi.pluginlibrary.pm;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.qiyi.pluginlibrary.ErrorType.ErrorType;
import org.qiyi.pluginlibrary.install.IActionFinishCallback;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.install.PluginInstaller;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginnew.ApkTargetMappingNew;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by xiepengchong on 15/10/29.
 */
public class CMPackageManagerImpl {

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

    private static class ActionFinishCallback extends IActionFinishCallback.Stub {

        private String mProcessName;

        public ActionFinishCallback(String processName) {
            mProcessName = processName;
        }

        @Override
        public void onActionComplete(String packageName, int errorCode) throws RemoteException {
            PluginDebugLog.log(TAG,
                    "onActionComplete with " + packageName + " errorcode " + errorCode);
            if (mActionMap.containsKey(packageName)) {
                CopyOnWriteArrayList<Action> list = mActionMap.get(packageName);
                PluginDebugLog.log(TAG, packageName + " has " + list.size() + " in action list");
                if (list.size() > 0) {
                    if (PluginDebugLog.isDebug()) {
                        for (int index = 0; index < list.size(); index++) {
                            Action action = list.get(index);
                            if (action != null) {
                                PluginDebugLog.log(TAG, index +
                                        " action in action list: " + action.toString());
                            }
                        }
                    }

                    Action finishedAction = list.remove(0);
                    if (finishedAction != null) {
                        PluginDebugLog.log(TAG,
                                "remove done action from action list for " +
                                        finishedAction.toString());
                    }

                    if (finishedAction != null && finishedAction instanceof PluginUninstallAction) {
                        PluginDebugLog.log(TAG,
                                "PluginUninstallAction onActionComplete for " + packageName);
                        PluginUninstallAction uninstallAction =
                                (PluginUninstallAction) finishedAction;
                        if (uninstallAction != null &&
                                uninstallAction.observer != null &&
                                uninstallAction.info != null &&
                                !TextUtils.isEmpty(uninstallAction.info.packageName)) {
                            PluginDebugLog.log(TAG,
                                    "PluginUninstallAction packageDeleted for " + packageName);
                            uninstallAction.observer.packageDeleted(
                                    uninstallAction.info.packageName, errorCode);
                        }
                    }

                    int index = 0;
                    while (index < list.size()) {
                        Action action = list.get(index);
                        if (action != null) {
                            if (action.meetCondition()) {
                                PluginDebugLog.log(TAG,
                                        "start doAction for " + action.toString());
                                action.doAction();
                                break;
                            } else {
                                PluginDebugLog.log(TAG,
                                        "remove deprecate action from action list for "
                                                + action.toString());
                                list.remove(index);
                            }
                        }
                    }

                    if (list.size() == 0) {
                        PluginDebugLog.log(TAG, "remove empty action list");
                        mActionMap.remove(packageName);
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
        public PluginPackageInfoExt info;
        public CMPackageManagerImpl callbackHost;

        @Override
        public String toString() {
            StringBuilder infoBuider = new StringBuilder();
            infoBuider.append("PluginInstallAction: ");
            infoBuider.append("filePath: ").append(filePath);
            infoBuider.append(" has IInstallCallBack: ").append(listener != null ? true : false);
            if (info != null) {
                infoBuider.append(" packagename: ").append(info.packageName);
                infoBuider.append(" plugin_ver: ").append(info.plugin_ver);
                infoBuider.append(" plugin_gray_version: ").append(info.plugin_gray_ver);
                infoBuider.append(" file_source_type: ").append(info.mFileSourceType);
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
            if (mService != null && info != null && !TextUtils.isEmpty(info.packageName)) {
                boolean packageInstalled = false;
                try {
                    packageInstalled = mService.isPackageInstalled(info.packageName);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                PluginDebugLog.log(TAG, info.packageName + " PluginInstallAction " +
                        "check condition, " + "packageInstalled: " + packageInstalled);

                if (packageInstalled) {
                    CMPackageInfo packageInfo = null;
                    try {
                        packageInfo = mService.getPackageInfo(info.packageName);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    if (packageInfo != null) {
                        PluginPackageInfoExt packageInfoExt = packageInfo.pluginInfo;
                        if (packageInfoExt != null) {
                            if (versionCompare(info.plugin_ver, info.plugin_gray_ver,
                                    packageInfoExt.plugin_ver, packageInfoExt.plugin_gray_ver) > 0) {
                                canMeetCondition = true;
                            }
                        }
                    }
                } else {
                    canMeetCondition = true;
                }
            }
            PluginDebugLog.log(TAG, info.packageName +
                    "PluginInstallAction check condition with result " + canMeetCondition);
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
                callbackHost.installBuildinAppsInternal(info.packageName, listener, info);
            }
        }
    }

    private static class PluginDeleteAction implements Action {

        IPackageDeleteObserver observer;
        public PluginPackageInfoExt info;
        public CMPackageManagerImpl callbackHost;

        @Override
        public String getPackageName() {
            return info != null ? info.packageName : null;
        }

        @Override
        public String toString() {
            StringBuilder infoBuider = new StringBuilder();
            infoBuider.append("PluginDeleteAction: ");
            infoBuider.append(
                    " has IPackageDeleteObserver: ").append(observer != null ? true : false);
            if (info != null) {
                infoBuider.append(" packagename: ").append(info.packageName);
                infoBuider.append(" plugin_ver: ").append(info.plugin_ver);
                infoBuider.append(" plugin_gray_ver: ").append(info.plugin_gray_ver);
                infoBuider.append(" file_source_type: ").append(info.mFileSourceType);
            }

            return infoBuider.toString();
        }

        @Override
        public boolean meetCondition() {
            boolean canMeetCondition = false;
            if (mService != null && info != null && !TextUtils.isEmpty(info.packageName)) {
                boolean packageInstalled = false;
                try {
                    packageInstalled = mService.isPackageInstalled(info.packageName);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                PluginDebugLog.log(TAG, info.packageName + " PluginDeleteAction " +
                        "check condition, " + "packageInstalled: " + packageInstalled);

                if (packageInstalled) {
                    CMPackageInfo packageInfo = null;
                    try {
                        packageInfo = mService.getPackageInfo(info.packageName);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    if (packageInfo != null) {
                        PluginPackageInfoExt packageInfoExt = packageInfo.pluginInfo;
                        if (packageInfoExt != null) {
                            if (TextUtils.equals(info.plugin_ver, packageInfoExt.plugin_ver) &&
                                    TextUtils.equals(
                                            info.plugin_gray_ver, packageInfoExt.plugin_gray_ver)) {
                                PluginDebugLog.log(TAG, info.packageName + " PluginDeleteAction " +
                                        info.plugin_ver + ":" + info.plugin_gray_ver + " " +
                                        packageInfoExt.plugin_ver + ":" +
                                        packageInfoExt.plugin_gray_ver);
                                canMeetCondition = true;
                            }
                        }
                    }
                }
            }
            PluginDebugLog.log(TAG, info.packageName +
                    " PluginDeleteAction check condition with result " + canMeetCondition);
            return canMeetCondition;
        }

        @Override
        public void doAction() {
            if (callbackHost != null) {
                callbackHost.deletePackageInternal(info, observer);
            }
        }

        @Override
        public int getStatus() {
            return STATUS_PACKAGE_DELETING;
        }
    }

    private static class PluginUninstallAction extends PluginDeleteAction {
        @Override
        public void doAction() {
            if (callbackHost != null) {
                callbackHost.uninstallInternal(info);
            }
        }
    }

    private static ConcurrentHashMap<String, CopyOnWriteArrayList<Action>> mActionMap = new ConcurrentHashMap();

    private static final String TAG = CMPackageManagerImpl.class.getSimpleName();

    private static class CMPackageManagerServiceConnection implements ServiceConnection {

        private Context mContext;

        public CMPackageManagerServiceConnection(Context context) {
            mContext = context;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (null != service) {
                mService = ICMPackageManager.Stub.asInterface(service);
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
            if (mInstalledPkgs != null) {
                mInstalledPkgs.clear();
                mInstalledPkgs = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    }

    private static ICMPackageManager mService = null;
    private static CMPackageManagerImpl sInstance = null;
    private Context mContext;
    private static ConcurrentMap<String, CMPackageInfo> mInstalledPkgs = null;
    private ServiceConnection mServiceConnection = null;
    private static String mProcessName = null;

    /**
     * 安装包任务队列。
     */
    private static ConcurrentLinkedQueue<ExecutionPackageAction> mPackageActions = new ConcurrentLinkedQueue<ExecutionPackageAction>();

    public static CMPackageManagerImpl getInstance(Context context) {
        if (sInstance == null) {
            synchronized (CMPackageManagerImpl.class) {
                if (sInstance == null) {
                    sInstance = new CMPackageManagerImpl(context);
                }
            }
        }
        return sInstance;
    }

    private CMPackageManagerImpl(Context context) {
        mContext = context;
        mProcessName = getCurrentProcessName(mContext);
    }

    public void init() {
        onBindService(mContext);
    }

    private void onBindService(Context context) {
        if (null == context || context.getApplicationContext() == null) {
            PluginDebugLog.log(TAG, "onBindService context is null return!");
            return;
        }

        Intent intent = new Intent(context.getApplicationContext(), CMPackageManagerService.class);
        try {
            Context appContext = context.getApplicationContext();
            appContext.bindService(intent,
                    getConnection(appContext), Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            // 灰度时出现binder，从系统代码查不可能出现这个异常，添加保护
            // Caused by: java.lang.NullPointerException
            // at android.os.Parcel.readException(Parcel.java:1437)
            // at android.os.Parcel.readException(Parcel.java:1385)
            // at android.app.ActivityManagerProxy.bindService(ActivityManagerNative.java:2801)
            // at android.app.ContextImpl.bindServiceAsUser(ContextImpl.java:1489)
            // at android.app.ContextImpl.bindService(ContextImpl.java:1464)
            // at android.content.ContextWrapper.bindService(ContextWrapper.java:496)
            // at org.qiyi.pluginlibrary.pm.CMPackageManagerImpl.onBindService(Unknown Source)
            e.printStackTrace();
        }
    }

    private static void executePendingAction() {
        for (Map.Entry<String, CopyOnWriteArrayList<Action>> entry : mActionMap.entrySet()) {
            if (entry != null) {
                CopyOnWriteArrayList<Action> actions = entry.getValue();
                PluginDebugLog.log(TAG, "execute " +
                        actions.size() + " Pending Actions");
                if (actions != null) {
                    int index = 0;
                    while (index < actions.size()) {
                        Action action = actions.get(index);
                        if (action != null) {
                            if (action.meetCondition()) {
                                PluginDebugLog.log(TAG,
                                        "start doAction for pending action " + action.toString());
                                action.doAction();
                                break;
                            } else {
                                PluginDebugLog.log(TAG,
                                        "remove deprecate pending action " +
                                                "from action list for " + action.toString());
                                actions.remove(index);
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
                        CMPackageManager.getInstance(context).packageAction(action.packageName, action.callBack);
                        break;
                }
                iterator.remove();
            }
        }
    }

    /**
     * 获取已经安装的插件列表，通过aidl到CMPackageManagerService中获取值，如果service不存在，直接在sharedPreference中读取值，并且启动service
     *
     * @return 返回所有安装插件信息
     */
    public List<CMPackageInfo> getInstalledApps() {
        if (mService != null) {
            try {
                return mService.getInstalledApps();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        List<CMPackageInfo> installedList = getInstalledAppsDirectly();
        //to read the sharedPreference directly，this is a protect,not always happened
        onBindService(mContext);
        return installedList;
    }

    /**
     * 根据应用包名，获取插件信息，通过aidl到CMPackageManagerService中获取值，如果service不存在，直接在sharedPreference中读取值，并且启动service
     *
     * @param pkg 插件包名
     * @return 返回插件信息
     */
    public CMPackageInfo getPackageInfo(String pkg) {
        if (mService != null) {
            try {
                return mService.getPackageInfo(pkg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        CMPackageInfo info = getPackageInfoDirectly(pkg);
        onBindService(mContext);
        return info;

    }

    /**
     * 判断某个插件是否已经安装，通过aidl到CMPackageManagerService中获取值，如果service不存在，直接在sharedPreference中读取值，并且启动service
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

        boolean isInstalled = isPackageInstalledDirectly(pkg);
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
    public void installApkFile(String filePath,
                               IInstallCallBack listener, PluginPackageInfoExt info) {
        PluginInstallAction action = new PluginInstallAction();
        action.filePath = filePath;
        action.listener = listener;
        action.info = info;
        action.callbackHost = this;
        if (addAction(action) && actionIsReady(action) && action.meetCondition()) {
            action.doAction();
        }
    }

    void installApkFileInternal(String filePath, IInstallCallBack listener, PluginPackageInfoExt info) {
        if (mService != null) {
            try {
                mService.installApkFile(filePath, listener, info);
                return;
            } catch (RemoteException e) {
                e.printStackTrace();
                // TODO: 15/10/29 catch should do something
            }
        }
        onBindService(mContext);
    }

    /**
     * 通知安装再asset中的插件，如果service不存在，则将事件加入列表，启动service，待service连接之后再执行。
     *
     * @param packageName
     * @param listener
     * @param info
     */
    public void installBuildinApps(
            String packageName, IInstallCallBack listener, PluginPackageInfoExt info) {
        BuildinPluginInstallAction action = new BuildinPluginInstallAction();
        action.listener = listener;
        action.info = info;
        action.callbackHost = this;
        if (addAction(action) && actionIsReady(action) && action.meetCondition()) {
            action.doAction();
        }
    }

    private void installBuildinAppsInternal(
            String packageName, IInstallCallBack listener, PluginPackageInfoExt info) {
        if (mService != null) {
            try {
                mService.installBuildinApps(packageName, listener, info);
                return;
            } catch (RemoteException e) {
                e.printStackTrace();
                // TODO: 15/10/29 catch should do something
            }
        }
        onBindService(mContext);
    }

    /**
     * 删除某个插件，如果service不存在，则将事件加入列表，启动service，待service连接之后再执行。
     *
     * @param packageName 删除的插件包名
     * @param observer    删除成功回调监听
     */
    public void deletePackage(PluginPackageInfoExt info, IPackageDeleteObserver observer) {
        PluginDeleteAction action = new PluginDeleteAction();
        action.info = info;
        action.callbackHost = this;
        action.observer = observer;
        if (addAction(action) && actionIsReady(action) && action.meetCondition()) {
            action.doAction();
        }
    }

    private void deletePackageInternal(PluginPackageInfoExt info, IPackageDeleteObserver observer) {
        if (mService != null) {
            try {
                mService.deletePackage(info.packageName, observer);
                return;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        onBindService(mContext);
    }

    public void uninstall(PluginPackageInfoExt info, IPackageDeleteObserver observer) {
        PluginUninstallAction action = new PluginUninstallAction();
        action.info = info;
        action.callbackHost = this;
        action.observer = observer;
        if (addAction(action) && actionIsReady(action) && action.meetCondition()) {
            action.doAction();
        }
    }

    /**
     * 卸载插件，如果service不存在，则判断apk是否存在，如果存在，我们假设删除apk成功，暂时未考虑因内存不足或文件占用等原因导致的删除失败（此case概率较小）
     *
     * @param pkgName
     * @return
     */
    public void uninstallInternal(PluginPackageInfoExt info) {
        if (mService != null) {
            try {
                mService.uninstall(info.packageName);
                return;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        onBindService(mContext);
    }

    /**
     * 执行action操作，异步执行，如果service不存在，待连接之后执行。
     *
     * @param packageName
     * @param callBack
     */
    public void packageAction(String packageName, IInstallCallBack callBack) {
        if (mService != null) {
            try {
                mService.packageAction(packageName, callBack);
                return;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        ExecutionPackageAction action = new ExecutionPackageAction();
        action.type = ActionType.PACKAGE_ACTION;
        action.time = System.currentTimeMillis();
        action.packageName = packageName;
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
                if (currentTime - action.time >= 1 * 60 * 1000) {// 1分钟
                    if (action != null && action.callBack != null) {
                        try {
                            action.callBack.onPackageInstallFail(action.packageName,
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

        ActionType type;//类型：
        long time;//时间；
        String filePath;
        IInstallCallBack callBack;//安装回调
        PluginPackageInfoExt pluginInfo;
        String packageName;//包名
        IPackageDeleteObserver observer;
    }

    enum ActionType {
        INSTALL_APK_FILE,//installApkFile
        INSTALL_BUILD_IN_APPS,//installBuildinApps
        DELETE_PACKAGE,//deletePackage
        PACKAGE_ACTION,//packageAction
        UNINSTALL_ACTION,//uninstall
    }

    /**
     * 从sharedPreference直接读取，此case只有当service不存在时会发生，概率较小。
     *
     * @return 已安装插件列表
     */
    private List<CMPackageInfo> getInstalledAppsDirectly() {
        Iterator iterator = getInstalledPackageList().entrySet().iterator();
        ArrayList<CMPackageInfo> list = new ArrayList<CMPackageInfo>();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();
            CMPackageInfo pkg = (CMPackageInfo)entry.getValue();
            if (pkg != null && TextUtils.equals(pkg.installStatus, CMPackageManager.PLUGIN_INSTALLED)) {
                list.add(pkg);
            }
        }

        return list;
    }

    /**
     * 从sharedPreference直接读取，此case只有当service不存在时会发生，概率较小。
     *
     * @param packageName
     * @return 返回插件信息
     */
    private CMPackageInfo getPackageInfoDirectly(String packageName) {
        if (packageName == null || packageName.length() == 0) {
            return null;
        }

        CMPackageInfo info = getInstalledPackageList().get(packageName);
        if (null != info && TextUtils.equals(info.installStatus, CMPackageManager.PLUGIN_INSTALLED)) {
            return info;
        }
        return null;
    }

    /**
     * 从sharedPreference直接读取，此case只有当service不存在时会发生，概率较小。
     *
     * @param packageName
     * @return 判断是否已安装
     */
    public boolean isPackageInstalledDirectly(String packageName) {
        CMPackageInfo info = getInstalledPackageList().get(packageName);
        if (null != info && TextUtils.equals(info.installStatus, CMPackageManager.PLUGIN_INSTALLED)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 从sharedPreference直接读取，此case只有当service不存在时会发生，概率较小。
     * 经测试此文件耗时：主进程 120ms   插件进程：1s   所以尽量放到非主线程使用
     *
     * @return 返回的是素有插件的信息，其中包括安装和未安装的插件
     */
    private ConcurrentMap<String, CMPackageInfo> getInstalledPackageList() {
        ConcurrentMap<String, CMPackageInfo> installedPkgsTemp = null;
        if (mInstalledPkgs == null) { //如果是正在读取，此时的mInstalledPkgs值是不对的，所以需要从新进入获取
            installedPkgsTemp = new ConcurrentHashMap<String, CMPackageInfo>();

            SharedPreferences sp = CMPackageManager.getPreferences(mContext, PluginInstaller.SHARED_PREFERENCE_NAME);
            String jsonPkgs = sp.getString(CMPackageManager.SP_APP_LIST, "");

            if (jsonPkgs != null && jsonPkgs.length() > 0) {
                try {
                    JSONArray pkgs = new JSONArray(jsonPkgs);
                    int count = pkgs.length();
                    for (int i = 0; i < count; i++) {
                        JSONObject pkg = (JSONObject) pkgs.get(i);
                        CMPackageInfo pkgInfo = new CMPackageInfo();
                        pkgInfo.packageName = pkg.optString(CMPackageInfo.TAG_PKG_NAME);
                        pkgInfo.srcApkPath = pkg.optString(CMPackageInfo.TAG_APK_PATH);
                        pkgInfo.installStatus = pkg.optString(CMPackageInfo.TAG_INSTALL_STATUS);
                        JSONObject ext = pkg.optJSONObject(PluginPackageInfoExt.INFO_EXT);
                        if (ext != null) {
                            pkgInfo.pluginInfo = new PluginPackageInfoExt(ext);
                        } else {
                            // try to do migrate for old version
                            SharedPreferences spf = CMPackageManager.getPreferences(mContext, pkgInfo.packageName);
                            if (null != spf && spf.getInt("plugin_state", 0) == 7) {
                                PluginPackageInfoExt extInfo = new PluginPackageInfoExt();
                                extInfo.id = spf.getString("ID", "");
                                extInfo.name = spf.getString("NAME", "");
                                extInfo.ver = spf.getInt("VER", -1);
                                extInfo.crc = spf.getString("CRC", "");
                                extInfo.type = spf.getInt("TYPE", 0);
                                extInfo.desc = spf.getString("DESC", "");
                                // Old version don't have this item
                                extInfo.icon_url = "";
                                extInfo.isAllowUninstall = spf.getInt("uninstall_flag", 0);
                                extInfo.pluginTotalSize = spf.getLong("plugin_total_size", 0);
                                extInfo.local = spf.getInt("plugin_local", 0);
                                extInfo.invisible = spf.getInt("plugin_visible", 0);
                                extInfo.scrc = spf.getString("SCRC", "");
                                extInfo.url = spf.getString("URL", "");
                                extInfo.mPluginInstallMethod = spf.getString("INSTALL_METHOD",
                                        CMPackageManager.PLUGIN_METHOD_DEFAULT);
                                pkgInfo.pluginInfo = extInfo;
                            } else {
                                // 如果存在packageinfo package name信息但是没有详细的插件信息，认为不是合法的配置
                                // TODO 需要考虑本地APK加载或者SO jar加载情况。
                                continue;
                            }
                        }
                        if (!mContext.getPackageName().equals(mProcessName)) { //主进程不需要各个插件信息，
                            ApkTargetMappingNew targetInfo = new ApkTargetMappingNew(mContext,
                                    new File(pkgInfo.srcApkPath));
                            pkgInfo.targetInfo = targetInfo;


                            if (pkgInfo.pluginInfo != null
                                    && TextUtils.isEmpty(pkgInfo.pluginInfo.plugin_ver)
                                    && pkgInfo.targetInfo != null) {
                                pkgInfo.pluginInfo.plugin_ver = pkgInfo.targetInfo.getVersionName();    //从配置文件中读取，正常不应该为null
                            }
                        }
                        installedPkgsTemp.put(pkgInfo.packageName, pkgInfo);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mInstalledPkgs = installedPkgsTemp;
        }
        return mInstalledPkgs;
    }

    private String getCurrentProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo process : manager.getRunningAppProcesses()) {
            if (process.pid == pid) {
                return process.processName;
            }
        }

        //try to read process name in /proc/pid/cmdline if no result from activity manager
        String cmdline = null;
        try {
            BufferedReader processFileReader = new BufferedReader(
                    new FileReader(String.format("/proc/%d/cmdline", Process.myPid())));
            cmdline = processFileReader.readLine().trim();
            processFileReader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return cmdline;
    }

    public ServiceConnection getConnection(Context context) {
        if (mServiceConnection == null) {
            mServiceConnection = new CMPackageManagerServiceConnection(context);
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
                Intent intent = new Intent(applicationContext, CMPackageManagerService.class);
                applicationContext.stopService(intent);
            }
        }
    }

    public boolean isPackageAvailable(String packageName)  {
        if (mActionMap.contains(packageName) && !TextUtils.isEmpty(packageName)) {
            List<Action> actions = mActionMap.get(packageName);
            if (actions != null && actions.size() > 0) {
                PluginDebugLog.log(TAG, actions.size() + " actions in action list for "
                        + packageName + " isPackageAvailable : true");
                if (PluginDebugLog.isDebug()) {
                    for (int index = 0; index < actions.size(); index ++) {
                        Action action = actions.get(index);
                        if (action != null) {
                            PluginDebugLog.log(TAG, index +
                                    " action in action list: " + action.toString());
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

    public int getPackageStatus(String packageName) {
        if (mActionMap.contains(packageName)) {
            List<Action> list = mActionMap.get(packageName);
            if (list != null && list.size() > 0) {
                PluginDebugLog.log(TAG, list.size() + " actions in action list for "
                        + packageName + " isPackageAvailable : true");
                if (PluginDebugLog.isDebug()) {
                    for (int index = 0; index < list.size(); index ++) {
                        Action action = list.get(index);
                        if (action != null) {
                            PluginDebugLog.log(TAG, index +
                                    " action in action list: " + action.toString());
                        }
                    }
                }
                Action action = list.get(0);
                if (action != null) {
                    return action.getStatus();
                }
            }
        }

        if (mService != null) {
            boolean packageInstalled = false;
            try {
                packageInstalled = mService.isPackageInstalled(packageName);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if (packageInstalled) {
                return STATUS_PACKAGE_INSTALLED;
            }
        }

        return STATUS_PACKAGE_NOT_INSTALLED;
    }

    private static int versionCompare(String leftPluginVersion, String leftPluginGreyVersion,
                                      String rightPluginVersion, String rightPluginGreyVersion) {

        PluginDebugLog.log(TAG, "version compare :" + leftPluginVersion + ":" +
                leftPluginGreyVersion + " " + rightPluginVersion + ":" + rightPluginGreyVersion);
        int pluginVersionCompareResult =
                PluginInstaller.comparePluginVersion(leftPluginVersion, rightPluginVersion);

        if (pluginVersionCompareResult == 0) {
            if (TextUtils.isEmpty(leftPluginGreyVersion) &&
                    !TextUtils.isEmpty(rightPluginGreyVersion)) {
                pluginVersionCompareResult = 1;
            } else if (!TextUtils.isEmpty(leftPluginGreyVersion) &&
                    TextUtils.isEmpty(rightPluginGreyVersion)) {
                pluginVersionCompareResult = -1;
            } else {
                // 与基线灰度升级策略一致，同一个主版本号下如果灰度版本号不同不能升级，均认为已安装
                pluginVersionCompareResult = 0;
            }
        }

        PluginDebugLog.log(TAG, "version compare with result: " + pluginVersionCompareResult);
        return pluginVersionCompareResult;
    }
}
