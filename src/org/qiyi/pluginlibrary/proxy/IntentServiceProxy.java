package org.qiyi.pluginlibrary.proxy;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import org.qiyi.pluginlibrary.ProxyEnvironment;
import org.qiyi.pluginlibrary.adapter.ServiceProxyAdapter;
import org.qiyi.pluginlibrary.api.TargetActivator;
import org.qiyi.pluginlibrary.component.service.CMIntentService;

import android.app.IntentService;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.IBinder;
import android.text.TextUtils;

public class IntentServiceProxy extends IntentService implements ServiceProxyAdapter {

    /**
     * @param name
     */
    public IntentServiceProxy() {
        super("ServiceProxy");
    }

    public static final String META_DATA_NAME = "target";
    /** 插件实例 */
    private CMIntentService target;
    /** 插件包名 */
    private String pacakgeName = "";
    /** 是否正在stopping */
    private boolean bIsStopping = false;

    public void loadTargetService(Intent paramIntent) {
        if (target == null && !bIsStopping) {
            String targetClassName = paramIntent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_SERVICE);
            String targetPackageName = paramIntent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME);
            if (!ProxyEnvironment.hasInstance(targetPackageName)) {
                bIsStopping = true;
                super.stopSelf();

                if (targetClassName == null) {
                    targetClassName = "";
                }

                if (!TextUtils.isEmpty(targetPackageName)) {
                    Intent intent = new Intent(paramIntent);
                    intent.setComponent(new ComponentName(targetPackageName, targetClassName));
                    TargetActivator.loadTargetAndRun(this, intent);
                }
                return;
            }

            try {
                target = ((CMIntentService) ProxyEnvironment.getInstance(targetPackageName).getDexClassLoader()
                        .loadClass(targetClassName).asSubclass(CMIntentService.class).newInstance());
                target.setServiceProxy(this);
                target.setTargetPackagename(targetPackageName);
                target.onCreate();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getPackageName() {
        return pacakgeName;
    }

    public boolean bindService(Intent paramIntent, ServiceConnection paramServiceConnection, int paramInt) {

        // TODO 这个bind逻辑是有问题的，待完善 —— by chenyangkun
        if (paramIntent != null) {
            loadTargetService(paramIntent);
        } else {
            return false;
        }

        if (target != null) {
            return this.target.bindService(paramIntent, paramServiceConnection, paramInt);
        } else {
            return false;
        }
    }

    public PackageManager getPackageManager() {
        if (target != null) {
            return this.target.getPackageManager();
        } else {
            return super.getPackageManager();
        }
    }

    public Service getService() {
        return this;
    }

    public IBinder onBind(Intent paramIntent) {
        if (target != null) {
            return this.target.onBind(paramIntent);
        } else {
            return super.onBind(paramIntent);
        }
    }

    public void onConfigurationChanged(Configuration paramConfiguration) {
        if (target != null) {
            this.target.onConfigurationChanged(paramConfiguration);
        } else {
            super.onConfigurationChanged(paramConfiguration);
        }
    }

    public void onCreate() {
        super.onCreate();
    }

    public void onDestroy() {
        if (target != null) {
            this.target.onDestroy();
        } else {
            super.onDestroy();
        }
    }

    public void onLowMemory() {
        if (target != null) {
            this.target.onLowMemory();
        } else {
            super.onLowMemory();
        }
    }

    public void onStart(Intent paramIntent, int paramInt) {
        if (paramIntent == null) {
            stopSelf();
            return;
        }

        if (target != null) {
            this.target.onStart(paramIntent, paramInt);
        } else {
            super.onStart(paramIntent, paramInt);
        }
    }

    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2) {
        if (paramIntent == null) {
            stopSelf();
            return super.onStartCommand(paramIntent, paramInt1, paramInt2);
        }
            
        loadTargetService(paramIntent);
        if (target != null) {
            return this.target.onStartCommand(paramIntent, paramInt1, paramInt2);
        } else {
            return super.onStartCommand(paramIntent, paramInt1, paramInt2);
        }
    }

    public boolean onUnbind(Intent paramIntent) {
        if (target != null) {
            return this.target.onUnbind(paramIntent);
        } else {
            return super.onUnbind(paramIntent);
        }
    }

    public boolean proxyBindService(Intent paramIntent, ServiceConnection paramServiceConnection, int paramInt) {
        ProxyEnvironment.getInstance(target.getPackageName()).remapStartServiceIntent(paramIntent);
        return super.bindService(paramIntent, paramServiceConnection, paramInt);
    }

    public void proxyDump(FileDescriptor paramFileDescriptor, PrintWriter paramPrintWriter, String[] paramArrayOfString) {
        super.dump(paramFileDescriptor, paramPrintWriter, paramArrayOfString);
    }

    public void proxyFinalize() throws Throwable {
        super.finalize();
    }

    public PackageManager proxyGetPackageManager() {
        return super.getPackageManager();
    }

    public void proxyOnConfigurationChanged(Configuration paramConfiguration) {
        super.onConfigurationChanged(paramConfiguration);
    }

    public void proxyOnDestroy() {
        super.onDestroy();
    }

    public void proxyOnLowMemory() {
        super.onLowMemory();
    }

    public void proxyOnRebind(Intent paramIntent) {
        super.onRebind(paramIntent);
    }

    public void proxyOnStart(Intent paramIntent, int paramInt) {
        super.onStart(paramIntent, paramInt);
    }

    public int proxyOnStartCommand(Intent paramIntent, int paramInt1, int paramInt2) {
        return super.onStartCommand(paramIntent, paramInt1, paramInt2);
    }

    public boolean proxyOnUnbind(Intent paramIntent) {
        return super.onUnbind(paramIntent);
    }

    public void proxyStartActivity(Intent paramIntent) {
        ProxyEnvironment.getInstance(target.getPackageName()).remapStartActivityIntent(paramIntent);
        super.startActivity(paramIntent);
    }

    public ComponentName proxyStartService(Intent paramIntent) {
        ProxyEnvironment.getInstance(target.getPackageName()).remapStartActivityIntent(paramIntent);
        return super.startService(paramIntent);
    }

    public boolean proxyStopService(Intent paramIntent) {
        ProxyEnvironment.getInstance(target.getPackageName()).remapStartActivityIntent(paramIntent);
        return super.stopService(paramIntent);
    }

    public void startActivity(Intent paramIntent) {
        if (target != null) {
            this.target.startActivity(paramIntent);
        } else {
            super.startActivity(paramIntent);
        }
    }

    public ComponentName startService(Intent paramIntent) {
        if (target != null) {
            return this.target.startService(paramIntent);
        } else {
            return super.startService(paramIntent);
        }
    }

    public boolean stopService(Intent paramIntent) {
        if (target != null) {
            return this.target.stopService(paramIntent);
        } else {
            return super.stopService(paramIntent);
        }
    }

    @Override
    public Resources getResources() {
        if (target != null) {
            return target.getResources();
        } else {
            return super.getResources();
        }
    }

    @Override
    public SharedPreferences proxyGetSharedPreferences(String name, int mode) {
        return super.getSharedPreferences(name, mode);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            loadTargetService(intent);
        }
        if (target != null) {
            this.target.onHandleIntent(intent);
        }
    }

	@Override
	public void proxyStopSelf() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void proxyStopSelf(int paramInt) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean proxyStopSelfResult(int paramInt) {
		// TODO Auto-generated method stub
		return false;
	}
}