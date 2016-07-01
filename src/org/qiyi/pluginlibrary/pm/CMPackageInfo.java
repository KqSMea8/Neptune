package org.qiyi.pluginlibrary.pm;

import java.io.File;
import java.util.Map;

import org.qiyi.pluginlibrary.ApkTargetMappingNew;
import org.qiyi.pluginlibrary.install.PluginInstaller;
import org.qiyi.pluginlibrary.utils.ContextUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * @author zhuchengjin 插件一些信息
 */
public class CMPackageInfo implements Parcelable {
    private static final String TAG = CMPackageInfo.class.getSimpleName();

    /**
     * 插件安装状态
     **/
    public static final String PLUGIN_INSTALLED = "installed";
    public static final String PLUGIN_UNINSTALLED = "uninstall";
    public static final String PLUGIN_UPGRADING = "upgrading";

    /**
     * 插件包名
     */
    public String packageName;

    /**
     * 安装后的apk file path
     */
    public String srcApkPath;

    public String installStatus;
    // public int versionCode;

    // public String versionName;

    public PluginPackageInfoExt pluginInfo;

    private ApkTargetMappingNew targetInfo;

    /** 存储在安装列表中的key */
    final static String TAG_PKG_NAME = "pkgName";
    /** 存储在安装列表中的key */
    final static String TAG_APK_PATH = "srcApkPath";
    /** 安装状态 **/
    final static String TAG_INSTALL_STATUS = "install_status";

    public CMPackageInfo() {

    }

    public ApkTargetMappingNew getTargetMapping(Context context) {
        if (null == targetInfo) {
            context = ContextUtils.getOriginalContext(context);
            updateSrcApkPath(context, this);
            targetInfo = CMPackageManagerImpl.getInstance(context).getApkTargetMapping(context, packageName,
                    srcApkPath);
        }
        return targetInfo;
    }

    /**
     * This method should only be called by CMPackageManager to keep only one
     * cache in all processes, other requirement should invoke
     * {@link CMPackageInfo#getTargetMapping(Context)}}
     *
     * @param context
     * @param pkgName
     * @param apkFilePath
     * @param cache
     * @return
     */
    ApkTargetMappingNew getTargetMapping(Context context, String pkgName, String apkFilePath,
            Map<String, ApkTargetMappingNew> cache) {
        if (null != targetInfo) {
            return targetInfo;
        } else if (cache != null && cache.containsKey(pkgName)) {
            targetInfo = cache.get(pkgName);
            return targetInfo;
        } else if (null != context && !TextUtils.isEmpty(apkFilePath)) {
            File file = new File(apkFilePath);
            if (file.exists()) {
                targetInfo = new ApkTargetMappingNew(ContextUtils.getOriginalContext(context), file);
                cache.put(pkgName, targetInfo);
                return targetInfo;
            }
        }
        return targetInfo;
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
        parcel.writeParcelable(targetInfo, i);
    }

    /**
     * 保护性的更新srcApkPath
     *
     * @param context
     * @param cmPkgInfo
     */
    public static void updateSrcApkPath(Context context, CMPackageInfo cmPkgInfo) {
        if (null != context && null != cmPkgInfo && TextUtils.isEmpty(cmPkgInfo.srcApkPath)) {
            cmPkgInfo.srcApkPath = PluginInstaller.getPluginappRootPath(ContextUtils.getOriginalContext(context))
                    .getAbsolutePath() + File.separator + cmPkgInfo.packageName + PluginInstaller.APK_SUFFIX;
            PluginDebugLog.log(TAG, "Special case srcApkPath is null!!! Set default value for srcApkPath! packageName: "
                    + cmPkgInfo.packageName + " srcApkPath: " + cmPkgInfo.srcApkPath);
        }
    }
}
