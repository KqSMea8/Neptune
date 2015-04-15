package org.qiyi.pluginlibrary.api;

import android.view.View;

/**
 * View异步创建成功的回调
 * 
 * @author chenyangkun
 * @since 2014年7月1日
 */
public interface ICreateViewCallBack {
    /**
     * 创建View后的回调
     * 
     * @param packageName
     *            插件包名
     * @param view
     *            View或者viewGroup的子类（Context为插件的MAApplication）
     */
    void onViewCreated(String packageName, View view);
}
