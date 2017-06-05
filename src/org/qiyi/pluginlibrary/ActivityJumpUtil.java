package org.qiyi.pluginlibrary;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.component.InstrActivityProxy;
import org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent;
import org.qiyi.pluginlibrary.debug.PluginCenterDebugHelper;
import org.qiyi.pluginlibrary.manager.ProxyEnvironment;
import org.qiyi.pluginlibrary.manager.ProxyEnvironmentManager;
import org.qiyi.pluginlibrary.plugin.TargetMapping;
import org.qiyi.pluginlibrary.pm.CMPackageInfo;
import org.qiyi.pluginlibrary.pm.CMPackageManager;
import org.qiyi.pluginlibrary.pm.CMPackageManagerImpl;
import org.qiyi.pluginlibrary.utils.IntentUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import java.util.List;

/**
 * Help class for plugin activity jumping.
 */
public class ActivityJumpUtil {
    private static final String TAG = ActivityJumpUtil.class.getSimpleName();

    public static final String TARGET_CLASS_NAME = "org.qiyi.PluginActivity";

    public static final String EXTRA_TARGET_ACTIVITY = "pluginapp_extra_target_activity";

    /**
     * pluginapp开关:表示activity的特殊处理，例如Translucent Theme 等
     */
    public static final String META_KEY_ACTIVITY_SPECIAL = "pluginapp_activity_special";

    public static final String PLUGIN_ACTIVITY_TRANSLUCENT = "Translucent";

    public static final String PLUGIN_ACTIVTIY_HANDLE_CONFIG_CHAGNE = "Handle_configuration_change";

    /**
     * Get proxy activity class name for all plugin activitys The proxy activity
     * should be register in the manifest.xml
     *
     * @param plunginInstallType plugin install type
     * {@link CMPackageManager#PLUGIN_METHOD_DEFAULT}
     * {@link CMPackageManager#PLUGIN_METHOD_DEXMAKER}
     * {@link CMPackageManager#PLUGIN_METHOD_INSTR}
     * @param actInfo plugin Activity's ActivityInfo
     * @return proxy activity's fully class name
     */
    public static String getProxyActivityClsName(String plunginInstallType, ActivityInfo actInfo, String processName) {
        boolean isTranslucent = false;
        boolean isHandleConfigChange = false;
        boolean isLandscape = false;
        if (actInfo != null && actInfo.metaData != null) {
            String special_cfg = actInfo.metaData.getString(META_KEY_ACTIVITY_SPECIAL);
            if (!TextUtils.isEmpty(special_cfg)) {
                if (special_cfg.contains(PLUGIN_ACTIVITY_TRANSLUCENT)) {
                    PluginDebugLog.log(TAG, "getProxyActivityClsName meta data contrains translucent flag");
                    isTranslucent = true;
                }

                if (special_cfg.contains(PLUGIN_ACTIVTIY_HANDLE_CONFIG_CHAGNE)) {
                    PluginDebugLog.log(TAG, "getProxyActivityClsName meta data contrains handleConfigChange flag");
                    isHandleConfigChange = true;
                }
            }
        }

        if (actInfo != null) {
            if (actInfo.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                PluginDebugLog.log(TAG, "getProxyActivityClsName activiy screenOrientation: "
                        + actInfo.screenOrientation + " isHandleConfigChange = false");
                isHandleConfigChange = false;
            }

            if (actInfo.screenOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                PluginDebugLog.log(TAG, "getProxyActivityClsName isLandscape = true");
                isLandscape = true;
            }
        }

        return getProxyActivityClsName(plunginInstallType, isTranslucent, isLandscape, isHandleConfigChange,
                processName);
    }

    static String getProxyActivityClsName(String plunginInstallType, boolean isTranslucent, boolean isLandscape,
            boolean handleConfigChange, String processName) {
        if (TextUtils.equals(CMPackageManager.PLUGIN_METHOD_DEXMAKER, plunginInstallType)) {
            return TARGET_CLASS_NAME;
        } else if (TextUtils.equals(CMPackageManager.PLUGIN_METHOD_INSTR, plunginInstallType)) {
            return ProxyComponentMappingByProcess.mappingActivity(isTranslucent, isLandscape, handleConfigChange,
                    processName);
        } else {
            // Default option
            return ProxyComponentMappingByProcess.mappingActivity(isTranslucent, isLandscape, handleConfigChange,
                    processName);
        }
    }

