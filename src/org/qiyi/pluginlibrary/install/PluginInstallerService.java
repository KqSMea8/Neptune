package org.qiyi.pluginlibrary.install;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.qiyi.pluginlibrary.ErrorType.ErrorType;
import org.qiyi.pluginlibrary.pm.CMPackageManager;
import org.qiyi.pluginlibrary.pm.PluginPackageInfoExt;
import org.qiyi.pluginlibrary.utils.JavaCalls;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.Util;
//import org.qiyi.pluginnew.ActivityClassGenerator;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import dalvik.system.DexClassLoader;

/**
 * apk 安装service，从srcfile安装到destfile，并且安装so，以及dexopt。
 * 因为android4.1 以下系统dexopt会导致线程hang住无法返回，所以我们放到了一个独立进程，减小概率。
 * dexopt系统bug：http://code.google.com/p/android/issues/detail?id=14962
 * 
 */
public class PluginInstallerService extends IntentService {
    
    /** TAG */
    public static final String TAG = PluginDebugLog.TAG;

    public static final String ACTION_INSTALL = "com.qiyi.plugin.installed";
    
    /** apk 中 lib 目录的前缀标示。比如 lib/x86/libshare_v2.so */
    public static String APK_LIB_DIR_PREFIX = "lib/";
    /** lib中so后缀*/
    public static final String APK_LIB_SUFFIX = ".so";
    /** lib目录的 cpu abi 其实位置。比如 x86 的起始位置 */
    public static int APK_LIB_CPUABI_OFFSITE = APK_LIB_DIR_PREFIX.length();

    public PluginInstallerService() {
        super(PluginInstallerService.class.getSimpleName());
    } 

    /**
     * @param name
     */
    public PluginInstallerService(String name) {
        super(name);
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
    	PluginDebugLog.log(TAG, "pluginInstallerService:onStart");
        super.onStart(intent, startId);
        
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 退出时结束进程
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    	if(intent == null){
    		PluginDebugLog.log(TAG, "onHandleIntent intent is null");
    		return;
    	}
    		
        String action = intent.getAction();
        PluginDebugLog.log(TAG, "pluginInstallerService:action"+action);
        if (action == null) {
            return;
        }
        
        if (action.equals(ACTION_INSTALL)) {//插件安装
            String srcFile = intent.getStringExtra(CMPackageManager.EXTRA_SRC_FILE);
            PluginPackageInfoExt pluginInfo = intent
					.getParcelableExtra(CMPackageManager.EXTRA_PLUGIN_INFO);
            //String destFile = intent.getStringExtra(EXTRA_DEST_FILE);
            //String pkgName = intent.getStringExtra(EXTRA_PKG_NAME);
            PluginDebugLog.log(TAG, "pluginInstallerService:srcFile"+srcFile);
            handleInstall(srcFile, pluginInfo);
        }
    }
    
    
	private void handleInstall(String srcFile, PluginPackageInfoExt info) {
		if (null == info) {
			PluginDebugLog.log(TAG, "Install srcFile:" + srcFile + " return due to info is null!");			
			return;
		}

		PluginDebugLog.log(TAG, "srcFile:" + srcFile);
		if (srcFile.startsWith(CMPackageManager.SCHEME_ASSETS)) {
			info.mSuffixType = CMPackageManager.PLUGIN_FILE_APK;
			installBuildinApk(srcFile, info);
		} else if (srcFile.startsWith(CMPackageManager.SCHEME_FILE)) {
			info.mSuffixType = CMPackageManager.PLUGIN_FILE_APK;
			installAPKFile(srcFile, info);
		} else if (srcFile.startsWith(CMPackageManager.SCHEME_SO)) {
			info.mSuffixType = CMPackageManager.PLUGIN_FILE_SO;
			String soFilePath = srcFile.substring(CMPackageManager.SCHEME_SO.length());
			
			int start = srcFile.lastIndexOf("/");
			int end = srcFile.lastIndexOf(PluginInstaller.SO_SUFFIX);
			String fileName = srcFile.substring(start + 1, end);
			
			boolean flag = Util.installNativeLibrary(soFilePath, PluginInstaller
					.getPluginappRootPath(this).getAbsolutePath() + File.separator + fileName);
			if (flag) {
				setInstallSuccess(fileName, srcFile, null, info);
			} else {
				setInstallFail(srcFile, ErrorType.ERROR_CLIENT_COPY_ERROR, info);
			}
		}

	}
    
