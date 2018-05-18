package org.qiyi.pluginlibrary.component.wraper;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.Window;
import android.view.WindowManager;

import org.qiyi.pluginlibrary.context.PluginContextWrapper;
import org.qiyi.pluginlibrary.runtime.PluginLoadedApk;
import org.qiyi.pluginlibrary.runtime.PluginManager;
import org.qiyi.pluginlibrary.utils.ComponetFinder;
import org.qiyi.pluginlibrary.utils.ContextUtils;
import org.qiyi.pluginlibrary.utils.IntentUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.ReflectionUtils;

/**
 * 负责转移插件的跳转目标和创建插件的Activity实例
 * 用于Hook ActivityThread中的全局Instrumentation
 *
 */
public class PluginHookedInstrument extends PluginInstrument {

    private static final String  TAG = "PluginHookedInstrument";

    public PluginHookedInstrument(Instrumentation hostInstr) {
        super(hostInstr);
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {

        if (className.startsWith(ComponetFinder.DEFAULT_ACTIVITY_PROXY_PREFIX)) {
            // 插件代理Activity，替换会插件真实的Activity
            String[] result = IntentUtils.parsePkgAndClsFromIntent(intent);
            String packageName = result[0];
            String targetClass = result[1];

            PluginDebugLog.runtimeLog(TAG, "newActivity: " + className + ", targetClass: " + targetClass);
            if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(targetClass)) {
                PluginLoadedApk loadedApk = PluginManager.getPluginLoadedApkByPkgName(packageName);
                if (loadedApk != null) {
                    Activity activity = mHostInstr.newActivity(loadedApk.getPluginClassLoader(), targetClass, intent);
                    activity.setIntent(intent);

                    ReflectionUtils.on(activity).set("mResources", loadedApk.getPluginResource());
                    return activity;
                }
            }
        }

        return mHostInstr.newActivity(cl, className, intent);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        final Intent intent = activity.getIntent();
        String[] result = IntentUtils.parsePkgAndClsFromIntent(intent);
        if (IntentUtils.isIntentForPlugin(intent)) {
            String packageName = result[0];
            String targetClass = result[1];
            if (!TextUtils.isEmpty(packageName)) {
                PluginDebugLog.runtimeLog(TAG, "callActivityOnCreate: " + packageName);
                PluginLoadedApk loadedApk = PluginManager.getPluginLoadedApkByPkgName(packageName);
                if (loadedApk != null) {
                    try {
                        ReflectionUtils activityRef = ReflectionUtils.on(activity);
                        activityRef.set("mResources", loadedApk.getPluginResource());
                        activityRef.set("mApplication", loadedApk.getPluginApplication());
                        Context pluginContext = new PluginContextWrapper(activity.getBaseContext(), packageName);
                        ReflectionUtils.on(activity, ContextWrapper.class).set("mBase", pluginContext);
                        ReflectionUtils.on(activity, ContextThemeWrapper.class).setNoException("mBase", pluginContext);
                        ReflectionUtils.on(activity).setNoException("mInstrumentation", loadedApk.getPluginInstrument());

                        changeActivityInfo(activity, targetClass, loadedApk);
                    } catch (Exception e) {
                        PluginDebugLog.runtimeLog(TAG, "callActivityOnCreate with exception: " + e.getMessage());
                        e.printStackTrace();
                    }

                    if (activity.getParent() == null) {
                        loadedApk.getActivityStackSupervisor().pushActivityToStack(activity);
                    }
                }
            }
            IntentUtils.resetAction(intent);  //恢复Action
            ContextUtils.notifyHostPluginStarted(activity, intent);
        }

        try {
            mHostInstr.callActivityOnCreate(activity, icicle);
        } catch (Exception e) {
            e.printStackTrace();
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

    /**
     * 修改插件Activity的ActivityInfo
     * 执行Activity#attach()和ActivityThread启动Activity过程中的逻辑
     *
     * @param activity
     * @param className
     * @param loadedApk
     */
    private void changeActivityInfo(Activity activity, String className, PluginLoadedApk loadedApk) {

        PluginDebugLog.runtimeFormatLog(TAG, "changeActivityInfo activity name:%s, pkgName:%s", className, loadedApk.getPluginPackageName());
        ActivityInfo origActInfo = ReflectionUtils.on(activity).get("mActivityInfo");
        ActivityInfo actInfo = loadedApk.getActivityInfoByClassName(className);
        if (actInfo != null) {
            if (loadedApk.getPackageInfo() != null) {
                actInfo.applicationInfo = loadedApk.getPackageInfo().applicationInfo;
            }
            if (origActInfo != null) {
                origActInfo.applicationInfo = actInfo.applicationInfo;
                origActInfo.configChanges = actInfo.configChanges;
                origActInfo.descriptionRes = actInfo.descriptionRes;
                origActInfo.enabled = actInfo.enabled;
                origActInfo.exported = actInfo.exported;
                origActInfo.flags = actInfo.flags;
                origActInfo.icon = actInfo.icon;
                origActInfo.labelRes = actInfo.labelRes;
                origActInfo.logo = actInfo.logo;
                origActInfo.metaData = actInfo.metaData;
                origActInfo.name = actInfo.name;
                origActInfo.nonLocalizedLabel = actInfo.nonLocalizedLabel;
                origActInfo.packageName = actInfo.packageName;
                origActInfo.permission = actInfo.permission;
                origActInfo.screenOrientation = actInfo.screenOrientation;
                origActInfo.softInputMode = actInfo.softInputMode;
                origActInfo.targetActivity = actInfo.targetActivity;
                origActInfo.taskAffinity = actInfo.taskAffinity;
                origActInfo.theme = actInfo.theme;
            }

            // 修改Window的属性
            Window window = activity.getWindow();
            if (actInfo.softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED) {
                window.setSoftInputMode(actInfo.softInputMode);
            }
            if (actInfo.uiOptions != 0) {
                window.setUiOptions(actInfo.uiOptions);
            }
            if (Build.VERSION.SDK_INT >= 26) {
                window.setColorMode(actInfo.colorMode);
            }
        }

        // 修改插件Activity的主题
        int resTheme = loadedApk.getActivityThemeResourceByClassName(className);
        if (resTheme != 0) {
            activity.setTheme(resTheme);
        }

        if (origActInfo != null) {
            // handle ActionBar title
            if (origActInfo.nonLocalizedLabel != null) {
                activity.setTitle(origActInfo.nonLocalizedLabel);
            } else if (origActInfo.labelRes != 0) {
                activity.setTitle(origActInfo.labelRes);
            } else if (origActInfo.applicationInfo != null) {
                if (origActInfo.applicationInfo.nonLocalizedLabel != null) {
                    activity.setTitle(origActInfo.applicationInfo.nonLocalizedLabel);
                } else if (origActInfo.applicationInfo.labelRes != 0) {
                    activity.setTitle(origActInfo.applicationInfo.labelRes);
                } else {
                    activity.setTitle(origActInfo.applicationInfo.name);
                }
            } else {
                activity.setTitle(origActInfo.name);
            }
        }

        if (actInfo != null) {
            // copy from VirtualApk, is it really need?
            if (actInfo.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                activity.setRequestedOrientation(actInfo.screenOrientation);
            }
            PluginDebugLog.log(TAG, "changeActivityInfo->changeTheme: " + " theme = " +
                    actInfo.getThemeResource() + ", icon = " + actInfo.getIconResource()
                    + ", logo = " + actInfo.logo + ", labelRes=" + actInfo.labelRes);
        }
    }
}
