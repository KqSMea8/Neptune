package org.qiyi.pluginlibrary.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.install.PluginInstaller;
import org.qiyi.pluginlibrary.plugin.InterfaceToGetHost;
import org.qiyi.pluginlibrary.runtime.PluginLoadedApk;
import org.qiyi.pluginlibrary.runtime.PluginManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class ContextUtils {
    private static final String TAG = ContextUtils.class.getSimpleName();

    /**
     * Try to get host context in the plugin environment or the param context
     * will be return
     */
    public static Context getOriginalContext(Context context) {

        if (context instanceof InterfaceToGetHost) {
            PluginDebugLog.log(TAG, "Return host  context for getOriginalContext");
            return ((InterfaceToGetHost) context).getOriginalContext();
        } else {
            if (context instanceof Activity) {
                Context base = ((Activity) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, "Return host  context for getOriginalContext");
                    return ((InterfaceToGetHost) base).getOriginalContext();
                } else if (base instanceof ContextWrapper) {
                    return getOriginalContext(base);
                }
            } else if (context instanceof Application) {
                Context base = ((Application) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, "Return Application host  context for getOriginalContext");
                    return ((InterfaceToGetHost) base).getOriginalContext();
                } else if (base instanceof ContextWrapper) {
                    return getOriginalContext(base);
                }
            } else if (context instanceof Service) {
                Context base = ((Service) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, "Return Service host  context for getOriginalContext");
                    return ((InterfaceToGetHost) base).getOriginalContext();
                } else if (base instanceof ContextWrapper) {
                    return getOriginalContext(base);
                }
            } else if (context instanceof ContextWrapper) {
                Context base = ((ContextWrapper) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, "getPluginPackageName context is ContextWrapper " +
                            "and base is InterfaceToGetHost!");
                    return ((InterfaceToGetHost) base).getOriginalContext();
                } else if (base instanceof ContextWrapper) {
                    // 递归调用
                    // DecorView的Context是com.android.internal.policy.DecorContext
                    // https://android.googlesource.com/platform/frameworks/base.git/+/master/core/java/com/android/internal/policy/DecorContext.java
                    return getOriginalContext(base);
                }
            }
            return context;
        }
    }

    public static String getTopActivityName(Context context, String packName) {
        String topActivity = getTopActivity(context);
        if (!TextUtils.isEmpty(topActivity)) {
            if (topActivity.startsWith("org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent1")
                    || topActivity.startsWith("org.qiyi.pluginlibrary.component.InstrActivityProxy1")) {
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
        for (Entry<String, PluginLoadedApk> entry : PluginManager.getAllPluginLoadedApk().entrySet()) {
            String packageName = (String) entry.getKey();
            PluginLoadedApk mLoadedApk = (PluginLoadedApk) entry.getValue();
            Activity topActivity = mLoadedApk.getActivityStackSupervisor().getTopActivity();
            if (topActivity != null && Util.isResumed(topActivity)) {
                topPackage = packageName;
                break;
            }
        }
        return topPackage;
    }

    public static List<String> getRunningPluginPackage() {
        List<String> mRunningPackage = new ArrayList<String>();
        for (Entry<String, PluginLoadedApk> entry : PluginManager.getAllPluginLoadedApk().entrySet()) {
            String packageName = (String) entry.getKey();
            PluginLoadedApk mLoadedApk = (PluginLoadedApk) entry.getValue();
            if (mLoadedApk.getActivityStackSupervisor().hasActivityRunning()) {
                mRunningPackage.add(packageName);
            }
        }

        return mRunningPackage;
    }

    /**
     * Try to get host ResourcesToolForPlugin in the plugin environment or the
     * ResourcesToolForPlugin with param context will be return
     */
    public static ResourcesToolForPlugin getHostResourceTool(Context context) {
        if (context instanceof InterfaceToGetHost) {
            PluginDebugLog.log(TAG, "Return host  resource tool for getHostResourceTool");
            return ((InterfaceToGetHost) context).getHostResourceTool();
        } else {
            if (context instanceof Activity) {
                Context base = ((Activity) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, "Return host  resource tool for getHostResourceTool");
                    return ((InterfaceToGetHost) base).getHostResourceTool();
                }
            } else if (context instanceof Application) {
                Context base = ((Application) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, "Return Application host  resource tool for getHostResourceTool");
                    return ((InterfaceToGetHost) base).getHostResourceTool();
                }
            } else if (context instanceof Service) {
                Context base = ((Service) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, "Return Service host  resource tool for getHostResourceTool");
                    return ((InterfaceToGetHost) base).getHostResourceTool();
                }
            }
            PluginDebugLog.log(TAG, "Return local resource tool for getHostResourceTool");
            return new ResourcesToolForPlugin(context);
        }
    }

    /**
     * Get the real package name for this plugin in plugin environment otherwise
     * return context's package name
     */
    public static String getPluginPackageName(Context context) {
        if (null == context) {
            return null;
        }
        if (context instanceof InterfaceToGetHost) {
            PluginDebugLog.log(TAG, "getPluginPackageName context is InterfaceToGetHost!");
            return ((InterfaceToGetHost) context).getPluginPackageName();
        } else {
            if (context instanceof Activity) {
                Context base = ((Activity) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, "getPluginPackageName context is Activity!");
                    return ((InterfaceToGetHost) base).getPluginPackageName();
                } else if (base instanceof ContextWrapper) {
                    return getPluginPackageName(base);
                }
            } else if (context instanceof Application) {
                Context base = ((Application) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, "getPluginPackageName context is Application!");
                    return ((InterfaceToGetHost) base).getPluginPackageName();
                } else if (base instanceof ContextWrapper) {
                    return getPluginPackageName(base);
                }
            } else if (context instanceof Service) {
                Context base = ((Service) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, "getPluginPackageName context is Service!");
                    return ((InterfaceToGetHost) base).getPluginPackageName();
                } else if (base instanceof ContextWrapper) {
                    return getPluginPackageName(base);
                }
            } else if (context instanceof ContextWrapper) {
                Context base = ((ContextWrapper) context).getBaseContext();
                if (base instanceof InterfaceToGetHost) {
                    PluginDebugLog.log(TAG, "getPluginPackageName context is ContextWrapper " +
                            "and base is InterfaceToGetHost!");
                    return ((InterfaceToGetHost) base).getPluginPackageName();
                } else if (base instanceof ContextWrapper) {
                    //递归调用
                    return getPluginPackageName(base);
                }
            }
            //PluginDebugLog.log(TAG, getInvokeInfo() + "getPluginPackageName context dont't match!");
            return context.getPackageName();
        }
    }

    /**
     * Try to exit current app and exit process
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

        PluginLoadedApk mLoadedApk = PluginManager.getPluginLoadedApkByPkgName(pkg);

        if (mLoadedApk == null) {
            return null;
        }
        PackageInfo pkgInfo = mLoadedApk.getPackageInfo();
        return pkgInfo;
    }
}
