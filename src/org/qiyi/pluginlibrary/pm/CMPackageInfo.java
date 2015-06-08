package org.qiyi.pluginlibrary.pm;

import org.qiyi.pluginnew.ApkTargetMappingNew;

/**
 * @author zhuchengjin
 *	插件一些信息
 */
public class CMPackageInfo {
    /**
     * 插件包名
     */
    public String packageName;
    
    /**
     * 安装后的apk file path
     */
    public String srcApkPath;

    public String installStatus;
//    public int versionCode;
//    
//    public String versionName;
    
    public PluginPackageInfoExt pluginInfo;
    
    public ApkTargetMappingNew targetInfo;
    
    /** 存储在安装列表中的key */
    final static String TAG_PKG_NAME = "pkgName";
    /** 存储在安装列表中的key */
    final static String TAG_APK_PATH = "srcApkPath";
    /** 安装状态 **/
    final static String TAG_INSTALL_STATUS = "install_status";
}
