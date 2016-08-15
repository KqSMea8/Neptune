package org.qiyi.pluginlibrary.component;

import org.qiyi.pluginlibrary.manager.ProxyEnvironment;
import org.qiyi.pluginlibrary.manager.ProxyEnvironmentManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BroadcastReceiverProxy extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String targetReceiver = intent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_RECEIVER);
            String targetPkgName = intent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME);
            try {
                BroadcastReceiver target = ((BroadcastReceiver) ProxyEnvironmentManager.getEnvByPkgName(targetPkgName).getDexClassLoader()
                        .loadClass(targetReceiver).asSubclass(BroadcastReceiver.class).newInstance());
                target.onReceive(ProxyEnvironmentManager.getEnvByPkgName(targetPkgName).getApplication(), intent);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
