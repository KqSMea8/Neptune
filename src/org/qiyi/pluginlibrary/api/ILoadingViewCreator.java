package org.qiyi.pluginlibrary.api;

import android.content.Context;
import android.view.View;

/**
 * 
 * 创建插件加载View的接口
 * 
 * @author chenyangkun
 * @since 2014-5-20
 */
public interface ILoadingViewCreator {
    
    /**
     * 创建插件加载界面
     * 
     * @param context
     * @return View
     */
    View createLoadingView(Context context);

}
