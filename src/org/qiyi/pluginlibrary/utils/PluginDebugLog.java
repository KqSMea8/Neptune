package org.qiyi.pluginlibrary.utils;

import android.text.TextUtils;
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
	 * Check the debug configuration and check TAG with
	 * android.util.Log.isLoggable
	 * 
	 * @return
	 */
	public static boolean isDebug() {
		return sIsDebug || android.util.Log.isLoggable(TAG, android.util.Log.VERBOSE);
	}

	public static void log(String tag, Object msg) {
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
}
