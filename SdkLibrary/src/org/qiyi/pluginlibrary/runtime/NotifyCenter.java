package org.qiyi.pluginlibrary.runtime;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.constant.IIntentConstant;

/**
 * 发送广播通知业务层做一些特殊处理
 * 比如插件加载成功
 *
 * author: liuchun
 * date: 2018/6/7
 */
public class NotifyCenter {


    /**
     * 通知插件加载完毕（解决快捷方式添加闪屏时，插件还没启动，闪屏就关闭了）
     *
     * @param context
     */
    public static void notifyPluginActivityLoaded(Context context) {
        Intent intent = new Intent();
        intent.setAction(IIntentConstant.ACTION_PLUGIN_LOADED);
        context.sendBroadcast(intent);
    }


    /**
     * 通知调用方可以取消Loading对话框
     * @param context
     * @param  intent
     */
    public static void notifyPluginStarted(Context context, Intent intent) {
        if (null != context && null != intent && !TextUtils.isEmpty(intent.getStringExtra(
                IIntentConstant.EXTRA_SHOW_LOADING))) {
            context.sendBroadcast(new Intent(IIntentConstant.EXTRA_SHOW_LOADING));
        }
    }

    /**
     * 通知插件启动错误
     *
     * @param context Context
     */
    public static void notifyStartPluginError(Context context) {
        Intent intent = new Intent();
        intent.setAction(IIntentConstant.ACTION_START_PLUGIN_ERROR);
        context.sendBroadcast(intent);
    }

    /**
     * 通知 Service 绑定成功
     * @param context context
     * @param service service class
     */
    public static void notifyServiceConnected(Context context, Class<? extends Service> service) {
        Intent intent = new Intent(IIntentConstant.ACTION_SERVICE_CONNECTED);
        intent.putExtra(IIntentConstant.EXTRA_SERVICE_CLASS, service);
        context.sendBroadcast(intent);
    }
}
