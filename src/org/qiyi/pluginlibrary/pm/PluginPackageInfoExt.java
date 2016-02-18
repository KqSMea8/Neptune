package org.qiyi.pluginlibrary.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * 网络请求的数据信息
 */
public class PluginPackageInfoExt implements Parcelable, Serializable {
	/**
	 * Serializable ID
	 */
	private static final long serialVersionUID = 3765059090601585743L;

	/** 插件配置信息 */
	public static final String INFO_EXT = "info_ext";

	public static final String ID = "id";// 插件ID
	public static final String NAME = "name";
	public static final String VER = "ver";
	public static final String CRC = "CRC";// 校验码
	public static final String SCRC = "SCRC";// 新校验码
	public static final String NEW_CRC = "NEW_CRC";// 升级文件校验码
	public static final String INSTALL_METHOD = "install_method";// 插件安装方法
	public static final String TYPE = "type";
	public static final String DESC = "desc";
	public static final String ICON_URL = "icon_url";   //插件的启动引导图的背景图url
	public static final String URL = "url";
	public static final String UNINSTALL_FLAG = "uninstall_flag";
	public static final String UPDATE_TIME = "time";// 插件更新时间
	public static final String PLUGIN_TOTAL_SIZE = "plugin_total_size";
	public static final String PLUGIN_LOCAL = "plugin_local";// 是否从本地读取的标识
	public static final String PLUGIN_VISIBLE = "plugin_visible";// 是否可见
	public static final String DOWNLOAD_URL = "plugin_download_url";// 插件下载地址
	public static final String SUFFIX_TYPE = "suffix_type";// 插件的文件APK\SO\JAR
	public static final String FILE_SOURCE_TYPE = "file_source_type";// 插件文件来源  type(内置、网络下载)
	public static final String PACKAGENAME = "packageName";// 插件文件来源
	public static final String START_ICON = "start_icon";// 显示插件启动按钮
	public static final String UPGRADE_TYPE = "upgrade_type"; //更新方式，自动，手动？
	public static final String GRAY_VER = "plugin_gray_ver"; //灰度版本号
	public static final String PLUGIN_VER = "plugin_ver"; //插件显示版本号


	// 插件ID
	public String id = "";
	// 插件名称
	public String name = "";
	// 插件版本
	public int ver = 0;
	// 文件crc校验码
	public String crc = "";
	// 0:默认下载安装 1:不主动下载安装
	public int type = 0;
	// 描述
	public String desc = "";
	// APP图片下载地址
	public String icon_url = "";
	// 是否显示卸载按钮 0表示不允许，1表示允许
	public int isAllowUninstall = 0;
	// 后台输出的大小
	public long pluginTotalSize;
	// 应用程序的包名
	public String packageName = "";
	// 默认不走本地
	public int local = 0;
	// 默认可见
	public int invisible = 0;
	// 新的scrc值
	public String scrc = "";
	// plugin download url
	public String url = "";
	// 插件安装方式
	public String mPluginInstallMethod = CMPackageManager.PLUGIN_METHOD_DEFAULT;
	// 插件的文件后缀类型 APK、SO、JAR
	public String mSuffixType = "";
	// 插件文件来源 type(内置、网络下载)
	public String mFileSourceType = CMPackageManager.PLUGIN_SOURCE_NETWORK;
	
	public int start_icon = 0;  //默认为0，不启动
	
	public int upgrade_type = 0;  //默认为0

	public String plugin_gray_ver = "";  //灰度版本，默认为""
	
	public String plugin_ver = "";//插件版本显示版本号。


