package org.qiyi.pluginlibrary.utils;

import java.io.File;

import android.os.Environment;
import android.util.Log;

public class PluginDebugLog {

	//插件统一调试TAG
	public static  final String TAG = "plugin";

	private static boolean isDebug = true;

	private static boolean isLogDebug = true;
	public static void setIsDebug(boolean b) {
		isDebug = b;
		isDebug = true;
	}
	
	public static void checkIsOpenDebug() {
		Log.d(TAG, "checkIsOpenDebug > isDebug = " + isDebug);
		
		if (!isDebug) {
			if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				File path = Environment.getExternalStorageDirectory();
				if(null != path) {
					String logFileName = path.getPath() + "/plug.log";
					
					File debugFile 	= new File(logFileName);
					isLogDebug		=  debugFile.exists();
				}
				Log.d(TAG, "log file exist  = " + isDebug);
			}
		}
	}

	public static boolean isDebug() {
		return isDebug;
	}

	public static void log(String LOG_CLASS_NAME, Object msg) {
		if ((null != LOG_CLASS_NAME && !LOG_CLASS_NAME.equals("")) && null != msg) {
			if (isDebug||isLogDebug) {
				Log.i(LOG_CLASS_NAME,
						"[INFO " + LOG_CLASS_NAME + "] " + String.valueOf(msg));
			}
		}
	}

	public static void log(String TAG, String LOG_CLASS_NAME, Object msg) {
		if ((null != LOG_CLASS_NAME && !LOG_CLASS_NAME.equals("")) && null != msg) {
			if (isDebug || isLogDebug) {
				Log.i(TAG,
						"[INFO " + LOG_CLASS_NAME + "] " + String.valueOf(msg));
			}
		}
	}

}
