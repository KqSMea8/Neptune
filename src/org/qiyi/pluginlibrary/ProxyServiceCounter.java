package org.qiyi.pluginlibrary;

import java.util.HashMap;

import org.qiyi.pluginlibrary.component.service.CMIntentService;
import org.qiyi.pluginlibrary.component.service.CMSelfLaunchService;
import org.qiyi.pluginlibrary.component.service.CMService;
import org.qiyi.pluginlibrary.proxy.IntentServiceProxy;
import org.qiyi.pluginlibrary.proxy.service.SelfLaunchServiceProxy;
import org.qiyi.pluginlibrary.proxy.service.ServiceProxy;

public class ProxyServiceCounter {
    private static final HashMap<Class<?>, String> serviceMap = new HashMap<Class<?>, String>();
    private static ProxyServiceCounter instance;

    
    static {
    	serviceMap.put(CMService.class, ServiceProxy.class.getName());
    	serviceMap.put(CMSelfLaunchService.class, SelfLaunchServiceProxy.class.getName());
    	serviceMap.put(CMIntentService.class, IntentServiceProxy.class.getName());
    }
    
    private ProxyServiceCounter() {

    }

    public static synchronized ProxyServiceCounter getInstance() {
        if (instance == null) {
            instance = new ProxyServiceCounter();
        }
        return instance;
    }

    public Class<?> getAvailableService(Class<?> clazz) {
        Class<?> serviceClass = findServiceClass(clazz);
        String className =  serviceMap.get(serviceClass);
        try {
            return Class.forName(className);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private Class<?> findServiceClass(Class<?> clazz) {

        if (CMSelfLaunchService.class.isAssignableFrom(clazz)) {
            return CMSelfLaunchService.class;
        }else if(CMIntentService.class.isAssignableFrom(clazz)){
        	return CMIntentService.class;
        }else if (CMService.class.isAssignableFrom(clazz)) {
            return CMService.class;
        }
        return CMService.class;
    }
}
