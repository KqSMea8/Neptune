package org.qiyi.pluginlibrary.pm;

import android.os.Parcel;
import android.os.Parcelable;

import org.qiyi.pluginnew.ApkTargetMappingNew;

/**
 * @author zhuchengjin
 *	插件一些信息
 */
public class CMPackageInfo implements Parcelable {
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

    public CMPackageInfo() {

    }

    protected CMPackageInfo(Parcel in) {
        packageName = in.readString();
        srcApkPath = in.readString();
        installStatus = in.readString();
        pluginInfo = in.readParcelable(PluginPackageInfoExt.class.getClassLoader());
        targetInfo = in.readParcelable(ApkTargetMappingNew.class.getClassLoader());
    }

    public static final Creator<CMPackageInfo> CREATOR = new Creator<CMPackageInfo>() {
        @Override
        public CMPackageInfo createFromParcel(Parcel in) {
            return new CMPackageInfo(in);
        }

        @Override
        public CMPackageInfo[] newArray(int size) {
            return new CMPackageInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(packageName);
        parcel.writeString(srcApkPath);
        parcel.writeString(installStatus);
        parcel.writeParcelable(pluginInfo, i);
        parcel.writeParcelable(targetInfo,i);
    }



}
