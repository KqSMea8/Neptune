/**
 *
 * Copyright 2018 iQIYI.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.qiyi.pluginlibrary.pm;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Parcelable;
import android.os.RemoteException;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.qiyi.pluginlibrary.constant.IIntentConstant;
import org.qiyi.pluginlibrary.error.ErrorType;
import org.qiyi.pluginlibrary.install.IActionFinishCallback;
import org.qiyi.pluginlibrary.install.IInstallCallBack;
import org.qiyi.pluginlibrary.install.PluginInstaller;
import org.qiyi.pluginlibrary.runtime.PluginManager;
import org.qiyi.pluginlibrary.utils.ContextUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负责安装卸载app，获取安装列表等工作.<br>
 * 提供安装插件的一些方法 功能类似系统中的PackageManager
 * @hide
 */
public class PluginPackageManager {

    private static final String TAG = "PluginPackageManager";

    /**
     * 安装成功，发送广播
     */
    public static final String ACTION_PACKAGE_INSTALLED = "com.qiyi.plugin.installed";

    /**
     * 安装失败，发送广播
     */
    public static final String ACTION_PACKAGE_INSTALLFAIL = "com.qiyi.plugin.installfail";

    /**
     * 卸载插件完成，发送广播
     */
    public static final String ACTION_PACKAGE_UNINSTALL = "com.qiyi.plugin.uninstall";

    /**
     * 如果发现某个插件异常，通知上层检查
     */
    public static final String ACTION_HANDLE_PLUGIN_EXCEPTION = "handle_plugin_exception";

    private Context mContext;
    @SuppressLint("StaticFieldLeak")
    private static PluginPackageManager sInstance;// 安装单例

    private ConcurrentHashMap<String, IActionFinishCallback> mActionFinishCallbacks =
            new ConcurrentHashMap<String, IActionFinishCallback>();
    // 插件PackageInfo的缓存
    private ConcurrentHashMap<String, PluginPackageInfo> mPackageInfoCache =
            new ConcurrentHashMap<String, PluginPackageInfo>();

    // 已安装插件列表
    private ConcurrentHashMap<String, PluginLiteInfo> mInstalledPlugins =
            new ConcurrentHashMap<>();

    /**
     * 插件安装任务列表
     */
    private List<PackageAction> mPackageActions = new LinkedList<PluginPackageManager.PackageAction>();
    /**
     * 插件安装监听列表
     */
    private Map<String, IInstallCallBack> listenerMap = new HashMap<String, IInstallCallBack>();
    /**
     * 验证插件基本信息、获取插件状态等信息接口，该接口通常交由主工程实现，并设置
     */
    private static IVerifyPluginInfo sVerifyPluginInfo = null;

    public static final int DELETE_SUCCEEDED = 1;

    public static final int INSTALL_SUCCESS = 2;

    public static final int INSTALL_FAILED = -2;

    public static final int UNINSTALL_SUCCESS = 3;

    public static final int UNINSTALL_FAILED = -3;

    private PluginPackageManager(Context context) {
        mContext = context.getApplicationContext();
        registerInstallReceiver();
        restoreInstallPluginInfos();
    }

    /**
     * 设置IVerifyPluginInfo接口，由应用层实现更高级的控制
     *
     * @param packageInfoManager
     */
    public static void setVerifyPluginInfoImpl(IVerifyPluginInfo packageInfoManager) {
        sVerifyPluginInfo = packageInfoManager;
    }

