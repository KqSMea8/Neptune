package org.qiyi.pluginlibrary.component.processmgr;


/**
 * 管理插件运行在哪个进程
 * Author:yuanzeyao
 * Date:2017/7/20 17:50
 * Email:yuanzeyao@qiyi.com
 */

public class ProcessManager {
    private static final String TAG = "ProcessManager";

    public static interface IProcessSelector {
        public int getProcessIndex(String processName);
    }

    private static IProcessSelector mOutterSelecter;

    /**
     * 外面设置的进程选择器
     *
     * @param mSelecter
     */
    public static void setOutterSelecter(IProcessSelector mSelecter) {
        mOutterSelecter = mSelecter;
    }


    /**
     * 获取进程名称对应的index
     *
     * @param processName 插件运行的进程名称
     * @return 插件的index
     */
    public static int getProcessIndex(String processName) {

        if (mOutterSelecter != null) {
            return mOutterSelecter.getProcessIndex(processName);
        }
        return 0;
    }
}