    /**
     * 安装内置的apk
     * @param assetsPath assets 目录
     */
    private void installBuildinApk(String assetsPathWithScheme, PluginPackageInfoExt info) {
        String assetsPath = assetsPathWithScheme.substring(CMPackageManager.SCHEME_ASSETS.length());
        PluginDebugLog.log(TAG, "pluginInstallerService:assetsPath"+assetsPath);
        // 先把 asset 拷贝到临时文件。
        InputStream is = null;
        try {
            is = this.getAssets().open(assetsPath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        doInstall(is, assetsPathWithScheme, info);
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 安装一个普通的文件 apk，用于外部或者下载后的apk安装。
     * @param apkFilePath 文件绝对目录
     */
    private void installAPKFile(String apkFilePathWithScheme, PluginPackageInfoExt info) {
        
    	PluginDebugLog.log(TAG, "installAPKFile:----------------------");
        String apkFilePath = apkFilePathWithScheme.substring(CMPackageManager.SCHEME_FILE.length());
        
        File source = new File(apkFilePath);
        InputStream is = null;
        try {
            is = new FileInputStream(source);
        } catch (FileNotFoundException e) {
        	setInstallFail(apkFilePathWithScheme, ErrorType.ERROR_CLIENT_FILE_NOTFOUND, info);
            e.printStackTrace();
        }
        PluginDebugLog.log(TAG, is == null?"判断流是否为空:true":"判断流是否为空:false-------------------");
        doInstall(is, apkFilePathWithScheme, info);
        
        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
        	setInstallFail(apkFilePathWithScheme, ErrorType.ERROR_CLIENT_CLOSE_IOEXCEPTION, info);
            e.printStackTrace();
        }
    }
    
    
	private String doInstall(InputStream is, String srcPathWithScheme, PluginPackageInfoExt info) {
        PluginDebugLog.log(TAG, "--- doInstall : " + srcPathWithScheme);
        PluginDebugLog.log(TAG, "pluginInstallerService:srcPathWithScheme"+srcPathWithScheme+"-------------");
        if (is == null || srcPathWithScheme == null) {
            return null;
        }
        File tempFile = new File(PluginInstaller.getPluginappRootPath(this), System.currentTimeMillis() + "");
        boolean result = Util.copyToFile(is, tempFile);
        PluginDebugLog.log(TAG, "pluginInstallerService:result"+result+"+++++++++++++");
        if (!result) {
            tempFile.delete();
            setInstallFail(srcPathWithScheme, ErrorType.ERROR_CLIENT_COPY_ERROR, info);
            return null;
        }
    	
        PackageManager pm = this.getPackageManager();     
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(tempFile.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
        if (pkgInfo == null) {
            tempFile.delete();
            setInstallFail(srcPathWithScheme, ErrorType.ERROR_CLIENT_PARSE_ERROR, info);
            return null;
        }
        
        
        String packageName = pkgInfo.packageName;
        
        // 校验签名  
        //插件内部不需要在进行校验
//        boolean isSignatureValid = verifySignature(packageName, tempFile.getAbsolutePath());
//        PluginDebugLog.log(TAG, "pluginInstallerService:isSignatureValid"+isSignatureValid);
//        if (!isSignatureValid) {
//            setInstallFail(srcPathWithScheme, ErrorType.ERROR_CLIENT_SIGNATURE_NOT_MATCH);
//            return null;
//        }
        
        // 如果是内置app，检查文件名是否以包名命名，处于效率原因，要求内置app必须以包名命名.
        if (srcPathWithScheme.startsWith(CMPackageManager.SCHEME_ASSETS)) {
            int start = srcPathWithScheme.lastIndexOf("/");
            int end = srcPathWithScheme.lastIndexOf(PluginInstaller.APK_SUFFIX);
            String fileName = srcPathWithScheme.substring(start + 1, end);
            info.pluginTotalSize = tempFile.length(); //如果是内置的apk，需要自己获取大小，并存储，
            if (!packageName.equals(fileName)) {
                tempFile.delete();
//                throw new RuntimeException(srcPathWithScheme + " must be named with it's package name : "
//                        + packageName + PluginInstaller.APK_SUFFIX);
                return null;
            }
        }
        
        File destFile = getPreferedInstallLocation(pkgInfo);
        if (destFile.exists()) {
            destFile.delete();
        }
        
        // 生成安装文件
        if (tempFile.getParent().equals(destFile.getParent())) {
            // 目标文件和临时文件在同一目录下 
            tempFile.renameTo(destFile);
        } else {
            // 拷贝到其他目录，比如安装到 sdcard
            try {
                InputStream tempIs = new FileInputStream(tempFile);
                boolean tempResult = Util.copyToFile(tempIs, destFile);
                tempIs.close();
                tempFile.delete(); // 删除临时文件
                if (!tempResult) {
                    setInstallFail(srcPathWithScheme, ErrorType.ERROR_CLIENT_COPY_ERROR, info);
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                tempFile.delete();
                setInstallFail(srcPathWithScheme, ErrorType.ERROR_CLIENT_COPY_ERROR, info);
                return null;
            } 
        }
        PluginDebugLog.log(TAG, "pluginInstallerService");
        File pkgDir = new File(PluginInstaller.getPluginappRootPath(this), packageName);

        if(!pkgDir.exists())
        	pkgDir.mkdir();
        File libDir = new File(pkgDir, PluginInstaller.NATIVE_LIB_PATH);
        libDir.mkdirs();
        Util.installNativeLibrary(destFile.getAbsolutePath(), libDir.getAbsolutePath());
    
        //dexopt
        installDex(destFile.getAbsolutePath(), packageName);
		setInstallSuccess(packageName, srcPathWithScheme, destFile.getAbsolutePath(), info);
        return packageName;
    }
    
    /**
     * 发送安装失败的广播
     * 
     * @param srcPathWithScheme
     *            安装文件路径
     * @param failReason
     *            失败原因
     */
	private void setInstallFail(String srcPathWithScheme, int failReason, PluginPackageInfoExt info) {
		Intent intent = new Intent(CMPackageManager.ACTION_PACKAGE_INSTALLFAIL);
		intent.setPackage(getPackageName());
		intent.putExtra(CMPackageManager.EXTRA_SRC_FILE, srcPathWithScheme);// 同时返回安装前的安装文件目录。
		intent.putExtra(ErrorType.ERROR_RESON, failReason);
		intent.putExtra(CMPackageManager.EXTRA_PLUGIN_INFO, (Parcelable)info);// 同时返回APK的插件信息
		sendBroadcast(intent);
	}

	private void setInstallSuccess(String pkgName, String srcPathWithScheme, String destPath,
			PluginPackageInfoExt info) {
		Intent intent = new Intent(CMPackageManager.ACTION_PACKAGE_INSTALLED);
		intent.setPackage(getPackageName());
		intent.putExtra(CMPackageManager.EXTRA_PKG_NAME, pkgName);
		intent.putExtra(CMPackageManager.EXTRA_SRC_FILE, srcPathWithScheme);// 同时返回安装前的安装文件目录。
		intent.putExtra(CMPackageManager.EXTRA_DEST_FILE, destPath);// 同时返回安装前的安装文件目录。
		intent.putExtra(CMPackageManager.EXTRA_PLUGIN_INFO, (Parcelable)info);// 同时返回APK的插件信息
		sendBroadcast(intent);
	}
    
    
    /**
     * 初始化 dex，因为第一次loaddex，如果放hostapp 进程，有可能会导致hang住(参考类的说明)。所以在安装阶段独立进程中执行。
     *
     * @param apkFile
     * @param packageName
     */
    private void installDex(String apkFile, String packageName) {
    	
    	File pkgDir = new File(PluginInstaller.getPluginappRootPath(this), packageName);
        
        ClassLoader classloader = new DexClassLoader(apkFile, pkgDir.getAbsolutePath(), null, getClassLoader()); // 构造函数会执行loaddex底层函数。
        
        //android 2.3以及以上会执行dexopt，2.2以及下不会执行。需要额外主动load一次类才可以
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
            try {
                classloader.loadClass(packageName + ".R");
            } catch (ClassNotFoundException e) {
                //e.printStackTrace();
            	
            }
        }
    }

//    /**
//     * Help to create all activity's wrapper class.dex
//     * 
//     * @param pkgInfo
//     */
//	private void createPluginActivityProxyDexes(PackageInfo pkgInfo) {
//		File parentData = new File(PluginInstaller.getPluginappRootPath(this), pkgInfo.packageName);
//		if (pkgInfo.activities == null) {
//			Log.d("TAG", "pkgInfo.activities is null pkgName: " + pkgInfo.packageName);
//			return;
//		}
//		File tempFile = null;
//		for (ActivityInfo info : pkgInfo.activities) {
//			tempFile = PluginInstaller.getProxyComponentDexPath(parentData, info.name);
//			ActivityClassGenerator.createProxyDex(info.packageName, info.name, tempFile);
//			if (tempFile.exists()) {
//				installDex(tempFile.getAbsolutePath(), pkgInfo.packageName);
//			}
//			tempFile = null;
//		}
//		parentData = null;
//	}

    /**
     * 获取安装路径，可能是外部 sdcard或者internal data dir
     * 
     * @param PluginPackageInfo
     *            .installLocation 是否优先安装到外部存储器
     * @return 返回插件安装位置，非空
     */
    @SuppressLint("NewApi")
	private File getPreferedInstallLocation(PackageInfo pkgInfo) {
        
        boolean preferExternal = false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            int installLocation = (Integer) (JavaCalls.getField(pkgInfo, "installLocation"));
            
            PluginDebugLog.log("plugin", "installLocation:"+installLocation);
            
            final int INSTALL_LOCATION_PREFER_EXTERNAL = 2; // see PackageInfo.INSTALL_LOCATION_PREFER_EXTERNAL
            
            if (installLocation == INSTALL_LOCATION_PREFER_EXTERNAL) {
                preferExternal = true;
            }
        } else {
            // android 2.1 以及以下不支持安装到sdcard
            preferExternal = false;
        }
        
        // 查看外部存储器是否可用
        if(preferExternal) {
            String state = Environment.getExternalStorageState();
            if (!Environment.MEDIA_MOUNTED.equals(state)) { // 不可用
                preferExternal = false;
            }
        }
        
        File destFile = null;
        if (!preferExternal) { 
            // 默认安装到 internal data dir
            destFile = new File(PluginInstaller.getPluginappRootPath(this), pkgInfo.packageName + PluginInstaller.APK_SUFFIX);
            PluginDebugLog.log("plugin", "默认安装到internal data dir:"+destFile.getPath());
        } else {
            // 安装到外部存储器
            destFile = new File(getExternalFilesDir(PluginInstaller.PLUGIN_PATH), pkgInfo.packageName + PluginInstaller.APK_SUFFIX);
            PluginDebugLog.log("plugin", "安装到外部存储器："+destFile.getPath());
        }
        return destFile;
    }

}
