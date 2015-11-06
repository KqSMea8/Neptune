package org.qiyi.pluginlibrary.pm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.install.IInstallCallBack;

import java.util.List;

/** 插件安装service管理，正常情况下此Service会持续存在，
 * Created by xiepengchong on 15/10/29.
 */
public class CMPackageManagerService extends Service {


    private static Context mContext;
    private static CMPackageManager mManager;

    @Override
    public void onCreate(){
        mContext = this;
        mManager = CMPackageManager.getInstance(mContext);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return initBinder();
    }



    private ICMPackageManager.Stub initBinder(){
        return new ICMPackageManager.Stub(){

            @Override
            public List<CMPackageInfo> getInstalledApps() throws RemoteException {
                if(mManager == null){
                    return null;
                }
                return mManager.getInstalledApps();
            }

            @Override
            public CMPackageInfo getPackageInfo(String pkg) throws RemoteException {
                if(mManager == null || TextUtils.isEmpty(pkg)){
                    return null;
                }
                return mManager.getPackageInfo(pkg);
            }

            @Override
            public boolean isPackageInstalled(String pkg) throws RemoteException {
                if(mManager == null || TextUtils.isEmpty(pkg)){
                    return false;
                }
                return mManager.isPackageInstalled(pkg);
            }

            @Override
            public void installApkFile(String filePath, IInstallCallBack listener, PluginPackageInfoExt pluginInfo) throws RemoteException {
                if(mManager == null || TextUtils.isEmpty(filePath)){
                    return;
                }
                mManager.installApkFile(filePath, listener, pluginInfo);
            }

            @Override
            public void installBuildinApps(String packageName, IInstallCallBack listener, PluginPackageInfoExt info) throws RemoteException {
                if(mManager == null || TextUtils.isEmpty(packageName)){
                    return;
                }
                mManager.installBuildinApps(packageName, listener, info);
            }

            @Override
            public void deletePackage(String packageName, IPackageDeleteObserver observer) throws RemoteException {
                if(mManager == null || TextUtils.isEmpty(packageName)){
                    return;
                }
                mManager.deletePackage(packageName, observer);
            }

            @Override
            public boolean uninstall(String pkgName) throws RemoteException {
                if(mManager == null || TextUtils.isEmpty(pkgName)){
                    return false;
                }
                return mManager.uninstall(pkgName);
            }

            @Override
            public void packageAction(String packageName, IInstallCallBack callBack) throws RemoteException {
                if(mManager == null || TextUtils.isEmpty(packageName)){
                    return;
                }
                mManager.packageAction(packageName, callBack);
            }
        };
    }
}
