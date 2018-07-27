package com.iqiyi.host.sample;

import android.app.Application;

import org.qiyi.pluginlibrary.HybirdPlugin;
import org.qiyi.pluginlibrary.HybirdPluginConfig;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

/**
 * author: liuchun
 * date: 2018/7/11
 */
public class HostApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initPluginFramework();
    }


    private void initPluginFramework() {
        final boolean useNewFeature = true;
        HybirdPluginConfig config = new HybirdPluginConfig.HybirdPluginConfigBuilder()
                .configSdkMode(1)
                .useNewCLMode(useNewFeature)
                .useNewResMode(useNewFeature)
                .useNewCompResolveMode(useNewFeature)
                .build();
        HybirdPlugin.init(this, config);

        PluginDebugLog.setIsDebug(true);
    }
}
