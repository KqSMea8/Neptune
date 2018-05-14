package org.qiyi.pluginlibrary.utils;

import android.content.Intent;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.constant.IIntentConstant;
import org.qiyi.pluginlibrary.runtime.PluginLoadedApk;
import org.qiyi.pluginlibrary.runtime.PluginManager;

/**
 * 设置和解析代理组件的包名和组件名<br/>
 * 这里将这些信息使用Action保存而不是Intent的extra保存是因为
 * 如果插件在Intent中放入了自定义Bean,会出现ClassNotFoundException,
 * 除非插件的ClassLoader注入到基线的ClassLoader
 * <p>
 * Author:yuanzeyao
 * Date:2017/5/31 15:45
 * Email:yuanzeyao@qiyi.com
 */

public class IntentUtils {
    private static final String TAG = "IntentUtils";
    private static final String TOKEN = "@#@#";

    private IntentUtils() {

    }

    /**
     * 将插件的包名信息放入Intent#action字段
     *
     * @param mIntent
     * @param pkgName
     */
    public static void setProxyInfo(Intent mIntent, String pkgName) {
        String mPkgName = TextUtils.isEmpty(pkgName) ? "" : pkgName;
        String oriAction = mIntent.getAction();

        StringBuilder mBuilder = new StringBuilder(mPkgName);
        mBuilder.append(TOKEN)
                .append(oriAction);
        if (PluginDebugLog.isDebug()) {
            PluginDebugLog.log(TAG, "setProxyInfo mLast Action is:" + mBuilder.toString());
        }
        mIntent.setAction(mBuilder.toString());
    }

    /**
     * 从intent中解析出插件包名和跳转组件的名
     *
     * @param intent
     * @return
     */
    public static String[] parsePkgAndClsFromIntent(Intent intent) {
        String pkgName = getPluginPackage(intent);
        if (!TextUtils.isEmpty(pkgName)) {
            PluginLoadedApk loadedApk = PluginManager.getPluginLoadedApkByPkgName(pkgName);
            if (loadedApk != null) {
                //解决插件中跳转自定义Bean对象失败的问题
                intent.setExtrasClassLoader(loadedApk.getPluginClassLoader());
            }
        }
        String[] result = new String[2];
        result[0] = intent.getStringExtra(IIntentConstant.EXTRA_TARGET_PACKAGE_KEY);
        result[1] = intent.getStringExtra(IIntentConstant.EXTRA_TARGET_CLASS_KEY);
        PluginDebugLog.runtimeFormatLog(TAG, "pluginPkg:%s, pluginCls:%s", result[0], result[1]);
        return result;
    }

    /**
     * 从Action里获取插件包名
     *
     * @param intent
     * @return
     */
    public static String getPluginPackage(Intent intent) {
        String action = intent.getAction();
        String pkgName = "";
        if (!TextUtils.isEmpty(action) && action.contains(TOKEN)) {
            PluginDebugLog.log(TAG, "getPluginPackage action is " + action);
            String[] info = action.split(TOKEN);
            if (info != null && info.length == 2) {
                pkgName = info[0];
            }
        }

        return pkgName;
    }

    public static boolean isIntentForPlugin(Intent intent) {
        return intent.getBooleanExtra(IIntentConstant.EXTRA_TARGET_IS_PLUGIN_KEY, false);
    }

    public static String getTargetPackage(Intent intent) {
        return intent.getStringExtra(IIntentConstant.EXTRA_TARGET_PACKAGE_KEY);
    }

    public static String getTargetClass(Intent intent) {
        return intent.getStringExtra(IIntentConstant.EXTRA_TARGET_CLASS_KEY);
    }

    /**
     * 重置恢复Intent中的Action
     */
    public static void resetAction(Intent intent) {
        String action = intent.getAction();
        if (!TextUtils.isEmpty(action) && action.contains(TOKEN)) {
            String[] info = action.split(TOKEN);
            if (info != null && info.length == 2) {
                action = info[1];
            }
        }

        PluginDebugLog.log(TAG, "resetAction: " + action);
        if (TextUtils.isEmpty(action) || action.equalsIgnoreCase("null")) {
            action = null;
        }
        intent.setAction(action);
    }
}
