package org.qiyi.pluginnew;

import java.util.List;

import org.qiyi.plugin.manager.ProxyEnvironmentNew;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

public class PluginPackageManager extends PackageManager {
	private PackageManager orig;

	public PluginPackageManager(PackageManager wrapper) {
		this.orig = wrapper;
	}

	@Override
	public PackageInfo getPackageInfo(String packageName, int flags)
			throws NameNotFoundException {
		ProxyEnvironmentNew plugin = ProxyEnvironmentNew.getInstance(packageName);
		if (plugin != null) {
			return plugin.getTargetMapping().getPackageInfo();
		}
		return orig.getPackageInfo(packageName, flags);
	}

	@Override
	public String[] currentToCanonicalPackageNames(String[] names) {
		return orig.currentToCanonicalPackageNames(names);
	}

	@Override
	public String[] canonicalToCurrentPackageNames(String[] names) {
		return orig.canonicalToCurrentPackageNames(names);
	}

	@Override
	public Intent getLaunchIntentForPackage(String packageName) {
		// PlugInfo plugin =
		// PluginManager.getInstance().getPluginByPackageName(packageName);
		// if (plugin != null) {
		// //TODO
		// }
		return orig.getLaunchIntentForPackage(packageName);
	}

	@Override
	public int[] getPackageGids(String packageName)
			throws NameNotFoundException {
		ProxyEnvironmentNew plugin = ProxyEnvironmentNew.getInstance(packageName);
		if (plugin != null) {
			return plugin.getTargetMapping().getPackageInfo().gids;
		}
		return orig.getPackageGids(packageName);
	}

	@Override
	public PermissionInfo getPermissionInfo(String name, int flags)
			throws NameNotFoundException {
		// TODO
		return orig.getPermissionInfo(name, flags);
	}

	@Override
	public List<PermissionInfo> queryPermissionsByGroup(String group, int flags)
			throws NameNotFoundException {
		// TODO
		return orig.queryPermissionsByGroup(group, flags);
	}

	@Override
	public PermissionGroupInfo getPermissionGroupInfo(String name, int flags)
			throws NameNotFoundException {
		// TODO
		return orig.getPermissionGroupInfo(name, flags);
	}

	@Override
	public List<PermissionGroupInfo> getAllPermissionGroups(int flags) {
		// TODO
		return orig.getAllPermissionGroups(flags);
	}

	@Override
	public ApplicationInfo getApplicationInfo(String packageName, int flags)
			throws NameNotFoundException {
		ProxyEnvironmentNew plugin = ProxyEnvironmentNew.getInstance(packageName);
		if (plugin != null) {
			return plugin.getTargetMapping().getPackageInfo().applicationInfo;
		}
		return orig.getApplicationInfo(packageName, flags);
	}

	@Override
	public ActivityInfo getActivityInfo(ComponentName component, int flags)
			throws NameNotFoundException {
		String packageName = component.getPackageName();
		if (packageName != null) {
			ProxyEnvironmentNew plugin = ProxyEnvironmentNew.getInstance(packageName);
			if (plugin != null) {
				String actClassName = component.getClassName();
				if (actClassName != null) {
					return plugin.findActivityByClassName(actClassName);
				}
			}
		}
		return orig.getActivityInfo(component, flags);
	}
	@Override
	public ActivityInfo getReceiverInfo(ComponentName component, int flags)
			throws NameNotFoundException {
		String packageName = component.getPackageName();
		if (packageName != null) {
			ProxyEnvironmentNew plugin = ProxyEnvironmentNew.getInstance(packageName);
			if (plugin != null) {
				String className = component.getClassName();
				if (className != null) {
//					return plugin.findReceiverByClassName(className);
				}
			}
		}
		return orig.getReceiverInfo(component, flags);
	}

	@Override
	public ServiceInfo getServiceInfo(ComponentName component, int flags)
			throws NameNotFoundException {
		String packageName = component.getPackageName();
		if (packageName != null) {
			ProxyEnvironmentNew plugin = ProxyEnvironmentNew.getInstance(packageName);
			if (plugin != null) {
				String className = component.getClassName();
				if (className != null) {
//					return plugin.findServiceByClassName(className);
				}
			}
		}
		return orig.getServiceInfo(component, flags);
	}

