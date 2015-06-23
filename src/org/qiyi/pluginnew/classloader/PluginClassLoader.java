package org.qiyi.pluginnew.classloader;

import org.qiyi.plugin.manager.ProxyEnvironmentNew;
import dalvik.system.DexClassLoader;

/**
 * 插件类加载器
 */
public class PluginClassLoader extends DexClassLoader {
//	private final String tag;
//	private final ProxyEnvironmentNew thisPlugin;
//	private final String optimizedDirectory;
//	private final String libraryPath;
	/**
	 * Activity 的类加载器
	 */
//	private final Map<String, ClassLoader> proxyActivityLoaderMap;

	public PluginClassLoader(String dexPath, String optimizedDir, ClassLoader parent,
			ProxyEnvironmentNew plugin) {
		super(dexPath, optimizedDir, plugin.getTargetMapping().getnativeLibraryDir(), parent);
//		thisPlugin = plugin;
//		proxyActivityLoaderMap = new HashMap<String, ClassLoader>(plugin.getTargetMapping()
//				.getPackageInfo().activities.length);
//		this.libraryPath = plugin.getTargetMapping().getnativeLibraryDir();
//		this.optimizedDirectory = optimizedDir;
//		tag = "PluginClassLoader( " + plugin.getTargetPackageName() + " )";
	}
//
//	public Class<?> loadActivityClass(final String actClassName) throws ClassNotFoundException {
//		Log.d(tag, "loadActivityClass: " + actClassName);
//
//		// 在类加载之前检查创建代理的Activity dex文件，以免调用者忘记生成此文件
//		File dexSavePath = PluginInstaller.getProxyComponentDexPath(new File(thisPlugin
//				.getTargetMapping().getDataDir()), actClassName);
//		ActivityClassGenerator.createProxyDex(thisPlugin.getTargetPackageName(), actClassName,
//				dexSavePath);
//		ClassLoader actLoader = proxyActivityLoaderMap.get(actClassName);
//		if (actLoader == null) {
//			actLoader = new DexClassLoader(dexSavePath.getAbsolutePath(), optimizedDirectory,
//					libraryPath, this);
//			proxyActivityLoaderMap.put(actClassName, actLoader);
//		}
//		return actLoader.loadClass(ActivityJumpUtil.TARGET_CLASS_NAME);
//	}

}
