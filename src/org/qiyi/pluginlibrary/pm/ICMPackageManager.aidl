// ICMPackageManager.aidl
package org.qiyi.pluginlibrary.pm;
import org.qiyi.pluginlibrary.pm.CMPackageInfo;
import org.qiyi.pluginlibrary.pm.PluginPackageInfoExt;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.install.IActionFinishCallback;
import org.qiyi.pluginlibrary.pm.IPackageDeleteObserver;
interface ICMPackageManager {

    List<CMPackageInfo> getInstalledApps();

    CMPackageInfo getPackageInfo(String pkg);

    boolean isPackageInstalled(String pkg);

    oneway void installApkFile(String filePath, IInstallCallBack listener,in PluginPackageInfoExt pluginInfo);

    oneway void installBuildinApps(String packageName, IInstallCallBack listener,in PluginPackageInfoExt info);

    oneway void deletePackage(String packageName, IPackageDeleteObserver observer);

    boolean uninstall(String pkgName);

    oneway void packageAction(String packageName, IInstallCallBack callBack);

    oneway void setActionFinishCallback(IActionFinishCallback actionFinishCallback);
}
