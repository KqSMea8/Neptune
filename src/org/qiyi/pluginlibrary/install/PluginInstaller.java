package org.qiyi.pluginlibrary.install;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.pm.CMPackageInfo;
import org.qiyi.pluginlibrary.pm.CMPackageManager;
import org.qiyi.pluginlibrary.pm.CMPackageManagerImpl;
import org.qiyi.pluginlibrary.pm.PluginPackageInfoExt;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

//import org.qiyi.pluginnew.ActivityClassGenerator;

/**
 * app 安装接口。实际安装采用 {@link PluginInstallerService} 独立进程异步安装。<br>
 * <p/>
 * 插件支持的后缀名为 {@value #APK_SUFFIX}, 内置在 assets/pluginapp 目录下，以{@value #APK_SUFFIX}后缀命名，安装完后缀名也是 {@value #APK_SUFFIX}。<br>
 * <p/>
 * 由于android 2.2 以及一下对 asset文件的大小有1M限制。所以我们需要在编译脚本中对 aapt 增加一个 -0 {@value #APK_SUFFIX}参数，告诉aapt不要对{@value #APK_SUFFIX} 进行压缩处理。
 * 对于此问题的解释:http://ponystyle.com/blog/2010/03/26/dealing-with-asset-compression-in-android-apps/
 */
public class PluginInstaller {

    /**
     * TAG
     */
    public static final String TAG = "PluginInstaller";

    public static final String PLUGIN_PATH = "pluginapp";

    /**
     * 内置app的目录 assets/pluginapp
     */
    public static final String ASSETS_PATH = "pluginapp";

    public static final String APK_SUFFIX = ".apk";

    public static final String NATIVE_LIB_PATH = "lib";
    public static final String SO_SUFFIX = ".so";

    public static final String DEX_SUFFIX = ".dex";

    /**
     * shared preference file name.
     */
    public static final String SHARED_PREFERENCE_NAME = "pluginapp";

    /** {@link #installBuildinApps(Context) 只能调用一次，再次调用，直接返回} */
//    private static boolean sInstallBuildinAppsCalled = false;
    /**
     * receiver 只注册一次。
     */
    private static boolean sInstallerReceiverRegistered = false;

    /**
     * 安装列表，用于存储正在安装的 文件列表（packageName），安装完从中删除。
     */
    private static LinkedList<String> sInstallList = new LinkedList<String>();

    /**
     * 安装阶段本次需要安装的内置app列表（packageName）
     */
    private static ArrayList<String> sBuildinAppList = new ArrayList<String>();

    /**
     * 返回 pluginapp的根目录 /data/data/pluginapp/app_pluginapp
     *
     * @param context host的context
     * @return 插件跟路径
     */
    public static File getPluginappRootPath(Context context) {
        File repoDir = context.getDir(PLUGIN_PATH, 0);

        if (!repoDir.exists()) {
            repoDir.mkdir();
        }
        PluginDebugLog.log(TAG, "getPluginappRootPath:" + repoDir);
        return repoDir;
    }

//    /**
//     * Help to generate folder for single dex file for dexmaker
//     * 
//     * @param parentFolder parent folder name
//     * @param componentName component name like activity etc...
//     * @return file represent xxx.dex
//     */
//	public static File getProxyComponentDexPath(File parentFolder, String componentName) {
//		File folder = new File(parentFolder.getAbsolutePath() + "/component/");
//		folder.mkdirs();
//		String suffix = ".dex";
//		if (android.os.Build.VERSION.SDK_INT < 11) {
//			suffix = ".jar";
//		}
//		File savePath = new File(folder, String.format("%s-%d%s", componentName,
//				ActivityClassGenerator.VERSION_CODE, suffix));
//		return savePath;
//	}

