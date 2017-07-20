package org.qiyi.pluginlibrary.context;

import org.qiyi.pluginlibrary.runtime.PluginLoadedApk;
import org.qiyi.pluginlibrary.runtime.PluginManager;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;

/**
 * 自定义ContextWrapper的实现类
 */
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
            PluginLoadedApk mLoadedApk = PluginManager.getPluginLoadedApkByPkgName(mPackagename);
            if (null != mLoadedApk) {
                mTargetTheme = mLoadedApk.getPluginResource().newTheme();
                mTargetTheme.setTo(mLoadedApk.getPluginTheme());
            }
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
    protected PluginLoadedApk getPluginLoadedApk() {
        return PluginManager.getPluginLoadedApkByPkgName(mPackagename);
    }

    @Override
    protected String getLogTag() {
        return CMContextWrapperNew.class.getSimpleName();
    }
}
