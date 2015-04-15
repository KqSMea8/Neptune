package org.qiyi.pluginnew;

import java.io.File;
import java.lang.reflect.Field;

import org.qiyi.plugin.manager.ProxyEnvironmentNew;
import org.qiyi.pluginlibrary.ErrorType.ErrorType;
import org.qiyi.pluginlibrary.plugin.TargetMapping;
import org.qiyi.pluginlibrary.pm.CMPackageInfo;
import org.qiyi.pluginlibrary.pm.CMPackageManager;
import org.qiyi.pluginnew.context.CMContextWrapperNew;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;

/**
 * 提供公共方法供自动生成的Activity调用
 */
public class ActivityOverider {
	private static final String tag = "ActivityOverider";
	/**
	 * 自动生成的 Activity 的全类名
	 */
	public static final String targetClassName = "org.qiyi.PluginActivity";
	public static final String PLUGIN_ID = "_pluginId";
	public static final String PLUGIN_ACTIVITY = "_targetAct";
    // ------------------- process service  ---------
	/**
	 * 覆盖 StarService 方法
	 * 
	 * @param intent
	 * @param fromAct
	 */
	public static ComponentName overrideStartService(Activity fromAct,String pluginId,Intent intent) {
		//TODO 覆盖 StarService 方法
		Log.d(tag, "overrideStartService");
		return fromAct.startService(intent);
	}
	public static boolean overrideBindService(Activity fromAct,String pluginId,Intent intent,ServiceConnection conn, int flags) {
		//TODO overrideBindService
		Log.d(tag, "overrideBindService");
		return fromAct.bindService(intent, conn, flags);
	}
	public static void overrideUnbindService(Activity fromAct,String pluginId,ServiceConnection conn) {
		//TODO overrideUnbindService
		Log.d(tag, "overrideUnbindService");
		fromAct.unbindService( conn);
	}
	public static boolean overrideStopService(Activity fromAct,String pluginId,Intent intent){
		//TODO overrideStopService
		Log.d(tag, "overrideStopService");
		return fromAct.stopService(intent);
	}
	
	public static Context overrideGetOriginalContext(Activity fromActivitym, String pluginId) {
		if (TextUtils.isEmpty(pluginId)) {
			return null;
		}
		ProxyEnvironmentNew mgr = ProxyEnvironmentNew.getInstance(pluginId);
		if (mgr != null) {
			return mgr.getHostContext();
		}
		return null;
	}

	// ------------------ process Activity ---------------------------
	private static Intent handleStartActivityIntent(Activity fromAct, String pluginId,
			Intent intent, int requestCode, Bundle options) {
		ActivityInfo targetActivity = null;
		// 主要做以下工作：
		// 1 、修改Intent的跳转目标
		// 2 、帮助插件类加载器决定使用哪个activity类加载器
		// 优先判断类名，若类名为空再判断 Action
		if (intent.getComponent() != null && intent.getComponent().getClassName() != null) {
			// action 为空，但是指定了包名和 activity类名
			ComponentName compname = intent.getComponent();
			String pkg = compname.getPackageName();
			String toActName = compname.getClassName();
			ProxyEnvironmentNew mgr = ProxyEnvironmentNew.getInstance(pluginId);
			// First find in the current apk
			if (mgr != null) {
				if (TextUtils.equals(pkg, pluginId)) {
					TargetMapping thisPlugin = mgr.getTargetMapping();
					targetActivity = thisPlugin.getActivityInfo(toActName);
				}
			}
			// Second find from other init plugin apk
			if (targetActivity == null) {
				// Check in pkg's apk
				if (!TextUtils.isEmpty(pkg) && ProxyEnvironmentNew.getInstance(pkg) != null) {
					TargetMapping otherPlug = ProxyEnvironmentNew.getInstance(pkg)
							.getTargetMapping();
					if (otherPlug != null) {
						targetActivity = otherPlug.getActivityInfo(toActName);
					}
				}
				// check in all other installed apk, but hasn't init.
				if (targetActivity == null) {
					ProxyEnvironmentNew originalEnv = ProxyEnvironmentNew.getInstance(pluginId);
					CMPackageManager pkm = CMPackageManager.getInstance(originalEnv
							.getHostContext());
					for (CMPackageInfo plugInfo : pkm.getInstalledApps()) {
						if (TextUtils.equals(pkg, plugInfo.packageName)
								&& !TextUtils.equals(plugInfo.packageName, pluginId)) {
							targetActivity = new ActivityInfo();
							targetActivity.packageName = pkg;
							targetActivity.name = toActName;
							break;
						}
					}
				}
			}
		} else {
			ProxyEnvironmentNew mgr = ProxyEnvironmentNew.getInstance(pluginId);
			if (mgr != null) {
				TargetMapping mapping = mgr.getTargetMapping();
				if (mapping != null) {
					targetActivity = mapping.resolveActivity(intent);
				}
			} else {
				// TODO CMPackageManager keep all intent filter, then loop from
				// CMPackageManager
			}
		}
		Log.d("TAG",
				"handleStartActivityIntent pluginId: " + pluginId + " intent: " + intent.toString()
						+ " targetActivity: " + targetActivity);
		if (targetActivity != null) {
			setPluginIntent(intent, targetActivity.packageName, targetActivity.name);
		}
		return intent;
	}

