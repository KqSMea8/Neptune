package org.qiyi.pluginlibrary.component.wraper;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;

import org.qiyi.pluginlibrary.HybirdPlugin;
import org.qiyi.pluginlibrary.component.BaseRecoveryActivity;
import org.qiyi.pluginlibrary.component.base.IPluginBase;
import org.qiyi.pluginlibrary.component.stackmgr.PluginActivityControl;
import org.qiyi.pluginlibrary.context.PluginContextWrapper;
import org.qiyi.pluginlibrary.runtime.NotifyCenter;
import org.qiyi.pluginlibrary.runtime.PluginLoadedApk;
import org.qiyi.pluginlibrary.runtime.PluginManager;
import org.qiyi.pluginlibrary.utils.ComponetFinder;
import org.qiyi.pluginlibrary.utils.IntentUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.ReflectionUtils;

/**
 * 负责转移插件的跳转目标和创建插件的Activity实例
 * 用于Hook ActivityThread中的全局Instrumentation
 */
public class PluginHookedInstrument extends PluginInstrument {

    private static final String TAG = "PluginHookedInstrument";
    private PluginActivityRecoveryHelper mRecoveryHelper = new PluginActivityRecoveryHelper();

    public PluginHookedInstrument(Instrumentation hostInstr) {
        super(hostInstr);
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (className.startsWith(ComponetFinder.DEFAULT_ACTIVITY_PROXY_PREFIX)) {
            intent = mRecoveryHelper.recoveryIntent(intent);
            // 插件代理Activity，替换回插件真实的Activity
            String[] result = IntentUtils.parsePkgAndClsFromIntent(intent);
            String packageName = result[0];
            String targetClass = result[1];

            PluginDebugLog.runtimeLog(TAG, "newActivity: " + className + ", targetClass: " + targetClass);
            if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(targetClass)) {
                PluginLoadedApk loadedApk = PluginManager.getPluginLoadedApkByPkgName(packageName);
                if (loadedApk != null) {
                    Activity activity = mHostInstr.newActivity(loadedApk.getPluginClassLoader(), targetClass, intent);
                    activity.setIntent(intent);

                    if (!dispatchToBaseActivity(activity)) {
                        // 这里需要替换Resources，是因为ContextThemeWrapper会缓存一个Resource对象，而在Activity#attach()和
                        // Activity#onCreate()之间，系统会调用Activity#setTheme()初始化主题，Android 4.1+
                        ReflectionUtils.on(activity).setNoException("mResources", loadedApk.getPluginResource());
                    }

                    return activity;
                } else {
                    // loadedApk 为空，可能是正常恢复进程，跳转到 RecoveryActivity
                    mRecoveryHelper.saveIntent(intent);
                    return mHostInstr.newActivity(cl, mRecoveryHelper.selectRecoveryActivity(className), new Intent(intent));
                }
            }
        }

