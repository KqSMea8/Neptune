// IPluginUninstallCallBack.aidl
package org.qiyi.pluginlibrary.pm;

interface IPluginUninstallCallBack {
    oneway void onPluginUnintall(String packageName, int returnCode);
}
