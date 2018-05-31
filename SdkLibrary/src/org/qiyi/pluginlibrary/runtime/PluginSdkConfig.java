package org.qiyi.pluginlibrary.runtime;

/**
 * 插件框架配置信息
 *
 * author: liuchun
 * date: 2018/5/31
 */
public class PluginSdkConfig {
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


    PluginSdkConfig(PluginConfigBuilder builder) {
        this.sdkMode = builder.sdkMode;
        this.useNewCLMode = builder.useNewCLMode;
        this.useNewResGen = builder.useNewResGen;
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


    public static class PluginConfigBuilder {
        int sdkMode = 0;
        boolean useNewCLMode = false;
        boolean useNewResGen = false;

        public PluginConfigBuilder configSdkMode(int sdkMode) {
            this.sdkMode = sdkMode;
            return this;
        }

        public PluginConfigBuilder useNewCLMode(boolean clMode) {
            this.useNewCLMode = clMode;
            return this;
        }

        public PluginConfigBuilder useNewResMode(boolean resGen) {
            this.useNewResGen = resGen;
            return this;
        }

        public PluginSdkConfig build() {
            return new PluginSdkConfig(this);
        }
    }
}
