package org.qiyi.pluginlibrary.component;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import org.qiyi.plugin.manager.ProxyEnvironmentNew;
import org.qiyi.pluginlibrary.PluginActivityControl;
import org.qiyi.pluginlibrary.pm.CMPackageInfo;
import org.qiyi.pluginlibrary.pm.CMPackageManager;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginnew.ActivityJumpUtil;
import org.qiyi.pluginnew.ActivityOverider;
import org.qiyi.pluginnew.context.CMContextWrapperNew;
import org.qiyi.pluginnew.service.PluginServiceWrapper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;

public class InstrActivityProxy extends Activity {

	private static final String TAG = InstrActivityProxy.class.getSimpleName();

	private ProxyEnvironmentNew mPluginEnv;
	private PluginActivityControl mPluginContrl;

	/**
	 * 装载插件的Activity
	 * 
	 * @param mPlugin
	 */
	private Activity fillPluginActivity(ProxyEnvironmentNew env, String actClsName) {
		try {
			Activity myPlugin = (Activity) env.getDexClassLoader().loadClass(actClsName)
					.newInstance();
			return myPlugin;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private String[] getPkgAndCls() {
		final Bundle pluginMessage = getIntent().getExtras();
		String[] result = new String[2];
		if (null != pluginMessage) {
			result[1] = pluginMessage.getString(ProxyEnvironmentNew.EXTRA_TARGET_ACTIVITY, "");
			result[0] = pluginMessage.getString(ProxyEnvironmentNew.EXTRA_TARGET_PACKAGNAME, "");
			if (!TextUtils.isEmpty(result[0]) && !TextUtils.isEmpty(result[1])) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Try to init proxy environment
	 * 
	 * @param pkgName
	 */
	private void tryToInitEnvironment(String pkgName) {
		if (!TextUtils.isEmpty(pkgName) && null == mPluginEnv) {
			if (!ProxyEnvironmentNew.hasInstance(pkgName)) {
				String installMethod = "";
				CMPackageInfo pkgInfo = CMPackageManager.getInstance(this).getPackageInfo(pkgName);
				if (null != pkgInfo && pkgInfo.pluginInfo != null) {
					installMethod = pkgInfo.pluginInfo.mPluginInstallMethod;
				} else {
					Log.e(TAG, "Cann't get pkginfo for: " + pkgName);
					return;
				}
				PluginDebugLog.log("plugin", "doInBackground:" + pkgName + ", installMethod: "
						+ installMethod);
				ProxyEnvironmentNew.initProxyEnvironment(InstrActivityProxy.this, pkgName, installMethod);
			}
			mPluginEnv = ProxyEnvironmentNew.getInstance(pkgName);
		}
	}

//	private boolean mNeedUpdateConfiguration = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String pluginActivityName = null;
		String pluginPkgName = null;
		String[] pkgAndCls = getPkgAndCls();
		if (pkgAndCls != null) {
			pluginPkgName = pkgAndCls[0];
			pluginActivityName = pkgAndCls[1];
		} else {
			Log.e(TAG, "Pkg or activity is null in LActivityProxy, just return!");
			this.finish();
			// throw new
			// PluginCreateFailedException("Please put the Plugin Path!");
		}

		tryToInitEnvironment(pluginPkgName);
		if (!ProxyEnvironmentNew.isEnterProxy(pluginActivityName)) {
			Intent i = new Intent();
			i.setComponent(new ComponentName(pluginPkgName,
					ProxyEnvironmentNew.EXTRA_VALUE_LOADTARGET_STUB));
			ProxyEnvironmentNew.launchIntent(InstrActivityProxy.this, null, i);
		}
		Activity plugin = fillPluginActivity(mPluginEnv, pluginActivityName);
		if (null == plugin) {
			Log.e(TAG, "Cann't get pluginActivityName class finish!");
			this.finish();
		}
		try {
			mPluginContrl = new PluginActivityControl(InstrActivityProxy.this, plugin,
					mPluginEnv.getApplication(), mPluginEnv.mPluginInstrument);
		} catch (Exception e1) {
			e1.printStackTrace();
			this.finish();
		}
		if (null != mPluginContrl) {
			CMContextWrapperNew pluginContextWrapper = new CMContextWrapperNew(InstrActivityProxy.this,
					pluginPkgName);
			ActivityInfo actInfo = mPluginEnv.findActivityByClassName(pluginActivityName);
			mPluginContrl.dispatchProxyToPlugin(mPluginEnv.mPluginInstrument, pluginContextWrapper);
			if (actInfo != null) {
				ActivityOverider.changeActivityInfo(this, pluginPkgName, pluginActivityName);

				int resTheme = actInfo.getThemeResource();
				setTheme(resTheme);
				// Set plugin's default theme.
				plugin.setTheme(resTheme);
			}
			try {
				mPluginContrl.callOnCreate(savedInstanceState);
				if (getParent() == null) {
					mPluginEnv.pushActivityToStack(this);
				}
			} catch (Exception e) {
				processError(e);
				this.finish();
			}
		}
	}

	private void processError(Exception e) {
		e.printStackTrace();
	}

	public PluginActivityControl getController() {
		return mPluginContrl;
	}

	@Override
	public Resources getResources() {
		if (mPluginEnv == null) {
			return super.getResources();
		}
		return mPluginEnv.getTargetResources() == null ? super.getResources() : mPluginEnv
				.getTargetResources();
	}

	@Override
	public void setTheme(int resid) {
//		String[] temp = getPkgAndCls();
//		if (mNeedUpdateConfiguration && (temp != null || mPluginEnv != null)) {
//			tryToInitEnvironment(temp[0]);
//			if (mPluginEnv != null) {
//				ActivityInfo actInfo = mPluginEnv.findActivityByClassName(temp[1]);
//				if (actInfo != null) {
//					int resTheme = actInfo.getThemeResource();
//					if (mNeedUpdateConfiguration) {
//						ActivityOverider.changeActivityInfo(InstrActivityProxy.this, temp[0], temp[1]);
//						super.setTheme(resTheme);
//						mNeedUpdateConfiguration = false;
//						return;
//					}
//				}
//			}
//		}
//		super.setTheme(resid);
		getTheme().applyStyle(resid, true);
	}

	@Override
	public Theme getTheme() {
		if (mPluginEnv == null) {
			String[] temp = getPkgAndCls();
			tryToInitEnvironment(temp[0]);
		}
		return super.getTheme();
	}

	@Override
	public AssetManager getAssets() {
		if (mPluginEnv == null) {
			return super.getAssets();
		}
		return mPluginEnv.getTargetAssetManager() == null ? super.getAssets() : mPluginEnv
				.getTargetAssetManager();
	}

	@Override
	public ClassLoader getClassLoader() {
		if (mPluginEnv == null) {
			return super.getClassLoader();
		}
		return mPluginEnv.getDexClassLoader();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (getController() != null) {
			getController().callOnResume();
			// LCallbackManager.callAllOnResume();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (getController() != null) {

			try {
				getController().callOnStart();
			} catch (Exception e) {

				processError(e);

			}
		}
	}

	@Override
	protected void onDestroy() {
		if (getController() != null) {

			try {
				getController().callOnDestroy();
				// LCallbackManager.callAllOnDestroy();
			} catch (Exception e) {
				processError(e);
			}
		}
		if (null == this.getParent() && mPluginEnv != null) {
			mPluginEnv.popActivityFromStack(this);
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (getController() != null) {

			try {
				getController().callOnPause();
				// LCallbackManager.callAllOnPause();
			} catch (Exception e) {
				processError(e);
			}
		}
	}

	@Override
	public void onBackPressed() {
		if (getController() != null) {
			try {
				getController().callOnBackPressed();
				// LCallbackManager.callAllOnBackPressed();
			} catch (Exception e) {
				processError(e);
			}

		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (getController() != null) {
			try {
				getController().callOnStop();
				// LCallbackManager.callAllOnStop();
			} catch (Exception e) {
				processError(e);
			}
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (getController() != null) {
			try {
				getController().callOnRestart();
				// LCallbackManager.callAllOnRestart();
			} catch (Exception e) {
				processError(e);
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (getController() != null) {
			// LCallbackManager.callAllOnKeyDown(keyCode, event);
			return getController().callOnKeyDown(keyCode, event);

		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public ComponentName startService(Intent service) {
		if (mPluginEnv != null) {
			mPluginEnv.remapStartServiceIntent(service);
		}
		return super.startService(service);
	}

	@Override
	public boolean stopService(Intent name) {
		if (mPluginEnv != null) {
			String actServiceClsName = name.getComponent().getClassName();
			PluginServiceWrapper plugin = ProxyEnvironmentNew.sAliveServices
					.get(PluginServiceWrapper.getIndeitfy(mPluginEnv.getTargetPackageName(),
							actServiceClsName));
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
		if (mPluginEnv != null) {
			mPluginEnv.remapStartServiceIntent(service);
		}
		return super.bindService(service, conn, flags);
	}

	public void startActivityForResult(Intent intent, int requestCode) {
		if (mPluginEnv != null) {
			super.startActivityForResult(ActivityJumpUtil.handleStartActivityIntent(
					mPluginEnv.getTargetPackageName(), intent, requestCode, null, this), requestCode);
		} else {
			super.startActivityForResult(intent, requestCode);
		}
	}

	@SuppressLint("NewApi")
	public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
		if (mPluginEnv != null) {
			super.startActivityForResult(ActivityJumpUtil.handleStartActivityIntent(
					mPluginEnv.getTargetPackageName(), intent, requestCode, options, this), requestCode,
					options);
		} else {
			super.startActivityForResult(intent, requestCode, options);
		}
	}

//	public void startActivityFromFragment(Fragment fragment, Intent intent, int requestCode) {
//		// TODO Auto-generated method stub
//		super.startActivityFromFragment(fragment, intent, requestCode);
//	}
//
//	@Override
//	public void startActivityFromFragment(Fragment fragment, Intent intent, int requestCode,
//			Bundle options) {
//		// TODO Auto-generated method stub
//		super.startActivityFromFragment(fragment, intent, requestCode, options);
//	}

	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		if (mPluginEnv != null && mPluginEnv.getTargetMapping() != null
				&& mPluginEnv.getTargetMapping().isDataNeedPrefix()) {
			name = mPluginEnv.getTargetPackageName() + "_" + name;
		}
		return super.getSharedPreferences(name, mode);
	}

	@Override
	public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
		super.dump(prefix, fd, writer, args);
		if (getController() != null) {
			getController().callDump(prefix, fd, writer, args);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
//		mNeedUpdateConfiguration = true;
		if (getController() != null) {
			getController().callOnConfigurationChanged(newConfig);
		}
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		if (getController() != null) {
			getController().callOnPostResume();
		}
	}

	@Override
	public void onDetachedFromWindow() {
		if (getController() != null) {
			getController().callOnDetachedFromWindow();
		}
		super.onDetachedFromWindow();
	}

	@Override
	public View onCreateView(String name, Context context, AttributeSet attrs) {
		if (getController() != null) {
			return getController().callOnCreateView(name, context, attrs);
		}
		return super.onCreateView(name, context, attrs);
	}

	@Override
	public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
		if (getController() != null) {
			return getController().callOnCreateView(parent, name, context, attrs);
		}
		return super.onCreateView(parent, name, context, attrs);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (getController() != null) {
			getController().callOnNewIntent(intent);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (getController() != null) {
			getController().getPluginRef().call("onActivityResult", requestCode, resultCode, data);
		}
	}

	@Override
	public void onAttachFragment(Fragment fragment) {

		super.onAttachFragment(fragment);
		getController().getPlugin().onAttachFragment(fragment);
	}

	@Override
	public View onCreatePanelView(int featureId) {

		if (getController() != null && getController().getPlugin() != null) {
			return getController().getPlugin().onCreatePanelView(featureId);
		} else {
			return super.onCreatePanelView(featureId);
		}
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		super.onOptionsMenuClosed(menu);
		if (getController() != null && getController().getPlugin() != null) {
			getController().getPlugin().onOptionsMenuClosed(menu);
		}
	}

	@Override
	public void onPanelClosed(int featureId, Menu menu) {
		super.onPanelClosed(featureId, menu);
		if (getController() != null && getController().getPlugin() != null) {
			getController().getPlugin().onPanelClosed(featureId, menu);
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (getController() != null && getController().getPlugin() != null) {
			return getController().getPlugin().onKeyUp(keyCode, event);
		} else {
			return super.onKeyUp(keyCode, event);
		}
	}

	@Override
	public void onAttachedToWindow() {

		super.onAttachedToWindow();
		if (getController() != null && getController().getPlugin() != null) {
			getController().getPlugin().onAttachedToWindow();
		}
	}

	@Override
	public CharSequence onCreateDescription() {
		if (getController() != null && getController().getPlugin() != null) {
			return getController().getPlugin().onCreateDescription();
		} else {
			return super.onCreateDescription();
		}
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (getController() != null && getController().getPlugin() != null) {
			return getController().getPlugin().onGenericMotionEvent(event);
		} else {
			return super.onGenericMotionEvent(event);
		}
	}

	@Override
	public void onContentChanged() {

		super.onContentChanged();
		if (getController() != null && getController().getPlugin() != null) {
			getController().getPlugin().onContentChanged();
		}
	}

	@Override
	public boolean onCreateThumbnail(Bitmap outBitmap, Canvas canvas) {
		if (getController() != null && getController().getPlugin() != null) {
			return getController().getPlugin().onCreateThumbnail(outBitmap, canvas);
		} else {
			return super.onCreateThumbnail(outBitmap, canvas);
		}
	}
}