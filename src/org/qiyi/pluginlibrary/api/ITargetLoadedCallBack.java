package org.qiyi.pluginlibrary.api;

/**
 *
 * 插件加载回调，用于后台加载插件
 *
 * @author chenyangkun
 * @since 2014年7月14日
 */
public interface ITargetLoadedCallBack {

    /**
     * 插件加载完成
     *
     * @param packageName 插件包名
     */
    void onTargetLoaded(String packageName);

}
