package org.qiyi.pluginlibrary.component.stackmgr;

import android.app.Service;

/**
 * 对插件Service的包装，记录每个Service的状态，便于对
 * Service的生命周期进行管理
 */
public class PluginServiceWrapper {
    /** status to indicate service has been created **/
    public static final int PLUGIN_SERVICE_DEFAULT = 0;
    public static final int PLUGIN_SERVICE_CREATED = PLUGIN_SERVICE_DEFAULT + 1;
    public static final int PLUGIN_SERVICE_STARTED = PLUGIN_SERVICE_CREATED + 1;
    public static final int PLUGIN_SERVICE_STOPED = PLUGIN_SERVICE_STARTED + 1;
    public static final int PLUGIN_SERVICE_DESTROYED = PLUGIN_SERVICE_STOPED + 1;

    int mState = PLUGIN_SERVICE_DEFAULT;
    /**插件中被代理Service的类名*/
    private String mServiceClassName;
    /**插件包名*/
    private String mPkgName;
    /**代理Service*/
    private Service mParentService;
    /**插件中被代理的Service对象*/
    private Service mCurrentService;
    /**Service被绑定的次数*/
    private int mBindCounter = 0;

    /**
     * Indicate service should launch after process killed illegal support
     * {@link android.app.Service#START_REDELIVER_INTENT}
     * {@link android.app.Service#START_STICKY}
     */
    public volatile boolean mNeedSelfLaunch = false;

    public PluginServiceWrapper(String serviceClsName, String pkgName, Service parent, Service current) {
        mServiceClassName = serviceClsName;
        mPkgName = pkgName;
        mParentService = parent;
        mCurrentService = current;
    }

    /**
     * 更新当前Service的状态
     * @param state
     */
    public void updateServiceState(int state) {
        mState = state;
    }

    public String getPkgName() {
        return mPkgName;
    }

    public String getServiceClassName() {
        return mServiceClassName;
    }

    public Service getCurrentService() {
        return mCurrentService;
    }

    public void updateBindCounter(int deta) {
        mBindCounter += deta;
        if (mBindCounter < 0) {
            mBindCounter = 0;
        }
    }


    /**
     * 判断当前Service 是否可以执行onDestroy
     * @return
     */
    private boolean shouldDestroy() {
        return mBindCounter == 0 &&
                (mState > PLUGIN_SERVICE_DEFAULT && mState != PLUGIN_SERVICE_DESTROYED);
    }

    /**
     * 尝试执行Service的onDestroy方法
     */
    public void tryToDestroyService() {
        if (mCurrentService != null && shouldDestroy()) {
            try {
                mCurrentService.onDestroy();
                mState = PLUGIN_SERVICE_DESTROYED;
            } catch (Exception e) {
                e.printStackTrace();
            }
            // remove service record.
            PServiceSupervisor.removeServiceByIdentifer(getIndeitfy(mPkgName, mServiceClassName));
            if (PServiceSupervisor.getAliveServices().size() == 0 && mParentService != null) {
                mParentService.stopSelf();
            }
        }
    }

    /**
     * 获取当前Service保存的key值
     * @param pkg
     * @param serviceClsName
     * @return
     */
    public static String getIndeitfy(String pkg, String serviceClsName) {
        return pkg + "." + serviceClsName;
    }
}
