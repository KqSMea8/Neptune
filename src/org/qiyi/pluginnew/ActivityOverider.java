package org.qiyi.pluginnew;

import java.lang.reflect.Field;

import org.qiyi.plugin.manager.ProxyEnvironmentNew;
import org.qiyi.pluginnew.context.CMContextWrapperNew;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
		return ActivityJumpUtil.handleStartActivityIntent(pluginId, intent, requestCode, options);
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
	public static Intent overrideStartActivityFromFragment(Activity fromAct, String pluginId,
			Intent intent, int requestCode, Bundle options) {
		Log.d("TAG", "Enter overrideStartActivityFromFragmen: pluginId: " + pluginId + " intent: "
				+ intent + " bundle: " + options);
		return ActivityJumpUtil.handleStartActivityIntent(pluginId, intent, requestCode, options);
	}

	public static Object[] overrideAttachBaseContext(final String pluginId,final Activity fromAct,Context base){
	
		Log.i(tag, "overrideAttachBaseContext: pluginId="+pluginId+", activity="+fromAct.getClass().getSuperclass().getName()
				);
		ProxyEnvironmentNew env = ProxyEnvironmentNew.getInstance(pluginId);
//		try {
//			Object loadedApk = ReflectionUtils.getFieldValue(base, "mPackageInfo");
//			Log.d(tag, "loadedApk = " + loadedApk);
//			ReflectionUtils.setFieldValue(loadedApk, "mClassLoader", env.getDexClassLoader());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		CMContextWrapperNew actWrapper = new CMContextWrapperNew(base, pluginId);
//		PluginActivityWrapper actWrapper = new PluginActivityWrapper(base, env.appWrapper, env);
		return new Object[] { actWrapper, env.getTargetAssetManager() };
	}
	
	public static void changeActivityInfo(Activity activity, String pkgName, String actName) {
		Log.d(tag, "changeActivityInfo: activity = " + activity + ", class = " + actName);
		ActivityInfo origActInfo = null;
		try {
			Field field_mActivityInfo = Activity.class.getDeclaredField("mActivityInfo");
			field_mActivityInfo.setAccessible(true);
			origActInfo = (ActivityInfo) field_mActivityInfo.get(activity);
		} catch (Exception e) {
			Log.e(tag, Log.getStackTraceString(e));
			return;
		}
		ProxyEnvironmentNew con = ProxyEnvironmentNew.getInstance(pkgName);

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
			changeActivityInfo(fromAct, pluginId, actName);
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
					changeActivityInfo(fromAct, pluginId, actName);
					fromAct.setTheme(resTheme);
				}
			}
		}
		if (fromAct.getParent() == null) {
			con.pushActivityToStack(fromAct);
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
		if (fromAct != null && fromAct.getParent() == null) { // 如果是子Activity，不进入栈管理
			ProxyEnvironmentNew con = ProxyEnvironmentNew.getInstance(pluginId);
			if (con != null) {
				con.popActivityFromStack(fromAct);
			}
		}
	}
}
