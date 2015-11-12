package org.qiyi.pluginlibrary.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.qiyi.pluginlibrary.ErrorType.ErrorType;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.install.PluginInstaller;
import org.qiyi.pluginnew.ApkTargetMappingNew;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by xiepengchong on 15/10/29.
 */
public class CMPackageManagerImpl {

    private static ICMPackageManager mService = null;
    private static CMPackageManagerImpl sInstance = null;
    private Context mContext;
    /**
     * 安装包任务队列。
     */
    private List<ExecutionPackageAction> mPackageActions = Collections.synchronizedList(new LinkedList<ExecutionPackageAction>());

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
    }

    public void init() {
        onBindService(mContext);
    }

    private void onBindService(Context context) {

        Intent intent = new Intent(context, CMPackageManagerService.class);
        context.bindService(intent, new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                if (null != iBinder) {
                    mService = ICMPackageManager.Stub.asInterface(iBinder);
                }
                if (mService != null) {
                    executePackageAction();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mService = null;
            }
        }, Context.BIND_AUTO_CREATE);
    }


    /**
     * 执行之前为执行的操做
     */
    private void executePackageAction() {

        Iterator<ExecutionPackageAction> iterator = mPackageActions.iterator();
        while (iterator.hasNext()) {
            ExecutionPackageAction action = iterator.next();
            ActionType type = action.type;
            switch (type) {
                case DELETE_PACKAGE:
                    deletePackage(action.packageName, action.observer);
                    break;
                case INSTALL_APK_FILE:
                    installApkFile(action.filePath, action.callBack, action.pluginInfo);
                    break;
                case INSTALL_BUILD_IN_APPS:
                    installBuildinApps(action.packageName, action.callBack, action.pluginInfo);
                    break;
                case PACKAGE_ACTION:
                    packageAction(action.packageName, action.callBack);
                    break;
                case UNINSTALL_ACTION:
                    uninstall(action.packageName);
                    break;
            }
            iterator.remove();
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

    /**
     * 通知安装插件，如果service不存在，则将事件加入列表，启动service，待service连接之后再执行。
     *
     * @param filePath
     * @param listener
     * @param info
     */
    public void installApkFile(String filePath, IInstallCallBack listener, PluginPackageInfoExt info) {
        if (mService != null) {
            try {
                mService.installApkFile(filePath, listener, info);
                return;
            } catch (RemoteException e) {
                e.printStackTrace();
                // TODO: 15/10/29 catch should do something
            }
        }
        ExecutionPackageAction action = new ExecutionPackageAction();
        action.type = ActionType.INSTALL_APK_FILE;
        action.time = System.currentTimeMillis();
        action.filePath = filePath;
        action.callBack = listener;
        action.pluginInfo = info;
        packageActionModified(action);
        onBindService(mContext);

    }

    /**
     * 通知安装再asset中的插件，如果service不存在，则将事件加入列表，启动service，待service连接之后再执行。
     *
     * @param packageName
     * @param listener
     * @param info
     */
    public void installBuildinApps(String packageName, IInstallCallBack listener, PluginPackageInfoExt info) {
        if (mService != null) {
            try {
                mService.installBuildinApps(packageName, listener, info);
                return;
            } catch (RemoteException e) {
                e.printStackTrace();
                // TODO: 15/10/29 catch should do something
            }
        }
        ExecutionPackageAction action = new ExecutionPackageAction();
        action.type = ActionType.INSTALL_BUILD_IN_APPS;
        action.time = System.currentTimeMillis();
        action.packageName = packageName;
        action.callBack = listener;
        action.pluginInfo = info;
        packageActionModified(action);
        onBindService(mContext);
    }

    /**
     * 删除某个插件，如果service不存在，则将事件加入列表，启动service，待service连接之后再执行。
     *
     * @param packageName 删除的插件包名
     * @param observer    删除成功回调监听
     */
    public void deletePackage(String packageName, IPackageDeleteObserver observer) {
        if (mService != null) {
            try {
                mService.deletePackage(packageName, observer);
                return;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        ExecutionPackageAction action = new ExecutionPackageAction();
        action.type = ActionType.DELETE_PACKAGE;
        action.time = System.currentTimeMillis();
        action.packageName = packageName;
        action.observer = observer;
        packageActionModified(action);
        onBindService(mContext);
    }

    /**
     * 卸载插件，如果service不存在，则判断apk是否存在，如果存在，我们假设删除apk成功，暂时未考虑因内存不足或文件占用等原因导致的删除失败（此case概率较小）
     *
     * @param pkgName
     * @return
     */
    public boolean uninstall(String pkgName) {
        if (mService != null) {
            try {
                return mService.uninstall(pkgName);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        boolean uninstallFlag;
        ExecutionPackageAction action = new ExecutionPackageAction();
        action.type = ActionType.UNINSTALL_ACTION;
        action.time = System.currentTimeMillis();
        action.packageName = pkgName;
        packageActionModified(action);
        onBindService(mContext);
        File apk = PluginInstaller.getInstalledApkFile(mContext, pkgName);
        if (apk != null && apk.exists()) {  //assume that if the apk is exist,it will delete successful,
            uninstallFlag = true;
        } else {
            uninstallFlag = false;
        }
        return uninstallFlag;
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
        Enumeration<CMPackageInfo> packages = getInstalledPackageList().elements();
        ArrayList<CMPackageInfo> list = new ArrayList<CMPackageInfo>();
        while (packages.hasMoreElements()) {
            CMPackageInfo pkg = packages.nextElement();
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
     *
     * @return 返回的是素有插件的信息，其中包括安装和未安装的插件
     */
    private Hashtable<String, CMPackageInfo> getInstalledPackageList() {
        Hashtable<String, CMPackageInfo> mInstalledPkgs;
        mInstalledPkgs = new Hashtable<String, CMPackageInfo>();

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
                    ApkTargetMappingNew targetInfo = new ApkTargetMappingNew(mContext,
                            new File(pkgInfo.srcApkPath));
                    pkgInfo.targetInfo = targetInfo;
                    if (pkgInfo.pluginInfo != null
                            && TextUtils.isEmpty(pkgInfo.pluginInfo.plugin_ver)
                            && pkgInfo.targetInfo != null) {
                        pkgInfo.pluginInfo.plugin_ver = pkgInfo.targetInfo.getVersionName();
                    }

                    mInstalledPkgs.put(pkgInfo.packageName, pkgInfo);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mInstalledPkgs;
    }
}
