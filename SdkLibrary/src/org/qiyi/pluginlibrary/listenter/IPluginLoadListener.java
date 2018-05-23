package org.qiyi.pluginlibrary.listenter;

/**
 * 加载插件实例到内存，PluginLoadedApk监听回调
 */
public interface IPluginLoadListener {

    /**
     * 加载成功的回调，主线程回调
     *
     * @param packageName 加载成功的插件包名
     */
    void onLoadSuccess(String packageName);

    /**
     * 加载失败的回调，主线程回调
     *
     * @param packageName 加载失败的插件包名
     */
    void onLoadFailed(String packageName);
}