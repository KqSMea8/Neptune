package org.qiyi.pluginlibrary.utils;

import android.os.Build;

/**
 * Android 版本判断工具类
 * Author:yuanzeyao
 * Date:2017/11/8 15:38
 * Email:yuanzeyao@qiyi.com
 */

public class VersionUtils {
    /**
     * 判断是否是6.0+
     * @return
     */
    public static boolean hasMarshmallow(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * 判断是否是7.0+
     * @return
     */
    public static boolean hasNougat(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    /**
     * 判断是否是8.0+
     * @return
     */
    public static boolean hasOreo(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }
}
