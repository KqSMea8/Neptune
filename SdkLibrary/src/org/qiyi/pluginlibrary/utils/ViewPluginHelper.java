package org.qiyi.pluginlibrary.utils;

import android.view.View;
import android.view.ViewGroup;

/**
 * View 插件化辅助类，主要用来关闭 view 的 onSaveInstanceState 防止进程恢复时的 ClassNotFound
 */
public class ViewPluginHelper {
    /**
     * 取消 View 以及其 children View 的 onSaveInstanceState 调用
     * <p>
     * 耗时大约 1ms
     *
     * @param view 顶层 view
     */
    public static void disableViewSaveInstanceRecursively(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0, n = viewGroup.getChildCount(); i < n; i++) {
                disableViewSaveInstanceRecursively(viewGroup.getChildAt(i));
            }
        } else if (view != null) {
            view.setSaveEnabled(false);
        }
    }
}
