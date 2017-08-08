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

import org.qiyi.pluginlibrary.constant.IIntentConstant;
import org.qiyi.pluginlibrary.pm.PluginLiteInfo;
import org.qiyi.pluginlibrary.pm.PluginPackageManager;
import org.qiyi.pluginlibrary.pm.PluginPackageManagerNative;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * 负责插件插件安装,卸载，数据删除
 */
public class PluginInstaller {
    public static final String TAG = "PluginInstaller";

    public static final String PLUGIN_PATH = "pluginapp";
    public static final String APK_SUFFIX = ".apk";
    public static final String NATIVE_LIB_PATH = "lib";
    public static final String SO_SUFFIX = ".so";
    public static final String DEX_SUFFIX = ".dex";
    /**标识插件安装广播是否注册，只注册一次*/
    private static boolean sInstallerReceiverRegistered = false;
    /**存放正在安装的插件列表*/
    private static LinkedList<String> sInstallList = new LinkedList<String>();
    /**内置插件列表*/
    private static ArrayList<String> sBuildinAppList = new ArrayList<String>();
    /**获取插件安装的根目录*/
    public static File getPluginappRootPath(Context context) {
        File repoDir = context.getDir(PLUGIN_PATH, 0);
        if (!repoDir.exists()) {
            repoDir.mkdir();
        }
        PluginDebugLog.installFormatLog(TAG, "getPluginappRootPath:%s" , repoDir);
        return repoDir;
    }

