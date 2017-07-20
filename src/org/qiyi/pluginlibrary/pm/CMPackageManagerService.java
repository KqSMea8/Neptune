package org.qiyi.pluginlibrary.pm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.install.IActionFinishCallback;
import org.qiyi.pluginlibrary.install.IInstallCallBack;

import java.util.List;

/**
 * 插件安装service管理，正常情况下此Service会持续存在，
 * Created by xiepengchong on 15/10/29.
 */
public class CMPackageManagerService extends Service {

    private static CMPackageManager mManager;

    @Override
    public void onCreate() {
        mManager = CMPackageManager.getInstance(this);
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

    private ICMPackageManager.Stub initBinder() {
        return new ICMPackageManager.Stub() {

            @Override
            public List<CMPackageInfo> getInstalledApps() throws RemoteException {
                if (mManager == null) {
                    return null;
                }
                return mManager.getInstalledApps();
            }

            @Override
            public CMPackageInfo getPackageInfo(String pkg) throws RemoteException {
                if (mManager == null || TextUtils.isEmpty(pkg)) {
                    return null;
                }
                return mManager.getPackageInfo(pkg);
            }

            @Override
            public boolean isPackageInstalled(String pkg) throws RemoteException {
                return !(mManager == null || TextUtils.isEmpty(pkg)) && mManager.isPackageInstalled(pkg);
            }

            @Override
            public boolean canInstallPackage(PluginPackageInfoExt info) throws RemoteException {
                return mManager != null && info != null && mManager.canInstallPackage(info);
            }

            @Override
            public boolean canUninstallPackage(CMPackageInfo info) throws RemoteException {
                return mManager != null && info != null && mManager.canUninstallPackage(info);
            }

            @Override
            public void installApkFile(String filePath, IInstallCallBack listener,
                                       PluginPackageInfoExt pluginInfo) throws RemoteException {
                if (mManager == null || TextUtils.isEmpty(filePath)) {
                    return;
                }
                mManager.installApkFile(filePath, listener, pluginInfo);
            }

            @Override
            public void installBuildinApps(String packageName, IInstallCallBack listener, PluginPackageInfoExt info)
                    throws RemoteException {
                if (mManager == null || TextUtils.isEmpty(packageName)) {
                    return;
                }
                mManager.installBuildinApps(packageName, listener, info);
            }

            @Override
            public void deletePackage(
                    CMPackageInfo packageInfo, IPackageDeleteObserver observer) throws RemoteException {
                if (mManager == null) {
                    return;
                }
                mManager.deletePackage(packageInfo, observer);
            }

            @Override
            public boolean uninstall(CMPackageInfo packageInfo) throws RemoteException {
                return mManager != null && mManager.uninstall(packageInfo);
            }

            @Override
            public void packageAction(
                    CMPackageInfo packageInfo, IInstallCallBack callBack) throws RemoteException {
                if (mManager == null ||
                        packageInfo == null || TextUtils.isEmpty(packageInfo.packageName)) {
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
            public ApkTargetMappingNew getApkTargetMapping(String pkgName) throws RemoteException {
                if (mManager != null) {
                    return mManager.getApkTargetMapping(pkgName);
                }
                return null;
            }
        };
    }
}
