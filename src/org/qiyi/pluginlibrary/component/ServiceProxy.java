package org.qiyi.pluginlibrary.component;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.Process;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.ErrorType.ErrorType;
import org.qiyi.pluginlibrary.PServiceSupervisor;
import org.qiyi.pluginlibrary.PluginServiceWrapper;
import org.qiyi.pluginlibrary.ServiceJumpUtil;
import org.qiyi.pluginlibrary.context.CMContextWrapperNew;
import org.qiyi.pluginlibrary.manager.ProxyEnvironment;
import org.qiyi.pluginlibrary.manager.ProxyEnvironmentManager;
import org.qiyi.pluginlibrary.utils.ContextUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Plugin service's host service(Real service)
 */
public class ServiceProxy extends Service {
    private static final String TAG = ServiceProxy.class.getSimpleName();

    private static ConcurrentMap<String, Vector<Method>> sMethods = new ConcurrentHashMap<String, Vector<Method>>(2);

    private boolean mKillProcessOnDestroy = false;

    @Override
    public void onCreate() {
        PluginDebugLog.log(TAG, "ServiceProxy>>>>>onCreate()");
        super.onCreate();
        handleSlefLaunchPluginService();
    }

    /**
     * Must invoke on the main thread
     */
    private void handleSlefLaunchPluginService() {
        List<PluginServiceWrapper> selfLaunchServices = new ArrayList<PluginServiceWrapper>(1);
        for (PluginServiceWrapper plugin : PServiceSupervisor.getAliveServices().values()) {
            PServiceSupervisor.removeServiceByIdentifer(PluginServiceWrapper.getIndeitfy(plugin.getPkgName(), plugin.getServiceClassName()));
            if (plugin.mNeedSelfLaunch) {
                selfLaunchServices.add(plugin);
            }
        }
        for (PluginServiceWrapper item : selfLaunchServices) {
            loadTargetService(item.getPkgName(), item.getServiceClassName());
        }
    }

    private PluginServiceWrapper findPluginService(String pkgName, String clsName) {
        return PServiceSupervisor.getServiceByIdentifer(PluginServiceWrapper.getIndeitfy(pkgName, clsName));
    }

    public PluginServiceWrapper loadTargetService(String targetPackageName, String targetClassName) {
        PluginServiceWrapper currentPlugin = findPluginService(targetPackageName, targetClassName);
        PluginDebugLog.log(TAG, "ServiceProxy>>>>>loadTargetService()" + "target:"
                + (currentPlugin == null ? "null" : currentPlugin.getClass().getName()));
        if (currentPlugin == null) {
            PluginDebugLog.log(TAG, "ServiceProxy>>>>ProxyEnvironment.hasInstance:"
                    + ProxyEnvironmentManager.hasEnvInstance(targetPackageName) + ";targetPackageName:" + targetPackageName);

            try {
                ProxyEnvironment env = ProxyEnvironmentManager.getEnvByPkgName(targetPackageName);
                if (null == env) {
                    return null;
                }
                Service pluginService = ((Service) env.getDexClassLoader().loadClass(targetClassName).newInstance());
                CMContextWrapperNew actWrapper = new CMContextWrapperNew(ServiceProxy.this.getBaseContext(),
                        targetPackageName);
                ReflectionUtils.on(pluginService).call("attach", sMethods, actWrapper,
                        ReflectionUtils.getFieldValue(this, "mThread"), targetClassName,
                        ReflectionUtils.getFieldValue(this, "mToken"), env.getApplication(),
                        ReflectionUtils.getFieldValue(this, "mActivityManager"));
                currentPlugin = new PluginServiceWrapper(targetClassName, targetPackageName, this, pluginService);
                pluginService.onCreate();
                currentPlugin.updateServiceState(PluginServiceWrapper.PLUGIN_SERVICE_CREATED);

                PServiceSupervisor.addServiceByIdentifer(targetPackageName + "." + targetClassName, currentPlugin);

                PluginDebugLog.log(TAG, "ServiceProxy>>>start service, pkgName: " + targetPackageName + ", clsName: "
                        + targetClassName);
            } catch (InstantiationException e) {
                currentPlugin = null;
                e.printStackTrace();
                ProxyEnvironmentManager.deliverPlug(this, false, targetPackageName,
                        ErrorType.ERROR_CLIENT_LOAD_INIT_EXCEPTION_INSTANTIATION);
            } catch (IllegalAccessException e) {
                currentPlugin = null;
                e.printStackTrace();
                ProxyEnvironmentManager.deliverPlug(this, false, targetPackageName,
                        ErrorType.ERROR_CLIENT_LOAD_INIT_EXCEPTION_ILLEGALACCESS);
            } catch (ClassNotFoundException e) {
                currentPlugin = null;
                e.printStackTrace();
                ProxyEnvironmentManager.deliverPlug(this, false, targetPackageName,
                        ErrorType.ERROR_CLIENT_LOAD_INIT_EXCEPTION_CLASSNOTFOUND);
            } catch (Exception e) {
                e.printStackTrace();
                ProxyEnvironmentManager.deliverPlug(this, false, targetPackageName,
                        ErrorType.ERROR_CLIENT_LOAD_INIT_EXCEPTION);
                currentPlugin = null;
                PluginDebugLog.log(TAG, "初始化target失败");
            }
        }
        return currentPlugin;
    }

