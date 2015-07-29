package org.qiyi.pluginlibrary.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

public class ContextUtils {
	private static final String TAG = ContextUtils.class.getSimpleName();

	/**
	 * Try to get host context in the plugin environment or the param context
	 * will be return
	 * 
	 * @param context
	 * @return
	 */
	public static Context getOriginalContext(Context context) {
		if (null == context) {
			return null;
		}
		Class<?> aClass = context.getClass();
		Object receiver = context;
		if (context instanceof Activity
				&& !TextUtils.equals(aClass.getSimpleName(), "InstrActivityProxy")
				&& !TextUtils.equals(aClass.getSimpleName(), "InstrActivityProxyTranslucent")) {
			receiver = ((Activity) context).getBaseContext();
			aClass = receiver.getClass();
		}
		try {
			Method method = aClass.getDeclaredMethod("getOriginalContext", new Class<?>[] {});
			if (null != method) {
				Object result = method.invoke(receiver, new Object[] {});
				if (result instanceof Context) {
					PluginDebugLog.log(TAG, "Return host  context for getOriginalContext");
					return (Context) result;
				}
			}
		} catch (NoSuchMethodException e) {
			if (PluginDebugLog.isDebug()) {
				e.printStackTrace();
			}
		} catch (IllegalAccessException e) {
			if (PluginDebugLog.isDebug()) {
				e.printStackTrace();
			}
		} catch (IllegalArgumentException e) {
			if (PluginDebugLog.isDebug()) {
				e.printStackTrace();
			}
		} catch (InvocationTargetException e) {
			if (PluginDebugLog.isDebug()) {
				e.printStackTrace();
			}
		}
		PluginDebugLog.log(TAG, "Return local context for getOriginalContext");
		return context;
	}

	/**
	 * Try to get host context in the plugin environment or the param activity
	 * will be return
	 * 
	 * @param activity
	 * @return
	 */
	public static Context getOriginalContext(Activity activity) {
		if (null == activity) {
			return null;
		}
		Context base = activity.getBaseContext();
		Class<?> aClass = base.getClass();
		if (TextUtils.equals(aClass.getSimpleName(), "InstrActivityProxy")
				|| TextUtils.equals(aClass.getSimpleName(), "InstrActivityProxyTranslucent")) {
			return getOriginalContext(base);
		}
		return activity;
	}

	/**
	 * Try to get host ResourcesToolForPlugin in the plugin environment or the
	 * ResourcesToolForPlugin with param context will be return
	 * 
	 * @param context
	 * @return
	 */
	public static ResourcesToolForPlugin getHostResourceTool(Context context) {
		if (null == context) {
			return null;
		}
		Class<?> aClass = context.getClass();
		try {
			Method method = aClass.getDeclaredMethod("getHostResourceTool", new Class<?>[] {});
			if (null != method) {
				Object result = method.invoke(context, new Object[] {});
				if (result instanceof ResourcesToolForPlugin) {
					PluginDebugLog.log(TAG, "Return host  resource tool for getHostResourceTool");
					return (ResourcesToolForPlugin) result;
				}
			}
		} catch (NoSuchMethodException e) {
			if (PluginDebugLog.isDebug()) {
				e.printStackTrace();
			}
		} catch (IllegalAccessException e) {
			if (PluginDebugLog.isDebug()) {
				e.printStackTrace();
			}
		} catch (IllegalArgumentException e) {
			if (PluginDebugLog.isDebug()) {
				e.printStackTrace();
			}
		} catch (InvocationTargetException e) {
			if (PluginDebugLog.isDebug()) {
				e.printStackTrace();
			}
		}
		PluginDebugLog.log(TAG, "Return local context for getHostResourceTool");
		return new ResourcesToolForPlugin(context);
	}

	/**
	 * Try to get host ResourcesToolForPlugin in the plugin environment or the
	 * ResourcesToolForPlugin with param activity will be return
	 * 
	 * @param activity
	 * @return
	 */
	public static ResourcesToolForPlugin getHostResourceTool(Activity activity) {
		if (null == activity) {
			return null;
		}
		Context base = activity.getBaseContext();
		Class<?> aClass = base.getClass();
		if (TextUtils.equals(aClass.getSimpleName(), "InstrActivityProxy")
				|| TextUtils.equals(aClass.getSimpleName(), "InstrActivityProxyTranslucent")) {
			return getHostResourceTool(base);
		}
		return new ResourcesToolForPlugin(activity);
	}
}
