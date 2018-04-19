// IPluginPackageManager.aidl
package org.qiyi.pluginlibrary.pm;
import org.qiyi.pluginlibrary.pm.PluginLiteInfo;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.install.IActionFinishCallback;
import org.qiyi.pluginlibrary.pm.IPluginUninstallCallBack;
import org.qiyi.pluginlibrary.pm.PluginPackageInfo;

interface IPluginPackageManager {

    List<PluginLiteInfo> getInstalledApps();

    PluginLiteInfo getPackageInfo(String pkg);

    boolean isPackageInstalled(String pkg);

    boolean canInstallPackage(in PluginLiteInfo info);

    boolean canUninstallPackage(in PluginLiteInfo info);

    oneway void installApkFile(String filePath, IInstallCallBack listener,in PluginLiteInfo pluginInfo);

    oneway void installBuildinApps(in PluginLiteInfo info, IInstallCallBack listener);

    oneway void deletePackage(in PluginLiteInfo packageInfo, IPluginUninstallCallBack observer);

    boolean uninstall(in PluginLiteInfo packageInfo);

    oneway void packageAction(in PluginLiteInfo packageInfo, IInstallCallBack callBack);

    oneway void setActionFinishCallback(IActionFinishCallback actionFinishCallback);

    PluginPackageInfo getPluginPackageInfo(in String pkgName);

    List<String> getPluginRefs(in String pkgName);

}
