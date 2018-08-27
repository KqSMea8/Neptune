package com.iqiyi.host.sample;

import android.app.Application;

import org.qiyi.pluginlibrary.Neptune;
import org.qiyi.pluginlibrary.NeptuneConfig;
<<<<<<< HEAD
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
=======
>>>>>>> sdk_open

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
        NeptuneConfig config = new NeptuneConfig.NeptuneConfigBuilder()
                .configSdkMode(NeptuneConfig.INSTRUMENTATION_MODE)
<<<<<<< HEAD
                .build();
        Neptune.init(this, config);

        PluginDebugLog.setIsDebug(true);
=======
                .enableDebug(true)
                .build();
        Neptune.init(this, config);
>>>>>>> sdk_open
    }
}
