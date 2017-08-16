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
public class PluginPackageManagerService extends Service {

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
                return !(mManager == null || TextUtils.isEmpty(pkg)) && mManager.isPackageInstalled(pkg);
            }

            @Override
            public boolean canInstallPackage(PluginLiteInfo info) throws RemoteException {
                return mManager != null && info != null && mManager.canInstallPackage(info);
            }

            @Override
            public boolean canUninstallPackage(PluginLiteInfo info) throws RemoteException {
                return mManager != null && info != null && mManager.canUninstallPackage(info);
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
            public void installBuildinApps(PluginLiteInfo info,IInstallCallBack listener)
                    throws RemoteException {
                if (mManager == null || TextUtils.isEmpty(info.packageName)) {
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
