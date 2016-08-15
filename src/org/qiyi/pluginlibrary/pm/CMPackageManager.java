package org.qiyi.pluginlibrary.pm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qiyi.pluginlibrary.ApkTargetMappingNew;
import org.qiyi.pluginlibrary.ErrorType.ErrorType;
import org.qiyi.pluginlibrary.install.IActionFinishCallback;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.install.PluginInstaller;
import org.qiyi.pluginlibrary.manager.TargetActivator;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.RemoteException;
import android.text.TextUtils;

/**
 * 负责安装卸载app，获取安装列表等工作.<br> 负责安装插件的一些方法 功能类似系统中的PackageManager
 */
public class CMPackageManager {

    private static final String TAG = PluginDebugLog.TAG;
    /**
     * 安装成功，发送广播
     */
    public static final String ACTION_PACKAGE_INSTALLED = "com.qiyi.plugin.installed";

    /**
     * 安装失败，发送广播
     */
    public static final String ACTION_PACKAGE_INSTALLFAIL = "com.qiyi.plugin.installfail";

    /**
     * 卸载插件，发送广播
     */
    public static final String ACTION_PACKAGE_UNINSTALL = "com.qiyi.plugin.uninstall";

    /**
     * 如果发现某个插件异常，通知上层检查
     */
    public static final String ACTION_HANDLE_PLUGIN_EXCEPTION = "handle_plugin_exception";

    /**
     * 安装插件方案的版本
     **/
    public static final String PLUGIN_METHOD_DEFAULT = "plugin_method_default";
    public static final String PLUGIN_METHOD_DEXMAKER = "plugin_method_dexmaker";
    public static final String PLUGIN_METHOD_INSTR = "plugin_method_instr";

    /**
     * 插件文件后缀类型
     **/
    public static final String PLUGIN_FILE_APK = "apk";
    public static final String PLUGIN_FILE_SO = "so";
    public static final String PLUGIN_FILE_DEX = "dex";

    /**
     * 插件文件来源类型
     **/
    public static final String PLUGIN_SOURCE_ASSETS = "assets";
    public static final String PLUGIN_SOURCE_SDCARD = "sdcard";
    public static final String PLUGIN_SOURCE_NETWORK = "network";

    /**
     * 安装完的pkg的包名
     */
    public static final String EXTRA_PKG_NAME = "package_name";
    /**
     * 支持 assets:// 和 file:// 两种，对应内置和外部apk安装。 比如 assets://megapp/xxxx.apk , 或者
     * file:///data/data/com.qiyi.xxx/files/xxx.apk
     */
    public static final String EXTRA_SRC_FILE = "install_src_file";
    /**
     * 安装完的apk path，没有scheme 比如 /data/data/com.qiyi.video/xxx.apk
     */
    public static final String EXTRA_DEST_FILE = "install_dest_file";

    // /** 安装完的pkg的 version code */
    // public static final String EXTRA_VERSION_CODE = "version_code";
    // /** 安装完的pkg的 version name */
    // public static final String EXTRA_VERSION_NAME = "version_name";
    /**
     * 安装完的pkg的 plugin info
     */
    public static final String EXTRA_PLUGIN_INFO = "plugin_info";

    public static final String SCHEME_ASSETS = "assets://";
    public static final String SCHEME_FILE = "file://";
    public static final String SCHEME_SO = "so://";
    public static final String SCHEME_DEX = "dex://";

    /**
     * application context
     */
    private Context mContext;

    private static CMPackageManager sInstance;// 安装对象

    private ConcurrentHashMap<String, IActionFinishCallback> mActionFinishCallbacks =
            new ConcurrentHashMap<String, IActionFinishCallback>();

    /**
     * 已安装列表。 !!!!!!! 不要直接引用该变量。 因为该变量是 lazy init 方式，不需要的时不进行初始化。 使用
     * {@link #getInstalledPkgsInstance()} 获取该实例
     */
    private ConcurrentHashMap<String, ApkTargetMappingNew> mTargetMappingCache =
            new ConcurrentHashMap<String, ApkTargetMappingNew>();

    /**
     * 安装包任务队列。
     */
    private List<PackageAction> mPackageActions = new LinkedList<CMPackageManager.PackageAction>();

    private Map<String, IInstallCallBack> listenerMap = new HashMap<String, IInstallCallBack>();

    private static ICMPackageInfoManager sCMPackageInfoManager = null;

