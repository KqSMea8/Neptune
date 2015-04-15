package org.qiyi.pluginnew.context;

import java.io.File;

import org.qiyi.plugin.manager.ProxyEnvironmentNew;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

/**
 * Plugin Context 包装类
 * 
 */
public class PluginContextWrapper extends ContextWrapper {
	private ProxyEnvironmentNew plugin;
	private static final String tag = "PluginContextWrapper";
	private ApplicationInfo applicationInfo;
	private File fileDir;
//	private PluginPackageManager pkgManager;
	public PluginContextWrapper(Context base, ProxyEnvironmentNew plugin) {
		super(base);
		this.plugin = plugin;
		applicationInfo = new ApplicationInfo(super.getApplicationInfo());
		applicationInfo.sourceDir = plugin.getTargetPath();
		applicationInfo.dataDir = plugin.getTargetDataRoot().getAbsolutePath();
		fileDir = new File(plugin.getTargetDataRoot().getAbsolutePath() + "/files/");
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
		// TODO Auto-generated method stub
		Log.d(tag, "getPackageResourcePath()");
		return super.getPackageResourcePath();
	}

	@Override
	public String getPackageCodePath() {
		// TODO Auto-generated method stub
		Log.d(tag, "getPackageCodePath()");
		return super.getPackageCodePath();
	}

	@Override
	public File getCacheDir() {
		// TODO Auto-generated method stub
		Log.d(tag, "getCacheDir()");
		return super.getCacheDir();
	}

	@Override
	public PackageManager getPackageManager() {
		// TODO Auto-generated method stub
		Log.d(tag, "PackageManager()");
//		return pkgManager;
		return super.getPackageManager();
	}

	@Override
	public ApplicationInfo getApplicationInfo() {
		return applicationInfo;
	}
	@Override
	public Context getApplicationContext() {
		Log.v(tag, "getApplicationContext()");
		return plugin.getApplication();
	}

	@Override
	public String getPackageName() {
		Log.d(tag, "getPackageName()");
		return plugin.getTargetPackageName();
	}

	@Override
	public Resources getResources() {
		Log.d(tag, "getResources()");
		return plugin.getTargetResources();
	}

	@Override
	public AssetManager getAssets() {
		Log.d(tag, "getAssets()");
		return plugin.getTargetAssetManager();
	}
	// @Override
	// public Object getSystemService(String name) {
	// if (name.equals(Context.ACTIVITY_SERVICE)) {
	// if (plugin.getApplicationInfo().process != null) {
	// return plugin.activityManager;
	// }
	// }
	// return super.getSystemService(name);
	// }
}
