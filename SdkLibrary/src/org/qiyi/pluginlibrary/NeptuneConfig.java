package org.qiyi.pluginlibrary;

import org.qiyi.pluginlibrary.pm.IPluginInfoProvider;
import org.qiyi.pluginlibrary.utils.IRecoveryCallback;

/**
 * 插件框架运行配置信息
 *
 * author: liuchun
 * date: 2018/6/4
 */
public final class NeptuneConfig {
    /**
     * 传统的插件框架模式，使用InstrActivityProxy进行代理转发
     */
    public static final int LEGACY_MODE = 0;
    /**
     * Hook Instrumentation方案
     */
    public static final int INSTRUMENTATION_MODE = 1;
    /**
     * Hook Instrumentation方案 + Base PluginActivity方案
     */
    public static final int INSTRUMENTATION_BASEACT_MODE = 2;
    /** 插件框架运行模式，已经全面切换到Hook Instrumentation方案，适配Android P */
    private int sdkMode;
    // 新特性，减少反射API调用
    private boolean useSeparateClassLoader;
    private boolean useNewResCreator;
    private boolean useNewCompParser;
    private boolean supportMultidex;

    private IPluginInfoProvider mPluginInfoProvider;
    private IRecoveryCallback mRecoveryCallback;

    NeptuneConfig(NeptuneConfigBuilder builder) {
        this.sdkMode = builder.sdkMode;
        this.useSeparateClassLoader = builder.useSeparateClassLoader;
        this.useNewResCreator = builder.useNewResCreator;
        this.useNewCompParser = builder.useNewCompParser;
        this.supportMultidex = builder.supportMultidex;
        this.mPluginInfoProvider = builder.pluginInfoProvider;
        this.mRecoveryCallback = builder.recoveryCallback;
    }


    public int getSdkMode() {
        return sdkMode;
    }


    public boolean withSeparteeClassLoader() {
        return useSeparateClassLoader;
    }

    public boolean withNewResCreator() {
        return useNewResCreator;
    }

    public boolean withNewCompParser() {
        return useNewCompParser;
    }

    public boolean supportMultidex() {
        return supportMultidex;
    }

    public IPluginInfoProvider getPluginInfoProvider() {
        return mPluginInfoProvider;
    }

    public IRecoveryCallback getRecoveryCallback() {
        return mRecoveryCallback;
    }

    public static class NeptuneConfigBuilder {
        int sdkMode = 0;
        boolean useSeparateClassLoader;
        boolean useNewResCreator;
        boolean useNewCompParser;
        boolean supportMultidex;
        IPluginInfoProvider pluginInfoProvider;
        IRecoveryCallback recoveryCallback;

        public NeptuneConfigBuilder configSdkMode(int sdkMode) {
            this.sdkMode = sdkMode;
            return this;
        }

        public NeptuneConfigBuilder withSeparteeClassLoader(boolean sepCl) {
            this.useSeparateClassLoader = sepCl;
            return this;
        }

        public NeptuneConfigBuilder withNewResCreator(boolean newResCreator) {
            this.useNewResCreator = newResCreator;
            return this;
        }

        public NeptuneConfigBuilder withNewCompParser(boolean newCompParser) {
            this.useNewCompParser = newCompParser;
            return this;
        }

        public NeptuneConfigBuilder supportMultidex(boolean multidexEnable) {
            this.supportMultidex = multidexEnable;
            return this;
        }

        public NeptuneConfigBuilder pluginInfoProvider(IPluginInfoProvider pluginInfoProvider) {
            this.pluginInfoProvider = pluginInfoProvider;
            return this;
        }

        public NeptuneConfigBuilder recoveryCallback(IRecoveryCallback callback) {
            this.recoveryCallback = callback;
            return this;
        }

        public NeptuneConfig build() {
            return new NeptuneConfig(this);
        }
    }

}
