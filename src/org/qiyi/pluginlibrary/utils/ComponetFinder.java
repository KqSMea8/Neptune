package org.qiyi.pluginlibrary.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.component.processmgr.ProcessManger;
import org.qiyi.pluginlibrary.pm.PluginLiteInfo;
import org.qiyi.pluginlibrary.pm.PluginPackageInfo;
import org.qiyi.pluginlibrary.constant.IIntentConstant;
import org.qiyi.pluginlibrary.debug.PluginCenterDebugHelper;
import org.qiyi.pluginlibrary.pm.PluginPackageManagerNative;
import org.qiyi.pluginlibrary.runtime.PluginLoadedApk;
import org.qiyi.pluginlibrary.runtime.PluginManager;

import java.util.List;

/**
 * 在{@link PluginLoadedApk}代表的插件中查找能够处理{@link Intent}的组件
 * 并设置组件代理,支持显式和隐式查找
 * Author:yuanzeyao
 * Date:2017/7/4 14:53
 * Email:yuanzeyao@qiyi.com
 */

public class ComponetFinder implements IIntentConstant {
    private static final String TAG = "ComponetFinder";

    public static final String DEFAULT_ACTIVITY_PROXY_PREFIX =
            "org.qiyi.pluginlibrary.component.InstrActivityProxy";
    public static final String DEFAULT_TRANSLUCENT_ACTIVITY_PROXY_PREFIX =
            "org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent";
    public static final String DEFAULT_LANDSCAPE_ACTIVITY_PROXY_PREFIX =
            "org.qiyi.pluginlibrary.component.InstrActivityProxyLandscape";
    public static final String DEFAULT_CONFIGCHANGE_ACTIVITY_PROXY_PREFIX =
            "org.qiyi.pluginlibrary.component.InstrActivityProxyHandleConfigChange";
    public static final String DEFAULT_SERVICE_PROXY_PREFIX =
            "org.qiyi.pluginlibrary.component.ServiceProxy";

    /**
     * 在插件中查找可以处理mIntent的Service组件,找到之后为其分配合适的Proxy
     *
     * @param mLoadedApk 插件的{@link PluginLoadedApk}对象
     * @param mIntent    需要查找的Intent
     */
    public static void switchToServiceProxy(PluginLoadedApk mLoadedApk, Intent mIntent) {
        if (mIntent == null || mLoadedApk == null) {
            return;
        }
        if (mIntent.getComponent() == null) {
            //隐式启动
            ServiceInfo mServiceInfo = mLoadedApk.getPluginPackageInfo().resolveService(mIntent);
            if (mServiceInfo != null) {
                switchToServiceProxy(mLoadedApk, mIntent, mServiceInfo.name);
            }
            return;
        } else {
            //显示启动
            String targetService = mIntent.getComponent().getClassName();
            switchToServiceProxy(mLoadedApk, mIntent, targetService);
        }
    }


