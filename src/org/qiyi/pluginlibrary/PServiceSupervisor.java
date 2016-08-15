package org.qiyi.pluginlibrary;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import android.content.ServiceConnection;
import android.text.TextUtils;

public class PServiceSupervisor {
    /**
     * 记录正在运行的service
     */
    public static ConcurrentMap<String, PluginServiceWrapper> sAliveServices = new ConcurrentHashMap<String, PluginServiceWrapper>(
            1);

    public static ConcurrentMap<String, ServiceConnection> sAliveServiceConnection = new ConcurrentHashMap<String, ServiceConnection>();

    public static ConcurrentMap<String, PluginServiceWrapper> getAliveServices() {
        return sAliveServices;
    }

    public static PluginServiceWrapper getServiceByIdentifer(String identity) {
        if (TextUtils.isEmpty(identity)) {
            return null;
        }
        return sAliveServices.get(identity);
    }

    public static void removeServiceByIdentifer(String identity) {
        if (TextUtils.isEmpty(identity)) {
            return;
        }
        sAliveServices.remove(identity);
    }

    public static void addServiceByIdentifer(String identity, PluginServiceWrapper serviceWrapper) {
        if (TextUtils.isEmpty(identity) || null == serviceWrapper) {
            return;
        }
        sAliveServices.put(identity, serviceWrapper);
    }

    public static void clearServices() {
        sAliveServices.clear();
    }

    public static ConcurrentMap<String, ServiceConnection> getAllServiceConnection() {
        return sAliveServiceConnection;
    }

    public static ServiceConnection getConnection(String identity) {
        if (TextUtils.isEmpty(identity)) {
            return null;
        }
        return sAliveServiceConnection.get(identity);
    }

    public static void clearConnections() {
        sAliveServiceConnection.clear();
    }

    public static void addServiceConnectionByIdentifer(String identity, ServiceConnection conn) {
        if (TextUtils.isEmpty(identity) || null == conn) {
            return;
        }
        sAliveServiceConnection.put(identity, conn);
    }

    public static void removeServiceConnection(ServiceConnection conn) {
        if (null == conn) {
            return;
        }
        if (sAliveServiceConnection.containsValue(conn)) {
            String key = null;
            for (Entry<String, ServiceConnection> entry : sAliveServiceConnection.entrySet()) {
                if (conn == entry.getValue()) {
                    key = entry.getKey();
                    break;
                }
            }
            if (!TextUtils.isEmpty(key)) {
                sAliveServiceConnection.remove(key);
            }
        }
    }
}
