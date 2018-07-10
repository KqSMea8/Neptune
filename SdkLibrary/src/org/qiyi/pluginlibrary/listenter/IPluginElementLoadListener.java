package org.qiyi.pluginlibrary.listenter;

/**
 * 加载插件中的元素回调
 *
 * @param <T>
 */
public interface IPluginElementLoadListener<T> {

    void onSuccess(T element, String packageName);

    void onFail(String packageName);
}
