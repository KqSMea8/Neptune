package org.qiyi.pluginlibrary.pm;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.qiyi.pluginlibrary.ErrorType.ErrorType;
import org.qiyi.pluginlibrary.api.TargetActivator;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.install.PluginInstaller;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

/**
 * 负责安装卸载app，获取安装列表等工作.<br>
 *  负责安装插件的一些方法
 *  功能类似系统中的PackageManager
 */
public class CMPackageManager {
	
    private static final String TAG = PluginDebugLog.TAG;
    /**
     * 安装成功，发送广播
     */
    public static final String ACTION_PACKAGE_INSTALLED = "com.qiyi.plugin.installed";
    
    /**
     *  安装失败，发送广播
     */
    public static final String ACTION_PACKAGE_INSTALLFAIL = "com.qiyi.plugin.installfail";

    /**
     * 卸载插件，发送广播 
     */
    public static final String ACTION_PACKAGE_UNINSTALL = "com.qiyi.plugin.uninstall";

    /** 安装插件方案的版本 **/
    public static final String PLUGIN_METHOD_DEFAULT = "plugin_method_default";
    public static final String PLUGIN_METHOD_DEXMAKER = "plugin_method_dexmaker";

    /** 安装完的pkg的包名 */
    public static final String EXTRA_PKG_NAME = "package_name"; 
    /** 
     * 支持 assets:// 和 file:// 两种，对应内置和外部apk安装。
     * 比如  assets://megapp/xxxx.apk , 或者 file:///data/data/com.qiyi.xxx/files/xxx.apk  */
    public static final String EXTRA_SRC_FILE = "install_src_file";
    /** 插件方案版本号 **/
    public static final String EXTRA_PLUGIN_METHOD_VERSION = "plugin_method_version";
    /** 安装完的apk path，没有scheme 比如 /data/data/com.qiyi.video/xxx.apk */
    public static final String EXTRA_DEST_FILE = "install_dest_file";
    
    /** 安装完的pkg的 version code */
    public static final String EXTRA_VERSION_CODE = "version_code";
    /** 安装完的pkg的 version name */
    public static final String EXTRA_VERSION_NAME = "version_name"; 
    
    public static final String SCHEME_ASSETS = "assets://";
    public static final String SCHEME_FILE = "file://";
    public static final String SCHEME_SO = "so://";
    
    /** application context */
    private Context mContext; 

    private static CMPackageManager sInstance;//安装对象
    
    /** 已安装列表。
     * !!!!!!! 不要直接引用该变量。 因为该变量是 lazy init 方式，不需要的时不进行初始化。  
     * 使用 {@link #getInstalledPkgsInstance()} 获取该实例
     * */
    private Hashtable<String, CMPackageInfo> mInstalledPkgs;
    
    /** 安装包任务队列。 */
    private List<PackageAction> mPackageActions = new LinkedList<CMPackageManager.PackageAction>();
    
    private Map<String ,IInstallCallBack> listenerMap = new HashMap<String, IInstallCallBack>();
    
    /** 存贮在sharedpreference 的安装列表  */
    private static final String SP_APP_LIST = "packages";
    
    /**
     * Return code for when package deletion succeeds. This is passed to the
     * {@link IPackageDeleteObserver} by {@link #deletePackage()} if the system
     * succeeded in deleting the package.
     *
     */
    public static final int DELETE_SUCCEEDED = 1;

    /**
     * Deletion failed return code: this is passed to the
     * {@link IPackageDeleteObserver} by {@link #deletePackage()} if the system
     * failed to delete the package for an unspecified reason.
     *
     */
    public static final int DELETE_FAILED_INTERNAL_ERROR = -1;
    
    
    private CMPackageManager(Context context) {
        mContext = context.getApplicationContext();
        registerInstallderReceiver();
    }
    
    /**
     * lazy init mInstalledPkgs 变量，没必要在构造函数中初始化该列表，减少hostapp 每次初始化时的时间消耗。
     * 已安装的插件包
     * @return
     */
    private Hashtable<String, CMPackageInfo> getInstalledPkgsInstance() {
        initInstalledPackageListIfNeeded();
        return mInstalledPkgs;
    }
    
