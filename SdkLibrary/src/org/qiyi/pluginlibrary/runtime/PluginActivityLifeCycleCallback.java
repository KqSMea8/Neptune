package org.qiyi.pluginlibrary.runtime;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import org.qiyi.pluginlibrary.utils.PluginDebugLog;

/**
 * author: liuchun
 * date: 2018/3/16
 */
public abstract class PluginActivityLifeCycleCallback implements Application.ActivityLifecycleCallbacks{
    private static final String TAG = "PluginActivityLifeCycleCallback";

    public abstract void onPluginActivityCreated(String pluginPkgName, Activity activity, Bundle savedInstanceState);
    public abstract void onPluginActivityStarted(String pluginPkgName, Activity activity);
    public abstract void onPluginActivityResumed(String pluginPkgName, Activity activity);
    public abstract void onPluginActivityPaused(String pluginPkgName, Activity activity);
    public abstract void onPluginActivityStopped(String pluginPkgName, Activity activity);
    public abstract void onPluginActivitySaveInstanceState(String pluginPkgName, Activity activity, Bundle outState);
    public abstract void onPluginActivityDestroyed(String pluginPkgName, Activity activity);

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        PluginDebugLog.runtimeLog(TAG, "onActivityCreated: " + activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        PluginDebugLog.runtimeLog(TAG, "onActivityStarted: " + activity);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        PluginDebugLog.runtimeLog(TAG, "onActivityResumed: " + activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        PluginDebugLog.runtimeLog(TAG, "onActivityPaused: " + activity);
    }

    @Override
    public void onActivityStopped(Activity activity) {
        PluginDebugLog.log(TAG, "onActivityStopped: " + activity);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        PluginDebugLog.runtimeLog(TAG, "onActivitySaveInstanceState: " + activity);
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        PluginDebugLog.runtimeLog(TAG, "onActivityDestroyed: " + activity);
    }
}
