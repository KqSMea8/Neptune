package org.qiyi.pluginlibrary;

import org.qiyi.pluginlibrary.debug.PluginCenterDebugHelper;
import org.qiyi.pluginlibrary.manager.ProxyEnvironment;

import android.content.Intent;
import android.text.TextUtils;

public class ServiceJumpUtil {
    public static final String EXTRA_TARGET_SERVICE = "pluginapp_extra_target_service";
    public static final String BIND_SERVICE_FLAGS = "bind_service_flags";

    private static final String EXTRA_TARGET_CATEGORY = "pluginapp_service_category";

    public static void remapStartServiceIntent(ProxyEnvironment env, Intent originIntent) {

        // 隐式启动Service不支持
        if (originIntent.getComponent() == null) {
            return;
        }

        String targetActivity = originIntent.getComponent().getClassName();
        remapStartServiceIntent(env, originIntent, targetActivity);
    }

    public static void remapStartServiceIntent(ProxyEnvironment env, Intent intent, String targetService) {
        if (null == env || null == intent || TextUtils.isEmpty(targetService)
                || env.getTargetMapping().getServiceInfo(targetService) == null) {
            return;
        }
        intent.setExtrasClassLoader(env.getDexClassLoader());
        intent.putExtra(EXTRA_TARGET_SERVICE, targetService);
        intent.putExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME, env.getTargetPackageName());
        // 同一个进程内service的bind是否重新走onBind方法，以intent的参数匹配为准FilterComparison.filterHashCode)
        // 手动添加一个category 让系统认为不同的插件activity每次bind都会走service的onBind的方法
        intent.addCategory(EXTRA_TARGET_CATEGORY + System.currentTimeMillis());
        try {
            intent.setClass(env.getHostContext(),
                    Class.forName(ProxyComponentMappingByProcess.mappingService(env.getRunningProcessName())));
            String intentInfo = "";
            if (null != intent) {
                intentInfo = intent.toString();
                if (null != intent.getExtras()) {
                    intentInfo = intentInfo + intent.getExtras().toString();
                }
            }
            PluginCenterDebugHelper.getInstance().savePluginActivityAndServiceJump(
                    PluginCenterDebugHelper.getInstance().getCurrentSystemTime(),
                    intentInfo);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
