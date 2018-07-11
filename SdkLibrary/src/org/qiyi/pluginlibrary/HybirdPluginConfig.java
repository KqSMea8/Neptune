package org.qiyi.pluginlibrary;

import org.qiyi.pluginlibrary.component.BaseRecoveryActivity;
import org.qiyi.pluginlibrary.pm.IVerifyPluginInfo;
import org.qiyi.pluginlibrary.utils.IPluginSpecificConfig;

/**
 * 插件框架运行配置信息
 *
 * author: liuchun
 * date: 2018/6/4
 */
public final class HybirdPluginConfig {

    /**
     * 插件框架运行模式
     * 0：InstrActivityProxy模式
     * 1：hook Instrumentation方案
     * 2: hook Instrumentation + Base PluginActivity方案
     * 3: 其他, 待定
     */
    private int sdkMode;

    // 是否使用新的ClassLoader模型
    private boolean useNewCLMode;

    // 是否使用新的资源方式
    private boolean useNewResGen;

    // 是否使用新的组件解析方式
    private boolean useNewCompResolve;

    // 是否支持MultiDex安装
    private boolean supportMultiDex;

    private IVerifyPluginInfo mVerifyPluginInfo;
    private BaseRecoveryActivity.IRecoveryUiCreator mRecoveryUiCreator;
    private IPluginSpecificConfig mPluginSpecificConfig;

    HybirdPluginConfig(HybirdPluginConfigBuilder builder) {
        this.sdkMode = builder.sdkMode;
        this.useNewCLMode = builder.useNewCLMode;
        this.useNewResGen = builder.useNewResGen;
        this.useNewCompResolve = builder.useNewCompResolve;
        this.supportMultiDex = builder.supportMultiDex;
        this.mVerifyPluginInfo = builder.verifyPluginInfo;
        this.mRecoveryUiCreator = builder.recoveryUiCreator;
        this.mPluginSpecificConfig = builder.pluginSpecificConfig;
    }


    public int getSdkMode() {
        return sdkMode;
    }

    public boolean shouldUseNewCLMode() {
        return useNewCLMode;
    }

    public boolean shouldUseNewResGen() {
        return useNewResGen;
    }

    public boolean shouldUseNewResolveMethod() {
        return useNewCompResolve;
    }

    public boolean shouldSupportMultidex() {
        return supportMultiDex;
    }

    public IVerifyPluginInfo getVerifyPluginInfo() {
        return mVerifyPluginInfo;
    }

    public BaseRecoveryActivity.IRecoveryUiCreator getRecoveryUiCreator() {
        return mRecoveryUiCreator;
    }

    public IPluginSpecificConfig getPluginSpecificConfig() {
        return mPluginSpecificConfig;
    }

    public static class HybirdPluginConfigBuilder {
        int sdkMode = 0;
        boolean useNewCLMode = false;
        boolean useNewResGen = false;
        boolean useNewCompResolve = false;
        boolean supportMultiDex = false;
        IVerifyPluginInfo verifyPluginInfo;
        BaseRecoveryActivity.IRecoveryUiCreator recoveryUiCreator;
        IPluginSpecificConfig pluginSpecificConfig;

        public HybirdPluginConfigBuilder configSdkMode(int sdkMode) {
            this.sdkMode = sdkMode;
            return this;
        }

        public HybirdPluginConfigBuilder useNewCLMode(boolean clMode) {
            this.useNewCLMode = clMode;
            return this;
        }

        public HybirdPluginConfigBuilder useNewResMode(boolean resGen) {
            this.useNewResGen = resGen;
            return this;
        }

        public HybirdPluginConfigBuilder useNewCompResolveMode(boolean resolve) {
            this.useNewCompResolve = resolve;
            return this;
        }

        public HybirdPluginConfigBuilder supportMultiDex(boolean multiDex) {
            this.supportMultiDex = multiDex;
            return this;
        }

        public HybirdPluginConfigBuilder setVerifyPluginInfo(IVerifyPluginInfo verifyPluginInfo) {
            this.verifyPluginInfo = verifyPluginInfo;
            return this;
        }

        public HybirdPluginConfigBuilder recoveryUiCreator(BaseRecoveryActivity.IRecoveryUiCreator creator) {
            this.recoveryUiCreator = creator;
            return this;
        }

        public HybirdPluginConfigBuilder pluginSpecificConfig(IPluginSpecificConfig pluginSpecificConfig) {
            this.pluginSpecificConfig = pluginSpecificConfig;
            return this;
        }

        public HybirdPluginConfig build() {
            return new HybirdPluginConfig(this);
        }
    }

}
