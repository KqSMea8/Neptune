package org.qiyi.pluginlibrary.pm;

import java.util.List;

public interface IVerifyPluginInfo {
    //ipc 获取插件信息
    List<PluginLiteInfo> getInstalledPackages();
    PluginLiteInfo getPackageInfo(String packageName);
    boolean isPackageInstalled(String packageName);
    boolean canInstallPackage(PluginLiteInfo info);
    boolean canUninstallPackage(PluginLiteInfo info);
    void handlePluginException(String pkgName, String exceptionStr);
    List<String> getPluginRefs(String packageName);

    //直接获取插件信息(仅仅在ipc还没有建立的时候调用这里)
    List<PluginLiteInfo> getInstalledPackagesDirectly();
    boolean isPackageInstalledDirectly(String packageName);
    List<String> getPluginRefsDirectly(String packageName);
    PluginLiteInfo getPackageInfoDirectly(String packageName);
}