    /**
     * 获取PluginPackageManager实例对象
     */
    synchronized static PluginPackageManager getInstance(Context context) {

        if (sInstance == null) {
            synchronized (PluginPackageManager.class) {
                if (sInstance == null) {
                    sInstance = new PluginPackageManager(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * 保护性的更新srcApkPath
     *
     * @param context
     * @param cmPkgInfo
     */
    public static void updateSrcApkPath(Context context, PluginLiteInfo cmPkgInfo) {
        if (null != context && null != cmPkgInfo && TextUtils.isEmpty(cmPkgInfo.srcApkPath)) {
            // srcApkPath is empty, set a default value
            File rootDir = PluginInstaller.getPluginappRootPath(ContextUtils.getOriginalContext(context));
            // <pkgName>.<pkgVer>.apk
            File mApkFile = new File(rootDir, cmPkgInfo.packageName + "." + cmPkgInfo.pluginVersion + PluginInstaller.APK_SUFFIX);
            if (!mApkFile.exists()) {
                // 安装在sd卡上
                mApkFile = new File(context.getExternalFilesDir(PluginInstaller.PLUGIN_ROOT_PATH),
                        cmPkgInfo.packageName + "." + cmPkgInfo.pluginVersion + PluginInstaller.APK_SUFFIX);
            }
            // 不带版本号 <pkgName>.apk
            if (!mApkFile.exists()) {
                mApkFile = new File(rootDir, cmPkgInfo.packageName + PluginInstaller.APK_SUFFIX);
            }
            if (!mApkFile.exists()) {
                mApkFile = new File(context.getExternalFilesDir(PluginInstaller.PLUGIN_ROOT_PATH), cmPkgInfo.packageName + PluginInstaller.APK_SUFFIX);
            }

            if (mApkFile.exists()) {
                cmPkgInfo.srcApkPath = mApkFile.getAbsolutePath();
                PluginDebugLog.runtimeFormatLog(TAG,
                        "special case srcApkPath is null! Set default value for srcApkPath:%s  packageName:%s",
                        mApkFile.getAbsolutePath(), cmPkgInfo.packageName);
            } else {
                PluginDebugLog.runtimeLog(TAG, "updateSrcApkPath fail!");
            }
        }
    }

    private static final String PLUGIN_INSTALL_SP_NAME = "plugin_install";
    private static final String PLUGIN_INSTALL_KEY = "install_status";
    /**
     * 保存已安装插件信息到本地
     */
    private void saveInstallPluginInfos() {

        SharedPreferences sp = mContext.getSharedPreferences(PLUGIN_INSTALL_SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        JSONArray jArray = new JSONArray();
        for (Map.Entry<String, PluginLiteInfo> entry : mInstalledPlugins.entrySet()) {
            String pkgName = entry.getKey();
            PluginLiteInfo liteInfo = entry.getValue();

            JSONObject jObj = new JSONObject();
            try {
                jObj.put("pkgName", pkgName);
                jObj.put("info", liteInfo.toJson());

                jArray.put(jObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        editor.putString(PLUGIN_INSTALL_KEY, jArray.toString());
        editor.apply();
    }

    /**
     * 从本地恢复已安装插件信息
     */
    private void restoreInstallPluginInfos() {

        SharedPreferences sp = mContext.getSharedPreferences(PLUGIN_INSTALL_SP_NAME, Context.MODE_PRIVATE);
        String content = sp.getString(PLUGIN_INSTALL_KEY, "");
        if (TextUtils.isEmpty(content)) {
            return;
        }

        // restore data
        try {
            JSONArray jArray = new JSONArray(content);
            for (int i = 0; i < jArray.length(); i++) {
                JSONObject jObj = jArray.optJSONObject(i);
                if (jObj != null) {
                    String pkgName = jObj.optString("pkgName");
                    String info = jObj.optString("info");
                    if (TextUtils.isEmpty(pkgName) || TextUtils.isEmpty(info)) {
                        continue;
                    }

                    PluginLiteInfo liteInfo = new PluginLiteInfo(info);
                    if (TextUtils.isEmpty(liteInfo.packageName) || !TextUtils.equals(liteInfo.packageName, pkgName)) {
                        continue;
                    }
                    // restore success
                    mInstalledPlugins.put(pkgName, liteInfo);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * 安装广播，用于监听安装过程中是否成功。
     */
    private BroadcastReceiver pluginInstallerReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (ACTION_PACKAGE_INSTALLED.equals(action)) {
                    // 插件安装成功
                    PluginLiteInfo pkgInfo = intent.getParcelableExtra(IIntentConstant.EXTRA_PLUGIN_INFO);
                    if (pkgInfo == null) {
                        pkgInfo = new PluginLiteInfo();
                        String pkgName = intent.getStringExtra(IIntentConstant.EXTRA_PKG_NAME);
                        String destApkPath = intent.getStringExtra(IIntentConstant.EXTRA_DEST_FILE);
                        pkgInfo.packageName = pkgName;
                        pkgInfo.srcApkPath = destApkPath;
                        pkgInfo.installStatus = PluginLiteInfo.PLUGIN_INSTALLED;
                    }
                    PluginDebugLog.installFormatLog(TAG, "plugin install success: %s", pkgInfo.packageName);
                    // 先更新内存状态，再回调给上层
                    mInstalledPlugins.put(pkgInfo.packageName, pkgInfo);
                    saveInstallPluginInfos();

                    IInstallCallBack callback = listenerMap.get(pkgInfo.packageName);
                    if (callback != null) {
                        try {
                            callback.onPackageInstalled(pkgInfo);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        } finally {
                            listenerMap.remove(pkgInfo.packageName);
                        }
                    }
                    // 执行等待执行的action
                    executePackageAction(pkgInfo, true, 0);
                    onActionFinish(pkgInfo.packageName, INSTALL_SUCCESS);
                } else if (ACTION_PACKAGE_INSTALLFAIL.equals(action)) {
                    // 插件安装失败
                    PluginLiteInfo pkgInfo = intent.getParcelableExtra(IIntentConstant.EXTRA_PLUGIN_INFO);
                    if (pkgInfo == null) {
                        pkgInfo = new PluginLiteInfo();
                        String pkgName = intent.getStringExtra(IIntentConstant.EXTRA_PKG_NAME);
                        String filePath = intent.getStringExtra(IIntentConstant.EXTRA_SRC_FILE);
                        if (!TextUtils.isEmpty(pkgName)) {
                            pkgInfo.packageName = pkgName;
                        } else if (!TextUtils.isEmpty(filePath)) {
                            pkgInfo.packageName = PluginInstaller.extractPkgNameFromPath(filePath);
                        }
                        pkgInfo.installStatus = PluginLiteInfo.PLUGIN_UNINSTALLED;
                    }
                    // 失败原因
                    int failReason = intent.getIntExtra(ErrorType.ERROR_REASON, ErrorType.SUCCESS);
                    PluginDebugLog.installFormatLog(TAG,
                            "plugin install fail:%s,reason:%d ", pkgInfo.packageName, failReason);
                    IInstallCallBack callBack = listenerMap.get(pkgInfo.packageName);
                    if (callBack != null) {
                        try {
                            callBack.onPackageInstallFail(pkgInfo, failReason);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        } finally {
                            listenerMap.remove(pkgInfo.packageName);
                        }
                    }
                    executePackageAction(pkgInfo, false, failReason);
                    onActionFinish(pkgInfo.packageName, INSTALL_FAILED);
                } else if (TextUtils.equals(ACTION_HANDLE_PLUGIN_EXCEPTION, action)) {
                    String pkgName = intent.getStringExtra(IIntentConstant.EXTRA_PKG_NAME);
                    String exception = intent.getStringExtra(ErrorType.ERROR_REASON);
                    PluginDebugLog.installFormatLog(TAG,
                            "plugin install exception:%s,exception:%s", pkgName
                            , exception);
                    if (null != sVerifyPluginInfo && !TextUtils.isEmpty(pkgName)) {
                        sVerifyPluginInfo.handlePluginException(pkgName, exception);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 注册插件安装成功/失败广播
     */
    private void registerInstallReceiver() {
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
        PluginLiteInfo pkgInfo;   // 插件基础信息
        String packageName;// 包名
    }

    /**
     * 执行依赖于安装包的 runnable，如果该package已经安装，则立即执行。如果pluginapp正在初始化，或者该包正在安装，
     * 则放到任务队列中等待安装完毕执行。
     *
     * @param packageInfo 插件信息
     * @param callBack
     */
    public void packageAction(PluginLiteInfo packageInfo, IInstallCallBack callBack) {
        boolean packageInstalled = isPackageInstalled(packageInfo.packageName);
        boolean installing = PluginInstaller.isInstalling(packageInfo.packageName);
        PluginDebugLog.installLog(TAG, "packageAction , " + packageInfo.packageName + " installed : "
                + packageInstalled + " installing: " + installing);

        if (packageInstalled && (!installing)) { // 安装了，并且没有更新操作
            try {
                if (callBack != null) {
                    callBack.onPackageInstalled(packageInfo);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            // 添加一个包安装任务
            PackageAction action = new PackageAction();
            action.pkgInfo = packageInfo;
            action.packageName = packageInfo.packageName;
            action.timestamp = System.currentTimeMillis();
            action.callBack = callBack;

            synchronized (this) {
                if (mPackageActions.size() < 1000) { // 防止溢出
                    mPackageActions.add(action);
                }
            }
        }
        // 清理过期的Action
        clearExpiredPkgAction();
    }

    /**
     * 执行队列中等待的Action，回调给上层
     *
     * @param packageInfo
     * @param isSuccess
     * @param failReason
     */
    private void executePackageAction(
            PluginLiteInfo packageInfo, boolean isSuccess, int failReason) {
        if (packageInfo == null) {
            return;
        }
        ArrayList<PackageAction> executeList = new ArrayList<>();

        String packageName = packageInfo.packageName;
        if (!TextUtils.isEmpty(packageName)) {
            for (PackageAction action : mPackageActions) {
                if (packageName.equals(action.packageName)) {
                    executeList.add(action);
                }
            }
        }

        synchronized (this) {
            for (PackageAction action : executeList) {
                mPackageActions.remove(action);
            }
        }

        for (PackageAction action : executeList) {
            if (action.callBack != null) {
                try {
                    if (isSuccess) {
                        action.callBack.onPackageInstalled(packageInfo);
                    } else {
                        action.callBack.onPackageInstallFail(packageInfo, failReason);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 删除过期没有执行的action，可能由于某种原因存在此问题。
     * 比如一个找不到package的任务。
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
                        action.callBack.onPackageInstallFail(action.pkgInfo, ErrorType.INSTALL_ERROR_CLIENT_TIME_OUT);
                    }

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 设置Action执行完成的Callback回调
     *
     * @param callback
     */
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

    /**
     * Action执行完成，回调给Client端
     *
     * @param packageName
     * @param errorCode
     */
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


    /**
     * 获取已安装插件列表
     *
     */
    public List<PluginLiteInfo> getInstalledApps() {
        if (sVerifyPluginInfo != null) {
            List<PluginLiteInfo> packageInfoList = sVerifyPluginInfo.getInstalledPackages();
            return packageInfoList;
        }

        return new ArrayList<>(mInstalledPlugins.values());
    }


    /**
     * 判断一个package是否安装
     */
    public boolean isPackageInstalled(String packageName) {
        if (sVerifyPluginInfo != null) {
            return sVerifyPluginInfo.isPackageInstalled(packageName);
        }
        return mInstalledPlugins.containsKey(packageName);
    }

    /**
     * 获取安装apk的信息
     *
     * @param packageName
     * @return 没有安装反馈null
     */
    public PluginLiteInfo getPackageInfo(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            PluginDebugLog.log(TAG, "getPackageInfo return null due to empty package name");
            return null;
        }

        if (sVerifyPluginInfo != null) {
            if (sVerifyPluginInfo.isPackageInstalled(packageName)) {
                PluginLiteInfo info = sVerifyPluginInfo.getPackageInfo(packageName);
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
                    packageName + " return null due to verifyPluginInfoImpl is null");
        }

        return mInstalledPlugins.get(packageName);
    }

    /**
     * 安装一个插件，安装过程采用独立进程异步安装
     * 启动Service进行安装操作，安装完成会有 {@link #ACTION_PACKAGE_INSTALLED} 广播。
     * @param pluginInfo  插件信息
     * @param listener    监听器
     */
    public void install(PluginLiteInfo pluginInfo, IInstallCallBack listener) {
        // 安装插件前，先清理部分遗留数据
        deletePackage(pluginInfo, null);

        listenerMap.put(pluginInfo.packageName, listener);
        PluginDebugLog.installLog(TAG, "install plugin: " + pluginInfo);
        PluginInstaller.install(mContext, pluginInfo);
    }

    /**
     * 安装一个 apk file 文件. 用于安装比如下载后的文件，或者从sdcard安装。安装过程采用独立进程异步安装。
     * 启动service进行安装操作。 安装完会有 {@link #ACTION_PACKAGE_INSTALLED} 广播。
     *
     * @param filePath   apk 文件目录 比如 /sdcard/xxxx.apk
     * @param pluginInfo 插件信息
     */
//    @Deprecated
//    public void installApkFile(final String filePath, IInstallCallBack listener, PluginLiteInfo pluginInfo) {
//        if (TextUtils.isEmpty(pluginInfo.packageName)) {
//            pluginInfo.packageName = PluginInstaller.extractPkgNameFromPath(filePath);
//        }
//        listenerMap.put(pluginInfo.packageName, listener);
//        PluginDebugLog.installFormatLog(TAG, "installApkFile:%s", pluginInfo.packageName);
//        PluginInstaller.installApkFile(mContext, pluginInfo);
//    }

    /**
     * 安装内置在 assets/puginapp 目录下的 apk。 内置app必须以 packageName 命名，比如
     * com.qiyi.xx.apk
     *
     * @param listener
     * @param info     插件信息
     */
//    @Deprecated
//    public void installBuildinApps(PluginLiteInfo info, IInstallCallBack listener) {
//        listenerMap.put(info.packageName, listener);
//        PluginInstaller.installBuiltinApps(mContext, info);
//    }

    /**
     * 删除安装包。 卸载插件应用程序,目前只有在升级时调用此方法，把插件状态改成upgrading状态
     *
     * @param packageInfo 需要删除的package 的 PluginLiteInfo
     * @param observer    卸载结果回调
     */
    private void deletePackage(final PluginLiteInfo packageInfo, IPluginUninstallCallBack observer) {
        deletePackage(packageInfo, observer, false, true);
    }

    /**
     * 删除安装包。 卸载插件应用程序
     *
     * @param packageInfo 需要删除的package 的 PluginLiteInfo
     * @param observer    卸载结果回调
     * @param deleteData  是否删除生成的data
     * @param upgrading   是否是升级之前的操作
     */
    private void deletePackage(final PluginLiteInfo packageInfo, IPluginUninstallCallBack observer,
                               boolean deleteData, boolean upgrading) {

        if (packageInfo != null) {
            String packageName = packageInfo.packageName;
            PluginDebugLog.installFormatLog(TAG, "delete plugin :%s,deleteData:%s,upgrading:%s", packageName
                    , String.valueOf(deleteData), String.valueOf(upgrading));

            try {
                // 先停止运行插件
                PluginManager.exitPlugin(packageName);
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
                        mContext, packageInfo, packageName);
                mPackageInfoCache.remove(packageName);

                // 回调
                if (observer != null) {
                    observer.onPluginUninstall(packageName, DELETE_SUCCEEDED);
                    // 发送广播给插件进程，清理PluginLoadedApk数据
                    Intent intent = new Intent(PluginPackageManager.ACTION_PACKAGE_UNINSTALL);
                    intent.setPackage(mContext.getPackageName());
                    intent.putExtra(IIntentConstant.EXTRA_PKG_NAME, packageInfo.packageName);
                    intent.putExtra(IIntentConstant.EXTRA_PLUGIN_INFO, (Parcelable) packageInfo);// 同时返回APK的插件信息
                    mContext.sendBroadcast(intent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 卸载插件，删除文件
     *
     * @param packageInfo PluginLiteInfo
     * @return
     */
    public boolean uninstall(final PluginLiteInfo packageInfo) {

        boolean uninstallFlag = false;

        if (packageInfo != null) {
            String packageName = packageInfo.packageName;
            PluginDebugLog.installFormatLog(TAG, "uninstall plugin:%s ", packageName);
            try {
                if (TextUtils.isEmpty(packageName)) {
                    PluginDebugLog.installLog(TAG, "uninstall plugin pkgName is empty return");
                    return false;
                }

                String apkPath = packageInfo.srcApkPath;
                if (!TextUtils.isEmpty(apkPath)) {
                    File apk = new File(apkPath);
                    if (apk.exists()) {
                        uninstallFlag = apk.delete();
                    }
                }

                if (uninstallFlag) {
                    deletePackage(packageInfo, new IPluginUninstallCallBack.Stub() {
                        @Override
                        public void onPluginUninstall(String packageName, int resultCode) throws RemoteException {
                            PluginDebugLog.runtimeFormatLog(TAG, "onPluginUninstall %s", packageName);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                uninstallFlag = false;
            }

            if (uninstallFlag) {
                mPackageInfoCache.remove(packageName);

                mInstalledPlugins.remove(packageName);
                saveInstallPluginInfos();
            }

            onActionFinish(packageName, uninstallFlag ? UNINSTALL_SUCCESS : UNINSTALL_FAILED);
        }

        return uninstallFlag;
    }

    /**
     * 判断能否安装该插件
     *
     * @param info
     * @return
     */
    public boolean canInstallPackage(PluginLiteInfo info) {
        if (sVerifyPluginInfo != null) {
            return sVerifyPluginInfo.canInstallPackage(info);
        }
        return true;
    }

    /**
     * 判断能否卸载插件
     *
     * @param info
     * @return
     */
    public boolean canUninstallPackage(PluginLiteInfo info) {
        if (sVerifyPluginInfo != null) {
            return sVerifyPluginInfo.canUninstallPackage(info);
        }
        return true;
    }

    /**
     * 获取插件的PluginPackageInfo信息
     *
     * @param pkgName
     * @return
     */
    public PluginPackageInfo getPluginPackageInfo(String pkgName) {
        PluginPackageInfo result = null;
        if (!TextUtils.isEmpty(pkgName)) {
            result = mPackageInfoCache.get(pkgName);
            if (result != null) {
                PluginDebugLog.runtimeLog(TAG, "getPackageInfo from local cache");
                return result;
            }
        }
        PluginLiteInfo packageInfo = getPackageInfo(pkgName);
        updateSrcApkPath(mContext, packageInfo);

        if (null != packageInfo) {
            if (!TextUtils.isEmpty(packageInfo.srcApkPath)) {
                File file = new File(packageInfo.srcApkPath);
                if (file.exists()) {
                    result = new PluginPackageInfo(mContext, file);
                }
            }
        }
        if (result != null) {
            mPackageInfoCache.put(pkgName, result);
        }
        return result;
    }

    /**
     * 获取插件的依赖列表
     *
     * @param pkgName
     * @return
     */
    List<String> getPluginRefs(String pkgName) {
        List<String> mRefs = Collections.emptyList();
        if (TextUtils.isEmpty(pkgName)) {
            PluginDebugLog.log(TAG, "getPackageInfo return null due to empty package name");
            return mRefs;
        }

        if (sVerifyPluginInfo != null) {
            mRefs = sVerifyPluginInfo.getPluginRefs(pkgName);
        } else {
            PluginLiteInfo liteInfo = mInstalledPlugins.get(pkgName);
            if (liteInfo != null && !TextUtils.isEmpty(liteInfo.plugin_refs)) {
                String[] refs = liteInfo.plugin_refs.split(",");
                for (String ref : refs) {
                    if (!TextUtils.isEmpty(ref)) {
                        mRefs.add(ref);
                    }
                }
            }
        }
        return mRefs;
    }


    /**
     * 获取内置存储的files根目录
     */
    public static File getExternalFilesRootDir() {
        if (null != sVerifyPluginInfo) {
            return sVerifyPluginInfo.getExternalFilesRootDirDirectly();
        }
        return null;
    }

    /**
     * 获取内置存储的cache根目录
     */
    public static File getExternalCacheRootDir() {
        if (null != sVerifyPluginInfo) {
            return sVerifyPluginInfo.getExternalCacheRootDirDirectly();
        }
        return null;
    }

    /**
     * 直接获取已经安装的插件列表(不经过ipc，直接读取sp)
     *
     * @return
     */
    List<PluginLiteInfo> getInstalledPackagesDirectly() {
        List<PluginLiteInfo> installPlugins = Collections.emptyList();
        if (sVerifyPluginInfo != null) {
            installPlugins = sVerifyPluginInfo.getInstalledPackagesDirectly();
        } else {
            PluginDebugLog.runtimeLog(TAG, "[warning] sVerifyPluginInfo is null");
            installPlugins.addAll(mInstalledPlugins.values());
        }
        return installPlugins;
    }

    /**
     * 直接判断指定插件是否安装
     *
     * @param packageName
     * @return
     */
    boolean isPackageInstalledDirectly(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        if (sVerifyPluginInfo != null) {
            return sVerifyPluginInfo.isPackageInstalledDirectly(packageName);
        } else {
            PluginDebugLog.runtimeLog(TAG, "[warning] sVerifyPluginInfo is null");
        }
        return mInstalledPlugins.containsKey(packageName);
    }

    /**
     * 直接获取插件依赖
     *
     * @param packageName
     * @return
     */
    List<String> getPluginRefsDirectly(String packageName) {
        List<String> mRefPlugins = Collections.emptyList();
        if (sVerifyPluginInfo != null) {
            mRefPlugins = sVerifyPluginInfo.getPluginRefsDirectly(packageName);
        } else {
            PluginDebugLog.runtimeLog(TAG, "[warning] sVerifyPluginInfo is null");
            PluginLiteInfo liteInfo = mInstalledPlugins.get(packageName);
            if (liteInfo != null && !TextUtils.isEmpty(liteInfo.plugin_refs)) {
                String[] refs = liteInfo.plugin_refs.split(",");
                for (String ref : refs) {
                    if (!TextUtils.isEmpty(ref)) {
                        mRefPlugins.add(ref);
                    }
                }
            }
        }
        return mRefPlugins;
    }

    /**
     * 直接获取插件的信息
     *
     * @param packageName
     * @return
     */
    PluginLiteInfo getPackageInfoDirectly(String packageName) {
        PluginLiteInfo liteInfo = null;
        if (!TextUtils.isEmpty(packageName) && sVerifyPluginInfo != null) {
            liteInfo = sVerifyPluginInfo.getPackageInfoDirectly(packageName);
        } else {
            PluginDebugLog.runtimeLog(TAG, "[warning] sVerifyPluginInfo is null");
            liteInfo = mInstalledPlugins.get(packageName);
        }

        return liteInfo;
    }


    public static void notifyClientPluginException(Context context, String pkgName, String exceptionMsg) {
        try {
            Intent intent = new Intent(ACTION_HANDLE_PLUGIN_EXCEPTION);
            intent.setPackage(context.getPackageName());
            intent.putExtra(IIntentConstant.EXTRA_PKG_NAME, pkgName);
            intent.putExtra(ErrorType.ERROR_REASON, exceptionMsg);
            context.sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