	/**
	 * 处理 插件Activity 通过 intent 跳转到别的Activity
	 * <p>
	 * 供插件中的 startActivity 调用
	 * 
	 * @param fromAct
	 *            - 发出请求的Activity
	 * @param pluginId
	 *            - 插件id
	 * @param intent
	 *            - 启动其他Activity的Intent请求
	 * @param requestCode
	 * @param options
	 * @return 修改后的 Intent
	 */
	public static Intent overrideStartActivityForResult(Activity fromAct,
			String pluginId, Intent intent, int requestCode, Bundle options) {
		Log.d("TAG", "Enter overrideStartActivityForResult: pluginId: "
				+ pluginId + " intent: " + intent + " bundle: " + options);
		return handleStartActivityIntent(fromAct, pluginId, intent,
				requestCode, options);
	}

	/**
	 * 处理 插件Activity 通过 intent 跳转到别的Activity
	 * <p>
	 * 供插件中的 startActivity 调用
	 * 
	 * @param fromAct
	 *            - 发出请求的Activity
	 * @param pluginId
	 *            - 插件id
	 * @param intent
	 *            - 启动其他Activity的Intent请求
	 * @param requestCode
	 * @param options
	 * @return 修改后的 Intent
	 */
	public static Intent overrideStartActivityFromFragment(Activity fromAct,
			String pluginId, Intent intent, int requestCode, Bundle options) {
		Log.d("TAG", "Enter overrideStartActivityFromFragmen: pluginId: "
				+ pluginId + " intent: " + intent + " bundle: " + options);
		return handleStartActivityIntent(fromAct, pluginId, intent,
				requestCode, options);
	}

	private static void setPluginIntent(Intent intent, String pluginId, String actName) {
		ProxyEnvironmentNew env = ProxyEnvironmentNew.getInstance(pluginId);
		createProxyDex(pluginId, actName, env.getProxyComponentDexPath(pluginId, actName));
		ComponentName compname = new ComponentName(env.getParentPackagename(), targetClassName);
		intent.setComponent(compname).putExtra(ActivityOverider.PLUGIN_ID, pluginId)
				.putExtra(ActivityOverider.PLUGIN_ACTIVITY, actName);
	}

	public static void createProxyDex(String pkgName, String activityClsName, File saveDir) {
		if (saveDir.exists()) {
			return;
		}
//		 Log.d(tag, "actName=" + activityClsName + ", saveDir=" + saveDir);
		try {
			ActivityClassGenerator.createActivityDex(activityClsName, targetClassName,
					saveDir, pkgName, pkgName);
		} catch (Throwable e) {
		    ProxyEnvironmentNew.deliverPlug(false, pkgName, ErrorType.ERROR_CLIENT_CREATE_ACTIVITY_DEX_FAIL);
			Log.e(tag, Log.getStackTraceString(e));
		}
	}
	
