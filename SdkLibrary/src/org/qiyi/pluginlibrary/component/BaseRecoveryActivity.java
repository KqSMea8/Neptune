package org.qiyi.pluginlibrary.component;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;

import org.qiyi.pluginlibrary.HybirdPlugin;
import org.qiyi.pluginlibrary.constant.IIntentConstant;
import org.qiyi.pluginlibrary.pm.PluginPackageManagerNative;
import org.qiyi.pluginlibrary.pm.PluginPackageManagerService;
import org.qiyi.pluginlibrary.runtime.PluginManager;
import org.qiyi.pluginlibrary.utils.IPluginSpecificConfig;
import org.qiyi.pluginlibrary.utils.IRecoveryUiCreator;
import org.qiyi.pluginlibrary.utils.IntentUtils;
import org.qiyi.pluginlibrary.utils.Util;

import java.io.Serializable;

/**
 * 进程因资源不足被回收以后，恢复时插件信息会丢失，这个页面作为临时页面处理插件恢复问题。
 */
public abstract class BaseRecoveryActivity extends Activity {

    /**
     * 启动插件 Receiver 的优先级
     * <p>
     * 在恢复 Activity 堆栈时，如果栈顶 Activity 是透明主题，会连续恢复多个 Activity 直到非透明主题 Activity，
     * 这个优先级递增，保证后恢复的 Activity 可以先打开被压到栈底。
     * <p>
     * 只有进程恢复时才需要，进程恢复时会自动重置。
     */
    private static int sLaunchPluginReceiverPriority;
    private BroadcastReceiver mFinishSelfReceiver;
    private BroadcastReceiver mLaunchPluginReceiver;
    private String mPluginPackageName;
    private String mPluginClassName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] packageAndClass = IntentUtils.parsePkgAndClsFromIntent(getIntent());
        mPluginPackageName = packageAndClass[0];
        mPluginClassName = packageAndClass[1];

        IPluginSpecificConfig pluginSpecificConfig = HybirdPlugin.getConfig().getPluginSpecificConfig();
        boolean enableRecovery = pluginSpecificConfig != null && pluginSpecificConfig.enableRecovery(mPluginPackageName);

        if (!enableRecovery || mPluginPackageName == null) {
            finish();
            return;
        }
        initUi();

        if (PluginPackageManagerNative.getInstance(this).isConnected()) {
            // 一般而言，这时候 Service 都是未 connect 的状态，这里只是严谨考虑
            PluginManager.launchPlugin(this, createLaunchPluginIntent(), Util.getCurrentProcessName(this));
            finish();
            return;
        }

        mLaunchPluginReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                Serializable serviceClass = intent.getSerializableExtra(IIntentConstant.EXTRA_SERVICE_CLASS);
                if (PluginPackageManagerService.class.equals(serviceClass)) {
                    PluginManager.launchPlugin(context, createLaunchPluginIntent(), Util.getCurrentProcessName(context));
                }
            }
        };
        IntentFilter serviceConnectedFilter = new IntentFilter(IIntentConstant.ACTION_SERVICE_CONNECTED);
        serviceConnectedFilter.setPriority(sLaunchPluginReceiverPriority++);
        registerReceiver(mLaunchPluginReceiver, serviceConnectedFilter);

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

    private Intent createLaunchPluginIntent() {
        Intent intent = new Intent(getIntent());
        intent.setComponent(new ComponentName(mPluginPackageName, mPluginClassName));
        return intent;
    }

    private void initUi() {
        IRecoveryUiCreator recoveryUiCreator = HybirdPlugin.getConfig().getRecoveryUiCreator();
        if (recoveryUiCreator == null) {
            recoveryUiCreator = new IRecoveryUiCreator.DefaultRecoveryUiCreator();
        }
        setContentView(recoveryUiCreator.createContentView(this));
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
}
