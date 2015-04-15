package org.qiyi.pluginlibrary;

import java.util.HashMap;
import java.util.Map;

import org.qiyi.pluginlibrary.component.CMActivity;
import org.qiyi.pluginlibrary.component.CMDialogActivity;
import org.qiyi.pluginlibrary.component.CMFragment;
import org.qiyi.pluginlibrary.component.CMFragmentActivity;
import org.qiyi.pluginlibrary.component.CMListActivity;
import org.qiyi.pluginlibrary.proxy.activity.ActivityProxy;
import org.qiyi.pluginlibrary.proxy.activity.DialogActivityProxy;
import org.qiyi.pluginlibrary.proxy.activity.FragmentActivityProxy;
import org.qiyi.pluginlibrary.proxy.activity.ListActivityProxy;
import org.qiyi.pluginlibrary.proxy.activity.translucent.ActivityProxyTranslucent;
import org.qiyi.pluginlibrary.proxy.activity.translucent.FragmentActivityProxyTranslucent;
import org.qiyi.pluginlibrary.proxy.activity.translucent.ListActivityProxyTranslucent;

/**
 * Activity映射的管理，并且做Activity代理的计数
 */
public class ProxyActivityCounter {

    private static final Map<Class<?>, String> CLASS_MAP = new HashMap<Class<?>, String>();
    // 透明背景的映射关系。因为透明背景不能动态修改所以需要提前声明
    private static final Map<Class<?>, String> CLASS_MAP_TRANSLUCENT = new HashMap<Class<?>, String>();
    private static final Map<Class<?>, Integer> CLASS_COUNTS = new HashMap<Class<?>, Integer>();

    private static ProxyActivityCounter instance;

    static {
        CLASS_MAP.put(CMActivity.class, ActivityProxy.class.getName());
        CLASS_MAP.put(CMFragmentActivity.class, FragmentActivityProxy.class.getName());
        CLASS_MAP.put(CMListActivity.class, ListActivityProxy.class.getName());
        CLASS_MAP.put(CMDialogActivity.class, DialogActivityProxy.class.getName());
    }
    
    /**
     * 需要在 host app的manifest中声明以下Activity。 主题设置为 Theme.Translucent.NoTitleBar
     */
    static {
        CLASS_MAP_TRANSLUCENT.put(CMActivity.class, ActivityProxyTranslucent.class.getName());
        CLASS_MAP_TRANSLUCENT.put(CMFragmentActivity.class, FragmentActivityProxyTranslucent.class.getName());
        CLASS_MAP_TRANSLUCENT.put(CMListActivity.class, ListActivityProxyTranslucent.class.getName());
        CLASS_MAP_TRANSLUCENT.put(CMDialogActivity.class, DialogActivityProxy.class.getName()); // 不需要透明的映射，本身自己透明
    }

    private ProxyActivityCounter() {

    }

    public static synchronized ProxyActivityCounter getInstance() {
        if (instance == null) {
            instance = new ProxyActivityCounter();
        }
        return instance;
    }

    public Class<?> getNextAvailableActivityClass(Class<?> clazz, int theme) {
        Class<?> iasClass = findIASClass(clazz);
        Integer count = CLASS_COUNTS.get(iasClass);
        if (count == null) {
            count = 0;
        }
        count++;
        CLASS_COUNTS.put(clazz, count);
        String className = null;
        // 使用 Theme_Translucent_NoTitleBar 作为透明背景的映射条件
        if (theme == android.R.style.Theme_Translucent_NoTitleBar) {
            className = CLASS_MAP_TRANSLUCENT.get(iasClass);
        } else {
            className = CLASS_MAP.get(iasClass);
        }
                
        try {
            return Class.forName(className);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Class<?> findIASClass(Class<?> clazz) {

        // 判断是哪类Activity，子类在前，父类在后
        if (CMFragmentActivity.class.isAssignableFrom(clazz)) {
            return CMFragmentActivity.class;
        } 
        else if (CMListActivity.class.isAssignableFrom(clazz)) {
        	return CMListActivity.class;
        } 
        else if (CMDialogActivity.class.isAssignableFrom(clazz)) {
        	return CMDialogActivity.class;
        } else if (CMActivity.class.isAssignableFrom(clazz)) {
        	return CMActivity.class;
        }else if(CMFragment.class.isAssignableFrom(clazz)){
        	return CMFragment.class;
        }
        return CMActivity.class;
//        else if (CMPreferenceActivity.class.isAssignableFrom(clazz)) {
//            return CMPreferenceActivity.class;
//        } 
//        else if (CMTabActivity.class.isAssignableFrom(clazz)) {
//            return CMTabActivity.class;
//        } else if (CMActivityGroup.class.isAssignableFrom(clazz)) {
//            return CMActivityGroup.class;
//        }
        
    }
}
