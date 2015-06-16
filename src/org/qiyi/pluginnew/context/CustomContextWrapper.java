package org.qiyi.pluginnew.context;

import org.qiyi.plugin.manager.ProxyEnvironmentNew;
import org.qiyi.pluginnew.ActivityJumpUtil;
import org.qiyi.pluginnew.service.PluginServiceWrapper;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

public abstract class CustomContextWrapper extends ContextWrapper {

	public CustomContextWrapper(Context base) {
		super(base);
	}

	@Override
	public ClassLoader getClassLoader() {
		return getEnvironment().getDexClassLoader();
	}

//	@Override
//	public String getPackageName() {
//		return getTargetPackageName();
//	}

	@Override
	public Context getApplicationContext() {
		return getEnvironment().getApplication();
	}

	@Override
	public Resources getResources() {
		return getEnvironment().getTargetResources();
	}

	@Override
	public AssetManager getAssets() {
		return getResources().getAssets();
	}

	@Override
	public ComponentName startService(Intent service) {
		Log.d(getLogTag(), "startService: " + service);
		ProxyEnvironmentNew env = getEnvironment();
		if (env != null) {
			env.remapStartServiceIntent(service);
		}
		return super.startService(service);
	}

	@Override
	public boolean stopService(Intent name) {
		Log.d(getLogTag(), "stopService: " + name);
		ProxyEnvironmentNew env = getEnvironment();
		if (env != null) {
			String actServiceClsName = name.getComponent().getClassName();
			PluginServiceWrapper plugin = ProxyEnvironmentNew.sAliveServices
					.get(PluginServiceWrapper.getIndeitfy(getTargetPackageName(), actServiceClsName));
			if (plugin != null) {
				plugin.updateStartStatus(PluginServiceWrapper.PLUGIN_SERVICE_STOPED);
				plugin.tryToDestroyService(name);
				return true;
			}
		}
		return super.stopService(name);
	}

	@Override
	public boolean bindService(Intent service, ServiceConnection conn, int flags) {
		Log.d(getLogTag(), "bindService: " + service);
		ProxyEnvironmentNew env = getEnvironment();
		if (env != null) {
			env.remapStartServiceIntent(service);
		}
		return super.bindService(service, conn, flags);
	}

	@Override
	public void unbindService(ServiceConnection conn) {
		super.unbindService(conn);
		Log.d(getLogTag(), "unbindService: " + conn);
	}

	@Override
	public void startActivity(Intent intent) {
		super.startActivity(ActivityJumpUtil.handleStartActivityIntent(getTargetPackageName(),
				intent, -1, null, this));
	}

	@Override
	public void startActivity(Intent intent, Bundle options) {
		super.startActivity(ActivityJumpUtil.handleStartActivityIntent(getTargetPackageName(),
				intent, -1, options, this), options);
	}

	
	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		if (getEnvironment().getTargetMapping().isDataNeedPrefix()) {
			name = getTargetPackageName() + "_" + name;
		}
		return super.getSharedPreferences(name, mode);
	}

	/**
	 * Return the real packageName(plugin)
	 * 
	 * @return real package name
	 */
	protected abstract String getTargetPackageName();

	/**
	 * Get proxy environment
	 * 
	 * @return plugin's environment
	 */
	protected abstract ProxyEnvironmentNew getEnvironment();

	/**
	 * Get log tag
	 * 
	 * @return log tag
	 */
	protected abstract String getLogTag();
}