    @Override
    public String toString() {
        try {
            JSONObject json = data2JsonObj();
            if (null != json) {
                return json.toString();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "Plugin [id=" + id + ", name=" + name + ", plugin_ver=" + plugin_ver
                + ", plugin_gray_ver=" + plugin_gray_ver + ", crc=" + crc + ", type=" + type
                + ", desc=" + desc + ", i_method=" + mPluginInstallMethod + ", url=" + url
                + ", mPluginFileType=" + mSuffixType + "]";
    }

	public PluginPackageInfoExt() {
	}

	public PluginPackageInfoExt(PluginPackageInfoExt packageInfo) {

		if (packageInfo != null) {
			id = packageInfo.id;
			name = packageInfo.name;
			ver = packageInfo.ver;
			crc = packageInfo.crc;
			type = packageInfo.type;
			desc = packageInfo.desc;
			icon_url = packageInfo.icon_url;
			isAllowUninstall = packageInfo.isAllowUninstall;
			pluginTotalSize = packageInfo.pluginTotalSize;
			packageName = packageInfo.packageName;
			local = packageInfo.local;
			invisible = packageInfo.invisible;
			scrc = packageInfo.scrc;
			mPluginInstallMethod = packageInfo.mPluginInstallMethod;
			url = packageInfo.url;
			mSuffixType = packageInfo.mSuffixType;
			mFileSourceType = packageInfo.mFileSourceType;
			start_icon = packageInfo.start_icon;
			upgrade_type = packageInfo.upgrade_type;
			plugin_gray_ver = packageInfo.plugin_gray_ver;
			plugin_ver = packageInfo.plugin_ver;
		}
	}
	
	public PluginPackageInfoExt(JSONObject ext) {
		if (ext != null) {
			id = ext.optString(ID);
			name = ext.optString(NAME);
			ver = ext.optInt(VER);
			crc = ext.optString(CRC);
			type = ext.optInt(TYPE);
			desc = ext.optString(DESC);
			icon_url = ext.optString(ICON_URL);
			isAllowUninstall = ext.optInt(UNINSTALL_FLAG);
			pluginTotalSize = ext.optLong(PLUGIN_TOTAL_SIZE);
			packageName = ext.optString(PACKAGENAME);
			local = ext.optInt(PLUGIN_LOCAL);
			invisible = ext.optInt(PLUGIN_VISIBLE);
			scrc = ext.optString(SCRC);
			mPluginInstallMethod = ext.optString(INSTALL_METHOD);
			url = ext.optString(URL);
			mSuffixType = ext.optString(SUFFIX_TYPE);
			mFileSourceType = ext.optString(FILE_SOURCE_TYPE);
			start_icon = ext.optInt(START_ICON); 
			upgrade_type = ext.optInt(UPGRADE_TYPE); 
			plugin_gray_ver = ext.optString(GRAY_VER); 
			plugin_ver = ext.optString(PLUGIN_VER);
		}
	}

	@SuppressWarnings("rawtypes")
	public static final android.os.Parcelable.Creator<PluginPackageInfoExt> CREATOR = new android.os.Parcelable.Creator<PluginPackageInfoExt>() {

		public PluginPackageInfoExt createFromParcel(Parcel parcel) {
			return new PluginPackageInfoExt(parcel);
		}

		public PluginPackageInfoExt[] newArray(int i) {
			return new PluginPackageInfoExt[i];
		}

	};

	public boolean haveUpdate(int ver) {
		return this.ver < ver;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		PluginPackageInfoExt other = (PluginPackageInfoExt) obj;
		if (!TextUtils.equals(id, other.id)) {
			return false;
		}

		if (!TextUtils.equals(url, other.url)) {
			return false;
		}
		return true;
	}

	public PluginPackageInfoExt(Parcel parcel) {
		id = parcel.readString();
		name = parcel.readString();
		ver = parcel.readInt();
		crc = parcel.readString();
		type = parcel.readInt();
		desc = parcel.readString();
		icon_url = parcel.readString();
		isAllowUninstall = parcel.readInt();
		pluginTotalSize = parcel.readLong();
		packageName = parcel.readString();
		local = parcel.readInt();
		invisible = parcel.readInt();
		scrc = parcel.readString();
		url = parcel.readString();
		mPluginInstallMethod = parcel.readString();
		mSuffixType = parcel.readString();
		mFileSourceType = parcel.readString();
		start_icon = parcel.readInt();
		upgrade_type = parcel.readInt();
		plugin_gray_ver = parcel.readString();
		plugin_ver = parcel.readString();

	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(id);
		dest.writeString(name);
		dest.writeInt(ver);
		dest.writeString(crc);
		dest.writeInt(type);
		dest.writeString(desc);
		dest.writeString(icon_url);
		dest.writeInt(isAllowUninstall);
		dest.writeLong(pluginTotalSize);
		dest.writeString(packageName);
		dest.writeInt(local);
		dest.writeInt(invisible);
		dest.writeString(scrc);
		dest.writeString(url);
		dest.writeString(mPluginInstallMethod);
		dest.writeString(mSuffixType);
		dest.writeString(mFileSourceType);
		dest.writeInt(start_icon);
		dest.writeInt(upgrade_type);
		dest.writeString(plugin_gray_ver);
		dest.writeString(plugin_ver);
	}

	public JSONObject data2JsonObj() throws JSONException {
		JSONObject result = new JSONObject();
		result.put(ID, id);
		result.put(NAME, name);
		result.put(VER, ver);
		result.put(CRC, crc);
		result.put(TYPE, type);
		result.put(DESC, desc);
		result.put(ICON_URL, icon_url);
		result.put(UNINSTALL_FLAG, isAllowUninstall);
		result.put(PLUGIN_TOTAL_SIZE, pluginTotalSize);
		result.put(PACKAGENAME, packageName);
		result.put(PLUGIN_LOCAL, local);
		result.put(PLUGIN_VISIBLE, invisible);
		result.put(SCRC, scrc);
		result.put(INSTALL_METHOD, mPluginInstallMethod);
		result.put(URL, url);
		result.put(SUFFIX_TYPE, mSuffixType);
		result.put(FILE_SOURCE_TYPE, mFileSourceType);
		result.put(START_ICON, start_icon);
		result.put(UPGRADE_TYPE, upgrade_type);
		result.put(GRAY_VER, plugin_gray_ver);
		result.put(PLUGIN_VER, plugin_ver);
		return result;
	}
	
}
