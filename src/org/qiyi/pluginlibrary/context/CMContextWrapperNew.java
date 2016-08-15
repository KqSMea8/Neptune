package org.qiyi.pluginlibrary.context;

import org.qiyi.pluginlibrary.manager.ProxyEnvironment;
import org.qiyi.pluginlibrary.manager.ProxyEnvironmentManager;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;

public class CMContextWrapperNew extends CustomContextWrapper {
    /** 插件包名 */
    private String mPackagename = null;

    // 插件自己保存一个theme，不用父类创建的，为了兼容OPPO手机上的bug
    private Resources.Theme mTargetTheme;

    public CMContextWrapperNew(Context paramContext, String pkgName) {
        super(paramContext);
        mPackagename = pkgName;
    }

    @Override
    public Theme getTheme() {
        if (mTargetTheme == null) {
            mTargetTheme = ProxyEnvironmentManager.getEnvByPkgName(mPackagename).getTargetResources().newTheme();
            mTargetTheme.setTo(ProxyEnvironmentManager.getEnvByPkgName(mPackagename).getTargetTheme());
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
    protected ProxyEnvironment getEnvironment() {
        return ProxyEnvironmentManager.getEnvByPkgName(mPackagename);
    }

    @Override
    protected String getLogTag() {
        return CMContextWrapperNew.class.getSimpleName();
    }
}
