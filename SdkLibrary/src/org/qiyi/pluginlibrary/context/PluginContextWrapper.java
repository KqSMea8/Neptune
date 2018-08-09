package org.qiyi.pluginlibrary.context;

import org.qiyi.pluginlibrary.runtime.PluginLoadedApk;
import org.qiyi.pluginlibrary.runtime.PluginManager;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.view.LayoutInflater;

/**
 * 自定义ContextWrapper的实现类
 */
public class PluginContextWrapper extends CustomContextWrapper {
    /** 插件包名 */
    private String mPackageName = "";
    /** 是否是Application, Service的Base Context*/
    private boolean forApp = false;
    // 插件自己保存一个theme，不用父类创建的，为了兼容OPPO手机上的bug
    private Resources.Theme mTargetTheme;
    // 插件Application的Context保存一个LayoutInflater
    private LayoutInflater mLayoutInflater;

    public PluginContextWrapper(Context paramContext, String pkgName) {
        this(paramContext, pkgName, false);
    }

    public PluginContextWrapper(Context paramContext, String pkgName, boolean forApplication) {
        super(paramContext);
        this.mPackageName = pkgName;
        this.forApp = forApplication;
    }

    @Override
    public Theme getTheme() {
        if (mTargetTheme == null) {
            PluginLoadedApk mLoadedApk = PluginManager.getPluginLoadedApkByPkgName(mPackageName);
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
    public Object getSystemService(String name) {

        if (forApp && LAYOUT_INFLATER_SERVICE.equals(name)) {
            // 重写插件Application Context的获取LayoutInflater方法，解决插件使用Application Context
            // 无法访问插件资源的问题，原因是LayoutInflater的构造函数使用的是Base Context的outerContext，
            // 而这个OuterContext是宿主的Application
            if (mLayoutInflater == null) {
                LayoutInflater inflater = (LayoutInflater) super.getSystemService(name);
                // 使用当前的Context新建一个
                mLayoutInflater = inflater.cloneInContext(this);
            }
            return mLayoutInflater;
        }

        return super.getSystemService(name);
    }

    @Override
    public String getPluginPackageName() {
        return mPackageName;
    }

    @Override
    protected PluginLoadedApk getPluginLoadedApk() {
        return PluginManager.getPluginLoadedApkByPkgName(mPackageName);
    }
}
