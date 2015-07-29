package org.qiyi.pluginlibrary.plugin;

import org.qiyi.pluginlibrary.utils.ResourcesToolForPlugin;

import android.content.Context;

public interface InterfeceToGetHost {
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
}