    @Override
    public IBinder onBind(Intent paramIntent) {

        PluginDebugLog.log(TAG, "ServiceProxy>>>>>onBind():" + (paramIntent == null ? "null" : paramIntent));
        mKillProcessOnDestroy = false;
        if (paramIntent == null) {
            return null;
        }
        String targetClassName = paramIntent.getStringExtra(ServiceJumpUtil.EXTRA_TARGET_SERVICE);
        String targetPackageName = paramIntent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME);
        PluginServiceWrapper currentPlugin = loadTargetService(targetPackageName, targetClassName);

        if (currentPlugin != null && currentPlugin.getCurrentService() != null) {
            currentPlugin.updateBindCounter(1);
            return currentPlugin.getCurrentService().onBind(paramIntent);
        } else {
            return null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration paramConfiguration) {
        ConcurrentMap<String, PluginServiceWrapper> aliveServices =
                PServiceSupervisor.getAliveServices();
        if (aliveServices != null) {
            // Notify all alive plugin service
            for (PluginServiceWrapper plugin : aliveServices.values()) {
                if (plugin != null && plugin.getCurrentService() != null) {
                    plugin.getCurrentService().onConfigurationChanged(paramConfiguration);
                }
            }
        } else {
            super.onConfigurationChanged(paramConfiguration);
        }
    }

    @Override
    public void onDestroy() {
        PluginDebugLog.log(TAG, "onDestroy " + getClass().getName());
        ConcurrentMap<String, PluginServiceWrapper> aliveServices =
                PServiceSupervisor.getAliveServices();
        if (aliveServices != null) {
            // Notify all alive plugin service to do destroy
            for (PluginServiceWrapper plugin : aliveServices.values()) {
                if (plugin != null && plugin.getCurrentService() != null) {
                    plugin.getCurrentService().onDestroy();
                }
            }
            PServiceSupervisor.clearServices();
        }
        super.onDestroy();
        if (mKillProcessOnDestroy) {
            Process.killProcess(Process.myPid());
        }
    }

    public void onLowMemory() {
        if (PServiceSupervisor.getAliveServices().size() > 0) {
            // Notify all alive plugin service to do destroy
            for (PluginServiceWrapper plugin : PServiceSupervisor.getAliveServices().values()) {
                if (plugin != null && plugin.getCurrentService() != null) {
                    plugin.getCurrentService().onLowMemory();
                }
            }
        } else {
            super.onLowMemory();
        }
    }