    /**
     * 安装内置在 assets/pluginapp 目录下的 apk
     *
     * @param context
     * @param info    插件方案版本号
     */
    public synchronized static void installBuildinApps(
            final String packageName, final Context context, final PluginPackageInfoExt info) {
        registerInstallderReceiver(context);
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                AssetManager am = context.getAssets();
                try {
                    String files[] = am.list(ASSETS_PATH);
                    String temp_file = "";
                    if (packageName != null) {
                        temp_file = packageName + APK_SUFFIX;
                    }
                    for (String file : files) {
                        if (!file.endsWith(APK_SUFFIX)
                                || (!TextUtils.isEmpty(packageName) && !TextUtils.equals(file, temp_file))) {
                            // 如果外面传递的packagename 为空则全部安装
                            continue;
                        }
                        PluginDebugLog.log("plugin", "file:" + file);
                        installBuildinApp(context, ASSETS_PATH + "/" + file, info);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * @param context
     * @param assetsPath
     * @param info       插件信息
     * @return 需要安装 返回 true，不需要安装 返回 false.
     */
    private static boolean installBuildinApp(
            Context context, String assetsPath, PluginPackageInfoExt info) {
        int start = assetsPath.lastIndexOf("/");
        int end = assetsPath.lastIndexOf(PluginInstaller.APK_SUFFIX);
        String mapPackagename = assetsPath.substring(start + 1, end);

        PluginPackageInfoExt infoExt = null;
        if (info == null && !TextUtils.isEmpty(mapPackagename)) {
            infoExt = new PluginPackageInfoExt();
            infoExt.packageName = mapPackagename;
            infoExt.mFileSourceType = CMPackageManager.PLUGIN_SOURCE_ASSETS;
            infoExt.mPluginInstallMethod = CMPackageManager.PLUGIN_METHOD_INSTR;
        } else {
            infoExt = info;
        }

        if (infoExt != null) {
            PluginDebugLog.log(TAG, "installBuildinApp" + infoExt.toString());
        }

        startInstall(context, CMPackageManager.SCHEME_ASSETS + assetsPath, infoExt);
        return true;
    }

    /**
     * 调用 {@link PluginInstallerService} 进行实际的安装过程。采用独立进程异步操作。
     *
     * @param context
     * @param filePath   支持两种scheme {@link CMPackageManager#SCHEME_ASSETS} 和 {@link CMPackageManager#SCHEME_FILE}
     * @param pluginInfo 插件信息
     */
    private static void startInstall(Context context, String filePath, PluginPackageInfoExt pluginInfo) {
        /*
         * 获取packagename
         * 1、内置app，要求必须以 packagename.apk 命名，处于效率考虑。
         * 2、外部文件的安装，直接从file中获取packagename, 消耗100ms级别，可以容忍。
         */
        String packageName = null;

        boolean isBuildin = false;

        if (pluginInfo != null) {
            PluginDebugLog.log(TAG, "startInstall with file path: " + filePath
                    + " and plugin info: " + pluginInfo.toString());
        }

        if (filePath.startsWith(CMPackageManager.SCHEME_ASSETS)) {
            int start = filePath.lastIndexOf("/");
            int end = filePath.lastIndexOf(PluginInstaller.APK_SUFFIX);
            packageName = filePath.substring(start + 1, end);
            isBuildin = true;

        } else if (filePath.startsWith(CMPackageManager.SCHEME_FILE)) {
            PackageManager pm = context.getPackageManager();
            String apkFilePath = filePath.substring(CMPackageManager.SCHEME_FILE.length());
            PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkFilePath, 0);
            if (pkgInfo != null) {
                packageName = pkgInfo.packageName;
            }
        }

        if (packageName != null) {
            add2InstallList(packageName); // 添加到安装列表中
            if (isBuildin) {
                PluginDebugLog.log(TAG, "add " + packageName + " in BuildinAppList");
                sBuildinAppList.add(packageName); // 添加到内置app安装列表中
            }
        }
        if (pluginInfo == null) {
            PluginDebugLog.log(TAG, "startInstall pluginInfo is null, just return!");
            return;
        }

        try {
            Intent intent = new Intent(PluginInstallerService.ACTION_INSTALL);
            intent.setClass(context, PluginInstallerService.class);
            intent.putExtra(CMPackageManager.EXTRA_SRC_FILE, filePath);
            intent.putExtra(CMPackageManager.EXTRA_PLUGIN_INFO, (Parcelable) pluginInfo);

            context.startService(intent);
        }catch (Exception e){
            //QOS_JAVA_211
            //Unable to launch app com.qiyi.video/10126 for service Intent
            // { act=com.qiyi.plugin.installed cmp=com.qiyi.video/org.qiyi.pluginlibrary.install.PluginInstallerService }:
            // user 0 is restricted
            e.printStackTrace();
        }
    }

    /**
     * 安装一个 apk file 文件. 用于安装比如下载后的文件，或者从sdcard安装。安装过程采用独立进程异步安装。
     * 安装完会有 {@link CMPackageManager＃ACTION_PACKAGE_INSTALLED} broadcast。
     *
     * @param context
     * @param filePath   apk 文件目录 比如  /sdcard/xxxx.apk
     * @param pluginInfo 插件信息
     */
    public static void installApkFile(Context context, String filePath,
                                      PluginPackageInfoExt pluginInfo) {
        if (TextUtils.isEmpty(filePath)) {
            PluginDebugLog.log(TAG, "filePath is empty and installApkFile return!");
            return;
        }

        registerInstallderReceiver(context);
        if (filePath.endsWith(SO_SUFFIX)) {
            startInstall(context, CMPackageManager.SCHEME_SO + filePath, pluginInfo);
        } else if (filePath.endsWith(DEX_SUFFIX)) {
            startInstall(context, CMPackageManager.SCHEME_DEX + filePath, pluginInfo);
        } else {
            startInstall(context, CMPackageManager.SCHEME_FILE + filePath, pluginInfo);
        }
    }

    /**
     * 返回已安装的应用列表。临时函数。可能为空，安装内置app还没有执行完毕。需要监听安装broadcast来更新安装列表。
     * <p/>
     * Deprecated, use {@link CMPackageManager#getInstalledApps()}
     *
     * @param context
     * @return 安装apk文件的 列表
     */
    @Deprecated
    public static ArrayList<File> getInstalledApps(Context context) {
        List<CMPackageInfo> pkgList = CMPackageManagerImpl.getInstance(context).getInstalledApps();

        ArrayList<File> result = new ArrayList<File>();
        for (CMPackageInfo pkg : pkgList) {
            String filePath = pkg.srcApkPath;
            result.add(new File(filePath));
        }

        return result;
    }

    /**
     * 获取安装插件的apk文件目录
     *
     * @param context     host的application context
     * @param packageName 包名
     * @return File
     */
    public static File getInstalledApkFile(Context context, String packageName) {
        CMPackageInfo info = CMPackageManagerImpl.getInstance(context).getPackageInfo(packageName);

        if (info != null && !TextUtils.isEmpty(info.srcApkPath)) {
            return new File(info.srcApkPath);
        } else {
            return null;
        }
    }

    public static File getInstalledDexFile(Context context, String packageName) {
        File dataDir = null;
        dataDir = new File(PluginInstaller.getPluginappRootPath(context), packageName);

        File dexFile = new File(dataDir, packageName + ".dex");
        return dexFile;
    }

    private static BroadcastReceiver sApkInstallerReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CMPackageManager.ACTION_PACKAGE_INSTALLED.equals(action)) {
                String pkgName = intent.getStringExtra(CMPackageManager.EXTRA_PKG_NAME);

                handleApkInstalled(context, pkgName);
            } else if (CMPackageManager.ACTION_PACKAGE_INSTALLFAIL.equals(action)) {
                String pkgName = intent.getStringExtra(CMPackageManager.EXTRA_PKG_NAME);
                if (!TextUtils.isEmpty(pkgName)) {
                    sInstallList.remove(pkgName);
                }
            }
        }
    };


    private static void registerInstallderReceiver(Context context) {
        if (sInstallerReceiverRegistered) {
            // 已经注册过就不再注册
            return;
        }
        sInstallerReceiverRegistered = true;
        Context appcontext = context.getApplicationContext();

        IntentFilter filter = new IntentFilter();
        filter.addAction(CMPackageManager.ACTION_PACKAGE_INSTALLED);
        filter.addAction(CMPackageManager.ACTION_PACKAGE_INSTALLFAIL);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);

        appcontext.registerReceiver(sApkInstallerReceiver, filter);
    }

    /**
     * 增加到安装列表
     *
     * @param packagename
     */
    private synchronized static void add2InstallList(String packagename) {
        PluginDebugLog.log(TAG, "add2InstallList with" + packagename);
        if (sInstallList.contains(packagename)) {
            return;
        }

        sInstallList.add(packagename);
    }


    private synchronized static void handleApkInstalled(Context context, String packageName) {

        sInstallList.remove(packageName); // 从安装列表中删除

        // 检查内置app是否安装完成
//        if (!sInstallBuildinAppsFinished) {
//            if (sInstallList.isEmpty()) {
//                setInstallBuildinAppsFinished(context, true);
//            } else {
//                boolean hasAssetsFileInstalling = false;
//                for (String pkg : sInstallList) {
//                    if (sBuildinAppList.contains(pkg)) {
//                        hasAssetsFileInstalling = true;
//                        break;
//                    }
//                }
//                if (!hasAssetsFileInstalling) {
//                    setInstallBuildinAppsFinished(context, true);
//                }
//            }
//        }
    }


    /**
     * 内置app安装处理逻辑完成，有可能是检查不需要安装，有可能是实际安装完成。
     * @param context
     * @param writeVersionCode 是否是真的发生了实际安装，而不是检查完毕不需要安装，如果版本号不一致，也需要记录。
     */
