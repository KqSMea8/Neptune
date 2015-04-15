package org.qiyi.pluginlibrary.adapter;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;

public interface ServiceProxyAdapter {
    Service getService();

    boolean proxyBindService(Intent paramIntent, ServiceConnection paramServiceConnection, int paramInt);

    void proxyDump(FileDescriptor paramFileDescriptor, PrintWriter paramPrintWriter, String[] paramArrayOfString);

    void proxyFinalize() throws Throwable;

    PackageManager proxyGetPackageManager();

    void proxyOnConfigurationChanged(Configuration paramConfiguration);

    void proxyOnDestroy();

    void proxyOnLowMemory();

    void proxyOnRebind(Intent paramIntent);

    void proxyOnStart(Intent paramIntent, int paramInt);

    int proxyOnStartCommand(Intent paramIntent, int paramInt1, int paramInt2);

    boolean proxyOnUnbind(Intent paramIntent);

    void proxyStartActivity(Intent paramIntent);

    ComponentName proxyStartService(Intent paramIntent);
    
    void proxyStopSelf();
    void proxyStopSelf(int paramInt);
    boolean proxyStopSelfResult(int paramInt);

    boolean proxyStopService(Intent paramIntent);

    public abstract SharedPreferences proxyGetSharedPreferences(String name, int mode);

}
