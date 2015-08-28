package org.qiyi.pluginlibrary.plugin;

import org.qiyi.pluginlibrary.utils.ResourcesToolForPlugin;

import android.content.Context;

public interface InterfaceToGetHost {
	/**
	 * Get the context which start this plugin
	 * 
	 * @return
	 */
	Context getOriginalContext();

	/**
	 * Get host resource
	 * 
	 * @return host resource tool
	 */
	ResourcesToolForPlugin getHostResourceTool();

	/**
	 * Get plugin's pkgName
	 */
	String getPluginPackageName();

	/**
	 * Finish all alive activity and kill the current process
	 */
	void exitApp();
}
