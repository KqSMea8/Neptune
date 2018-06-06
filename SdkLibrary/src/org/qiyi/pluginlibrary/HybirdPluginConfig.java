package org.qiyi.pluginlibrary;

import org.qiyi.pluginlibrary.pm.IVerifyPluginInfo;

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
     * 2: 其他方案，待定
     */
    private int sdkMode;

    // 是否使用新的ClassLoader模型
    private boolean useNewCLMode;

    // 是否使用新的资源方式
    private boolean useNewResGen;

    private IVerifyPluginInfo mVerifyPluginInfo;


    HybirdPluginConfig(HybirdPluginConfigBuilder builder) {
        this.sdkMode = builder.sdkMode;
        this.useNewCLMode = builder.useNewCLMode;
        this.useNewResGen = builder.useNewResGen;
        this.mVerifyPluginInfo = builder.verifyPluginInfo;
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

    public IVerifyPluginInfo getVerifyPluginInfo() {
        return mVerifyPluginInfo;
    }



    public static class HybirdPluginConfigBuilder {
        int sdkMode = 0;
        boolean useNewCLMode = false;
        boolean useNewResGen = false;
        IVerifyPluginInfo verifyPluginInfo;

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

        public HybirdPluginConfigBuilder setVerifyPluginInfo(IVerifyPluginInfo verifyPluginInfo) {
            this.verifyPluginInfo = verifyPluginInfo;
            return this;
        }

        public HybirdPluginConfig build() {
            return new HybirdPluginConfig(this);
        }
    }

}
