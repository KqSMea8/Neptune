package org.qiyi.pluginlibrary.plugin;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;

/**
 * 
 * 插件信息映射接口
 */
public interface TargetMapping {

    String getPackageName();

    String getVersionName();

    int getVersionCode();

    PackageInfo getPackageInfo();

    int getThemeResource(String activity);

    ActivityInfo getActivityInfo(String activity);
    
    ServiceInfo getServiceInfo(String service);
    
    String getApplicationClassName();

    String getDefaultActivityName();

    PermissionInfo[] getPermissions();

    Bundle getMetaData();

    ActivityInfo resolveActivity(Intent intent);

    ServiceInfo resolveService(Intent intent);
    
    String getDataDir();

    String getnativeLibraryDir();
    
    boolean isDataNeedPrefix();
}
