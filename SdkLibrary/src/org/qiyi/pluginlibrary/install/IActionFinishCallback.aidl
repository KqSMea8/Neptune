package org.qiyi.pluginlibrary.install;
import org.qiyi.pluginlibrary.pm.PluginLiteInfo;
interface IActionFinishCallback {
    /**
     * Action执行完成回调
     *
     * @param info
     *            插件基础信息
     * @param resultCode
     *            结果码
     */
    void onActionComplete(in PluginLiteInfo info, int resultCode);
    /**
     * client端对应的进程名
     */
    String getProcessName();
}
