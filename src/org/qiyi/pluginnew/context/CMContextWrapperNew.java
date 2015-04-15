package org.qiyi.pluginnew.context;

import org.qiyi.plugin.manager.ProxyEnvironmentNew;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;

public class CMContextWrapperNew extends ContextWrapper {
	/** 插件包名 */
	private String mPackagename = null;

	public CMContextWrapperNew(Context paramContext) {
		super(paramContext);
	}

	/**
	 * @param packagename
	 *            the mPackagename to set
	 * 
	 * @hide
	 */
	public void setTargetPackagename(String packagename) {
		mPackagename = packagename;
	}

	public String getTargetPackageName() {
		return mPackagename;
	}

	@Override
	public ClassLoader getClassLoader() {
		return ProxyEnvironmentNew.getInstance(mPackagename).getDexClassLoader();
	}

	@Override
	public Context getApplicationContext() {
		return ProxyEnvironmentNew.getInstance(mPackagename).getApplication();
	}

	@Override
	public Resources getResources() {
		return ProxyEnvironmentNew.getInstance(mPackagename).getTargetResources();
	}

	@Override
	public AssetManager getAssets() {
		return getResources().getAssets();
	}

	// 插件自己保存一个theme，不用父类创建的，为了兼容OPPO手机上的bug
	private Resources.Theme mTargetTheme;

	@Override
	public Theme getTheme() {
		if (mTargetTheme == null) {
			mTargetTheme = ProxyEnvironmentNew.getInstance(mPackagename).getTargetResources()
					.newTheme();
			mTargetTheme.setTo(ProxyEnvironmentNew.getInstance(mPackagename).getTargetTheme());
		}
		return mTargetTheme;
	}

	@Override
	public void setTheme(int resid) {
		getTheme().applyStyle(resid, true);
	}
}