    /**
     * 在插件中查找可以处理mIntent的Service组件,找到之后为其分配合适的Proxy
     *
     * @param mLoadedApk    插件的{@link PluginLoadedApk}对象
     * @param mIntent       需要查找的Intent
     * @param targetService 处理当前Intent的组件的类名
     */
    public static void switchToServiceProxy(PluginLoadedApk mLoadedApk,
                                            Intent mIntent,
                                            String targetService) {
        if (null == mLoadedApk || null == mIntent || TextUtils.isEmpty(targetService)
                || mLoadedApk.getPluginPackageInfo().getServiceInfo(targetService) == null) {
            return;
        }
        mIntent.setExtrasClassLoader(mLoadedApk.getPluginClassLoader());
        mIntent.putExtra(EXTRA_TARGET_CLASS_KEY, targetService);
        mIntent.putExtra(EXTRA_TARGET_PACKAGNAME_KEY, mLoadedApk.getPluginPackageName());
        // 同一个进程内service的bind是否重新走onBind方法，以intent的参数匹配为准FilterComparison.filterHashCode)
        // 手动添加一个category 让系统认为不同的插件activity每次bind都会走service的onBind的方法
        mIntent.addCategory(EXTRA_TARGET_CATEGORY + System.currentTimeMillis());
        try {
            mIntent.setClass(mLoadedApk.getHostContext(),
                    Class.forName(matchServiceProxyByFeature(mLoadedApk.getProcessName())));
            String intentInfo = "";
            intentInfo = mIntent.toString();
            if (null != mIntent.getExtras()) {
                intentInfo = intentInfo + mIntent.getExtras().toString();
            }
            PluginCenterDebugHelper.getInstance().savePluginActivityAndServiceJump(
                    PluginCenterDebugHelper.getInstance().getCurrentSystemTime(),
                    intentInfo);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    /**
     *  在插件中查找可以处理mIntent的Activity组件,找到之后为其分配合适的Proxy
     * @param mPluginPackageName
     * @param mIntent
     * @param requestCode
     * @param context
     * @return
     */
    public static Intent switchToActivityProxy(String mPluginPackageName,
                                               Intent mIntent,
                                               int requestCode,
                                               Context context) {

        if (mIntent == null) {
            PluginDebugLog.log(TAG, "handleStartActivityIntent intent is null!");
            return mIntent;
        }
        PluginDebugLog.log(TAG, "handleStartActivityIntent: pluginId: "
                + mPluginPackageName + ", intent: " + mIntent
                + ", requestCode: " + requestCode);
        if (hasProxyActivity(mIntent)) {
            PluginDebugLog.log(TAG,
                    "handleStartActivityIntent has change the intent just return the original intent! "
                            + mIntent);
            return mIntent;
        }
        ActivityInfo targetActivity = null;
        PluginLoadedApk mLoadedApk = PluginManager.getPluginLoadedApkByPkgName(mPluginPackageName);
        if (mIntent.getComponent() != null
                && !TextUtils.isEmpty(mIntent.getComponent().getClassName())) {
            // action 为空，但是指定了包名和 activity类名
            ComponentName compname = mIntent.getComponent();
            String pkg = compname.getPackageName();
            String toActName = compname.getClassName();
            if (mLoadedApk != null) {
                if (TextUtils.equals(pkg, mPluginPackageName)
                        || TextUtils.equals(pkg, mLoadedApk.getHostPackageName())) {
                    PluginPackageInfo thisPlugin = mLoadedApk.getPluginPackageInfo();
                    targetActivity = thisPlugin.getActivityInfo(toActName);
                }

            }

            if (targetActivity == null) {
                mLoadedApk = PluginManager.getPluginLoadedApkByPkgName(pkg);
                // Check in pkg's apk
                if (!TextUtils.isEmpty(pkg) && mLoadedApk != null) {
                    PluginPackageInfo otherPlug = mLoadedApk.getPluginPackageInfo();
                    if (otherPlug != null) {
                        targetActivity = otherPlug.getActivityInfo(toActName);
                    }
                }
            }

        } else {
            if (mLoadedApk != null) {
                PluginPackageInfo mapping = mLoadedApk.getPluginPackageInfo();
                if (mapping != null) {
                    targetActivity = mapping.resolveActivity(mIntent);
                }
            } else {
                if (null != context) {
                    List<PluginLiteInfo> packageList = PluginPackageManagerNative.getInstance(context).getInstalledApps();
                    if (packageList != null) {
                        for (PluginLiteInfo pkgInfo : PluginPackageManagerNative.getInstance(context).getInstalledApps()) {
                            if (pkgInfo != null) {
                                PluginPackageInfo target = PluginPackageManagerNative.getInstance(context)
                                .getPluginPackageInfo(context,pkgInfo); //pkgInfo.getTargetMapping(context);
                                if (null != target) {
                                    targetActivity = target.resolveActivity(mIntent);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        PluginDebugLog.log(TAG, "handleStartActivityIntent pluginId: "
                + mPluginPackageName + " intent: " + mIntent.toString()
                + " targetActivity: " + targetActivity);
        if (targetActivity != null) {
            setActivityProxy(mIntent, targetActivity.packageName, targetActivity.name);
        }
        if (mLoadedApk != null) {
            mLoadedApk.getActivityStackSupervisor().dealLaunchMode(mIntent);
        }
        String intentInfo = "";
        if (null != mIntent) {
            intentInfo = mIntent.toString();
            if (null != mIntent.getExtras()) {
                intentInfo = intentInfo + mIntent.getExtras().toString();
            }
        }
        PluginCenterDebugHelper.getInstance().savePluginActivityAndServiceJump(
                PluginCenterDebugHelper.getInstance().getCurrentSystemTime(), intentInfo);
        return mIntent;

    }

    /**
     * 判断Intent代表的Activity组件是否已经设置代理，如果已经设置，直接返回
     *
     * @param mIntent
     *      Activity对应的Intent
     * @return
     *      true:已经设置代理，false：没有设置
     */
    private static boolean hasProxyActivity(Intent mIntent) {
        if (mIntent != null && mIntent.getComponent() != null
                && !TextUtils.isEmpty(mIntent.getStringExtra(EXTRA_TARGET_CLASS_KEY))
                && !TextUtils.isEmpty(mIntent.getStringExtra(EXTRA_TARGET_PACKAGNAME_KEY))) {
            if (mIntent.getComponent().getClassName().startsWith(ComponetFinder.DEFAULT_ACTIVITY_PROXY_PREFIX)
                    || mIntent.getComponent().getClassName()
                    .startsWith(ComponetFinder.DEFAULT_TRANSLUCENT_ACTIVITY_PROXY_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 为插件中的Activity 设置代理
     * @param mIntent
     *          需要设置代理的Activity的Intent
     * @param mPackageName
     *          插件的包名
     * @param activityName
     *          需要设置代理的Activity的类名
     */
    private static void setActivityProxy(Intent mIntent, String mPackageName, String activityName) {
        PluginLoadedApk mLoadedApk = PluginManager.getPluginLoadedApkByPkgName(mPackageName);
        if (null == mLoadedApk) {
            PluginDebugLog.formatLog(TAG,
                    "ActivityJumpUtil setPluginIntent failed, %s, ProxyEnvironmentNew is null",
                    mPackageName);
            return;
        }
        ActivityInfo info = mLoadedApk.getPluginPackageInfo().getActivityInfo(activityName);
        if (null == info) {
            PluginDebugLog.formatLog(TAG,
                    "ActivityJumpUtil setPluginIntent failed, activity info is null. actName: %s",
                    activityName);
            return;
        }
        ComponentName compname = new ComponentName(mLoadedApk.getHostPackageName(),
                findActivityProxy(mLoadedApk,info));
        mIntent.setComponent(compname).putExtra(EXTRA_TARGET_PACKAGNAME_KEY, mPackageName)
                .putExtra(EXTRA_TARGET_CLASS_KEY, activityName);
        IntentUtils.setProxyInfo(mIntent, mPackageName);

    }

    /**
     * 为插件中的Activity分配代理
     * @param mLoadedApk
     *        插件的实例
     * @param actInfo
     *        插件Activity对应的ActivityInfo
     * @return
     */
    public static String findActivityProxy(PluginLoadedApk mLoadedApk,ActivityInfo actInfo) {
        boolean isTranslucent = false;
        boolean isHandleConfigChange = false;
        boolean isLandscape = false;

        //通过主题判断是否是透明的
        Resources.Theme mTheme = mLoadedApk.getPluginTheme();
        mTheme.applyStyle(actInfo.getThemeResource(), true);
        TypedArray array = mTheme.obtainStyledAttributes(new int[]{
                android.R.attr.windowIsTranslucent,
                android.R.attr.windowBackground
        });
        isTranslucent=array.getBoolean(0, false);
        PluginDebugLog.runtimeLog(TAG,"activity theme is translucent:"+isTranslucent);
        array.recycle();
        if(!isTranslucent){
            //兼容遗留逻辑
            if (actInfo != null && actInfo.metaData != null) {
                String special_cfg = actInfo.metaData.getString(META_KEY_ACTIVITY_SPECIAL);
                if (!TextUtils.isEmpty(special_cfg)) {
                    if (special_cfg.contains(PLUGIN_ACTIVITY_TRANSLUCENT)) {
                        PluginDebugLog.log(TAG, "getProxyActivityClsName meta data contrains translucent flag");
                        isTranslucent = true;
                    }

                    if (special_cfg.contains(PLUGIN_ACTIVTIY_HANDLE_CONFIG_CHAGNE)) {
                        PluginDebugLog.log(TAG, "getProxyActivityClsName meta data contrains handleConfigChange flag");
                        isHandleConfigChange = true;
                    }
                }
            }
        }

        if (actInfo != null) {
            if (actInfo.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                PluginDebugLog.log(TAG, "getProxyActivityClsName activiy screenOrientation: "
                        + actInfo.screenOrientation + " isHandleConfigChange = false");
                isHandleConfigChange = false;
            }

            if (actInfo.screenOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                PluginDebugLog.log(TAG, "getProxyActivityClsName isLandscape = true");
                isLandscape = true;
            }
        }

        return matchActivityProxyByFeature(isTranslucent, isLandscape,
                isHandleConfigChange,mLoadedApk.getProcessName());
    }

    /**
     * 根据被代理的Activity的Feature和进程名称选择代理
     * @param isTranslucent
     *          是否透明
     * @param isLandscape
     *          是否横屏
     * @param isHandleConfig
     *          配置变化是否仅仅执行onConfiguration方法
     * @param mProcessName
     *          当前插件运行的进程名称
     * @return
     */
    public static String matchActivityProxyByFeature(boolean isTranslucent,
                                              boolean isLandscape,
                                              boolean isHandleConfig,
                                              String mProcessName){
        int index = ProcessManger.getProcessIndex(mProcessName);
        if(index <0 || index >2){
            //越界检查
            PluginDebugLog.log(TAG,"matchActivityProxyByFeature index is out of bounds!");
            index = 1;
        }

        if (isTranslucent) {
            PluginDebugLog.log(TAG,"matchActivityProxyByFeature:"+
                    ComponetFinder.DEFAULT_TRANSLUCENT_ACTIVITY_PROXY_PREFIX +index);
            return ComponetFinder.DEFAULT_TRANSLUCENT_ACTIVITY_PROXY_PREFIX +index;
        } else if (isLandscape) {
            PluginDebugLog.log(TAG,"matchActivityProxyByFeature:"+
                    ComponetFinder.DEFAULT_LANDSCAPE_ACTIVITY_PROXY_PREFIX +index);
            return ComponetFinder.DEFAULT_LANDSCAPE_ACTIVITY_PROXY_PREFIX +index;
        }
        if (isHandleConfig) {
            PluginDebugLog.log(TAG,"matchActivityProxyByFeature:"+
                    ComponetFinder.DEFAULT_CONFIGCHANGE_ACTIVITY_PROXY_PREFIX +index);
            return ComponetFinder.DEFAULT_CONFIGCHANGE_ACTIVITY_PROXY_PREFIX +index;
        } else {
            PluginDebugLog.log(TAG,"matchActivityProxyByFeature:"+
                    ComponetFinder.DEFAULT_ACTIVITY_PROXY_PREFIX +index);
            return ComponetFinder.DEFAULT_ACTIVITY_PROXY_PREFIX +index;
        }

    }

    /**
     * 通过进程名称匹配ServiceProxy
     * @param processName
     *          进程名称
     * @return
     *          Service代理名称
     */
    public static String matchServiceProxyByFeature(String processName){
        String proxyServiceName =
                DEFAULT_SERVICE_PROXY_PREFIX + ProcessManger.getProcessIndex(processName);
        PluginDebugLog.log(TAG,"matchServiceProxyByFeature:"+proxyServiceName);
        return proxyServiceName;
    }



}
