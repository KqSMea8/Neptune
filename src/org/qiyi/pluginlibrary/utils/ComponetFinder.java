package org.qiyi.pluginlibrary.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.pm.ApkTargetMappingNew;
import org.qiyi.pluginlibrary.ProxyComponentMappingByProcess;
import org.qiyi.pluginlibrary.component.InstrActivityProxy;
import org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent;
import org.qiyi.pluginlibrary.constant.IIntentConstant;
import org.qiyi.pluginlibrary.debug.PluginCenterDebugHelper;
import org.qiyi.pluginlibrary.plugin.TargetMapping;
import org.qiyi.pluginlibrary.pm.CMPackageInfo;
import org.qiyi.pluginlibrary.pm.CMPackageManagerImpl;
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

    /**
     * 在插件中查找可以处理mIntent的Service组件,找到之后为其分配合适的Proxy
     *
     * @param mLoadedApk 插件的{@link PluginLoadedApk}对象
     * @param mIntent    需要查找的Intent
     */
    public static void findSuitableServiceByIntent(PluginLoadedApk mLoadedApk, Intent mIntent) {
        if (mIntent == null || mLoadedApk == null) {
            return;
        }
        if (mIntent.getComponent() == null) {
            //隐式启动
            ServiceInfo mServiceInfo = mLoadedApk.getPluginMapping().resolveService(mIntent);
            if (mServiceInfo != null) {
                findSuitableServiceByIntent(mLoadedApk, mIntent, mServiceInfo.name);
            }
            return;
        } else {
            //显示启动
            String targetService = mIntent.getComponent().getClassName();
            findSuitableServiceByIntent(mLoadedApk, mIntent, targetService);
        }
    }


    /**
     * 在插件中查找可以处理mIntent的Service组件,找到之后为其分配合适的Proxy
     *
     * @param mLoadedApk    插件的{@link PluginLoadedApk}对象
     * @param mIntent       需要查找的Intent
     * @param targetService 处理当前Intent的组件的类名
     */
    public static void findSuitableServiceByIntent(PluginLoadedApk mLoadedApk,
                                                   Intent mIntent,
                                                   String targetService) {
        if (null == mLoadedApk || null == mIntent || TextUtils.isEmpty(targetService)
                || mLoadedApk.getPluginMapping().getServiceInfo(targetService) == null) {
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
                    Class.forName(ProxyComponentMappingByProcess.mappingService(mLoadedApk.getProcessName())));
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
    public static Intent findSuitableActivityByIntent(String mPluginPackageName,
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
                    TargetMapping thisPlugin = mLoadedApk.getPluginMapping();
                    targetActivity = thisPlugin.getActivityInfo(toActName);
                }

            }

            if (targetActivity == null) {
                mLoadedApk = PluginManager.getPluginLoadedApkByPkgName(pkg);
                // Check in pkg's apk
                if (!TextUtils.isEmpty(pkg) && mLoadedApk != null) {
                    TargetMapping otherPlug = mLoadedApk.getPluginMapping();
                    if (otherPlug != null) {
                        targetActivity = otherPlug.getActivityInfo(toActName);
                    }
                }
            }

        } else {
            if (mLoadedApk != null) {
                TargetMapping mapping = mLoadedApk.getPluginMapping();
                if (mapping != null) {
                    targetActivity = mapping.resolveActivity(mIntent);
                }
            } else {
                if (null != context) {
                    List<CMPackageInfo> packageList = CMPackageManagerImpl.getInstance(context).getInstalledApps();
                    if (packageList != null) {
                        for (CMPackageInfo pkgInfo : CMPackageManagerImpl.getInstance(context).getInstalledApps()) {
                            if (pkgInfo != null) {
                                ApkTargetMappingNew target = pkgInfo.getTargetMapping(context);
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
            if (mIntent.getComponent().getClassName().startsWith(InstrActivityProxy.class.getName())
                    || mIntent.getComponent().getClassName()
                    .startsWith(InstrActivityProxyTranslucent.class.getName())) {
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
        ActivityInfo info = mLoadedApk.getPluginMapping().getActivityInfo(activityName);
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

        return ProxyComponentMappingByProcess.mappingActivity(isTranslucent, isLandscape,
                isHandleConfigChange,mLoadedApk.getProcessName());
    }


}