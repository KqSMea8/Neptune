package org.qiyi.pluginlibrary;

import java.lang.reflect.Field;

import org.qiyi.plugin.manager.ProxyEnvironmentNew;
import org.qiyi.pluginlibrary.ErrorType.ErrorType;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.util.Log;

/**
 * 提供公共方法供自动生成的Activity调用
 */
public class ActivityOverider {
    private static final String tag = "ActivityOverider";

    public static void changeActivityInfo(Activity activity, String pkgName, String actName) {
        Log.d(tag, "changeActivityInfo: activity = " + activity + ", class = " + actName);
        ActivityInfo origActInfo = null;
        try {
            Field field_mActivityInfo = Activity.class.getDeclaredField("mActivityInfo");
            field_mActivityInfo.setAccessible(true);
            origActInfo = (ActivityInfo) field_mActivityInfo.get(activity);
        } catch (Exception e) {
            ProxyEnvironmentNew.deliverPlug(activity, false, pkgName, ErrorType.ERROR_CLIENT_CHANGE_ACTIVITYINFO_FAIL);
            Log.e(tag, Log.getStackTraceString(e));
            return;
        }
        ProxyEnvironmentNew con = ProxyEnvironmentNew.getInstance(pkgName);

        ActivityInfo actInfo = con.findActivityByClassName(actName);
        if (null != actInfo) {
            actInfo.applicationInfo = con.getTargetMapping().getPackageInfo().applicationInfo;
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
                // origActInfo.processName
                origActInfo.screenOrientation = actInfo.screenOrientation;
                origActInfo.softInputMode = actInfo.softInputMode;
                origActInfo.targetActivity = actInfo.targetActivity;
                origActInfo.taskAffinity = actInfo.taskAffinity;
                origActInfo.theme = actInfo.theme;
            }
        }
        // Handle ActionBar title
        if (origActInfo.nonLocalizedLabel != null) {
            activity.setTitle(origActInfo.nonLocalizedLabel);
        } else if (origActInfo.labelRes != 0) {
            activity.setTitle(origActInfo.labelRes);
        } else {
            if (origActInfo.applicationInfo != null) {
                if (origActInfo.applicationInfo.nonLocalizedLabel != null) {
                    activity.setTitle(origActInfo.applicationInfo.nonLocalizedLabel);
                } else if (origActInfo.applicationInfo.labelRes != 0) {
                    activity.setTitle(origActInfo.applicationInfo.labelRes);
                } else {
                    activity.setTitle(origActInfo.applicationInfo.packageName);
                }
            }
        }
        Log.i(tag, "changeActivityInfo->changeTheme: " + " theme = " + actInfo.getThemeResource() + ", icon = " + actInfo.getIconResource()
                + ", logo = " + actInfo.logo + ", labelRes" + actInfo.labelRes);
    }
}
