package org.qiyi.pluginlibrary.pm;

import java.util.List;

public interface ICMPackageInfoManager {
    List<CMPackageInfo> getInstalledPackages();
    CMPackageInfo getPackageInfo(String packageName);
    boolean isPackageInstalled(String packageName);
    boolean canInstallPackage(PluginPackageInfoExt info);
    boolean canUninstallPackage(CMPackageInfo info);
    void handlePluginException(String pkgName, String exceptionStr);
}
