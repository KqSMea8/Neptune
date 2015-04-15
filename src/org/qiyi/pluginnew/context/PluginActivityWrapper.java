package org.qiyi.pluginnew.context;

import java.io.File;

import org.qiyi.pluginlibrary.ProxyEnvironment;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;
import android.view.ContextThemeWrapper;

class PluginActivityWrapper extends ContextThemeWrapper {
	private static final String tag = "PluginActivityWrapper";
	private ProxyEnvironment plugin;
	private Context appWrapper;

	public PluginActivityWrapper(Context base, Context appWrapper, ProxyEnvironment plugin) {
		attachBaseContext(base);
		this.plugin = plugin;
		this.appWrapper = appWrapper;
	}

	// @Override
	// public Theme getTheme() {
	// Log.d(tag, "getTheme()");
	// return null;
	// }

	@Override
	public File getFilesDir() {
		return appWrapper.getFilesDir();
	}

	@Override
	public String getPackageResourcePath() {
		Log.d(tag, "getPackageResourcePath()");
		return appWrapper.getPackageResourcePath();
	}

	@Override
	public String getPackageCodePath() {
		Log.d(tag, "getPackageCodePath()");
		return appWrapper.getPackageCodePath();
	}

	@Override
	public File getCacheDir() {
		Log.d(tag, "getCacheDir()");
		return appWrapper.getCacheDir();
	}

	@Override
	public PackageManager getPackageManager() {
		Log.d(tag, "PackageManager()");
		return appWrapper.getPackageManager();
	}

	@Override
	public ApplicationInfo getApplicationInfo() {
		return appWrapper.getApplicationInfo();
	}

	@Override
	public Context getApplicationContext() {
		Log.d(tag, "getApplicationContext()");
		return appWrapper.getApplicationContext();
	}

	@Override
	public String getPackageName() {
		Log.d(tag, "getPackageName()");
		return appWrapper.getPackageName();
	}

	@Override
	public Resources getResources() {
		Log.d(tag, "getResources()");
		return appWrapper.getResources();
	}

	@Override
	public AssetManager getAssets() {
		Log.d(tag, "getAssets()");
		return appWrapper.getAssets();
	}
}
