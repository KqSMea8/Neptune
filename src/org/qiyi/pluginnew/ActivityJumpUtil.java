package org.qiyi.pluginnew;

import org.qiyi.plugin.manager.ProxyEnvironmentNew;
import org.qiyi.pluginlibrary.ProxyEnvironment;
import org.qiyi.pluginlibrary.component.InstrActivityProxy;
import org.qiyi.pluginlibrary.plugin.TargetMapping;
import org.qiyi.pluginlibrary.pm.CMPackageInfo;
import org.qiyi.pluginlibrary.pm.CMPackageManager;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

/**
 * Help class for plugin activity jumping.
 */
public class ActivityJumpUtil {
	private static final String TAG = ActivityJumpUtil.class.getSimpleName();

	public static final String TARGET_CLASS_NAME = "org.qiyi.PluginActivity";

	/**
	 * Get proxy activity class name for all plugin activitys The proxy activity
	 * should be register in the manifest.xml
	 * 
	 * @param plunginInstallType
	 *            plugin install type
	 *            {@link CMPackageManager#PLUGIN_METHOD_DEFAULT}
	 *            {@link CMPackageManager#PLUGIN_METHOD_DEXMAKER}
	 *            {@link CMPackageManager#PLUGIN_METHOD_INSTR}
	 * 
	 * @return proxy activity's fully class name
	 */
	public static String getProxyActivityClsName(String plunginInstallType) {
		if (TextUtils.equals(CMPackageManager.PLUGIN_METHOD_DEXMAKER, plunginInstallType)) {
			return TARGET_CLASS_NAME;
		} else if (TextUtils.equals(CMPackageManager.PLUGIN_METHOD_INSTR, plunginInstallType)) {
			return InstrActivityProxy.class.getName();
		} else {
			// Default option
			return InstrActivityProxy.class.getName();
		}
	}
	
	private static void setPluginIntent(Intent intent, String pluginId, String actName) {
		ProxyEnvironmentNew env = ProxyEnvironmentNew.getInstance(pluginId);
		if (null == env) {
			Log.e(TAG, "ActivityJumpUtil setPluginIntent failed, " + pluginId
					+ " ProxyEnvironmentNew is null");
			return;
		}
		ComponentName compname = new ComponentName(env.getParentPackagename(),
				getProxyActivityClsName(env.getInstallType()));
		intent.setComponent(compname).putExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME, pluginId)
				.putExtra(ProxyEnvironment.EXTRA_TARGET_ACTIVITY, actName);
	}

	public static Intent handleStartActivityIntent(String pluginId, Intent intent, int requestCode,
			Bundle options) {
		ActivityInfo targetActivity = null;
		ProxyEnvironmentNew mgr = null;
		// 主要做以下工作：
		// 1 、修改Intent的跳转目标
		// 2 、帮助插件类加载器决定使用哪个activity类加载器
		// 优先判断类名，若类名为空再判断 Action
		if (intent.getComponent() != null && intent.getComponent().getClassName() != null) {
			// action 为空，但是指定了包名和 activity类名
			ComponentName compname = intent.getComponent();
			String pkg = compname.getPackageName();
			String toActName = compname.getClassName();
			mgr = ProxyEnvironmentNew.getInstance(pluginId);
			// First find in the current apk
			if (mgr != null) {
				if (TextUtils.equals(pkg, pluginId)
						|| TextUtils.equals(pkg, mgr.getParentPackagename())) {
					TargetMapping thisPlugin = mgr.getTargetMapping();
					targetActivity = thisPlugin.getActivityInfo(toActName);
				}
			}
			// Second find from other init plugin apk
			if (targetActivity == null) {
				mgr = ProxyEnvironmentNew.getInstance(pkg);
				// Check in pkg's apk
				if (!TextUtils.isEmpty(pkg) && mgr != null) {
					TargetMapping otherPlug = mgr.getTargetMapping();
					if (otherPlug != null) {
						targetActivity = otherPlug.getActivityInfo(toActName);
					}
				}
				// check in all other installed apk, but hasn't init.
				if (targetActivity == null) {
					mgr = ProxyEnvironmentNew.getInstance(pluginId);
					CMPackageManager pkm = CMPackageManager.getInstance(mgr.getHostContext());
					for (CMPackageInfo plugInfo : pkm.getInstalledApps()) {
						if (TextUtils.equals(pkg, plugInfo.packageName)
								&& !TextUtils.equals(plugInfo.packageName, pluginId)) {
							targetActivity = new ActivityInfo();
							targetActivity.packageName = pkg;
							targetActivity.name = toActName;
							break;
						}
					}
				}
			}
		} else {
			mgr = ProxyEnvironmentNew.getInstance(pluginId);
			if (mgr != null) {
				TargetMapping mapping = mgr.getTargetMapping();
				if (mapping != null) {
					targetActivity = mapping.resolveActivity(intent);
				}
			} else {
				// TODO CMPackageManager keep all intent filter, then loop from
				// CMPackageManager
			}
		}
		Log.d(TAG,
				"handleStartActivityIntent pluginId: " + pluginId + " intent: " + intent.toString()
						+ " targetActivity: " + targetActivity);
		if (targetActivity != null) {
			setPluginIntent(intent, targetActivity.packageName, targetActivity.name);
		}
		if (mgr != null) {
			mgr.dealLaunchMode(intent);
		}
		return intent;
	}
}
