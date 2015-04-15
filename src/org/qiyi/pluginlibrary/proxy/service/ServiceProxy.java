package org.qiyi.pluginlibrary.proxy.service;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import org.qiyi.pluginlibrary.ProxyEnvironment;
import org.qiyi.pluginlibrary.ErrorType.ErrorType;
import org.qiyi.pluginlibrary.adapter.ServiceProxyAdapter;
import org.qiyi.pluginlibrary.api.TargetActivator;
import org.qiyi.pluginlibrary.component.service.CMService;
import org.qiyi.pluginlibrary.utils.PlugServiceSp;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

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

public class ServiceProxy extends Service implements ServiceProxyAdapter {

	private static final String TAG = "plugin";
	
    public static final String META_DATA_NAME = "target";
    /** 插件实例 */
    private CMService target;
    
    private String targetClassName;
    
    private String targetPackageName;
    
    public static final String  ACTION_SERVICE_PROXY = "org.qiyi.pluginlibrary.proxy.service.ServiceProxy.com.qiyi.video";
    
    
//    private boolean isTargetServiceRunning = false;
    @Override
    public void onCreate() {
    	PluginDebugLog.log(TAG, "ServiceProxy>>>>>onCreate()");
        super.onCreate();
    }

    
    public void loadTargetService(Intent paramIntent) {
    	PluginDebugLog.log(TAG, "ServiceProxy>>>>>loadTargetService()"+"target:"+(target == null?"null":target.getClass().getName()));
        if (target == null) {
            targetClassName = paramIntent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_SERVICE);
            targetPackageName = paramIntent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME);
            PluginDebugLog.log("plugin", "ServiceProxy>>>>ProxyEnvironment.hasInstance:"+ProxyEnvironment.hasInstance(targetPackageName)+";targetPackageName:"+targetPackageName);
            if (!ProxyEnvironment.hasInstance(targetPackageName)) {
//                super.stopSelf();
                if (targetClassName == null) {
                    targetClassName = "";
                }

                if (!TextUtils.isEmpty(targetPackageName)) {
                    Intent intent = new Intent(paramIntent);
                    intent.setComponent(new ComponentName(targetPackageName, targetClassName));
//                  TargetActivator.startSeviceProxy(getApplicationContext(), intent);
                	new TargetThread(intent).start();
                }
                return;
            }