	public static Object[] overrideAttachBaseContext(final String pluginId,final Activity fromAct,Context base){
	
		Log.i(tag, "overrideAttachBaseContext: pluginId="+pluginId+", activity="+fromAct.getClass().getSuperclass().getName()
				);
		ProxyEnvironmentNew env = ProxyEnvironmentNew.getInstance(pluginId);
		try {
			Object loadedApk = ReflectionUtils.getFieldValue(base, "mPackageInfo");
			Log.d(tag, "loadedApk = " + loadedApk);
			ReflectionUtils.setFieldValue(loadedApk, "mClassLoader", env.getDexClassLoader());
		} catch (Exception e) {
			e.printStackTrace();
		}
		CMContextWrapperNew actWrapper = new CMContextWrapperNew(base);
		actWrapper.setTargetPackagename(pluginId);
//		PluginActivityWrapper actWrapper = new PluginActivityWrapper(base, env.appWrapper, env);
		return new Object[] { actWrapper, env.getTargetAssetManager() };
	}
	
	private static void changeActivityInfo(Activity activity) {
		final String actName = activity.getClass().getSuperclass().getName();
		Log.d(tag, "changeActivityInfo: activity = " + activity + ", class = " + actName);
		if (!activity.getClass().getName().equals(targetClassName)) {
			Log.w(tag, "not a Proxy Activity ,then return.");
			return;
		}
		ActivityInfo origActInfo = null;
		try {
			Field field_mActivityInfo = Activity.class.getDeclaredField("mActivityInfo");
			field_mActivityInfo.setAccessible(true);
			origActInfo = (ActivityInfo) field_mActivityInfo.get(activity);
		} catch (Exception e) {
			Log.e(tag, Log.getStackTraceString(e));
			return;
		}
		ProxyEnvironmentNew con = ProxyEnvironmentNew.getInstance(activity.getPackageName());

		ActivityInfo actInfo = con.findActivityByClassName(actName);
		if (null != actInfo) {
			actInfo.applicationInfo = con.getTargetMapping().getPackageInfo().applicationInfo;
			if (origActInfo != null) {
				origActInfo.applicationInfo = actInfo.applicationInfo;
				origActInfo.configChanges = actInfo.configChanges;
				origActInfo.descriptionRes = actInfo.descriptionRes;
				origActInfo.enabled = actInfo.enabled;
				origActInfo.exported = actInfo.exported;
				origActInfo.flags = actInfo.flags;
				origActInfo.icon = actInfo.icon;
				origActInfo.labelRes = actInfo.labelRes;
				origActInfo.logo = actInfo.logo;
				origActInfo.metaData = actInfo.metaData;
				origActInfo.name = actInfo.name;
				origActInfo.nonLocalizedLabel = actInfo.nonLocalizedLabel;
				origActInfo.packageName = actInfo.packageName;
				origActInfo.permission = actInfo.permission;
				// origActInfo.processName
				origActInfo.screenOrientation = actInfo.screenOrientation;
				origActInfo.softInputMode = actInfo.softInputMode;
				origActInfo.targetActivity = actInfo.targetActivity;
				origActInfo.taskAffinity = actInfo.taskAffinity;
				origActInfo.theme = actInfo.theme;
			}
		}
		// Handle ActionBar title
		if (origActInfo.nonLocalizedLabel != null) {
			activity.setTitle(origActInfo.nonLocalizedLabel);
		} else if (origActInfo.labelRes != 0) {
			activity.setTitle(origActInfo.labelRes);
		} else {
			if (origActInfo.applicationInfo != null) {
				if (origActInfo.applicationInfo.nonLocalizedLabel != null) {
					activity.setTitle(origActInfo.applicationInfo.nonLocalizedLabel);
				} else if (origActInfo.applicationInfo.labelRes != 0) {
					activity.setTitle(origActInfo.applicationInfo.labelRes);
				} else {
					activity.setTitle(origActInfo.applicationInfo.packageName);
				}
			}
		}
		Log.i(tag, "changeActivityInfo->changeTheme: " + " theme = " + actInfo.getThemeResource()
				+ ", icon = " + actInfo.getIconResource() + ", logo = " + actInfo.logo
				+ ", labelRes" + actInfo.labelRes);
	}
	
