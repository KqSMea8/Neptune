package org.qiyi.pluginlibrary.utils;

import android.util.Log;

/**
 * Debug configuration for plugin model you can enable debug by following method<br>
 * 1. Change code sIsDebug = false to true<br>
 * 2. Invoke setIsDebug(boolean enableDebug)<br>
 * 3. Change TAG's properties during the runtime by
 * "adb shell setprop log.tag.plugin VERBOSE" on the terminal
 */
public class PluginDebugLog {

	//插件统一调试TAG
	public static  final String TAG = "plugin";

	private static boolean sIsDebug = false;

	public static void setIsDebug(boolean b) {
		sIsDebug = b;
	}
	
    /**
     * Check the debug configuration and check TAG with android.util.Log.isLoggable
     * 
     * @return
     */
    public static boolean isDebug() {
        return sIsDebug || android.util.Log.isLoggable(TAG, android.util.Log.VERBOSE);
    }

	public static void log(String LOG_CLASS_NAME, Object msg) {
		if ((null != LOG_CLASS_NAME && !LOG_CLASS_NAME.equals("")) && null != msg) {
			if (isDebug()) {
				Log.i(LOG_CLASS_NAME,
						"[INFO " + LOG_CLASS_NAME + "] " + String.valueOf(msg));
			}
		}
	}

	public static void log(String TAG, String LOG_CLASS_NAME, Object msg) {
		if ((null != LOG_CLASS_NAME && !LOG_CLASS_NAME.equals("")) && null != msg) {
			if (isDebug()) {
				Log.i(TAG,
						"[INFO " + LOG_CLASS_NAME + "] " + String.valueOf(msg));
			}
		}
	}

}
