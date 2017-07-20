// ICMPackageManager.aidl
package org.qiyi.pluginlibrary.pm;
import org.qiyi.pluginlibrary.pm.CMPackageInfo;
import org.qiyi.pluginlibrary.pm.PluginPackageInfoExt;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.install.IActionFinishCallback;
import org.qiyi.pluginlibrary.pm.IPackageDeleteObserver;
import org.qiyi.pluginlibrary.pm.ApkTargetMappingNew;

interface ICMPackageManager {

    List<CMPackageInfo> getInstalledApps();

    CMPackageInfo getPackageInfo(String pkg);

    boolean isPackageInstalled(String pkg);

    boolean canInstallPackage(in PluginPackageInfoExt info);

    boolean canUninstallPackage(in CMPackageInfo info);

    oneway void installApkFile(String filePath, IInstallCallBack listener,in PluginPackageInfoExt pluginInfo);

    oneway void installBuildinApps(String packageName, IInstallCallBack listener,in PluginPackageInfoExt info);

    oneway void deletePackage(in CMPackageInfo packageInfo, IPackageDeleteObserver observer);

    boolean uninstall(in CMPackageInfo packageInfo);

    oneway void packageAction(in CMPackageInfo packageInfo, IInstallCallBack callBack);

    oneway void setActionFinishCallback(IActionFinishCallback actionFinishCallback);

    ApkTargetMappingNew getApkTargetMapping(in String pkgName);
}