    /**
     * 安装内置在 assets/pluginapp 目录下的 apk
     *
     * @param context
     * @param info 插件方案版本号
     */
    public synchronized static void installBuildinApps(final Context context,
                                                       final PluginLiteInfo info) {
        registerInstallderReceiver(context);
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                AssetManager am = context.getAssets();
                try {
                    String files[] = am.list(PLUGIN_PATH);
                    String temp_file = "";
                    if (info.packageName != null) {
                        temp_file = info.packageName + APK_SUFFIX;
                    }
                    for (String file : files) {
                        if (!file.endsWith(APK_SUFFIX) || (!TextUtils.isEmpty(info.packageName) && !TextUtils.equals(file, temp_file))) {
                            // 如果外面传递的packagename 为空则全部安装
                            continue;
                        }
                        PluginDebugLog.installFormatLog(TAG,"InstallBuildInPlugin:%s", info.packageName);
                        String buildInPath = PluginPackageManager.SCHEME_ASSETS + PLUGIN_PATH + "/" + file;
                        startInstall(context, buildInPath, info);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * 调用 {@link PluginInstallerService} 进行实际的安装过程。采用独立进程异步操作。
     *
     * @param context
     * @param filePath 支持两种scheme {@link PluginPackageManager#SCHEME_ASSETS} 和
     * {@link PluginPackageManager#SCHEME_FILE}
     * @param info 插件信息
     */
    private static void startInstall(Context context, String filePath, PluginLiteInfo info) {
        /*
         * 获取packagename 1、内置app，要求必须以 packagename.apk 命名，处于效率考虑。
         * 2、外部文件的安装，直接从file中获取packagename, 消耗100ms级别，可以容忍。
         */
        boolean isBuildin = false;
        PluginDebugLog.installFormatLog(TAG, "startInstall with file path:%s and plugin pkgName:%s"
                ,filePath , info.packageName);

        if (filePath.startsWith(PluginPackageManager.SCHEME_ASSETS)) {
            isBuildin = true;

        }

        if (!TextUtils.isEmpty(info.packageName)) {
            add2InstallList(info.packageName); // 添加到安装列表中
            if (isBuildin) {
                PluginDebugLog.installFormatLog(TAG, "add %s in buildInAppList" ,info.packageName);
                sBuildinAppList.add(info.packageName); // 添加到内置app安装列表中
            }
        }else{
            PluginDebugLog.installLog(TAG, "startInstall PluginLiteInfo.packagename is null, just return!");
            return;
        }

        try {
            Intent intent = new Intent(PluginInstallerService.ACTION_INSTALL);
            intent.setClass(context, PluginInstallerService.class);
            intent.putExtra(IIntentConstant.EXTRA_SRC_FILE, filePath);
            intent.putExtra(IIntentConstant.EXTRA_PLUGIN_INFO, (Parcelable) info);

            context.startService(intent);
        } catch (Exception e) {
            // QOS_JAVA_211
            // Unable to launch app com.qiyi.video/10126 for service Intent
            // { act=com.qiyi.plugin.installed
            // cmp=com.qiyi.video/org.qiyi.pluginlibrary.install.PluginInstallerService
            // }:
            // user 0 is restricted
            e.printStackTrace();
        }
    }

    /**
     * 安装一个 apk file 文件. 用于安装比如下载后的文件，或者从sdcard安装。安装过程采用独立进程异步安装。 安装完会有
     * {@link PluginPackageManager ＃ACTION_PACKAGE_INSTALLED} broadcast。
     *
     * @param context
     * @param filePath apk 文件目录 比如 /sdcard/xxxx.apk
     * @param pluginInfo 插件信息
     */
    public static void installApkFile(Context context, String filePath, PluginLiteInfo pluginInfo) {
        if (TextUtils.isEmpty(filePath)) {
            PluginDebugLog.installLog(TAG, "filePath is empty and installApkFile return!");
            return;
        }

        registerInstallderReceiver(context);
        if (filePath.endsWith(SO_SUFFIX)) {
            startInstall(context, PluginPackageManager.SCHEME_SO + filePath, pluginInfo);
        } else if (filePath.endsWith(DEX_SUFFIX)) {
            startInstall(context, PluginPackageManager.SCHEME_DEX + filePath, pluginInfo);
        } else {
            startInstall(context, PluginPackageManager.SCHEME_FILE + filePath, pluginInfo);
        }
    }

    /**
     * 获取安装插件的apk文件目录
     *
     * @param context host的application context
     * @param packageName 包名
     * @return File
     */
    public static File getInstalledApkFile(Context context, String packageName) {
        PluginLiteInfo info = PluginPackageManagerNative.getInstance(context).getPackageInfo(packageName);

        if (info != null && !TextUtils.isEmpty(info.srcApkPath)) {
            return new File(info.srcApkPath);
        } else {
            return null;
        }
    }

    /**
     * 获取安装的插件的dex目录
     * @param context
     * @param packageName
     * @return
     */
    public static File getInstalledDexFile(Context context, String packageName) {
        File dataDir = null;
        dataDir = new File(PluginInstaller.getPluginappRootPath(context), packageName);

        File dexFile = new File(dataDir, packageName + ".dex");
        return dexFile;
    }

    private static BroadcastReceiver sApkInstallerReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String pkgName = intent.getStringExtra(IIntentConstant.EXTRA_PKG_NAME);
                if(!TextUtils.isEmpty(pkgName)){
                    PluginDebugLog.installFormatLog(TAG,"install success and remove pkg:%s",pkgName);
                    sInstallList.remove(pkgName);
                }
            } catch (Exception e) {
                e.printStackTrace();
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
        filter.addAction(PluginPackageManager.ACTION_PACKAGE_INSTALLED);
        filter.addAction(PluginPackageManager.ACTION_PACKAGE_INSTALLFAIL);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        appcontext.registerReceiver(sApkInstallerReceiver, filter);
    }

    /**
     * 增加到安装列表
     *
     * @param packagename
     */
    private synchronized static void add2InstallList(String packagename) {
        PluginDebugLog.installFormatLog(TAG, "add2InstallList with %s" ,packagename);
        if (sInstallList.contains(packagename)) {
            return;
        }
        sInstallList.add(packagename);
    }




    /**
     * 删除已经安装插件的apk,dex,so库等文件
     *
     * @param context
     * @param packageName
     */
    public static void deleteInstallerPackage(
            Context context, String apkPath, String packageName) {
        PluginDebugLog.installFormatLog(TAG,"deleteInstallerPackage:%s",packageName);
        if (context == null || TextUtils.isEmpty(apkPath) || TextUtils.isEmpty(packageName)) {
            return;
        }

        File dataDir = new File(PluginInstaller.getPluginappRootPath(context), packageName);
        File apk = new File(apkPath);
        File dexPath = getInstalledDexFile(context, packageName);
        if (apk.exists()) {
            boolean result = apk.delete();
            if(result){
                PluginDebugLog.installFormatLog(TAG,"deleteInstallerPackage apk  %s succcess!",packageName);
            }else{
                PluginDebugLog.installFormatLog(TAG,"deleteInstallerPackage apk  %s fail!",packageName);
            }

        }

        if (dexPath.exists()) {
            boolean result = dexPath.delete();
            if(result){
                PluginDebugLog.installFormatLog(TAG,"deleteInstallerPackage dex  %s succcess!",packageName);
            }else{
                PluginDebugLog.installFormatLog(TAG,"deleteInstallerPackage dex  %s fail!",packageName);
            }
        }

        File lib = new File(dataDir, "lib");

        if (lib != null) {
            try {
                Util.deleteDirectory(lib);
                PluginDebugLog.installFormatLog(TAG,"deleteInstallerPackage lib %s success!",packageName);
            } catch (IOException e) {
                PluginDebugLog.installFormatLog(TAG,"deleteInstallerPackage lib %s fail!",packageName);
                e.printStackTrace();
            }
        }
    }

    /**
     * 删除已经安装插件的数据目录(包括db,sp,cache和files目录)
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
                PluginDebugLog.installFormatLog(TAG,"deletePluginData db %s success!",packageName);
            } catch (IOException e) {
                PluginDebugLog.installFormatLog(TAG,"deletePluginData db %s fail!",packageName);
                e.printStackTrace();
            }
        }

        if (sharedPreference != null) {
            try {
                Util.deleteDirectory(sharedPreference);
                PluginDebugLog.installFormatLog(TAG,"deletePluginData sp %s success!",packageName);
            } catch (IOException e) {
                PluginDebugLog.installFormatLog(TAG,"deletePluginData sp %s fail!",packageName);
                e.printStackTrace();
            }
        }

        if (file != null) {
            try {
                Util.deleteDirectory(file);
                PluginDebugLog.installFormatLog(TAG,"deletePluginData file %s success!",packageName);
            } catch (IOException e) {
                PluginDebugLog.installFormatLog(TAG,"deletePluginData file %s fail!",packageName);
                e.printStackTrace();
            }
        }

        if (cache != null) {
            try {
                Util.deleteDirectory(cache);
                PluginDebugLog.installFormatLog(TAG,"deletePluginData cache %s success!",packageName);
            } catch (IOException e) {
                PluginDebugLog.installFormatLog(TAG,"deletePluginData cache %s fail!",packageName);
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


}
