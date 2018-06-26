package org.qiyi.pluginlibrary.component;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import org.qiyi.pluginlibrary.HybirdPlugin;
import org.qiyi.pluginlibrary.constant.IIntentConstant;
import org.qiyi.pluginlibrary.pm.PluginPackageManagerNative;
import org.qiyi.pluginlibrary.pm.PluginPackageManagerService;
import org.qiyi.pluginlibrary.runtime.PluginManager;
import org.qiyi.pluginlibrary.utils.IntentUtils;

import java.io.Serializable;

/**
 * 进程因资源不足被回收以后，恢复时插件信息会丢失，这个页面作为临时页面处理插件恢复问题。
 */
public abstract class BaseRecoveryActivity extends Activity {

    private BroadcastReceiver mFinishSelfReceiver;
    private BroadcastReceiver mLaunchPluginReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUi();

        String pkgName = IntentUtils.parsePkgAndClsFromIntent(getIntent())[0];
        if (pkgName == null) {
            finish();
            return;
        }

        if (PluginPackageManagerNative.getInstance(this).isConnected()) {
            // 一般而言，这时候 Service 都是未 connect 的状态，这里只是严谨考虑
            PluginManager.launchPlugin(this, pkgName);
        } else {
            mLaunchPluginReceiver = new LaunchPluginReceiver(pkgName);
            registerReceiver(mLaunchPluginReceiver, new IntentFilter(IIntentConstant.ACTION_SERVICE_CONNECTED));
        }

        // 启动插件成功或者失败后，都应该 finish
        mFinishSelfReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BaseRecoveryActivity.this.finish();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(IIntentConstant.ACTION_START_PLUGIN_ERROR);
        filter.addAction(IIntentConstant.ACTION_PLUGIN_LOADED);
        registerReceiver(mFinishSelfReceiver, filter);
    }

    private void initUi() {
        IRecoveryUiCreator recoveryUiCreator = HybirdPlugin.getConfig().getRecoveryUiCreator();
        if (recoveryUiCreator != null) {
            setContentView(recoveryUiCreator.createContentView(this));
        } else {
            int size = (int) (50 * getResources().getDisplayMetrics().density + 0.5f);
            ProgressBar progressBar = new ProgressBar(this);
            FrameLayout frameLayout = new FrameLayout(this);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size, Gravity.CENTER);
            frameLayout.addView(progressBar, lp);
            setContentView(frameLayout);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLaunchPluginReceiver != null) {
            unregisterReceiver(mLaunchPluginReceiver);
        }
        if (mFinishSelfReceiver != null) {
            unregisterReceiver(mFinishSelfReceiver);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 限制 back 按键，不允许退出
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
    }

    /**
     * 接收 Service Connected 通知后启动插件。因为启动插件需要依赖 PluginPackageManagerService
     */
    private static class LaunchPluginReceiver extends BroadcastReceiver {

        private String mPluginPackageName;

        public LaunchPluginReceiver(String pkgName) {
            this.mPluginPackageName = pkgName;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Serializable serviceClass = intent.getSerializableExtra(IIntentConstant.EXTRA_SERVICE_CLASS);
            if (PluginPackageManagerService.class.equals(serviceClass)) {
                PluginManager.launchPlugin(context, mPluginPackageName);
            }
        }
    }

    /**
     * 恢复插件 Activity 时的准备阶段 UI，由宿主提供
     */
    public interface IRecoveryUiCreator {
        View createContentView(Context context);
    }
}
