package org.qiyi.pluginlibrary.utils;

import android.text.TextUtils;
import android.util.Log;

/**
 * Debug configuration for plugin model you can enable debug by following
 * method<br> 1. Change code sIsDebug = false to true<br> 2. Invoke
 * setIsDebug(boolean enableDebug)<br> 3. Change TAG's properties during the
 * runtime by "adb shell setprop log.tag.plugin VERBOSE" on the terminal
 */
public class PluginDebugLog {

    // 插件统一调试TAG
    public static final String TAG = "plugin";

    /** 插件中心下载Log TAG*/
    private static final String DOWNLOAD_TAG = "download_plugin";
    /** 插件中心安装Log TAG*/
    private static final String INSTALL_TAG = "install_plugin";
    /** 插件中心运行时Log TAG*/
    private static final String RUNTIME_TAG = "runtime_plugin";

    private static final String GENERAL_TAG = "general_plugin";

    private static boolean sIsDebug = false;

    public static void setIsDebug(boolean b) {
        sIsDebug = b;
    }

    /**
     * Check the debug configuration and check TAG with
     * android.util.Log.isLoggable
     *
     * @return
     */
    public static boolean isDebug() {
        return sIsDebug || android.util.Log.isLoggable(TAG, android.util.Log.VERBOSE);
    }

    private static void logInternal(String tag, Object msg) {
        if (isDebug()) {
            if (!TextUtils.isEmpty(tag) && null != msg) {
                Log.i(tag, String.valueOf(msg));
            }
        }
    }

    public static void log(String tag, String identify, Object msg) {
        if (isDebug()) {
            if (!TextUtils.isEmpty(tag) && null != msg) {
                Log.i(tag, "[INFO " + identify + "] " + String.valueOf(msg));
            }
        }
    }

    /**
     * 插件中心下载过程log
     *
     * @param tag subtag
     * @param msg log信息
     */
    public static void downloadLog(String tag, Object msg) {
        logInternal(DOWNLOAD_TAG, "[ " + tag + " ] : " + msg);
    }

    /**
     * 插件中心插件安装log
     *
     * @param tag subtag
     * @param msg log信息
     */

    public static void installLog(String tag, Object msg) {
        logInternal(INSTALL_TAG, "[ " + tag + " ] : " + msg);
    }

    /**
     *  插件中心运行时Log
     *
     * @param tag subtag
     * @param msg log信息
     */
    public static void runtimeLog(String tag, Object msg) {
        logInternal(RUNTIME_TAG, "[ " + tag + " ] : " + msg);
    }

    /**
     *  插件中心Log
     *
     * @param tag subtag
     * @param msg log信息
     */
    public static void log(String tag, Object msg) {
        logInternal(GENERAL_TAG, "[ " + tag + " ] : " + msg);
    }

}