    /**
     * Return code for when package deletion succeeds. This is passed to the
     * {@link IPackageDeleteObserver} by {@link #deletePackage()} if the system
     * succeeded in deleting the package.
     */
    public static final int DELETE_SUCCEEDED = 1;

    /**
     * Deletion failed return code: this is passed to the
     * {@link IPackageDeleteObserver} by {@link #deletePackage()} if the system
     * failed to delete the package for an unspecified reason.
     */
    public static final int DELETE_FAILED_INTERNAL_ERROR = -1;

    public static final int INSTALL_SUCCESS = 2;

    public static final int INSTALL_FAILED = -2;

    public static final int UNINSTALL_SUCCESS = 3;

    public static final int UNINSTALL_FAILED = -3;

    private CMPackageManager(Context context) {
        mContext = context.getApplicationContext();
        registerInstallderReceiver();
    }

    public static void setPackageInfoManager(ICMPackageInfoManager packageInfoManager) {
        sCMPackageInfoManager = packageInfoManager;
    }

    /**
     * 获取packageManager实例对象
     *
     * @param context
     * @return
     */
    synchronized static CMPackageManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CMPackageManager(context);
        }
        return sInstance;
    }

    /**
     * 获取安装列表。
     *
     * @return
     */
    public List<CMPackageInfo> getInstalledApps() {
        if (sCMPackageInfoManager != null) {
            List<CMPackageInfo> packageInfoList = sCMPackageInfoManager.getInstalledPackages();
            return packageInfoList;
        }
        return null;
    }

    /**
     * 安装广播，用于监听安装过程中是否成功。
     */
    private BroadcastReceiver pluginInstallerReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            PluginDebugLog.log(TAG, "ACTION_PACKAGE_INSTALLED " + action + " intent: " + intent);
            if (ACTION_PACKAGE_INSTALLED.equals(action)) {
                String pkgName = intent.getStringExtra(EXTRA_PKG_NAME);
                String destApkPath = intent.getStringExtra(CMPackageManager.EXTRA_DEST_FILE);
                PluginPackageInfoExt infoExt = intent.getParcelableExtra(CMPackageManager.EXTRA_PLUGIN_INFO);
                PluginDebugLog.log(TAG, "ACTION_PACKAGE_INSTALLED " + infoExt);
                CMPackageInfo pkgInfo = new CMPackageInfo();
                pkgInfo.packageName = pkgName;
                pkgInfo.srcApkPath = destApkPath;
                pkgInfo.installStatus = CMPackageInfo.PLUGIN_INSTALLED;
                pkgInfo.pluginInfo = infoExt;

                IInstallCallBack callback = listenerMap.get(pkgName);
                if (callback != null) {
                    try {
                        callback.onPacakgeInstalled(pkgInfo);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } finally {
                        listenerMap.remove(pkgName);
                    }
                }
                // 执行等待执行的action
                executePackageAction(pkgInfo, true, 0);
                onActionFinish(pkgName, INSTALL_SUCCESS);
            } else if (ACTION_PACKAGE_INSTALLFAIL.equals(action)) {
                String assetsPath = intent.getStringExtra(CMPackageManager.EXTRA_SRC_FILE);
                if (!TextUtils.isEmpty(assetsPath)) {
                    int start = assetsPath.lastIndexOf("/");
                    int end = start + 1;
                    if (assetsPath.endsWith(PluginInstaller.APK_SUFFIX)) {
                        end = assetsPath.lastIndexOf(PluginInstaller.APK_SUFFIX);
                    } else if (assetsPath.endsWith(PluginInstaller.SO_SUFFIX)) {
                        end = assetsPath.lastIndexOf(PluginInstaller.SO_SUFFIX);
                    }
                    String mapPackagename = assetsPath.substring(start + 1, end);
                    // 失败原因
                    int failReason = intent.getIntExtra(ErrorType.ERROR_RESON, ErrorType.SUCCESS);
                    PluginDebugLog.log(TAG, "ACTION_PACKAGE_INSTALLFAIL mapPackagename: " + mapPackagename
                            + " failReason: " + failReason + " assetsPath: " + assetsPath);
                    if (listenerMap.get(mapPackagename) != null) {
                        try {
                            listenerMap.get(mapPackagename).
                                    onPackageInstallFail(mapPackagename, failReason);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        } finally {
                            listenerMap.remove(mapPackagename);
                        }
                    }
                    PluginPackageInfoExt infoExt = intent
                            .getParcelableExtra(CMPackageManager.EXTRA_PLUGIN_INFO);
                    CMPackageInfo pkgInfo = new CMPackageInfo();
                    pkgInfo.packageName = mapPackagename;
                    pkgInfo.installStatus = CMPackageInfo.PLUGIN_UNINSTALLED;
                    pkgInfo.pluginInfo = infoExt;
                    executePackageAction(pkgInfo, false, failReason);
                    onActionFinish(mapPackagename, INSTALL_FAILED);
                }
            } else if (TextUtils.equals(ACTION_HANDLE_PLUGIN_EXCEPTION, action)) {
                String pkgName = intent.getStringExtra(EXTRA_PKG_NAME);
                String exception = intent.getStringExtra(ErrorType.ERROR_RESON);
                if (null != sCMPackageInfoManager && !TextUtils.isEmpty(pkgName)) {
                    sCMPackageInfoManager.handlePluginException(pkgName, exception);
                }
            }
        }
    };

    /**
     * 监听安装列表变化.
     */
    private void registerInstallderReceiver() {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_PACKAGE_INSTALLED);
            filter.addAction(ACTION_PACKAGE_INSTALLFAIL);
            filter.addAction(ACTION_HANDLE_PLUGIN_EXCEPTION);
            filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
            // 注册一个安装广播
            mContext.registerReceiver(pluginInstallerReceiver, filter);

        } catch (Exception e) {
            // 该广播被其他应用UID 抢先注册
            // Receiver requested to register for uid 10100 was previously
            // registered for uid 10105
            e.printStackTrace();
        }
    }

    /**
     * 包依赖任务队列对象。
     */
    private class PackageAction {
        long timestamp;// 时间
        IInstallCallBack callBack;// 安装回调
        String packageName;// 包名
    }

    /**
     * 执行依赖于安装包的 runnable，如果该package已经安装，则立即执行。如果pluginapp正在初始化，或者该包正在安装，
     * 则放到任务队列中等待安装完毕执行。
     *
     * @param packageInfo 插件信息
     * @param callBack
     */
    public void packageAction(CMPackageInfo packageInfo, IInstallCallBack callBack) {
        boolean packageInstalled = isPackageInstalled(packageInfo.packageName);
        boolean installing = PluginInstaller.isInstalling(packageInfo.packageName);
        PluginDebugLog.log(TAG, "packageAction , " + packageInfo.packageName + " installed : "
                + packageInstalled + " installing: " + installing);

        if (packageInstalled && (!installing)) { // 安装了，并且没有更新操作
            try {
                if (callBack != null) {
                    callBack.onPacakgeInstalled(packageInfo);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            PackageAction action = new PackageAction();
            action.packageName = packageInfo.packageName;
            action.timestamp = System.currentTimeMillis();
            action.callBack = callBack;

            synchronized (this) {
                if (mPackageActions.size() < 1000) { // 防止溢出
                    mPackageActions.add(action);
                }
            }
        }

        clearExpiredPkgAction();
    }

    private void executePackageAction(
            CMPackageInfo packageInfo, boolean isSuccess, int failReason) {
        if (packageInfo == null) {
            return;
        }
        ArrayList<PackageAction> executeList = new ArrayList<CMPackageManager.PackageAction>();

        String packageName = packageInfo.packageName;
        if (!TextUtils.isEmpty(packageName)) {
            for (PackageAction action : mPackageActions) {
                if (packageName.equals(action.packageName)) {
                    executeList.add(action);
                }
            }
        }

        // 首先从总列表中删除
        synchronized (this) {
            for (PackageAction action : executeList) {
                mPackageActions.remove(action);
            }
        }

        // 挨个执行
        for (PackageAction action : executeList) {
            if (action.callBack != null) {
                try {
                    if (isSuccess) {
                        action.callBack.onPacakgeInstalled(packageInfo);
                    } else {
                        action.callBack.onPackageInstallFail(action.packageName, failReason);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
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
                try {
                    if (action != null && action.callBack != null) {
                        action.callBack.onPackageInstallFail(action.packageName, ErrorType.ERROR_CLIENT_TIME_OUT);
                    }

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 判断一个package是否安装
     */
    public boolean isPackageInstalled(String packageName) {
        if (sCMPackageInfoManager != null) {
            return sCMPackageInfoManager.isPackageInstalled(packageName);
        }
        return false;
    }

    /**
     * 获取安装apk的信息
     *
     * @param packageName
     * @return 没有安装反馈null
     */
    public CMPackageInfo getPackageInfo(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            PluginDebugLog.log(TAG, "getPackageInfo return null due to empty package name");
            return null;
        }

        if (sCMPackageInfoManager != null) {
            if (sCMPackageInfoManager.isPackageInstalled(packageName)) {
                CMPackageInfo info = sCMPackageInfoManager.getPackageInfo(packageName);
                if (null != info) {
                    return info;
                } else {
                    PluginDebugLog.log(TAG, "getPackageInfo " +
                            packageName + " return null due to null package info");
                }
            } else {
                PluginDebugLog.log(TAG, "getPackageInfo " +
                                packageName + " return null due to not installed");
            }
        } else {
            PluginDebugLog.log(TAG, "getPackageInfo " +
                    packageName + " return null due to CMPackageInfoManager is null");
        }

        return null;
    }

    /**
     * 安装一个 apk file 文件. 用于安装比如下载后的文件，或者从sdcard安装。安装过程采用独立进程异步安装。
     * 启动service进行安装操作。 安装完会有 {@link #ACTION_PACKAGE_INSTALLED} broadcast。
     *
     * @param filePath apk 文件目录 比如 /sdcard/xxxx.apk
     * @param pluginInfo 插件信息
     */
    public void installApkFile(final String filePath, IInstallCallBack listener, PluginPackageInfoExt pluginInfo) {
        int start = filePath.lastIndexOf("/");
        int end = start + 1;
        if (filePath.endsWith(PluginInstaller.SO_SUFFIX)) {
            end = filePath.lastIndexOf(PluginInstaller.SO_SUFFIX);
        } else if (filePath.endsWith(PluginInstaller.DEX_SUFFIX)) {
            end = filePath.lastIndexOf(PluginInstaller.DEX_SUFFIX);
        } else {
            end = filePath.lastIndexOf(PluginInstaller.APK_SUFFIX);
        }
        String mapPackagename = filePath.substring(start + 1, end);
        listenerMap.put(mapPackagename, listener);
        PluginDebugLog.log(TAG, "installApkFile:" + mapPackagename);
        if (pluginInfo != null && !TextUtils.equals(pluginInfo.mFileSourceType, CMPackageManager.PLUGIN_SOURCE_SDCARD)) {
            PluginDebugLog.log(TAG, "installApkFile: change mFileSourceType to PLUGIN_SOURCE_SDCARD");
            pluginInfo.mFileSourceType = CMPackageManager.PLUGIN_SOURCE_NETWORK;
        }
        PluginInstaller.installApkFile(mContext, filePath, pluginInfo);
    }

    /**
     * 安装内置在 assets/puginapp 目录下的 apk。 内置app必须以 packageName 命名，比如
     * com.qiyi.xx.apk
     *
     * @param packageName
     * @param listener
     * @param info 插件信息
     */
    public void installBuildinApps(String packageName, IInstallCallBack listener, PluginPackageInfoExt info) {
        listenerMap.put(packageName, listener);
        PluginInstaller.installBuildinApps(packageName, mContext, info);
    }

    /**
     * 删除安装包。 卸载插件应用程序,目前只有在升级时调用次方法，把插件状态改成upgrading状态
     *
     * @param packageInfo 需要删除的package 的 CMPackageInfo
     * @param observer 卸载结果回调
     */
    public void deletePackage(final CMPackageInfo packageInfo, IPackageDeleteObserver observer) {
        deletePackage(packageInfo, observer, false, true);
    }

    /**
     * 删除安装包。 卸载插件应用程序
     *
     * @param packageInfo 需要删除的package 的 CMPackageInfo
     * @param observer 卸载结果回调
     * @param deleteData 是否删除生成的data
     * @param upgrading 是否是升级之前的操作
     */
    private void deletePackage(final CMPackageInfo packageInfo, IPackageDeleteObserver observer,
                               boolean deleteData, boolean upgrading) {

        if (packageInfo != null) {
            String packageName = packageInfo.packageName;
            PluginDebugLog.log(TAG, "deletePackage with " + packageName +
                    " deleteData: " + deleteData + " upgrading: " + upgrading);

            try {
                // 先停止运行插件
                TargetActivator.unLoadTarget(packageName);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                if (deleteData) {
                    // 删除生成的data数据文件
                    // 清除environment中相关的数据:按前缀匹配
                    PluginInstaller.deletePluginData(mContext, packageName);
                }

                //删除安装文件，apk，dex，so
                PluginInstaller.deleteInstallerPackage(
                        mContext, packageInfo.srcApkPath, packageName);
                mTargetMappingCache.remove(packageName);

                // 回调
                if (observer != null) {
                    observer.packageDeleted(packageName, DELETE_SUCCEEDED);
                }
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            } finally {
                onActionFinish(packageName, DELETE_SUCCEEDED);
            }
        }
    }

    /**
     * 卸载，删除文件
     *
     * @param packageInfo CMPackageInfo
     * @return
     */
    public boolean uninstall(CMPackageInfo packageInfo) {

        boolean uninstallFlag = false;

        if (packageInfo != null) {
            String packageName = packageInfo.packageName;
            PluginDebugLog.log(TAG, "CMPackageManager::uninstall: " + packageName);
            try {
                if (TextUtils.isEmpty(packageName)) {
                    PluginDebugLog.log(TAG, "CMPackageManager::uninstall pkgName is empty return");
                    return false;
                }

                String apkPath = packageInfo.srcApkPath;
                if (!TextUtils.isEmpty(apkPath)) {
                    File apk = new File(apkPath);
                    if (apk != null && apk.exists()) {
                        uninstallFlag = apk.delete();
                    }
                }

                // 暂时不去真正的卸载，只是去删除下载的文件,如果真正删除会出现以下两个问题
                // 1，卸载语音插件之后会出现，找不到库文件
                // 2.卸载了啪啪奇插件之后，会出现 .so库 已经被打开，无法被另一个打开
                // CMPackageManager.getInstance(pluginContext).deletePackage(pluginData.mPlugin.packageName,
                // observer);
            } catch (Exception e) {
                e.printStackTrace();
                uninstallFlag = false;
            }

            if (uninstallFlag) {
                mTargetMappingCache.remove(packageName);
            }

            onActionFinish(packageName, uninstallFlag ? UNINSTALL_SUCCESS : UNINSTALL_FAILED);
        }

        return uninstallFlag;
    }

    public static SharedPreferences getPreferences(Context context, String shareName) {
        SharedPreferences spf = null;
        if (hasHoneycomb()) {
            spf = context.getSharedPreferences(shareName, Context.MODE_MULTI_PROCESS);
        } else {
            spf = context.getSharedPreferences(shareName, Context.MODE_PRIVATE);
        }
        return spf;
    }

    private static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= 11;
    }

    public void setActionFinishCallback(IActionFinishCallback callback) {
        if (callback != null) {
            try {
                String processName = callback.getProcessName();
                if (!TextUtils.isEmpty(processName)) {
                    PluginDebugLog.log(TAG, "setActionFinishCallback with process name: " + processName);
                    mActionFinishCallbacks.put(processName, callback);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void onActionFinish(String packageName, int errorCode) {
        for (Map.Entry<String, IActionFinishCallback> entry : mActionFinishCallbacks.entrySet()) {
            IActionFinishCallback callback = entry.getValue();
            if (callback != null) {
                try {
                    callback.onActionComplete(packageName, errorCode);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean canInstallPackage(PluginPackageInfoExt info) {
        if (sCMPackageInfoManager != null) {
            return sCMPackageInfoManager.canInstallPackage(info);
        }
        return false;
    }

    public boolean canUninstallPackage(CMPackageInfo info) {
        if (sCMPackageInfoManager != null) {
            return sCMPackageInfoManager.canUninstallPackage(info);
        }
        return false;
    }

    public ApkTargetMappingNew getApkTargetMapping(String pkgName) {
        CMPackageInfo packageInfo = getPackageInfo(pkgName);
        CMPackageInfo.updateSrcApkPath(mContext, packageInfo);
        ApkTargetMappingNew result = null;
        if (null != packageInfo) {
            result = packageInfo.getTargetMapping(mContext, pkgName, packageInfo.srcApkPath, mTargetMappingCache);
        }
        return result;
    }

    public static void notifyClientPluginException(Context context, String pkgName, String exceptionMsg) {
        try {
            Intent intent = new Intent(CMPackageManager.ACTION_HANDLE_PLUGIN_EXCEPTION);
            intent.setPackage(context.getPackageName());
            intent.putExtra(EXTRA_PKG_NAME, pkgName);
            intent.putExtra(ErrorType.ERROR_RESON, exceptionMsg);
            context.sendBroadcast(intent);
            PluginDebugLog.log(TAG,
                    "notifyClientPluginException Success " + " pkgName: " + pkgName + " exceptionMsg: " + exceptionMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