	public static int getPlugActivityTheme(Activity fromAct,String pluginId) {
		ProxyEnvironmentNew con = ProxyEnvironmentNew.getInstance(pluginId);
		String actName = fromAct.getClass().getSuperclass().getName();
		ActivityInfo actInfo = con.getTargetMapping().getActivityInfo(actName);
		if (actInfo != null) {
			int rs =  actInfo.getThemeResource();
//		if (rs == 0) {
//			rs = android.R.style.Theme;
//		}
			Log.d(tag, "getPlugActivityTheme: theme=" + rs + ", actName=" + actName);
			changeActivityInfo(fromAct);
			return rs;
		} else {
			return 0;
		}
	}
	
	/**
	 * 按下back键的方法调用
	 * 
	 * @param pluginId
	 * @param fromAct
	 * @return 是否调用父类的onBackPressed()方法
	 */
	public static boolean overrideOnbackPressed(Activity fromAct,String pluginId) {
		ProxyEnvironmentNew env = ProxyEnvironmentNew.getInstance(pluginId);
		String actName = fromAct.getClass().getSuperclass().getName();
		ActivityInfo actInfo = env.findActivityByClassName(actName);
//		boolean finish = plinfo.isFinishActivityOnbackPressed(actInfo);
//		if (finish) {
//			fromAct.finish();
//		}
//		boolean ivsuper = plinfo.isInvokeSuperOnbackPressed(actInfo);
//		Log.d(tag, "finish? " + finish + ", ivsuper? " + ivsuper);
//		return ivsuper;
		return true;
	}

	//
	// =================== Activity 生命周期回调方法 ==================
	//
	public static void callback_onCreate(String pluginId, Activity fromAct) {
		Log.d(tag, "callback_onCreate(act=" + fromAct.getClass().getSuperclass().getName()
				+ ", window=" + fromAct.getWindow() + ")");
		ProxyEnvironmentNew con = ProxyEnvironmentNew.getInstance(pluginId);
//		PlugInfo plugin = con.getPluginById(pluginId);
		// replace Application
		try {
			Field applicationField = Activity.class
					.getDeclaredField("mApplication");
			applicationField.setAccessible(true);
			applicationField.set(fromAct, con.getApplication());
		}  catch (Exception e) {
			e.printStackTrace();
		}

		String actName = fromAct.getClass().getSuperclass().getName();
		ActivityInfo actInfo = con.findActivityByClassName(actName);
		if (actInfo != null) {
			int resTheme = actInfo.getThemeResource();
			if (resTheme != 0) {
				boolean hasNotSetTheme = true;
				try {
					Field mTheme = ContextThemeWrapper.class.getDeclaredField("mTheme");
					mTheme.setAccessible(true);
					hasNotSetTheme = mTheme.get(fromAct) == null;
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (hasNotSetTheme) {
					changeActivityInfo(fromAct);
					fromAct.setTheme(resTheme);
				}
			}
		}
	}

	public static void callback_onResume(String pluginId, Activity fromAct) {
	}

	public static void callback_onStart(String pluginId, Activity fromAct) {
	}

	public static void callback_onRestart(String pluginId, Activity fromAct) {
	}

	public static void callback_onPause(String pluginId, Activity fromAct) {
	}

	public static void callback_onStop(String pluginId, Activity fromAct) {
	}

	public static void callback_onDestroy(String pluginId, Activity fromAct) {
	}
}
