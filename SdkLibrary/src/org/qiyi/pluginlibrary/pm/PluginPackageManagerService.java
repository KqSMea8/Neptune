package org.qiyi.pluginlibrary.pm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.install.IActionFinishCallback;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import java.util.List;

/**
 * 插件安装service管理，正常情况下此Service会持续存在，
 * 该Service运行在主进程，所有的操作都代理给PluginPackageManager实现
 *
 * Created by xiepengchong on 15/10/29.
 */
public class PluginPackageManagerService extends Service {
    private static final String TAG = "PluginPackageManagerService";

    private static PluginPackageManager mManager;

    @Override
    public void onCreate() {
        mManager = PluginPackageManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return initBinder();
    }

    private IPluginPackageManager.Stub initBinder() {
        return new IPluginPackageManager.Stub() {

            @Override
            public List<PluginLiteInfo> getInstalledApps() throws RemoteException {
                if (mManager == null) {
                    return null;
                }
                return mManager.getInstalledApps();
            }

            @Override
            public PluginLiteInfo getPackageInfo(String pkg) throws RemoteException {
                if (mManager == null || TextUtils.isEmpty(pkg)) {
                    return null;
                }
                return mManager.getPackageInfo(pkg);
            }

            @Override
            public boolean isPackageInstalled(String pkg) throws RemoteException {
                if (mManager == null || TextUtils.isEmpty(pkg)) {
                    return false;
                }
                return mManager.isPackageInstalled(pkg);
            }

            @Override
            public boolean canInstallPackage(PluginLiteInfo info) throws RemoteException {
                if (mManager == null || info == null || TextUtils.isEmpty(info.packageName)) {
                    return false;
                }
                return mManager.canInstallPackage(info);
            }

            @Override
            public boolean canUninstallPackage(PluginLiteInfo info) throws RemoteException {
                if (mManager == null || info == null || TextUtils.isEmpty(info.packageName)) {
                    return false;
                }
                return mManager.canUninstallPackage(info);
            }

            @Override
            public void installApkFile(String filePath, IInstallCallBack listener,
                                       PluginLiteInfo pluginInfo) throws RemoteException {
                if (mManager == null || TextUtils.isEmpty(filePath)) {
                    return;
                }
                mManager.installApkFile(filePath, listener, pluginInfo);
            }

            @Override
            public void installBuildinApps(PluginLiteInfo info, IInstallCallBack listener)
                    throws RemoteException {
                if (mManager == null || info == null || TextUtils.isEmpty(info.packageName)) {
                    return;
                }
                mManager.installBuildinApps(info, listener);
            }

            @Override
            public void deletePackage(
                    PluginLiteInfo packageInfo, IPluginUninstallCallBack observer) throws RemoteException {
                if (mManager == null) {
                    return;
                }
                mManager.deletePackage(packageInfo, observer);
            }

            @Override
            public boolean uninstall(PluginLiteInfo packageInfo) throws RemoteException {
                return mManager != null && mManager.uninstall(packageInfo);
            }

            @Override
            public void packageAction(
                    PluginLiteInfo packageInfo, IInstallCallBack callBack) throws RemoteException {
                if (mManager == null ||
                        packageInfo == null || TextUtils.isEmpty(packageInfo.packageName)) {
                    PluginDebugLog.runtimeLog(TAG, "packageAction param error, packageInfo is null or packageName is empty");
                    return;
                }
                mManager.packageAction(packageInfo, callBack);
            }

            @Override
            public void setActionFinishCallback(
                    IActionFinishCallback actionFinishCallback) throws RemoteException {
                if (mManager != null) {
                    mManager.setActionFinishCallback(actionFinishCallback);
                }
            }

            @Override
            public PluginPackageInfo getPluginPackageInfo(String pkgName) throws RemoteException {
                if (mManager != null) {
                    return mManager.getPluginPackageInfo(pkgName);
                }
                return null;
            }

            @Override
            public List<String> getPluginRefs(String pkgName) throws RemoteException {
                if (mManager != null) {
                    return mManager.getPluginRefs(pkgName);
                }
                return null;
            }
        };
    }
}
