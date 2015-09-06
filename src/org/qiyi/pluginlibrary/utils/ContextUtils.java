package org.qiyi.pluginlibrary.utils;

import java.util.List;

import org.qiyi.plugin.manager.ProxyEnvironmentNew;
import org.qiyi.pluginlibrary.plugin.InterfaceToGetHost;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.text.TextUtils;

public class ContextUtils {
	private static final String TAG = ContextUtils.class.getSimpleName();

	/**
	 * Try to get host context in the plugin environment or the param context
	 * will be return
	 * 
	 * @param context
	 * @return
	 */
	public static Context getOriginalContext(Context context) {
		if (context instanceof InterfaceToGetHost) {
			PluginDebugLog.log(TAG, "Return host  context for getOriginalContext");
			return ((InterfaceToGetHost) context).getOriginalContext();
		} else {
			if (context instanceof Activity) {
				Context base = ((Activity) context).getBaseContext();
				if (context instanceof InterfaceToGetHost) {
					PluginDebugLog.log(TAG, "Return host  context for getOriginalContext");
					return ((InterfaceToGetHost) base).getOriginalContext();
				}
			}
			PluginDebugLog.log(TAG, "Return local context for getOriginalContext");
			return context;
		}
	}

	public static String getTopActivityName(Context context, String packName) {
		String topActivity = getTopActivity(context);
		if (!TextUtils.isEmpty(topActivity)) {
			if (topActivity
					.startsWith("org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent")
					|| topActivity
							.startsWith("org.qiyi.pluginlibrary.component.InstrActivityProxy")) {
				return "plugin:" + ProxyEnvironmentNew.getTopActivity();
			} else {
				return topActivity;
			}
		}
		return null;
	}

	private static String getTopActivity(Context context) {
		ActivityManager manager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);
		if (runningTaskInfos != null)
			return runningTaskInfos.get(0).topActivity.getClassName();
		else
			return null;
	}

	/**
	 * Try to get host ResourcesToolForPlugin in the plugin environment or the
	 * ResourcesToolForPlugin with param context will be return
	 * 
	 * @param context
	 * @return
	 */
	public static ResourcesToolForPlugin getHostResourceTool(Context context) {
		if (context instanceof InterfaceToGetHost) {
			PluginDebugLog.log(TAG, "Return host  resource tool for getHostResourceTool");
			return ((InterfaceToGetHost) context).getHostResourceTool();
		} else {
			if (context instanceof Activity) {
				Context base = ((Activity) context).getBaseContext();
				if (context instanceof InterfaceToGetHost) {
					PluginDebugLog.log(TAG, "Return host  resource tool for getHostResourceTool");
					return ((InterfaceToGetHost) base).getHostResourceTool();
				}
			}
			PluginDebugLog.log(TAG, "Return local context for getHostResourceTool");
			return new ResourcesToolForPlugin(context);
		}
	}

	/**
	 * Get the real package name for this plugin in plugin environment otherwise
	 * return context's package name
	 * 
	 * @return
	 */
	public static String getPluginPackageName(Context context) {
		if (null == context) {
			return null;
		}
		if (context instanceof InterfaceToGetHost) {
			PluginDebugLog.log(TAG, "Return plugin's package name for getPluginPackageName");
			return ((InterfaceToGetHost) context).getPluginPackageName();
		} else {
			if (context instanceof Activity) {
				Context base = ((Activity) context).getBaseContext();
				if (context instanceof InterfaceToGetHost) {
					PluginDebugLog
							.log(TAG, "Return plugin's package name for getPluginPackageName");
					return ((InterfaceToGetHost) base).getPluginPackageName();
				}
			}
			PluginDebugLog.log(TAG, "Return context's package name for getPluginPackageName");
			return context.getPackageName();
		}
	}

	/**
	 * Try to exit current app and exit process
	 * 
	 * @param context
	 */
	public static void exitApp(Context context) {
		if (null != context) {
			if (context instanceof InterfaceToGetHost) {
				((InterfaceToGetHost)context).exitApp();
			}
		}
	}
}
