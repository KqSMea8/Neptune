package org.qiyi.pluginlibrary.pm;

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
    
    public int versionCode;
    
    public String versionName;
    
    /** 存储在安装列表中的key */
    final static String TAG_PKG_NAME = "pkgName";
    /** 存储在安装列表中的key */
    final static String TAG_APK_PATH = "srcApkPath";
    /** 存储在安装列表中的key
     */
    final static String TAG_PKG_VC = "versionCode";
    /** 存储在安装列表中的key
     */
    final static String TAG_PKG_VN = "versionName";
}
