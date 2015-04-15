package org.qiyi.pluginlibrary.component.service;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.qiyi.pluginlibrary.ProxyEnvironment;
import org.qiyi.pluginlibrary.adapter.ServiceProxyAdapter;
import org.qiyi.pluginlibrary.component.CMApplication;
import org.qiyi.pluginlibrary.component.CMContextWrapper;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.IBinder;


public abstract class CMService extends CMContextWrapper {
    public static final int START_CONTINUATION_MASK = 15;
    public static final int START_FLAG_REDELIVERY = 1;
    public static final int START_FLAG_RETRY = 2;
    public static final int START_NOT_STICKY = 2;
    public static final int START_REDELIVER_INTENT = 3;
    public static final int START_STICKY = 1;
    // public static final int START_STICKY_COMPATIBILITY;
     Service service;
     ServiceProxyAdapter serviceProxy;

    public boolean isServiceRunning(){
    	return false;
    }
    public CMService() {
        super(null);
    }

    public boolean bindService(Intent paramIntent, ServiceConnection paramServiceConnection, int paramInt) {
        return this.serviceProxy.proxyBindService(paramIntent, paramServiceConnection, paramInt);
    }

    protected void dump(FileDescriptor paramFileDescriptor, PrintWriter paramPrintWriter, String[] paramArrayOfString) {
        this.serviceProxy.proxyDump(paramFileDescriptor, paramPrintWriter, paramArrayOfString);
    }

    protected void finalize() throws Throwable {
        this.serviceProxy.proxyFinalize();
    }

    public final CMApplication getApplication() {
        return ProxyEnvironment.getInstance(getTargetPackageName()).getApplication();
    }

    public PackageManager getPackageManager() {
        return this.serviceProxy.proxyGetPackageManager();
    }

    public Service getService() {
        return this.service;
    }

    public abstract IBinder onBind(Intent paramIntent);

    public void onConfigurationChanged(Configuration paramConfiguration) {
        this.serviceProxy.proxyOnConfigurationChanged(paramConfiguration);
    }

    public void onCreate() {
    }

    public void onDestroy() {
        this.serviceProxy.proxyOnDestroy();
    }

    public void onLowMemory() {
        this.serviceProxy.proxyOnLowMemory();
    }

    public void onRebind(Intent paramIntent) {
        this.serviceProxy.proxyOnRebind(paramIntent);
    }

    public void onStart(Intent paramIntent, int paramInt) {
        this.serviceProxy.proxyOnStart(paramIntent, paramInt);
    }

    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2) {
        return this.serviceProxy.proxyOnStartCommand(paramIntent, paramInt1, paramInt2);
    }

    public boolean onUnbind(Intent paramIntent) {
        return this.serviceProxy.proxyOnUnbind(paramIntent);
    }

    public void setServiceProxy(ServiceProxyAdapter paramServiceProxyAdapter) {
        this.service = paramServiceProxyAdapter.getService();
        this.serviceProxy = paramServiceProxyAdapter;
    }

    public void startActivity(Intent paramIntent) {
        serviceProxy.proxyStartActivity(paramIntent);
    }

    public final void startForeground(int paramInt, Notification paramNotification) {
        this.service.startForeground(paramInt, paramNotification);
    }

    public ComponentName startService(Intent paramIntent) {
    	PluginDebugLog.log("plugin", "CMService >>>>>startService");
        return this.serviceProxy.proxyStartService(paramIntent);
    }

    public final void stopSelf() {
        this.serviceProxy.proxyStopSelf();
    }

    public final void stopSelf(int paramInt) {
        this.serviceProxy.proxyStopSelf(paramInt);
    }

    public final boolean stopSelfResult(int paramInt) {
        return this.serviceProxy.proxyStopSelfResult(paramInt);
    }

    public boolean stopService(Intent paramIntent) {
        return this.serviceProxy.proxyStopService(paramIntent);
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (ProxyEnvironment.getInstance(getTargetPackageName()).isDataNeedPrefix()) {
            name = getTargetPackageName() + "_" + name;
        }
        return serviceProxy.proxyGetSharedPreferences(name, mode);
    }
    
    @Override
    public void sendBroadcast(Intent intent) {
        ProxyEnvironment.getInstance(getTargetPackageName()).remapReceiverIntent(intent);
        super.sendBroadcast(intent);
    }
    
    @Override
    public void sendBroadcast(Intent intent, String receiverPermission) {
        ProxyEnvironment.getInstance(getTargetPackageName()).remapReceiverIntent(intent);
        super.sendBroadcast(intent,receiverPermission);
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        // TODO 加前缀导致as下载到手机内存bug，需要整体实现改变数据路径
        // name = getTargetPackageName() + "_" + name;
        return service.openFileInput(name);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        // TODO 加前缀导致as下载到手机内存bug，需要整体实现改变数据路径
        // name = getTargetPackageName() + "_" + name;
        return service.openFileOutput(name, mode);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {
        if (ProxyEnvironment.getInstance(getTargetPackageName()).isDataNeedPrefix()) {
            name = getTargetPackageName() + "_" + name;
        }
        return service.openOrCreateDatabase(name, mode, factory);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory,
            DatabaseErrorHandler errorHandler) {
        if (ProxyEnvironment.getInstance(getTargetPackageName()).isDataNeedPrefix()) {
            name = getTargetPackageName() + "_" + name;
        }
        return service.openOrCreateDatabase(name, mode, factory, errorHandler);
    }

    @Override
    public File getFileStreamPath(String name) {
        // name = getTargetPackageName() + "_" + name;
        return service.getFileStreamPath(name);
    }
    
    public void setPluginDebug(boolean isdebug){
		
	}
    
    public final void stopForeground(boolean removeNotification){
    	this.service.stopForeground(removeNotification);
    }
}