	@Override
	public ProviderInfo getProviderInfo(ComponentName component, int flags)
			throws NameNotFoundException {
		// TODO getProviderInfo
		return orig.getProviderInfo(component, flags);
	}

	@Override
	public List<PackageInfo> getInstalledPackages(int flags) {
		return orig.getInstalledPackages(flags);
	}

	@Override
	public int checkPermission(String permName, String pkgName) {
		// TODO checkPermission
		return orig.checkPermission(permName, pkgName);
	}

	@Override
	public boolean addPermission(PermissionInfo info) {
		// TODO addPermission
		return orig.addPermission(info);
	}

	@Override
	public boolean addPermissionAsync(PermissionInfo info) {
		// TODO addPermissionAsync
		return orig.addPermission(info);
	}

	@Override
	public void removePermission(String name) {
		// TODO removePermission
		orig.removePermission(name);
	}

	@Override
	public int checkSignatures(String pkg1, String pkg2) {
		// TODO checkSignatures
		return orig.checkSignatures(pkg1, pkg2);
	}

	@Override
	public int checkSignatures(int uid1, int uid2) {
		// TODO checkSignatures
		return orig.checkSignatures(uid1, uid2);
	}

	@Override
	public String[] getPackagesForUid(int uid) {
		// TODO getPackagesForUid
		return orig.getPackagesForUid(uid);
	}

	@Override
	public String getNameForUid(int uid) {
		// TODO getNameForUid
		return orig.getNameForUid(uid);
	}

	@Override
	public List<ApplicationInfo> getInstalledApplications(int flags) {
		return orig.getInstalledApplications(flags);
	}

	@Override
	public String[] getSystemSharedLibraryNames() {
		// TODO getSystemSharedLibraryNames
		return orig.getSystemSharedLibraryNames();
	}

	@Override
	public FeatureInfo[] getSystemAvailableFeatures() {
		// TODO getSystemAvailableFeatures
		return orig.getSystemAvailableFeatures();
	}

	@Override
	public boolean hasSystemFeature(String name) {
		// TODO hasSystemFeature
		return orig.hasSystemFeature(name);
	}

	@Override
	public ResolveInfo resolveActivity(Intent intent, int flags) {
		ResolveInfo result = null;
		ComponentName compname = intent.getComponent();
		if (compname != null) {
			String packageName = compname.getPackageName();
			if (packageName != null) {
				ProxyEnvironmentNew plugin = ProxyEnvironmentNew.getInstance(packageName);
//				if (plug != null) {
//					result = plug.resolveActivity(intent);
//					if (result != null) {
//						return result;
//					}
//				}
			}
		}
		return orig.resolveActivity(intent, flags);
	}