        return mHostInstr.newActivity(cl, className, intent);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        boolean isRecovery = activity instanceof BaseRecoveryActivity;
        if (isRecovery) {
            mRecoveryHelper.saveIcicle(icicle);
            mHostInstr.callActivityOnCreate(activity, null);
            return;
        }
        final Intent intent = activity.getIntent();
        String[] result = IntentUtils.parsePkgAndClsFromIntent(intent);
        boolean isLaunchPlugin = false;
        if (IntentUtils.isIntentForPlugin(intent)) {
            String packageName = result[0];
            String targetClass = result[1];
            if (!TextUtils.isEmpty(packageName)) {
                PluginDebugLog.runtimeLog(TAG, "callActivityOnCreate: " + packageName);
                PluginLoadedApk loadedApk = PluginManager.getPluginLoadedApkByPkgName(packageName);
                if (loadedApk != null) {
                    icicle = mRecoveryHelper.recoveryIcicle(icicle);
                    // 设置 extra 的 ClassLoader，不然可能会出现 BadParcelException, ClassNotFound
                    if (icicle != null) {
                        icicle.setClassLoader(loadedApk.getPluginClassLoader());
                    }
                    if (!dispatchToBaseActivity(activity)) {
                        // 如果分发给插件Activity的基类了，就不需要在这里反射hook替换相关成员变量了
                        try {
                            ReflectionUtils activityRef = ReflectionUtils.on(activity);
                            activityRef.setNoException("mResources", loadedApk.getPluginResource());
                            activityRef.setNoException("mApplication", loadedApk.getPluginApplication());
                            Context pluginContext = new PluginContextWrapper(activity.getBaseContext(), packageName);
                            ReflectionUtils.on(activity, ContextWrapper.class).set("mBase", pluginContext);
                            // 5.0以下ContextThemeWrapper内会保存一个mBase，也需要反射替换掉
                            ReflectionUtils.on(activity, ContextThemeWrapper.class).setNoException("mBase", pluginContext);
                            ReflectionUtils.on(activity).setNoException("mInstrumentation", loadedApk.getPluginInstrument());

                            // 修改插件Activity的ActivityInfo, theme, window等信息
                            PluginActivityControl.changeActivityInfo(activity, targetClass, loadedApk);
                        } catch (Exception e) {
                            PluginDebugLog.runtimeLog(TAG, "callActivityOnCreate with exception: " + e.getMessage());
                            e.printStackTrace();
                        }

                    }

                    if (activity.getParent() == null) {
                        loadedApk.getActivityStackSupervisor().pushActivityToStack(activity);
                    }
                    isLaunchPlugin = true;
                }
            }
            IntentUtils.resetAction(intent);  //恢复Action
        }

        try {
            mHostInstr.callActivityOnCreate(activity, icicle);

            if (isLaunchPlugin) {
                NotifyCenter.notifyPluginStarted(activity, intent);
                NotifyCenter.notifyPluginActivityLoaded(activity);
            }
            mRecoveryHelper.mockActivityOnRestoreInstanceStateIfNeed(this, activity);
        } catch (Exception e) {
            e.printStackTrace();
            if (isLaunchPlugin) {
                NotifyCenter.notifyStartPluginError(activity);
            }
            if (PluginDebugLog.isDebug()) {
                throw e;
            }
            activity.finish();
        }
    }

    @Override
    public void callActivityOnDestroy(Activity activity) {
        super.callActivityOnDestroy(activity);
        if (activity.getParent() != null) {
            return;
        }

        final Intent intent = activity.getIntent();
        String[] result = IntentUtils.parsePkgAndClsFromIntent(intent);
        if (IntentUtils.isIntentForPlugin(intent)) {
            String packageName = result[0];
            if (!TextUtils.isEmpty(packageName)) {
                PluginDebugLog.runtimeLog(TAG, "callActivityOnDestroy: " + packageName);
                PluginLoadedApk loadedApk = PluginManager.getPluginLoadedApkByPkgName(packageName);
                if (loadedApk != null) {
                    loadedApk.getActivityStackSupervisor().popActivityFromStack(activity);
                }
            }
        }
    }

    @Override
    public void callActivityOnRestoreInstanceState(Activity activity, Bundle savedInstanceState) {
        if (activity instanceof BaseRecoveryActivity) {
            mRecoveryHelper.saveSavedInstanceState(savedInstanceState);
            return;
        }
        if (IntentUtils.isIntentForPlugin(activity.getIntent())) {
            String pkgName = IntentUtils.parsePkgAndClsFromIntent(activity.getIntent())[0];
            PluginLoadedApk loadedApk = PluginManager.getPluginLoadedApkByPkgName(pkgName);
            if (loadedApk != null && savedInstanceState != null) {
                savedInstanceState.setClassLoader(loadedApk.getPluginClassLoader());
                super.callActivityOnRestoreInstanceState(activity, savedInstanceState);
            }
        } else {
            super.callActivityOnRestoreInstanceState(activity, savedInstanceState);
        }
    }

    /**
     * 将Activity反射相关操作分发给插件Activity的基类
     *
     * @param activity
     * @return
     */
    private boolean dispatchToBaseActivity(Activity activity) {

        return HybirdPlugin.getConfig().getSdkMode() == 2
                && activity instanceof IPluginBase;
    }
}
