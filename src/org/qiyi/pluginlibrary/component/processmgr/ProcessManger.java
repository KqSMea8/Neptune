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
    private static final String HOST_PROCESS_NAME = "com.qiyi.video";
    public static interface IProcessSelecter{
        public String selectProcess(String packageName);

        public int getProcessIndex(String processName);
    }

    private static IProcessSelecter mOutterSelecter;
    private static IProcessSelecter mDefaultSelecter = new DefaultProcessSelecter();

    /**
     * 外面设置的进程选择器
     * @param mSelecter
     */
    public static void setOutterSelecter(IProcessSelecter mSelecter){
        mOutterSelecter = mSelecter;
    }

    /**
     * 选择插件运行的进程名称,优先通过{@link #mDefaultSelecter}获取进程名称
     * 如果没有获取到并且{@link #mOutterSelecter}不为空，那么通过{@link #mOutterSelecter}
     * 获取
     * @param packageName
     *          需要查找运行进程名称的插件包名
     * @return
     *          找到返回插件运行进程的名称，没有找到返回Null
     */
    public static String selectProcess(String packageName){
        if(TextUtils.isEmpty(packageName)){
            return null;
        }
        String mProcessName = mDefaultSelecter.selectProcess(packageName);
        if(mProcessName == null && mOutterSelecter != null){
            mProcessName = mOutterSelecter.selectProcess(packageName);
        }
        return mProcessName;
    }

    /**
     * 获取进程名称对应的index
     * @param processName
     * @return
     */
    public static int getProcessIndex(String processName){
        if(TextUtils.isEmpty(processName)){
            return -1;
        }
        int index = mDefaultSelecter.getProcessIndex(processName);
        if(index== -1 && mOutterSelecter!= null){
            index = mOutterSelecter.getProcessIndex(processName);
        }
        return index;
    }


    /**
     * 默认的进程选择器，如果插件方有在Application组件中设置android:process 属性，那么默认选择器返回
     * android:process的值，否则返回Null
     */
    private static class DefaultProcessSelecter implements IProcessSelecter{

        @Override
        public String selectProcess(String packageName) {
            if(TextUtils.isEmpty(packageName)){
                return null;
            }
            PluginLoadedApk mLoadedApk = PluginManager.getPluginLoadedApkByPkgName(packageName);
            if(mLoadedApk != null){
                TargetMapping mapping = mLoadedApk.getPluginMapping();
                if(mapping != null){
                    String processName = mapping.getProcessName();
                    if(!TextUtils.isEmpty(processName)){
                        PluginDebugLog.log(TAG,"DefaultProcessSelecter getProcessName:"+processName);
                        return processName;
                    }
                }
            }
            return null;
        }

        @Override
        public int getProcessIndex(String processName) {
            if(processName != null){
                if(HOST_PROCESS_NAME.equals(processName)){
                    //主进程
                    return 0;
                }else if(processName.endsWith(":plugin1")){
                    //:plugin 进程
                    return 1;
                }else if(processName.endsWith(":plugin2")){
                    //:plugin2进程
                    return 2;
                }
            }
            return -1;
        }
    }
}
