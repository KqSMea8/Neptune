package org.qiyi.pluginlibrary.plugin;

import java.io.File;
import java.util.HashMap;

import org.qiyi.plugin.manager.ProxyEnvironmentNew;
import org.qiyi.pluginlibrary.ErrorType.ErrorType;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;

/**
 * @author zhuchengjin
 *
 * 插件apk资源初始化
 */
public class ApkTargetMapping implements TargetMapping {

    private final Context context;
    private final File apkFile;

    private String versionName;
    private int versionCode;
    private String packageName;
    private String applicationClassName;
    private String defaultActivityName;
    private PermissionInfo[] permissions;
    private PackageInfo packageInfo;
    private HashMap<String, ActivityInfo> mAcitivtyMap = new HashMap<String, ActivityInfo>();
    private HashMap<String, ServiceInfo> mServiceMap = new HashMap<String, ServiceInfo>();

    private Bundle metaData;

    public ApkTargetMapping(Context context, File apkFile) {
        this.context = context;
        this.apkFile = apkFile;
        init();
    }

    private void init() {
        final PackageInfo pkgInfo;
        try {
            pkgInfo = context.getPackageManager().getPackageArchiveInfo(apkFile.getAbsolutePath(),
                    PackageManager.GET_ACTIVITIES | PackageManager.GET_PERMISSIONS | PackageManager.GET_META_DATA
                            | PackageManager.GET_SERVICES | PackageManager.GET_CONFIGURATIONS);
            packageName = pkgInfo.packageName;
            applicationClassName = pkgInfo.applicationInfo.className;
            permissions = pkgInfo.permissions;
            versionCode = pkgInfo.versionCode;
            versionName = pkgInfo.versionName;
            packageInfo = pkgInfo;

            // 2.2 上获取不到application的meta-data，所以取默认activity里的meta作为开关 
            if(pkgInfo.activities != null  && pkgInfo.activities.length > 0){
            	defaultActivityName = pkgInfo.activities[0].name;
            	metaData = pkgInfo.activities[0].metaData;
            }
            if(metaData == null){
            	metaData = pkgInfo.applicationInfo.metaData;
            }
            ActivityInfo infos[] = pkgInfo.activities;
            ServiceInfo serviceInfo[]= pkgInfo.services;
            if (infos != null && infos.length > 0) {
                for (ActivityInfo info : infos) {
                    mAcitivtyMap.put(info.name, info);
                }
            }
            if (serviceInfo != null && serviceInfo.length > 0) {
                for (ServiceInfo info : serviceInfo) {
                    mServiceMap.put(info.name, info);
                }
            }
            packageInfo.applicationInfo.publicSourceDir = apkFile.getAbsolutePath();
        } catch (RuntimeException e) {
        	ProxyEnvironmentNew.deliverPlug(false, packageName, ErrorType.ERROR_CLIENT_LOAD_INIT_APK_FAILE);
            e.printStackTrace();
            return;
        }
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public String getApplicationClassName() {
        return applicationClassName;
    }

    @Override
    public String getDefaultActivityName() {
        return defaultActivityName;
    }

    public PermissionInfo[] getPermissions() {
        return permissions;
    }

    @Override
    public String getVersionName() {
        return versionName;
    }

    @Override
    public int getVersionCode() {
        return versionCode;
    }

    @Override
    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    @Override
    public int getThemeResource(String activity) {
        if (activity == null) {
            return android.R.style.Theme;
        }
        ActivityInfo info = mAcitivtyMap.get(activity);

        /**
         * 指定默认theme为android.R.style.Theme
         * 有些OPPO手机上，把theme设置成0，其实会把Theme设置成holo主题，带ActionBar，导致插件黑屏，目前插件SDK不支持ActionBar
         */
        if (info == null || info.getThemeResource() == 0) {
            return android.R.style.Theme;
        }
        return info.getThemeResource();
    }

    @Override
    public ActivityInfo getActivityInfo(String activity) {
        if (activity == null) {
            return null;
        }
        return mAcitivtyMap.get(activity);
    }

    @Override
    public ServiceInfo getServiceInfo(String service) {
        if(service==null){
            return null;
        }
        return mServiceMap.get(service);
    }
    
    /**
     * @return the metaData
     */
    public Bundle getMetaData() {
        return metaData;
    }

	@Override
	public ActivityInfo resolveActivity(Intent intent) {
		// TODO Will not implements
		return null;
	}

	@Override
	public ServiceInfo resolveService(Intent intent) {
		// TODO Will not implements
		return null;
	}

	@Override
	public String getDataDir() {
		return null;
	}

	@Override
	public String getnativeLibraryDir() {
		return null;
	}

	@Override
	public boolean isDataNeedPrefix() {
		return false;
	}
}
