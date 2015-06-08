package org.qiyi.pluginnew.context;

import java.io.File;

import org.qiyi.plugin.manager.ProxyEnvironmentNew;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Plugin Context 包装类
 * 
 */
public class PluginContextWrapper extends CustomContextWrapper {
	private static final String tag = "PluginContextWrapper";
	private ProxyEnvironmentNew plugin;
	private File fileDir;
//	private PluginPackageManager pkgManager;
	public PluginContextWrapper(Context base, ProxyEnvironmentNew plugin) {
		super(base);
		this.plugin = plugin;
		fileDir = new File(plugin.getTargetMapping().getDataDir() + "/files/");
//		pkgManager = new PluginPackageManager(base.getPackageManager());
	}

	@Override
	public File getFilesDir() {
		if (!fileDir.exists()) {
			fileDir.mkdirs();
		}
		return fileDir;
	}

	@Override
	public String getPackageResourcePath() {
		PluginDebugLog.log(tag, "getPackageResourcePath()");
		return super.getPackageResourcePath();
	}

	@Override
	public String getPackageCodePath() {
		PluginDebugLog.log(tag, "getPackageCodePath()");
		return super.getPackageCodePath();
	}

	@Override
	public File getCacheDir() {
		PluginDebugLog.log(tag, "getCacheDir()");
		return super.getCacheDir();
	}

	@Override
	public PackageManager getPackageManager() {
		PluginDebugLog.log(tag, "PackageManager()");
//		return pkgManager;
		return super.getPackageManager();
	}

	@Override
	protected String getTargetPackageName() {
		return plugin.getTargetPackageName();
	}

	@Override
	protected ProxyEnvironmentNew getEnvironment() {
		return plugin;
	}

	@Override
	protected String getLogTag() {
		return PluginContextWrapper.class.getSimpleName();
	}
}
