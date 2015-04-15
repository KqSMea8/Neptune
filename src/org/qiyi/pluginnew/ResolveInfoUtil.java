package org.qiyi.pluginnew;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.qiyi.pluginnew.ApkTargetMappingNew.ActivityIntentInfo;
import org.qiyi.pluginnew.ApkTargetMappingNew.ReceiverIntentInfo;
import org.qiyi.pluginnew.ApkTargetMappingNew.ServiceIntentInfo;

import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;
import android.util.DisplayMetrics;

/**
 * Utility class to get activity/receiver/service's resolve info
 * 
 * @author huangbo
 * 
 */
public class ResolveInfoUtil {
	/**
	 * Get activity/receiver/service's resolve info
	 * 
	 * @param dexPath apk path
	 * @param target
	 */
	public static void parseResolveInfo(String dexPath, ApkTargetMappingNew target) {

		try { // 先得到解析类PackageParser并实例化
			Class<?> packageParserClass = Class.forName("android.content.pm.PackageParser");
			Object packageParser = packageParserClass.getConstructor(String.class).newInstance(
					dexPath);
			// 构建参数
			DisplayMetrics metrics = new DisplayMetrics();
			metrics.setToDefaults();
			File sourceFile = new File(dexPath);
			// 调用PackageParser的parsePackage解析数据
			Method method = packageParserClass.getDeclaredMethod("parsePackage", File.class,
					String.class, DisplayMetrics.class, int.class);
			method.setAccessible(true);
			Object pkg = method.invoke(packageParser, sourceFile, dexPath, metrics, 0);
			if (null != pkg) {
				// 获取Activity
				Field activities = pkg.getClass().getDeclaredField("activities");
				activities.setAccessible(true);
				ArrayList<?> activityFilters = (ArrayList<?>) activities.get(pkg);
				for (int i = 0; i < activityFilters.size(); i++) {
					Object activity = activityFilters.get(i);
					Field intentsClassName = activity.getClass().getField("className");
					intentsClassName.setAccessible(true);
					String className = (String) intentsClassName.get(activity);
					System.out.println("className: " + className);
					ActivityInfo info = target.findActivityByClassName(className);
					if (null != info) {
						ActivityIntentInfo actInfo = new ActivityIntentInfo(info);
						Field intentsField = activity.getClass().getField("intents");
						intentsField.setAccessible(true);
						@SuppressWarnings("unchecked")
						ArrayList<IntentFilter> intents = (ArrayList<IntentFilter>) intentsField
								.get(activity);
						actInfo.setFilter(intents);
						target.addActivity(actInfo);
					}
				}

				// 获取Receivers
				Field receivers = pkg.getClass().getDeclaredField("receivers");
				receivers.setAccessible(true);
				ArrayList<?> receiverFilters = (ArrayList<?>) receivers.get(pkg);
				for (int i = 0; i < receiverFilters.size(); i++) {
					Object receiver = receiverFilters.get(i);

					Field intentsClassName = receiver.getClass().getField("className");
					intentsClassName.setAccessible(true);
					String className = (String) intentsClassName.get(receiver);
					System.out.println("className: " + className);
					ActivityInfo info = target.findReceiverByClassName(className);
					if (null != info) {
						ReceiverIntentInfo receiverInfo = new ReceiverIntentInfo(info);
						Field intentsField = receiver.getClass().getField("intents");
						intentsField.setAccessible(true);
						@SuppressWarnings("unchecked")
						ArrayList<IntentFilter> intents = (ArrayList<IntentFilter>) intentsField
								.get(receiver);
						receiverInfo.setFilter(intents);
						target.addReceiver(receiverInfo);
					}
				}

				// 获取Service
				Field services = pkg.getClass().getDeclaredField("services");
				services.setAccessible(true);
				ArrayList<?> serviceFilters = (ArrayList<?>) services.get(pkg);
				for (int i = 0; i < serviceFilters.size(); i++) {
					Object service = serviceFilters.get(i);

					Field intentsClassName = service.getClass().getField("className");
					intentsClassName.setAccessible(true);
					String className = (String) intentsClassName.get(service);
					System.out.println("className: " + className);
					ServiceInfo info = target.findServiceByClassName(className);
					if (null != info) {
						ServiceIntentInfo serviceInfo = new ServiceIntentInfo(info);
						Field intentsField = service.getClass().getField("intents");
						intentsField.setAccessible(true);
						@SuppressWarnings("unchecked")
						ArrayList<IntentFilter> intents = (ArrayList<IntentFilter>) intentsField
								.get(service);
						serviceInfo.setFilter(intents);
						target.addService(serviceInfo);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
