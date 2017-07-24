package org.qiyi.pluginlibrary.component.processmgr;

import android.content.Context;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.plugin.TargetMapping;
import org.qiyi.pluginlibrary.runtime.PluginLoadedApk;
import org.qiyi.pluginlibrary.runtime.PluginManager;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

/**
 * 管理插件运行在哪个进程
 * Author:yuanzeyao
 * Date:2017/7/20 17:50
 * Email:yuanzeyao@qiyi.com
 */

public class ProcessManger {
    private static final String TAG = "ProcessManger";
    public static interface IProcessSelecter{
        public int getProcessIndex(String processName);
    }

    private static IProcessSelecter mOutterSelecter;

    /**
     * 外面设置的进程选择器
     * @param mSelecter
     */
    public static void setOutterSelecter(IProcessSelecter mSelecter){
        mOutterSelecter = mSelecter;
    }


    /**
     * 获取进程名称对应的index
     * @param processName
     *          插件运行的进程名称
     * @return
     *          插件的index
     */
    public static int getProcessIndex(String processName){
        if(TextUtils.isEmpty(processName)){
            return 1;
        }
        if(mOutterSelecter!= null){
            int index = mOutterSelecter.getProcessIndex(processName);
            return index;
        }
        return 1;
    }
}