            try {
                target = ((CMService) ProxyEnvironment.getInstance(targetPackageName).getDexClassLoader().loadClass(targetClassName).asSubclass(CMService.class).newInstance());
                target.setServiceProxy(this);
                target.setTargetPackagename(targetPackageName);
                PluginDebugLog.log("plugin", "ServiceProxy>>>启动插件:"+target);
//                target.setPluginDebug(PluginDebugLog.isDebug());
                target.onCreate();
            } catch (InstantiationException e) {
                e.printStackTrace();
                ProxyEnvironment.deliverPlug(false,targetPackageName , ErrorType.ERROR_CLIENT_LOAD_INIT_EXCEPTION_INSTANTIATION);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                ProxyEnvironment.deliverPlug(false,targetPackageName , ErrorType.ERROR_CLIENT_LOAD_INIT_EXCEPTION_ILLEGALACCESS);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                ProxyEnvironment.deliverPlug(false,targetPackageName , ErrorType.ERROR_CLIENT_LOAD_INIT_EXCEPTION_CLASSNOTFOUND);
            }catch(Exception e){
            	e.printStackTrace();
            	ProxyEnvironment.deliverPlug(false,targetPackageName , ErrorType.ERROR_CLIENT_LOAD_INIT_EXCEPTION);
            	target = null;
            	PluginDebugLog.log("plugin","初始化target失败");
            }
        }
    }

    class TargetThread extends Thread{
    	Intent intent;
    	public TargetThread(Intent intent){
    		this.intent = intent;
    	}
    	@Override
    	public void run() {
    		// TODO Auto-generated method stub
    		TargetActivator.startSeviceProxy(getApplicationContext(), intent);
    		super.run();
    	}
    	
    }
    @Override
    public boolean bindService(Intent paramIntent, ServiceConnection paramServiceConnection, int paramInt) {

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

    @Override
    public IBinder onBind(Intent paramIntent) {
    	
    	PluginDebugLog.log(TAG, "ServiceProxy>>>>>onBind():"+(paramIntent == null? "null":paramIntent));
        if (paramIntent == null) {
        	paramIntent = PlugServiceSp.getInstance(getApplicationContext()).setParamIntent();//重设
        }
        target = null;
        targetClassName = paramIntent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_SERVICE);
        PluginDebugLog.log(TAG, "ServiceProxy_onBind_targetClassName:"+targetClassName);
        if(ProxyEnvironment.aliveServiceMap.containsKey(targetClassName)){//如果service已经运行
        	target = ProxyEnvironment.aliveServiceMap.get(targetClassName);
        }
        loadTargetService(paramIntent);
    	
        if (target != null) {
        	target.setPluginDebug(PluginDebugLog.isDebug());
        	if(!ProxyEnvironment.aliveServiceMap.containsKey(targetClassName)){
        		PlugServiceSp.getInstance(getApplicationContext()).saveAliveService(paramIntent);
        	}
        	ProxyEnvironment.aliveServiceMap.put(targetClassName, target);
            return this.target.onBind(paramIntent);
        } else {
            return null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration paramConfiguration) {
        if (target != null) {
            this.target.onConfigurationChanged(paramConfiguration);
        } else {
            super.onConfigurationChanged(paramConfiguration);
        }
    }

    @Override
    public void onDestroy() {
    	PluginDebugLog.log(TAG, "ServiceProxy_onDestory");
    	ProxyEnvironment.aliveServiceMap.clear();
        if (target != null) {
            this.target.onDestroy();
        } else {
            super.onDestroy();
        }
        target = null;
    }

    public void onLowMemory() {
        if (target != null) {
            this.target.onLowMemory();
        } else {
            super.onLowMemory();
        }
    }


    @Override
    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2) {
    	PluginDebugLog.log(TAG, "ServiceProxy>>>>>onStartCommand():"+(paramIntent == null? "null":paramIntent));
        if (paramIntent == null) {
        	paramIntent = PlugServiceSp.getInstance(getApplicationContext()).setParamIntent();//重设
//            stopSelf();
//            return super.onStartCommand(paramIntent, paramInt1, paramInt2);
        }
        target = null;
        targetClassName = paramIntent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_SERVICE);
        PluginDebugLog.log(TAG, "ServiceProxy_targetClassName:"+targetClassName);
        if(ProxyEnvironment.aliveServiceMap.containsKey(targetClassName)){//如果service已经运行
        	target = ProxyEnvironment.aliveServiceMap.get(targetClassName);
        }
        
        loadTargetService(paramIntent);
        
        if (target != null) {
//        	target.setPluginDebug(PluginDebugLog.isDebug());
        	if(!ProxyEnvironment.aliveServiceMap.containsKey(targetClassName)){
        		PlugServiceSp.getInstance(getApplicationContext()).saveAliveService(paramIntent);
        	}
        	ProxyEnvironment.aliveServiceMap.put(targetClassName, target);
            return this.target.onStartCommand(paramIntent, paramInt1, paramInt2);
        } else {
            return super.onStartCommand(paramIntent, paramInt1, paramInt2);
        }
    }
    


    @Override
    public boolean onUnbind(Intent paramIntent) {
        if (target != null) {
            return this.target.onUnbind(paramIntent);
        } else {
            return super.onUnbind(paramIntent);
        }
    }

    public boolean proxyBindService(Intent paramIntent, ServiceConnection paramServiceConnection, int paramInt) {
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

    @SuppressWarnings("deprecation")
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
    	PluginDebugLog.log("plugin", "ServiceProxy>>>>>proxyStartActivity ："+paramIntent);
        super.startActivity(paramIntent);
    }

    public ComponentName proxyStartService(Intent paramIntent) {
    	PluginDebugLog.log("plugin", "ServiceProxy>>>>>proxyStartService ："+paramIntent);
        return super.startService(paramIntent);
    }

    public boolean proxyStopService(Intent paramIntent) {
    	targetServiceOnDestory(paramIntent);
        return super.stopService(paramIntent);
    }

    public void targetServiceOnDestory(Intent paramIntent){
      	if(paramIntent != null){
    		String targetService = paramIntent.getComponent().getClassName();
    		if(ProxyEnvironment.aliveServiceMap.containsKey(targetService)){
    			ProxyEnvironment.aliveServiceMap.get(targetService).onDestroy();
    			ProxyEnvironment.aliveServiceMap.remove(targetService);
    		}
    		PlugServiceSp.getInstance(getApplicationContext()).removeAliveService(targetService);
    		PluginDebugLog.log(TAG, "ServiceProxy targetServiceOnDestory target:"+target+";targetService:"+targetService);
    	}
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
	public void proxyStopSelf() {
		// TODO Auto-generated method stub
		if(target != null){
			target.onDestroy();
			if(ProxyEnvironment.aliveServiceMap.containsKey(target.getClass().getName())){
				ProxyEnvironment.aliveServiceMap.remove(target.getClass().getName());
			}
			PlugServiceSp.getInstance(getApplicationContext()).removeAliveService(target.getClass().getName());
			PluginDebugLog.log(TAG, "ServiceProxy_proxyStopSelf_target:"+target+";targetService:"+target.getClass().getName());
		}
		
		target = null;
	}


	@Override
	public void proxyStopSelf(int paramInt) {
		// TODO Auto-generated method stub
		if(target != null){
			target.onDestroy();
			if(ProxyEnvironment.aliveServiceMap.containsKey(target.getClass().getName())){
				ProxyEnvironment.aliveServiceMap.remove(target.getClass().getName());
			}
			PlugServiceSp.getInstance(getApplicationContext()).removeAliveService(target.getClass().getName());
			PluginDebugLog.log(TAG, "ServiceProxy_proxyStopSelf_target:"+target+";targetService:"+target.getClass().getName());
		}
		target = null;
	}


	@Override
	public boolean proxyStopSelfResult(int paramInt) {
		// TODO Auto-generated method stub
		if(target != null){
			target.onDestroy();
			if(ProxyEnvironment.aliveServiceMap.containsKey(target.getClass().getName())){
				ProxyEnvironment.aliveServiceMap.remove(target.getClass().getName());
			}
			PlugServiceSp.getInstance(getApplicationContext()).removeAliveService(target.getClass().getName());
			PluginDebugLog.log(TAG, "ServiceProxy_proxyStopSelf_target:"+target+";targetService:"+target.getClass().getName());
		}
		target = null;
		return false;
	}
}