	@Override
	public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
		// TODO 查询插件 Activity
		return orig.queryIntentActivities(intent, flags);
	}

	@Override
	public List<ResolveInfo> queryIntentActivityOptions(ComponentName caller,
			Intent[] specifics, Intent intent, int flags) {
		// TODO Auto-generated method stub
		return orig
				.queryIntentActivityOptions(caller, specifics, intent, flags);
	}

	@Override
	public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
		// TODO Auto-generated method stub
		return orig.queryBroadcastReceivers(intent, flags);
	}

	@Override
	public ResolveInfo resolveService(Intent intent, int flags) {
		// TODO Auto-generated method stub
		return orig.resolveService(intent, flags);
	}

	@Override
	public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
		// TODO Auto-generated method stub
		return orig.queryIntentServices(intent, flags);
	}

	@Override
	public ProviderInfo resolveContentProvider(String name, int flags) {
		// TODO Auto-generated method stub
		return orig.resolveContentProvider(name, flags);
	}

	@Override
	public List<ProviderInfo> queryContentProviders(String processName,
			int uid, int flags) {
		// TODO Auto-generated method stub
		return orig.queryContentProviders(processName, uid, flags);
	}

	@Override
	public InstrumentationInfo getInstrumentationInfo(ComponentName className,
			int flags) throws NameNotFoundException {
		// TODO Auto-generated method stub
		return orig.getInstrumentationInfo(className, flags);
	}

	@Override
	public List<InstrumentationInfo> queryInstrumentation(String targetPackage,
			int flags) {
		// TODO Auto-generated method stub
		return orig.queryInstrumentation(targetPackage, flags);
	}

	@Override
	public Drawable getDrawable(String packageName, int resid,
			ApplicationInfo appInfo) {
		// TODO getDrawable
		return orig.getDrawable(packageName, resid, appInfo);
	}

	@Override
	public Drawable getActivityIcon(ComponentName activityName) throws NameNotFoundException {
		// TODO getActivityIcon
		ProxyEnvironmentNew plugin = ProxyEnvironmentNew.getInstance(activityName.getPackageName());
		if (plugin != null) {
			ActivityInfo actInfo = plugin.findActivityByClassName(activityName.getClassName());
			int icon = 0;
			if (actInfo != null) {
				icon = actInfo.icon;
			}
			if (icon != 0) {
				return plugin.getTargetResources().getDrawable(icon);
			} else {
				return getDefaultActivityIcon();
			}
		}
		return orig.getActivityIcon(activityName);
	}

	@Override
	public Drawable getActivityIcon(Intent intent) throws NameNotFoundException {
		// TODO getActivityIcon(Intent intent)
		return orig.getActivityIcon(intent);
	}

	@Override
	public Drawable getDefaultActivityIcon() {
		// TODO getDefaultActivityIcon
		return orig.getDefaultActivityIcon();
	}

	@Override
	public Drawable getApplicationIcon(ApplicationInfo info) {
		// TODO getApplicationIcon
		return orig.getApplicationIcon(info);
	}

	@Override
	public Drawable getApplicationIcon(String packageName)
			throws NameNotFoundException {
		ProxyEnvironmentNew plug = ProxyEnvironmentNew.getInstance(packageName);
		
		if (plug != null) {
			int appIcon = plug.getTargetMapping().getPackageInfo().applicationInfo.icon;
			if (appIcon != 0) {
				return plug.getTargetResources().getDrawable(appIcon);
			} else {
				return getDefaultActivityIcon();
			}
		}
		return orig.getApplicationIcon(packageName);
	}

	@Override
	public Drawable getActivityLogo(ComponentName activityName)
			throws NameNotFoundException {
		// TODO Auto-generated method stub
		return orig.getActivityLogo(activityName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.pm.PackageManager#getActivityLogo(android.content.Intent)
	 */
	@Override
	public Drawable getActivityLogo(Intent intent) throws NameNotFoundException {
		// TODO Auto-generated method stub
		return orig.getActivityLogo(intent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.pm.PackageManager#getApplicationLogo(android.content.
	 * pm.ApplicationInfo)
	 */
	@Override
	public Drawable getApplicationLogo(ApplicationInfo info) {
		// TODO Auto-generated method stub
		return orig.getApplicationLogo(info);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.pm.PackageManager#getApplicationLogo(java.lang.String)
	 */
	@Override
	public Drawable getApplicationLogo(String packageName)
			throws NameNotFoundException {
		// TODO Auto-generated method stub
		return orig.getApplicationLogo(packageName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.pm.PackageManager#getText(java.lang.String, int,
	 * android.content.pm.ApplicationInfo)
	 */
	@Override
	public CharSequence getText(String packageName, int resid,
			ApplicationInfo appInfo) {
		// TODO Auto-generated method stub
		ProxyEnvironmentNew plug = ProxyEnvironmentNew.getInstance(packageName);
		if (plug != null && plug.getTargetResources() != null) {
			return plug.getTargetResources().getText(resid);
		}
		return orig.getText(packageName, resid, appInfo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.pm.PackageManager#getXml(java.lang.String, int,
	 * android.content.pm.ApplicationInfo)
	 */
	@Override
	public XmlResourceParser getXml(String packageName, int resid,
			ApplicationInfo appInfo) {
		// TODO Auto-generated method stub
		ProxyEnvironmentNew plug = ProxyEnvironmentNew.getInstance(packageName);
		if (plug != null && plug.getTargetResources() != null) {
			return plug.getTargetResources().getXml(resid);
		}
		return orig.getXml(packageName, resid, appInfo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.pm.PackageManager#getApplicationLabel(android.content
	 * .pm.ApplicationInfo)
	 */
	@Override
	public CharSequence getApplicationLabel(ApplicationInfo info) {
		// TODO Auto-generated method stub
		ProxyEnvironmentNew plug = ProxyEnvironmentNew.getInstance(info.packageName);
		if (plug != null && plug.getTargetResources() != null) {
			return plug.getTargetResources().getText(info.labelRes);
		}
		return orig.getApplicationLabel(info);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.pm.PackageManager#getResourcesForActivity(android.content
	 * .ComponentName)
	 */
	@Override
	public Resources getResourcesForActivity(ComponentName activityName)
			throws NameNotFoundException {
		String pkgName = activityName.getPackageName();
		if (!TextUtils.isEmpty(pkgName)) {
			ProxyEnvironmentNew plug = ProxyEnvironmentNew.getInstance(pkgName);
			if (plug != null && plug.getTargetResources() != null) {
				return plug.getTargetResources();
			}
		}
		return orig.getResourcesForActivity(activityName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.pm.PackageManager#getResourcesForApplication(android.
	 * content.pm.ApplicationInfo)
	 */
	@Override
	public Resources getResourcesForApplication(ApplicationInfo app)
			throws NameNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Resources getResourcesForApplication(String appPackageName)
			throws NameNotFoundException {
		ProxyEnvironmentNew plugin = ProxyEnvironmentNew.getInstance(appPackageName);
		if (plugin != null) {
			return plugin.getTargetResources();
		}
		return orig.getResourcesForApplication(appPackageName); 
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.pm.PackageManager#getInstallerPackageName(java.lang.String
	 * )
	 */
	@Override
	public String getInstallerPackageName(String packageName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addPackageToPreferred(String packageName) {
        orig.addPackageToPreferred(packageName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.pm.PackageManager#removePackageFromPreferred(java.lang
	 * .String)
	 */
	@Override
	public void removePackageFromPreferred(String packageName) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.pm.PackageManager#getPreferredPackages(int)
	 */
	@Override
	public List<PackageInfo> getPreferredPackages(int flags) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.pm.PackageManager#addPreferredActivity(android.content
	 * .IntentFilter, int, android.content.ComponentName[],
	 * android.content.ComponentName)
	 */
	@Override
	public void addPreferredActivity(IntentFilter filter, int match,
			ComponentName[] set, ComponentName activity) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.pm.PackageManager#clearPackagePreferredActivities(java
	 * .lang.String)
	 */
	@Override
	public void clearPackagePreferredActivities(String packageName) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.pm.PackageManager#getPreferredActivities(java.util.List,
	 * java.util.List, java.lang.String)
	 */
	@Override
	public int getPreferredActivities(List<IntentFilter> outFilters,
			List<ComponentName> outActivities, String packageName) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.pm.PackageManager#setComponentEnabledSetting(android.
	 * content.ComponentName, int, int)
	 */
	@Override
	public void setComponentEnabledSetting(ComponentName componentName,
			int newState, int flags) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.pm.PackageManager#getComponentEnabledSetting(android.
	 * content.ComponentName)
	 */
	@Override
	public int getComponentEnabledSetting(ComponentName componentName) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.pm.PackageManager#setApplicationEnabledSetting(java.lang
	 * .String, int, int)
	 */
	@Override
	public void setApplicationEnabledSetting(String packageName, int newState,
			int flags) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.content.pm.PackageManager#getApplicationEnabledSetting(java.lang
	 * .String)
	 */
	@Override
	public int getApplicationEnabledSetting(String packageName) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.pm.PackageManager#isSafeMode()
	 */
	@Override
	public boolean isSafeMode() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<PackageInfo> getPackagesHoldingPermissions(
			String[] permissions, int flags) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ResolveInfo> queryIntentContentProviders(Intent intent,
			int flags) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void verifyPendingInstall(int id, int verificationCode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void extendVerificationTimeout(int id,
			int verificationCodeAtTimeout, long millisecondsToDelay) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInstallerPackageName(String targetPackage,
			String installerPackageName) {
		// TODO Auto-generated method stub
		
	}

}
