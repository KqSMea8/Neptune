package org.qiyi.pluginlibrary.utils;

/**
 * 每个插件特定的配置项，比如插件是否支持进程恢复
 */
public interface IPluginSpecificConfig {

    /**
     * 是否适配了进程恢复
     * @param packageName 插件包名
     * @return 是否支持
     */
    boolean enableRecovery(String packageName);
}