    static boolean isChangedIntent(Intent originalIntent) {
        if (originalIntent != null && originalIntent.getComponent() != null
                && !TextUtils.isEmpty(originalIntent.getStringExtra(EXTRA_TARGET_ACTIVITY))
                && !TextUtils.isEmpty(originalIntent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME))) {
            if (originalIntent.getComponent().getClassName().startsWith(InstrActivityProxy.class.getName())
                    || originalIntent.getComponent().getClassName()
                            .startsWith(InstrActivityProxyTranslucent.class.getName())) {
                return true;
            }
        }
        return false;
    }

    public static void setPluginIntent(Intent intent, String pluginId, String actName) {
        ProxyEnvironment env = ProxyEnvironmentManager.getEnvByPkgName(pluginId);
        if (null == env) {
            PluginDebugLog.formatLog(TAG,
                    "ActivityJumpUtil setPluginIntent failed, %s, ProxyEnvironmentNew is null", pluginId);
            return;
        }
        ActivityInfo info = env.getTargetMapping().getActivityInfo(actName);
        if (null == info) {
            PluginDebugLog.formatLog(TAG,
                    "ActivityJumpUtil setPluginIntent failed, activity info is null. actName: %s", actName);
            return;
        }
        ComponentName compname = new ComponentName(env.getParentPackagename(),
                getProxyActivityClsName(env.getInstallType(), info, env.getRunningProcessName()));
        intent.setComponent(compname).putExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME, pluginId)
                .putExtra(EXTRA_TARGET_ACTIVITY, actName);
        IntentUtils.setProxyInfo(intent,pluginId);

    }

    public static Intent handleStartActivityIntent(String pluginId, Intent intent, int requestCode, Bundle options,
            Context context) {
        if (intent == null) {
            PluginDebugLog.log(TAG, "handleStartActivityIntent intent is null!");
            return intent;
        }
        PluginDebugLog.log(TAG, "handleStartActivityIntent: pluginId: " + pluginId + ", intent: " + intent
                + ", requestCode: " + requestCode);
        if (isChangedIntent(intent)) {
            PluginDebugLog.log(TAG,
                    "handleStartActivityIntent has change the intent just return the original intent! " + intent);
            return intent;
        }
        ActivityInfo targetActivity = null;
        ProxyEnvironment mgr = null;
        // 主要做以下工作：
        // 1 、修改Intent的跳转目标
        // 2 、帮助插件类加载器决定使用哪个activity类加载器
        // 优先判断类名，若类名为空再判断 Action
        if (intent.getComponent() != null && !TextUtils.isEmpty(intent.getComponent().getClassName())) {
            // action 为空，但是指定了包名和 activity类名
            ComponentName compname = intent.getComponent();
            String pkg = compname.getPackageName();
            String toActName = compname.getClassName();
            mgr = ProxyEnvironmentManager.getEnvByPkgName(pluginId);
            // First find in the current apk
            if (mgr != null) {
                if (TextUtils.equals(pkg, pluginId) || TextUtils.equals(pkg, mgr.getParentPackagename())) {
                    TargetMapping thisPlugin = mgr.getTargetMapping();
                    targetActivity = thisPlugin.getActivityInfo(toActName);
                }
            }
            // Second find from other init plugin apk
            if (targetActivity == null) {
                mgr = ProxyEnvironmentManager.getEnvByPkgName(pkg);
                // Check in pkg's apk
                if (!TextUtils.isEmpty(pkg) && mgr != null) {
                    TargetMapping otherPlug = mgr.getTargetMapping();
                    if (otherPlug != null) {
                        targetActivity = otherPlug.getActivityInfo(toActName);
                    }
                }
                // check in all other installed apk, but hasn't init.
                // In multiple process environment, currently plugin shouldn't
                // invoke other plugin directly, only can invoke other plugin
                // by host
                // if (targetActivity == null) {
                // mgr = ProxyEnvironmentNew.getInstance(pluginId);
                // CMPackageManager pkm =
                // CMPackageManager.getInstance(mgr.getHostContext());
                // for (CMPackageInfo plugInfo : pkm.getInstalledApps()) {
                // if (TextUtils.equals(pkg, plugInfo.packageName)
                // && !TextUtils.equals(plugInfo.packageName, pluginId)) {
                // targetActivity = new ActivityInfo();
                // targetActivity.packageName = pkg;
                // targetActivity.name = toActName;
                // break;
                // }
                // }
                // }
            }
        } else {
            mgr = ProxyEnvironmentManager.getEnvByPkgName(pluginId);
            if (mgr != null) {
                TargetMapping mapping = mgr.getTargetMapping();
                if (mapping != null) {
                    targetActivity = mapping.resolveActivity(intent);
                }
            } else {
                if (null != context) {
                    List<CMPackageInfo> packageList = CMPackageManagerImpl.getInstance(context).getInstalledApps();
                    if (packageList != null) {
                        for (CMPackageInfo pkgInfo : CMPackageManagerImpl.getInstance(context).getInstalledApps()) {
                            if (pkgInfo != null) {
                                ApkTargetMappingNew target = pkgInfo.getTargetMapping(context);
                                if (null != target) {
                                    targetActivity = target.resolveActivity(intent);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        PluginDebugLog.log(TAG, "handleStartActivityIntent pluginId: " + pluginId + " intent: " + intent.toString()
                + " targetActivity: " + targetActivity);
        if (targetActivity != null) {
            setPluginIntent(intent, targetActivity.packageName, targetActivity.name);
        }
        if (mgr != null) {
            mgr.getActivityStackSupervisor().dealLaunchMode(intent);
        }
        String intentInfo = "";
        if (null != intent) {
            intentInfo = intent.toString();
            if (null != intent.getExtras()) {
                intentInfo = intentInfo + intent.getExtras().toString();
            }
        }
        PluginCenterDebugHelper.getInstance().savePluginActivityAndServiceJump(
                PluginCenterDebugHelper.getInstance().getCurrentSystemTime(),intentInfo);
        return intent;
    }
}
