package org.qiyi.pluginlibrary.pm;

import java.util.concurrent.ConcurrentMap;

public interface ICMPackageInfoDelegate {
    ConcurrentMap<String, CMPackageInfo> getInstalledPackageList();
    int versionCompare(PluginPackageInfoExt left, PluginPackageInfoExt right);
}
