/*
 *
 * Copyright 2018 iQIYI.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.qiyi.pluginlibrary.utils;

import android.text.TextUtils;
import android.util.Log;

import org.qiyi.pluginlibrary.Neptune;
import org.qiyi.pluginlibrary.debug.PluginDebugCacheProxy;

import java.util.Locale;

/**
 * Debug configuration for plugin model you can enable debug by following
 * method<br>
 * 1. Change code sIsDebug = false to true<br>
 * 2. Invoke setIsDebug(boolean enableDebug)<br>
 * 3. Change TAG's properties during the runtime by "adb shell setprop log.tag.plugin VERBOSE" on the terminal
 */
public class PluginDebugLog {

    // 插件统一调试TAG
    public static final String TAG = "plugin";

    /* 插件SDK下载Log TAG */
    public static final String DOWNLOAD_TAG = "download_plugin";
    /* 插件SDK安装Log TAG */
    public static final String INSTALL_TAG = "install_plugin";
    /* 插件SDK运行时Log TAG */
    public static final String RUNTIME_TAG = "runtime_plugin";
    /* 插件SDK通用Log TAG */
    public static final String GENERAL_TAG = "general_plugin";
    private static boolean sIsDebug = false;

    public static void setIsDebug(boolean b) {
        sIsDebug = b;
    }

    /**
     * Check the debug configuration and check TAG with
     * android.util.Log.isLoggable
     */
    public static boolean isDebug() {
        return sIsDebug || android.util.Log.isLoggable(TAG, android.util.Log.VERBOSE);
    }

    /**
     * 插件SDK下载过程log
     */
    public static void downloadLog(String tag, Object msg) {
        if (isDebug()) {
            logInternal(DOWNLOAD_TAG, "[ " + tag + " ] : " + msg);
        }
    }

    /**
     * 插件SDK下载过程log,格式化输出log，主要避免大量使用+连接String的情况
     */
    public static void downloadFormatLog(String tag, String format, Object... args) {
        if (isDebug()) {
            try {
                String msg = (args == null) ? format : String.format(Locale.US, format, args);
                downloadLog(tag, msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 插件SDK插件安装log
     */
    public static void installLog(String tag, Object msg) {
        if (isDebug()) {
            logInternal(INSTALL_TAG, "[ " + tag + " ] : " + msg);
        } else {
            // 安装日志尝试持久化
            PluginDebugCacheProxy.getInstance().savePluginLogBuffer(Neptune.getHostContext(),
                    INSTALL_TAG, "[ " + tag + " ] : " + msg);
        }
    }

    /**
     * 插件SDK安装log,格式化输出log，主要避免大量使用+连接String的情况
     */
    public static void installFormatLog(String tag, String format, Object... args) {
        try {
            String msg = (args == null) ? format : String.format(Locale.US, format, args);
            installLog(tag, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 插件SDK运行时Log
     */
    public static void runtimeLog(String tag, Object msg) {
        if (isDebug()) {
            logInternal(RUNTIME_TAG, "[ " + tag + " ] : " + msg);
        }
    }

    /**
     * 插件SDK运行时Log,格式化输出log，主要避免大量使用+连接String的情况
     */
    public static void runtimeFormatLog(String tag, String format, Object... args) {
        if (isDebug()) {
            try {
                String msg = (args == null) ? format : String.format(Locale.US, format, args);
                runtimeLog(tag, msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 插件SDKLog
     */
    public static void log(String tag, Object msg) {
        if (isDebug()) {
            logInternal(GENERAL_TAG, "[ " + tag + " ] : " + msg);
        }
    }

    /**
     * 格式化输出log，主要避免大量使用+连接String的情况
     */
    public static void formatLog(String tag, String format, Object... args) {
        if (isDebug()) {
            try {
                String msg = (args == null) ? format : String.format(Locale.US, format, args);
                log(tag, msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void logInternal(String tag, Object msg) {
        logInternal(tag, msg, Log.INFO);
    }

    private static void logInternal(String tag, Object msg, int logLevel) {
        if (!TextUtils.isEmpty(tag) && null != msg) {
            String content = String.valueOf(msg);
            switch (logLevel) {
                case Log.ERROR:
                    Log.e(tag, content);
                    break;
                case Log.WARN:
                    Log.w(tag, content);
                    break;
                case Log.INFO:
                    Log.i(tag, content);
                    break;
                case Log.DEBUG:
                    Log.d(tag, content);
                    break;
                default:
                    Log.v(tag, content);
                    break;
            }
            // 提交给缓存系统决定是否需要持久化
            if (logLevel >= Log.INFO) {
                PluginDebugCacheProxy.getInstance().savePluginLogBuffer(Neptune.getHostContext(), tag, content);
            }
        }
    }
}
