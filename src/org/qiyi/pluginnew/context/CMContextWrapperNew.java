package org.qiyi.pluginnew.context;

import org.qiyi.plugin.manager.ProxyEnvironmentNew;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;

public class CMContextWrapperNew extends CustomContextWrapper {
	/** 插件包名 */
	private String mPackagename = null;

	public CMContextWrapperNew(Context paramContext, String pkgName) {
		super(paramContext);
		mPackagename = pkgName;
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
	
	@Override
	public String getPluginPackageName() {
		return mPackagename;
	}

	@Override
	protected ProxyEnvironmentNew getEnvironment() {
		return ProxyEnvironmentNew.getInstance(mPackagename);
	}

	@Override
	protected String getLogTag() {
		return CMContextWrapperNew.class.getSimpleName();
	}
}
