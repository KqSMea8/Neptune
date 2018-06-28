package org.qiyi.pluginlibrary.component.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.utils.ComponetFinder;
import org.qiyi.pluginlibrary.utils.ContextUtils;

/**
 * author: liuchun
 * date: 2018/6/14
 */
public class PluginAppCompatActivity extends AppCompatActivity implements IPluginBase{

    private PluginActivityDelegate mDelegate;

    @Override
    protected void attachBaseContext(Context newBase) {
        mDelegate = new PluginActivityDelegate();
        newBase = mDelegate.createActivityContext(this, newBase);

        super.attachBaseContext(newBase);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (mDelegate != null) {
            mDelegate.handleActivityOnCreateBefore(this, savedInstanceState);
        }
        super.onCreate(savedInstanceState);
        if (mDelegate != null) {
            mDelegate.handleActivityOnCreateAfter(this, savedInstanceState);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDelegate != null) {
            mDelegate.handleActivityOnDestroy(this);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        // 替换成坑位Activity
        String pkgName = getPluginPackageName();
        intent = ComponetFinder.switchToActivityProxy(pkgName, intent, requestCode, this);

        super.startActivityForResult(intent, requestCode);
    }

    // Api 16新增
    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        // 替换成坑位Activity
        String pkgName = getPluginPackageName();
        intent = ComponetFinder.switchToActivityProxy(pkgName, intent, requestCode, this);

        super.startActivityForResult(intent, requestCode, options);
    }


    ////////////////////////////////////////////////////////////////////
    // 以下是IPluginBase接口实现
    ///////////////////////////////////////////////////////////////////

    @Override
    public String getPluginPackageName() {
        String pkgName = mDelegate != null ? mDelegate.getPluginPackageName() : ContextUtils.getPluginPackageName(this);
        if (TextUtils.isEmpty(pkgName)) {
            pkgName = getPackageName();
        }
        return pkgName;
    }
}
