package org.qiyi.pluginlibrary;

import android.app.Service;
import android.content.Intent;

/**
 * Wrapper for plugin service
 */
public class PluginServiceWrapper {
    /** status to indicate service has been created **/
    public static final int PLUGIN_SERVICE_DEFAULT = 0;
    public static final int PLUGIN_SERVICE_CREATED = PLUGIN_SERVICE_DEFAULT + 1;
    public static final int PLUGIN_SERVICE_STARTED = PLUGIN_SERVICE_CREATED + 1;
    public static final int PLUGIN_SERVICE_STOPED = PLUGIN_SERVICE_STARTED + 1;
    public static final int PLUGIN_SERVICE_DESTROYED = PLUGIN_SERVICE_STOPED + 1;

    int mState = PLUGIN_SERVICE_DEFAULT;

    private String mServiceClassName;

    private String mPkgName;

    private Service mParentService;

    private Service mCurrentService;

    private int mBindCounter = 0;
    private int mStartCounter = PLUGIN_SERVICE_DEFAULT;

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

    public void updateStartStatus(int status) {
        mStartCounter = status;
    }

    public boolean shouldDestroy() {
        if (mBindCounter == 0 && (mStartCounter == PLUGIN_SERVICE_STOPED || mStartCounter == PLUGIN_SERVICE_DEFAULT)
                && mState == PLUGIN_SERVICE_CREATED) {
            return true;
        } else {
            return false;
        }
    }

    public void tryToDestroyService(Intent service) {
        if (mCurrentService != null && shouldDestroy()) {
            try {
                mCurrentService.onDestroy();
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

    public static String getIndeitfy(String pkg, String serviceClsName) {
        return pkg + "." + serviceClsName;
    }
}