    /**
     * 获取packageManager实例对象
     * @param context
     * @return
     */
    public synchronized static CMPackageManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CMPackageManager(context);
        }
        return sInstance;
    }
    
    /**
     * 获取安装列表。
     * @return
     */
    public List<CMPackageInfo> getInstalledApps() {
        Enumeration<CMPackageInfo> packages = getInstalledPkgsInstance().elements();
        ArrayList<CMPackageInfo> list = new ArrayList<CMPackageInfo>();
        while (packages.hasMoreElements()) {
            CMPackageInfo pkg = packages.nextElement();
            list.add(pkg);
        }
        
        return list;
    }
    
    /**
     * 初始化安装列表
     */
    private void initInstalledPackageListIfNeeded() {
        // 第一次初始化安装列表。
        if (mInstalledPkgs == null) {
            mInstalledPkgs = new Hashtable<String, CMPackageInfo>();
            
            SharedPreferences sp = mContext.getSharedPreferences(PluginInstaller.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
            String jsonPkgs = sp.getString(SP_APP_LIST, "");
            
            if (jsonPkgs != null && jsonPkgs.length() > 0) {
                try {
                    
                    boolean needReSave = false;
                    
                    JSONArray pkgs = new JSONArray(jsonPkgs);
                    int count = pkgs.length();
                    for (int i = 0; i < count; i++) {
                        JSONObject pkg = (JSONObject) pkgs.get(i);
                        CMPackageInfo pkgInfo = new CMPackageInfo();
                        pkgInfo.packageName = pkg.optString(CMPackageInfo.TAG_PKG_NAME);
                        pkgInfo.srcApkPath = pkg.optString(CMPackageInfo.TAG_APK_PATH);
                        pkgInfo.versionCode = pkg.optInt(CMPackageInfo.TAG_PKG_VC, 0);
                        pkgInfo.versionName = pkg.optString(CMPackageInfo.TAG_PKG_VN);
                        if (pkgInfo.versionCode == 0 || TextUtils.isEmpty(pkgInfo.versionName)) {
                            // 这两个值是从2.0版本才有的，为了兼容，做下处理。
                            PackageManager pm = mContext.getPackageManager();
                            PackageInfo pi = pm.getPackageArchiveInfo(pkgInfo.srcApkPath, 0);
                            if(pi != null){
                            	pkgInfo.versionCode = pi.versionCode;
                            	pkgInfo.versionName = pi.versionName;
                            }
                            
                            needReSave = true;
                        }
                        
                        mInstalledPkgs.put(pkgInfo.packageName, pkgInfo);
                    }
                    
                    if (needReSave) { // 把兼容数据重新写回文件
                        saveInstalledPackageList();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * 将安装列表写入文件存储。
     * 调用这个之前确保  mInstalledPkgs 已经初始化过了
     */
    private void saveInstalledPackageList() {
        JSONArray pkgs = new JSONArray();
        
        // 调用这个之前确保  mInstalledPkgs 已经初始化过了
        Enumeration<CMPackageInfo> packages = mInstalledPkgs.elements();
        while (packages.hasMoreElements()) {
            CMPackageInfo pkg = packages.nextElement();
            
            JSONObject object = new JSONObject();
            try {
                object.put(CMPackageInfo.TAG_PKG_NAME, pkg.packageName);
                object.put(CMPackageInfo.TAG_APK_PATH, pkg.srcApkPath);
                object.put(CMPackageInfo.TAG_PKG_VC, pkg.versionCode);
                object.put(CMPackageInfo.TAG_PKG_VN, pkg.versionName);
                
                pkgs.put(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        SharedPreferences sp = mContext.getSharedPreferences(PluginInstaller.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        String value = pkgs.toString();
        Editor editor = sp.edit();
        editor.putString(SP_APP_LIST, value);
        editor.commit();
    }
    
    /**
     * 安装广播，用于监听安装过程中是否成功。
     */
    private BroadcastReceiver pluginInstallerReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_PACKAGE_INSTALLED.equals(action)) {
                String pkgName = intent.getStringExtra(EXTRA_PKG_NAME);
                String destApkPath = intent.getStringExtra(CMPackageManager.EXTRA_DEST_FILE);
                String versionName = intent.getStringExtra(CMPackageManager.EXTRA_VERSION_NAME);
                int versionCode = intent.getIntExtra(CMPackageManager.EXTRA_VERSION_CODE, 0);
                
                CMPackageInfo pkgInfo = new CMPackageInfo();
                pkgInfo.packageName = pkgName;
                pkgInfo.srcApkPath = destApkPath;
                pkgInfo.versionCode = versionCode;
                pkgInfo.versionName = versionName;
                
                getInstalledPkgsInstance().put(pkgName, pkgInfo);//将安装的插件名称保存到集合中。
                saveInstalledPackageList(); // 存储变化后的安装列表
                if(listenerMap.get(pkgName) != null){
                	listenerMap.get(pkgName).onPacakgeInstalled(pkgName);
                	listenerMap.remove(pkgName);
                }
                // 执行等待执行的action
                executePackageAction(pkgName, true, 0);
            } else if (ACTION_PACKAGE_INSTALLFAIL.equals(action)) {

                // 针对内置应用，包名为文件名
                String assetsPath = intent.getStringExtra(CMPackageManager.EXTRA_SRC_FILE);
                int start = assetsPath.lastIndexOf("/");
                int end = assetsPath.lastIndexOf(PluginInstaller.APK_SUFFIX);
                String mapPackagename = assetsPath.substring(start + 1, end);
                //失败原因
                int failReason = intent.getIntExtra(ErrorType.ERROR_RESON,ErrorType.SUCCESS);
                if(listenerMap.get(mapPackagename) !=null){
                	listenerMap.get(mapPackagename).onPackageInstallFail(mapPackagename, failReason);
                	listenerMap.remove(mapPackagename);
                }
                executePackageAction(mapPackagename, false, failReason);
            }
        }
        
    };
    
    /**
     * 监听安装列表变化.
     */
    private void registerInstallderReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PACKAGE_INSTALLED);
        filter.addAction(ACTION_PACKAGE_INSTALLFAIL);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        
        //注册一个安装广播
        mContext.registerReceiver(pluginInstallerReceiver, filter);
    }
    
    /**
     * 包依赖任务队列对象。
     */
    private class PackageAction {
        long timestamp;//时间
        IInstallCallBack callBack;//安装回调
        String packageName;//包名
    }
    
    /**
     * 执行依赖于安装包的 runnable，如果该package已经安装，则立即执行。如果pluginapp正在初始化，或者该包正在安装，
     * 则放到任务队列中等待安装完毕执行。
     * 
     * @param packageName
     *            插件包名
     * @param callBack
     *            插件安装回调
     */
    public void packageAction(String packageName, IInstallCallBack callBack) {
//        PluginInstaller.installBuildinApps(mContext); //
        boolean packageInstalled = isPackageInstalled(packageName);
        boolean installing = PluginInstaller.isInstalling(packageName);
		PluginDebugLog.log(TAG, "packageAction , " + packageName + " installed : "
				+ packageInstalled + " installing: " + installing);
        
        if (packageInstalled && (!installing)) { // 安装了，并且没有更新操作
            callBack.onPacakgeInstalled(packageName);
        } else {
            PackageAction action = new PackageAction();
            action.packageName = packageName;
            action.timestamp = System.currentTimeMillis();
            action.callBack = callBack;
            
            synchronized(this) {
                if (mPackageActions.size() < 1000) { // 防止溢出
                    mPackageActions.add(action);
                }
            }
        }
        
        clearExpiredPkgAction();
    }
    
    private void executePackageAction(String packageName, boolean isSuccess, int failReason) {
        ArrayList<PackageAction> executeList = new ArrayList<CMPackageManager.PackageAction>();
       
        for (PackageAction action : mPackageActions) {
            if (packageName.equals(action.packageName)) {
                executeList.add(action);
            }
        }
        
        // 首先从总列表中删除
        synchronized(this) {
            for (PackageAction action : executeList) {
                mPackageActions.remove(action);
            }
        }
        
        // 挨个执行
        for (PackageAction action : executeList) {
            if (action.callBack != null) {
                if (isSuccess) {
                    action.callBack.onPacakgeInstalled(packageName);
                } else {
                    action.callBack.onPackageInstallFail(action.packageName, failReason);
                }
            }
        }
    }
    
    /**
     * 删除过期没有执行的 action，可能由于某种原因存在此问题。比如一个找不到package的任务。
     */
    private void clearExpiredPkgAction() {
        long currentTime = System.currentTimeMillis();
        
        ArrayList<PackageAction> deletedList = new ArrayList<PackageAction>();
        
        synchronized (this) {
            // 查找需要删除的
            for (PackageAction action : mPackageActions) {
                if (currentTime - action.timestamp >= 1 * 60 * 1000) {
                    deletedList.add(action);
                }
            }
            // 实际删除
            for (PackageAction action : deletedList) {
                mPackageActions.remove(action);
				action.callBack.onPackageInstallFail(action.packageName,
						ErrorType.ERROR_CLIENT_TIME_OUT);
            }
        }
    }
    
    

    
    /**
     * 判断一个package是否安装
     */
    public boolean isPackageInstalled(String packageName) {
        if (getInstalledPkgsInstance().containsKey(packageName)) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * 获取安装apk的信息
     * @param packageName
     * @return 没有安装反馈null
     */
    public CMPackageInfo getPackageInfo(String packageName) {
        if (packageName == null || packageName.length() == 0) {
            return null;
        }
        
        CMPackageInfo info = getInstalledPkgsInstance().get(packageName);
        
        return info;
    }
    
    /**
     * 安装一个 apk file 文件. 用于安装比如下载后的文件，或者从sdcard安装。安装过程采用独立进程异步安装。
     * 启动service进行安装操作。
     * 安装完会有 {@link #ACTION_PACKAGE_INSTALLED} broadcast。

     * @param filePath apk 文件目录 比如  /sdcard/xxxx.apk
     */
    public void installApkFile(final String filePath,IInstallCallBack listener) {
////    	if(listener != null && !listenerMap.containsKey(pakName) ){
//    	int start = filePath.lastIndexOf("/");
//        int end = filePath.lastIndexOf(PluginInstaller.APK_SUFFIX);
//        String mapPackagename = filePath.substring(start + 1, end);
//    	listenerMap.put(mapPackagename, listener);
//    	PluginDebugLog.log(TAG, "installApkFile:"+mapPackagename);
////    	}
////       this.listener = listener;
//       PluginInstaller.installApkFile(mContext, filePath);
    	installApkFile(filePath, listener, PLUGIN_METHOD_DEFAULT);
    }

    /**
     * 安装一个 apk file 文件. 用于安装比如下载后的文件，或者从sdcard安装。安装过程采用独立进程异步安装。
     * 启动service进行安装操作。
     * 安装完会有 {@link #ACTION_PACKAGE_INSTALLED} broadcast。

     * @param filePath apk 文件目录 比如  /sdcard/xxxx.apk
     * @param pluginMethodVersion 插件方案的版本号
     */
	public void installApkFile(final String filePath, IInstallCallBack listener,
			String pluginMethodVersion) {
//    	if(listener != null && !listenerMap.containsKey(pakName) ){
		int end= 0;
		if(filePath.endsWith(PluginInstaller.SO_SUFFIX)){
			end = filePath.lastIndexOf(PluginInstaller.SO_SUFFIX);
		}else{
			end = filePath.lastIndexOf(PluginInstaller.APK_SUFFIX);
		}
    	int start = filePath.lastIndexOf("/");
        String mapPackagename = filePath.substring(start + 1, end);
    	listenerMap.put(mapPackagename, listener);
    	PluginDebugLog.log(TAG, "installApkFile:"+mapPackagename);
//    	}
//       this.listener = listener;
		PluginInstaller.installApkFile(mContext, filePath, pluginMethodVersion);
	}

    /**
     * 安装内置在 assets/puginapp 目录下的 apk。
     * 内置app必须以 packageName 命名，比如 com.qiyi.xx.apk 
     */
    public void installBuildinApps(String packageName,IInstallCallBack listener) {
//    	this.listener = listener;
//    	listenerMap.put(packageName, listener);
//    	PluginInstaller.installBuildinApps(packageName,mContext);
    	installBuildinApps(packageName, listener, PLUGIN_METHOD_DEFAULT);
    }

    /**
     * 安装内置在 assets/puginapp 目录下的 apk。
     * 内置app必须以 packageName 命名，比如 com.qiyi.xx.apk 
     * 
     * @param packageName
     * @param listener
     * @param pluginMethodVersion 插件方案的版本号
     */
	public void installBuildinApps(String packageName, IInstallCallBack listener,
			String pluginMethodVersion) {
		listenerMap.put(packageName, listener);
		PluginInstaller.installBuildinApps(packageName, mContext, pluginMethodVersion);
	}

    /**
     * 删除安装包。
     * 卸载插件应用程序
     * @param packageName 需要删除的package 的 packageName
     * @param observer 卸载结果回调
     */
    public void deletePackage(final String packageName, IPackageDeleteObserver observer) {

    	try{
    		// 先停止运行插件
    		TargetActivator.unLoadTarget(packageName);
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	
    	try {
    		// 删除生成的data数据文件
    		// 清除environment中相关的数据:按前缀匹配
    		PluginInstaller.deleteData(mContext, packageName);
    		
    		//删除安装文件，apk，dex，so
    		PluginInstaller.deletePackage(mContext, packageName);
    		
    		//从安装列表中删除，并且更新存储安装列表的文件
    		getInstalledPkgsInstance().remove(packageName);
    		saveInstalledPackageList(); 
    		
    		// 回调
    		if (observer != null) {
    			observer.packageDeleted(packageName, DELETE_SUCCEEDED);
    		}
    		
    		//发送广播
    		Intent intent = new Intent(ACTION_PACKAGE_UNINSTALL);
    		intent.putExtra(EXTRA_PKG_NAME, EXTRA_PKG_NAME);
    		mContext.sendBroadcast(intent);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
    	
    }
}
