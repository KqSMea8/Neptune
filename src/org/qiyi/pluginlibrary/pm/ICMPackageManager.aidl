// ICMPackageManager.aidl
package org.qiyi.pluginlibrary.pm;
import org.qiyi.pluginlibrary.pm.CMPackageInfo;
import org.qiyi.pluginlibrary.pm.PluginPackageInfoExt;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.pm.IPackageDeleteObserver;
interface ICMPackageManager {

    List<CMPackageInfo> getInstalledApps();

    CMPackageInfo getPackageInfo(String pkg);

    boolean isPackageInstalled(String pkg);

    void installApkFile(String filePath, IInstallCallBack listener,in PluginPackageInfoExt pluginInfo);

    void installBuildinApps(String packageName, IInstallCallBack listener,in PluginPackageInfoExt info);

    void deletePackage(String packageName, IPackageDeleteObserver observer);

    boolean uninstall(String pkgName);

    void packageAction(String packageName, IInstallCallBack callBack);
}
