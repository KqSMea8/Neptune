package org.qiyi.pluginlibrary.loader;

import org.qiyi.pluginlibrary.HybirdPlugin;
import org.qiyi.pluginlibrary.pm.PluginPackageInfo;
import org.qiyi.pluginlibrary.utils.MultiDex;

import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexClassLoader;

/**
 * 插件的DexClassLoader，用来做一些"更高级"的特性，
 * 比如添加插件依赖，支持multidex
 *
 * author: liuchun
 * date: 2018/5/15
 */
public class PluginClassLoader extends DexClassLoader{
    // 插件的包名
    private String pkgName;
    // 依赖的插件的ClassLoader
    private List<DexClassLoader> dependencies;

    public PluginClassLoader(PluginPackageInfo packageInfo, String dexPath, String optimizedDirectory,
                             String librarySearchPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
        this.pkgName = packageInfo.getPackageName();
        this.dependencies = new ArrayList<>();

        if (HybirdPlugin.getConfig().shouldSupportMultidex()) {
            MultiDex.install(packageInfo, dexPath, this);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // 根据Java ClassLoader的双亲委托模型，执行到此在parent ClassLoader中没有找到
        // 类似的，我们优先在依赖的插件ClassLoader中查找
        for (ClassLoader classLoader : dependencies) {
            try {
                Class<?> c = classLoader.loadClass(name);
                if (c != null) {
                    // find class in the dependency
                    return c;
                }
            } catch (ClassNotFoundException e) {
                // ClassNotFoundException thrown if class not found ini dependency class loader
            }
        }
        // If still not found, find in this class loader
        return super.findClass(name);
    }

    /**
     * 获取插件ClassLoader对应的插件包名
     */
    public String getPackageName() {
        return pkgName;
    }

    /**
     * 添加依赖的插件ClassLoader
     */
    public void addDependency(DexClassLoader classLoader) {
        dependencies.add(classLoader);
    }

    @Override
    public String toString() {
        String self = super.toString();
        StringBuilder deps = new StringBuilder();
        for (ClassLoader classLoader : dependencies) {
            deps.append(classLoader.toString());
        }
        String parent = getParent().toString();
        return "self: " + self
                + "; deps: size=" + dependencies.size()
                +", content=" + deps
                + "; parent: " + parent;
    }
}
