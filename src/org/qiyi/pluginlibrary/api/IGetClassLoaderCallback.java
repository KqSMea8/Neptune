package org.qiyi.pluginlibrary.api;

/**
 * 获取megapp apk对应的classloader 回调接口。
 *
 * @since 2014年5月30日
 */
public interface IGetClassLoaderCallback {
    /**
     * 加载插件完成，获取classloader后的回调
     *
     * @param classLoader 插件的子classloader
     */
    void getClassLoaderCallback(ClassLoader classLoader);
}
