package org.qiyi.pluginlibrary.component;

import org.qiyi.pluginlibrary.ProxyEnvironment;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;


public class CMContextWrapper extends ContextWrapper {

    /** 插件包名 */
    private String mPackagename = null;

    public CMContextWrapper(Context paramContext) {
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
        attachBaseContext(ProxyEnvironment.getInstance(mPackagename).getApplication().getBaseContext());
    }

    public String getTargetPackageName() {
        return mPackagename;
    }

    @Override
    public ClassLoader getClassLoader() {
        return ProxyEnvironment.getInstance(mPackagename).getDexClassLoader();
    }

    @Override
    public Context getApplicationContext() {
        return ProxyEnvironment.getInstance(mPackagename).getApplication();
    }

    @Override
    public Resources getResources() {
        return ProxyEnvironment.getInstance(mPackagename).getTargetResources();
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
            mTargetTheme = ProxyEnvironment.getInstance(mPackagename).getTargetResources().newTheme();
            mTargetTheme.setTo(ProxyEnvironment.getInstance(mPackagename).getTargetTheme());
        }
        return mTargetTheme;
    }

    @Override
    public void setTheme(int resid) {
        getTheme().applyStyle(resid, true);
    }
}
