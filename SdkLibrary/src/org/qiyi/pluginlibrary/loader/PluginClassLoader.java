package org.qiyi.pluginlibrary.loader;

import java.util.ArrayList;
import java.util.List;

import dalvik.system.DexClassLoader;

/**
 * 插件的ClassLoader，支持添加依赖
 *
 * author: liuchun
 * date: 2018/5/15
 */
public class PluginClassLoader extends DexClassLoader{
    // 插件的包名
    private String pkgName;
    // 依赖的插件的ClassLoader
    private List<DexClassLoader> dependencies;

    public PluginClassLoader(String packageName, String dexPath, String optimizedDirectory,
                             String librarySearchPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
        this.pkgName = packageName;
        this.dependencies = new ArrayList<>();
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
     * 添加依赖的插件ClassLoader
     */
    public void addDependency(DexClassLoader classLoader) {
        dependencies.add(classLoader);
    }
}
