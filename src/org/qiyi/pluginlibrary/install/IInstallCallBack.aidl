// IInstallCallBack.aidl
package org.qiyi.pluginlibrary.install;
import org.qiyi.pluginlibrary.pm.PluginLiteInfo;
interface IInstallCallBack {

    /**
     * 安装成功回调
     *
     * @param packageName
     *            插件包名
     */
    void onPacakgeInstalled(in PluginLiteInfo info);

    /**
     * 安装失败回调
     *
     * @param packageName
     *            插件包名
     * @param failReason
     *            失败原因
     */
    void onPackageInstallFail(String packageName, int failReason);
}
