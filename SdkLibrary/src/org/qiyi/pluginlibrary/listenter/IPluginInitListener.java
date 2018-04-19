package org.qiyi.pluginlibrary.listenter;


public interface IPluginInitListener {

    /**
     * 加载成功的回调，主线程回调
     *
     * @param packageName 加载成功的插件包名
     */
    void onInitFinished(String packageName);
}