//    private static void setInstallBuildinAppsFinished(Context context, boolean writeVersionCode) {
//        sInstallBuildinAppsFinished =  true;
//        
//        if (writeVersionCode) {
//            // 获取 hostapp verison code
//            int hostVersionCode = -1;
//            try {
//                hostVersionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
//            } catch (NameNotFoundException e1) {
//                e1.printStackTrace();
//            }
//            // 保存当前的 hostapp verisoncode // 使用hostapp 默认 sharedpref，减少初始化开销。
//            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);//context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
//            Editor editor = sp.edit();
//            editor.putInt(SP_HOSTAPP_VERSIONCODE_FOR_INSTALL, hostVersionCode);
//            editor.commit();
//        }
//    }

    /**
     * delete the apk,so.dex
     *
     * @param context
     * @param packageName
     */
    public static void deleteInstallerPackage(Context context, String packageName) {

        File dataDir = null;
        dataDir = new File(PluginInstaller.getPluginappRootPath(context), packageName);
        File apkPath = getInstalledApkFile(context, packageName);
        File dexPath = getInstalledDexFile(context, packageName);
        if (apkPath != null) {
            apkPath.delete();
        }

        if (dexPath != null) {
            dexPath.delete();
        }

        File lib = new File(dataDir, "lib");

        if (lib != null) {
            try {
                Util.deleteDirectory(lib);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * delete the database,sharedPreference,file and cache.
     *
     * @param context
     * @param packageName
     */
    public static void deletePluginData(Context context, String packageName) {
        File dataDir = null;
        dataDir = new File(PluginInstaller.getPluginappRootPath(context), packageName);
        File db = new File(dataDir, "databases");
        File sharedPreference = new File(dataDir, "shared_prefs");
        File file = new File(dataDir, "files");
        File cache = new File(dataDir, "cache");

        if (db != null) {
            try {
                Util.deleteDirectory(db);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (sharedPreference != null) {
            try {
                Util.deleteDirectory(sharedPreference);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (file != null) {
            try {
                Util.deleteDirectory(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (cache != null) {
            try {
                Util.deleteDirectory(cache);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 查看某个app是否正在安装
     *
     * @param packageName
     */
    public static synchronized boolean isInstalling(String packageName) {
        return sInstallList.contains(packageName);
    }

    /**
     * Compare left and right version with style a.b.c...
     *
     * @param leftVersionName
     * @param rightVersionName
     * @return return positive if left > right, 0 for left equals right and negative for left <
     * right
     */
    public static int comparePluginVersion(String leftVersionName, String rightVersionName) {
        if (TextUtils.equals(leftVersionName, rightVersionName)) {
            return 0;
        }
        if (TextUtils.isEmpty(leftVersionName)) {
            return -1;
        } else if (TextUtils.isEmpty(rightVersionName)) {
            return 1;
        }
        String[] lVersions = leftVersionName.split("\\.");
        String[] rVersions = rightVersionName.split("\\.");
        if (lVersions == null || lVersions.length == 0) {
            return -1;
        } else if (rVersions == null || rVersions.length == 0) {
            return 1;
        }
        try {
            for (int i = 0; i < lVersions.length; i++) {
                if (rVersions.length < i + 1) {
                    return 1;
                }
                if (Integer.valueOf(lVersions[i]) != Integer.valueOf(rVersions[i])) {
                    return Integer.valueOf(lVersions[i]) - Integer.valueOf(rVersions[i]);
                } else {
                    continue;
                }
            }
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        }
        return -1;
    }
}
