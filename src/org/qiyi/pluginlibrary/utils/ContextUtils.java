package org.qiyi.pluginlibrary.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.qiyi.pluginlibrary.install.PluginInstaller;
import org.qiyi.pluginlibrary.manager.ProxyEnvironment;
import org.qiyi.pluginlibrary.manager.ProxyEnvironmentManager;
import org.qiyi.pluginlibrary.plugin.InterfaceToGetHost;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.text.TextUtils;

public class ContextUtils {
    private static final String TAG = ContextUtils.class.getSimpleName();

    private static final String QIYI_PACKAGE = "com.qiyi";
    private static final String IQIYI_PACKAGE = "com.iqiyi";

    /**
     * Try to get host context in the plugin environment or the param context
     * will be return
     *
     * @param context
     * @return
     */
    public static Context getOriginalContext(Context context) {

        if (context instanceof InterfaceToGetHost) {
            PluginDebugLog.log(TAG, getInvokeInfo() +  "Return host  context for getOriginalContext");
            return ((InterfaceToGetHost) context).getOriginalContext();
        } else {
            if (context instanceof Activity) {
                Context base = ((Activity) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, getInvokeInfo() + "Return host  context for getOriginalContext");
                    return ((InterfaceToGetHost) base).getOriginalContext();
                }
            } else if (context instanceof Application) {
                Context base = ((Application) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, getInvokeInfo() + "Return Application host  context for getOriginalContext");
                    return ((InterfaceToGetHost) base).getOriginalContext();
                }
            } else if (context instanceof Service) {
                Context base = ((Service) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, getInvokeInfo() + "Return Service host  context for getOriginalContext");
                    return ((InterfaceToGetHost) base).getOriginalContext();
                }
            }
            PluginDebugLog.log(TAG, getInvokeInfo() + "Return local context for getOriginalContext");
            return context;
        }
    }

    public static String getTopActivityName(Context context, String packName) {
        String topActivity = getTopActivity(context);
        if (!TextUtils.isEmpty(topActivity)) {
            if (topActivity.startsWith("org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent")
                    || topActivity.startsWith("org.qiyi.pluginlibrary.component.InstrActivityProxy")) {
                return "plugin:" + getTopActivity();
            } else {
                return topActivity;
            }
        }
        return null;
    }

    private static String getTopActivity(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);
        if (runningTaskInfos != null)
            return runningTaskInfos.get(0).topActivity.getClassName();
        else
            return null;
    }

    public static String getTopActivity() {
        String topPackage = null;
        for (Entry<String, ProxyEnvironment> entry : ProxyEnvironmentManager.getAllEnv().entrySet()) {
            String packageName = (String) entry.getKey();
            ProxyEnvironment env = (ProxyEnvironment) entry.getValue();
            if (env.getActivityStackSupervisor().getActivityStack().size() == 0) {
                continue;
            } else if (Util.isResumed(env.getActivityStackSupervisor().getActivityStack().getFirst())) {
                topPackage = packageName;
            }
        }
        return topPackage;
    }

    public static List<String> getRunningPluginPackage(){
        List<String> mRunningPackage = new ArrayList<String>();
        for (Entry<String, ProxyEnvironment> entry : ProxyEnvironmentManager.getAllEnv().entrySet()) {
            String packageName = (String) entry.getKey();
            ProxyEnvironment env = (ProxyEnvironment) entry.getValue();
            if (env.getActivityStackSupervisor().getActivityStack().size() > 0) {
                mRunningPackage.add(packageName);
            }
        }

        return mRunningPackage;
    }

    /**
     * Try to get host ResourcesToolForPlugin in the plugin environment or the
     * ResourcesToolForPlugin with param context will be return
     *
     * @param context
     * @return
     */
    public static ResourcesToolForPlugin getHostResourceTool(Context context) {
        if (context instanceof InterfaceToGetHost) {
            PluginDebugLog.log(TAG, getInvokeInfo() + "Return host  resource tool for getHostResourceTool");
            return ((InterfaceToGetHost) context).getHostResourceTool();
        } else {
            if (context instanceof Activity) {
                Context base = ((Activity) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, getInvokeInfo() + "Return host  resource tool for getHostResourceTool");
                    return ((InterfaceToGetHost) base).getHostResourceTool();
                }
            } else if (context instanceof Application) {
                Context base = ((Application) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, getInvokeInfo() + "Return Application host  resource tool for getHostResourceTool");
                    return ((InterfaceToGetHost) base).getHostResourceTool();
                }
            } else if (context instanceof Service) {
                Context base = ((Service) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, getInvokeInfo() + "Return Service host  resource tool for getHostResourceTool");
                    return ((InterfaceToGetHost) base).getHostResourceTool();
                }
            }
            PluginDebugLog.log(TAG, getInvokeInfo() + "Return local resource tool for getHostResourceTool");
            return new ResourcesToolForPlugin(context);
        }
    }

    /**
     * Get the real package name for this plugin in plugin environment otherwise
     * return context's package name
     *
     * @return
     */
    public static String getPluginPackageName(Context context) {
        if (null == context) {
            return null;
        }
        if (context instanceof InterfaceToGetHost) {
            PluginDebugLog.log(TAG, getInvokeInfo() + "getPluginPackageName context is InterfaceToGetHost!");
            return ((InterfaceToGetHost) context).getPluginPackageName();
        } else {
            if (context instanceof Activity) {
                Context base = ((Activity) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, getInvokeInfo() + "getPluginPackageName context is Activity!");
                    return ((InterfaceToGetHost) base).getPluginPackageName();
                }else if(base instanceof ContextWrapper){
                    return getPluginPackageName(base);
                }
            } else if (context instanceof Application) {
                Context base = ((Application) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, getInvokeInfo() + "getPluginPackageName context is Application!");
                    return ((InterfaceToGetHost) base).getPluginPackageName();
                }else if(base instanceof ContextWrapper){
                    return getPluginPackageName(base);
                }
            } else if (context instanceof Service) {
                Context base = ((Service) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, getInvokeInfo() + "getPluginPackageName context is Service!");
                    return ((InterfaceToGetHost) base).getPluginPackageName();
                }else if(base instanceof ContextWrapper){
                    return getPluginPackageName(base);
                }
            } else if(context instanceof ContextWrapper){
                Context base =((ContextWrapper)context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, getInvokeInfo() + "getPluginPackageName context is ContextWrapper " +
                            "and base is InterfaceToGetHost!");
                    return ((InterfaceToGetHost) base).getPluginPackageName();
                }else if(base instanceof ContextWrapper){
                    //递归调用
                    return getPluginPackageName(base);
                }
            }
            PluginDebugLog.log(TAG, getInvokeInfo() + "getPluginPackageName context dont't match!");
            return context.getPackageName();
        }
    }

    /**
     * Try to exit current app and exit process
     *
     * @param context
     */
    public static void exitApp(Context context) {
        if (null != context) {
            if (context instanceof InterfaceToGetHost) {
                ((InterfaceToGetHost) context).exitApp();
            } else if (context instanceof Activity) {
                Context base = ((Activity) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, "Activity exit");
                    ((InterfaceToGetHost) base).exitApp();
                }
            } else if (context instanceof Application) {
                Context base = ((Application) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, "Application exit");
                    ((InterfaceToGetHost) base).exitApp();
                }
            } else if (context instanceof Service) {
                Context base = ((Service) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, "Service exit");
                    ((InterfaceToGetHost) base).exitApp();
                }
            }
        }
    }

    /**
     * 获取调用者的信息
     *
     */
    private static String getInvokeInfo() {
        if (PluginDebugLog.isDebug()) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                if (stackTraceElement.toString().startsWith(QIYI_PACKAGE) ||
                        stackTraceElement.toString().startsWith(IQIYI_PACKAGE)) {
                    PluginDebugLog.log(TAG, stackTraceElement.toString());
                }
            }
        }
        return "";
    }

    /**
     * @param context
     * @param pkg
     * @return /data/data/com.qiyi.video/app_pluginapp/pkg/databases
     */
    public static String getPluginappDBPath(Context context, String pkg) {
        if (context == null || TextUtils.isEmpty(pkg)) {
            return null;
        }
        return PluginInstaller.getPluginappRootPath(context) + File.separator + pkg + File.separator + "databases";
    }

    public static PackageInfo getPluginPluginInfo(Context context) {
        String pkg = getPluginPackageName(context);
        if (context == null || TextUtils.isEmpty(pkg)) {
            return null;
        }

        ProxyEnvironment proxyNew = ProxyEnvironmentManager.getEnvByPkgName(pkg);

        if (proxyNew == null) {
            return null;
        }
        PackageInfo pkgInfo = proxyNew.getTargetPackageInfo();
        return pkgInfo;
    }

    /**
     * 判断是否Android N系统
     */
    public static boolean isAndroidN() {
        return isParticularAndroidVersion(24);
    }

    /**
     * 判断是否Android O系统
     */
    public static boolean isAndroidO() {
        return isParticularAndroidVersion(25);
    }

    /**
     * 判断当前系统是否是Android N或者Android O
     *
     * @param sdk       Android SDK值（24或者25）
     */
    private static boolean isParticularAndroidVersion(int sdk) {
        int compareSDKValue = 0;
        String compareSDKName = "";
        if (sdk == 24) {
            compareSDKValue = Build.VERSION_CODES.N;
            compareSDKName = "N";
        }
        if (sdk == 25) {
            compareSDKValue = 25;
            compareSDKName = "O";
        }
        return Build.VERSION.SDK_INT == compareSDKValue
                || TextUtils.equals(Build.VERSION.CODENAME, compareSDKName)
                || TextUtils.equals(Build.VERSION.RELEASE, compareSDKName);
    }

    /**
     * 通知调用方可以取消loading progress dialog
     *
     * @param context
     * @param intent
     */
    public static void notifyHostPluginStarted(Context context, Intent intent) {
        if (null != context && null != intent && !TextUtils.isEmpty(intent.getStringExtra(
                ProxyEnvironmentManager.EXTRA_SHOW_LOADING))) {
            context.sendBroadcast(new Intent(ProxyEnvironmentManager.EXTRA_SHOW_LOADING));
        }
    }
}
