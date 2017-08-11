package org.qiyi.pluginlibrary.pm;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 存放插件最基本的信息：
 *  包名(必须有)
 *  版本号(可以为空)
 *  灰度版本号(可以为空)
 *  安装位置(安装完成后，将安装路径放在此字段返回给调用者)
 */
public class PluginLiteInfo implements Parcelable {
    private static final String TAG = "PluginLiteInfo";

    /**
     * 插件安装状态
     **/
    public static final String PLUGIN_INSTALLED = "installed";
    public static final String PLUGIN_UNINSTALLED = "uninstall";
    public static final String PLUGIN_UPGRADING = "upgrading";

    /**插件包名*/
    public String packageName;
    /**插件的安装路径*/
    public String srcApkPath;
    /**插件的安装状态*/
    public String installStatus;
    /**插件的正式版本*/
    public String mPluginVersion="";
    /**插件的灰度版本*/
    public String mPluginGrayVersion="";
    /**插件唯一标识符*/
    public String id = "";



    public PluginLiteInfo() {

    }

    protected PluginLiteInfo(Parcel in) {
        packageName = in.readString();
        srcApkPath = in.readString();
        installStatus = in.readString();
        mPluginVersion = in.readString();
        mPluginGrayVersion = in.readString();
        id = in.readString();
    }

    public static final Creator<PluginLiteInfo> CREATOR = new Creator<PluginLiteInfo>() {
        @Override
        public PluginLiteInfo createFromParcel(Parcel in) {
            return new PluginLiteInfo(in);
        }

        @Override
        public PluginLiteInfo[] newArray(int size) {
            return new PluginLiteInfo[size];
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
        parcel.writeString(mPluginVersion);
        parcel.writeString(mPluginGrayVersion);
        parcel.writeString(id);
    }


}
