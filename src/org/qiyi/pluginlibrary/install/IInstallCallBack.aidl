// IInstallCallBack.aidl
package org.qiyi.pluginlibrary.install;
interface IInstallCallBack {

    /**
     * 安装成功回调
     *
     * @param packageName
     *            插件包名
     */
    void onPacakgeInstalled(String packageName);

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