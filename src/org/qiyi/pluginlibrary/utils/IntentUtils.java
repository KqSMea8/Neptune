package org.qiyi.pluginlibrary.utils;

import android.content.Intent;
import android.text.TextUtils;

/**
 * 设置和解析代理组件的包名和组件名<br/>
 * 这里将这些信息使用Action保存而不是Intent的extra保存是因为
 * 如果插件在Intent中放入了自定义Bean,会出现ClassNotFoundExcepion,
 * 除非插件的ClassLoader注入到基线的ClassLoader
 * Author:yuanzeyao
 * Date:2017/5/31 15:45
 * Email:yuanzeyao@qiyi.com
 */

public class IntentUtils {
    private static final String TAG = "IntentUtils";
    private static final String TOKEN ="@#@#";
    private IntentUtils(){

    }

    public static void setProxyInfo(Intent mIntent,String pkgName,String clsName){
        if(mIntent == null){
            PluginDebugLog.log(TAG,"setProxyInfo mIntent is null!");
            return;
        }
        String mPkgName = TextUtils.isEmpty(pkgName)?"":pkgName;
        String mClsName = TextUtils.isEmpty(clsName)?"":clsName;
        String oriAction = mIntent.getAction();
        StringBuilder mBuilder = new StringBuilder(mPkgName);
        mBuilder.append(TOKEN).append(mClsName).append(TOKEN)
                .append(oriAction);
        if(PluginDebugLog.isDebug()){
            PluginDebugLog.log(TAG,"setProxyInfo mLast Action is:"+mBuilder.toString());
        }
        mIntent.setAction(mBuilder.toString());
    }

    /**
     * 获取插件包名
     * @param mIntent
     * @return
     */
    public static String getPluginPackage(Intent mIntent){
        if(mIntent == null){
            PluginDebugLog.log(TAG,"getPluginPackage:mIntent is null!");
            return "";
        }
        String actionValue = mIntent.getAction();
        if(TextUtils.isEmpty(actionValue) || !actionValue.contains(TOKEN)){
            PluginDebugLog.log(TAG,"getPluginPackage:action not contain token!");
            return "";
        }

        String[] mInfo = actionValue.split(TOKEN);
        if(mInfo.length != 3){
            PluginDebugLog.log(TAG,"getPluginPackage:mInfo lenght is not 3!");
            return "";
        }
        PluginDebugLog.log(TAG,"getPluginPackage:"+mInfo[0]);
        return mInfo[0];

    }

    /**
     * 获取需要代理的组件名
     * @param mIntent
     * @return
     */
    public static String getClsName(Intent mIntent){
        if(mIntent == null){
            PluginDebugLog.log(TAG,"getClsName:mIntent is null!");
            return "";
        }
        String actionValue = mIntent.getAction();
        if(TextUtils.isEmpty(actionValue) || !actionValue.contains(TOKEN)){
            PluginDebugLog.log(TAG,"getClsName:action not contain token!");
            return "";
        }

        String[] mInfo = actionValue.split(TOKEN);
        if(mInfo.length != 3){
            PluginDebugLog.log(TAG,"getClsName:mInfo lenght is not 3!");
            return "";
        }
        PluginDebugLog.log(TAG,"getClsName:"+mInfo[1]);
        return mInfo[1];
    }

    /**
     * 获取Action
     * @param mIntent
     * @return
     */
    public static String getAction(Intent mIntent){
        if(mIntent == null){
            PluginDebugLog.log(TAG,"getAction:mIntent is null!");
            return "";
        }
        String actionValue = mIntent.getAction();
        if(TextUtils.isEmpty(actionValue) || !actionValue.contains(TOKEN)){
            PluginDebugLog.log(TAG,"getAction:action not contain token!");
            return actionValue;
        }

        String[] mInfo = actionValue.split(TOKEN);
        if(mInfo.length != 3){
            PluginDebugLog.log(TAG,"getAction:mInfo lenght is not 3!");
            return actionValue;
        }
        PluginDebugLog.log(TAG,"getAction:"+mInfo[2]);
        return mInfo[2];
    }


}