    @Override
    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2) {
        PluginDebugLog.log(TAG, "ServiceProxy>>>>>onStartCommand():" + (paramIntent == null ? "null" : paramIntent));
        if (paramIntent == null) {
            mKillProcessOnDestroy = false;
            super.onStartCommand(null, paramInt1, paramInt2);
            return START_NOT_STICKY;
        }

        if (!TextUtils.isEmpty(paramIntent.getAction())) {
            if (paramIntent.getAction().equals(ProxyEnvironmentManager.ACTION_QUIT)) {
                PluginDebugLog.log(TAG, "service " + getClass().getName() + " received quit intent action");
                mKillProcessOnDestroy = true;
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        ContextUtils.notifyHostPluginStarted(this, paramIntent);
        String targetClassName = paramIntent.getStringExtra(ServiceJumpUtil.EXTRA_TARGET_SERVICE);
        String targetPackageName = paramIntent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME);
        PluginServiceWrapper currentPlugin = loadTargetService(targetPackageName, targetClassName);
        PluginDebugLog.log(TAG, "ServiceProxy>>>>>onStartCommand() currentPlugin: " + currentPlugin);
        if (currentPlugin != null && currentPlugin.getCurrentService() != null) {
            currentPlugin.updateStartStatus(PluginServiceWrapper.PLUGIN_SERVICE_STARTED);
            int result = currentPlugin.getCurrentService().onStartCommand(paramIntent, paramInt1, paramInt2);
            PluginDebugLog.log(TAG, "ServiceProxy>>>>>onStartCommand() result: " + result);
            if (result == START_REDELIVER_INTENT || result == START_STICKY) {
                currentPlugin.mNeedSelfLaunch = true;
            }
            mKillProcessOnDestroy = false;
            return START_NOT_STICKY;
        } else {
            PluginDebugLog.log(TAG, "ServiceProxy>>>>>onStartCommand() currentPlugin is null!");
            mKillProcessOnDestroy = false;
            super.onStartCommand(paramIntent, paramInt1, paramInt2);
            return START_NOT_STICKY;
        }
    }

    @Override
    public boolean onUnbind(Intent paramIntent) {
        PluginDebugLog.log(TAG, "ServiceProxy>>>>>onUnbind():" + (paramIntent == null ? "null" : paramIntent));
        boolean result = false;
        if (null != paramIntent) {
            String targetClassName = paramIntent.getStringExtra(ServiceJumpUtil.EXTRA_TARGET_SERVICE);
            String targetPackageName = paramIntent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME);
            PluginServiceWrapper plugin = findPluginService(targetPackageName, targetClassName);
            if (plugin != null && plugin.getCurrentService() != null) {
                plugin.updateBindCounter(-1);
                result = plugin.getCurrentService().onUnbind(paramIntent);
                plugin.tryToDestroyService(paramIntent);
            }
        }
        super.onUnbind(paramIntent);
        return result;
    }

    @Override
    @Deprecated
    public void onStart(Intent intent, int startId) {
        PluginDebugLog.log(TAG, "ServiceProxy>>>>>onStart():" + (intent == null ? "null" : intent));
        if (intent == null) {
            super.onStart(null, startId);
            return;
        }
        String targetClassName = intent.getStringExtra(ServiceJumpUtil.EXTRA_TARGET_SERVICE);
        String targetPackageName = intent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME);
        PluginServiceWrapper currentPlugin = loadTargetService(targetPackageName, targetClassName);

        if (currentPlugin != null && currentPlugin.getCurrentService() != null) {
            currentPlugin.updateBindCounter(1);
            currentPlugin.getCurrentService().onStart(intent, startId);
        }
        super.onStart(intent, startId);
    }

    @Override
    public void onTrimMemory(int level) {
        if (PServiceSupervisor.getAliveServices().size() > 0) {
            // Notify all alive plugin service to do onTrimMemory
            for (PluginServiceWrapper plugin : PServiceSupervisor.getAliveServices().values()) {
                if (plugin != null && plugin.getCurrentService() != null) {
                    plugin.getCurrentService().onTrimMemory(level);
                }
            }
        } else {
            super.onTrimMemory(level);
        }
    }

    @Override
    public void onRebind(Intent intent) {
        PluginDebugLog.log(TAG, "ServiceProxy>>>>>onRebind():" + (intent == null ? "null" : intent));
        if (intent == null) {
            super.onRebind(null);
            return;
        }
        String targetClassName = intent.getStringExtra(ServiceJumpUtil.EXTRA_TARGET_SERVICE);
        String targetPackageName = intent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME);
        PluginServiceWrapper currentPlugin = findPluginService(targetPackageName, targetClassName);

        if (currentPlugin != null && currentPlugin.getCurrentService() != null) {
            currentPlugin.updateBindCounter(1);
            currentPlugin.getCurrentService().onRebind(intent);
        }
        super.onRebind(intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }
}
