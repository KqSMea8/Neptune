package org.qiyi.pluginlibrary.listenter;

import android.content.Context;

/**
 * Created by xiepengchong on 2015/8/5.
 */
public class IResourchStaticsticsControllerManager {

    private static IResourchStaticsticsControllerListener listener;

    public static void setControllerListener(IResourchStaticsticsControllerListener l) {
        listener = l;
    }

    public static void onResume(Context context) {
        if (null != listener) {
            listener.onResume(context);
        }
    }

    public static void onPause(Context context) {
        if (null != listener) {
            listener.onPause(context);
        }
    }

    public interface IResourchStaticsticsControllerListener {
        public void onResume(Context context);

        public void onPause(Context context);
    }

}
