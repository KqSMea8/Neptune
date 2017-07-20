package org.qiyi.pluginlibrary;

import org.qiyi.pluginlibrary.component.InstrActivityProxy;
import org.qiyi.pluginlibrary.component.InstrActivityProxyHandleConfigChange;
import org.qiyi.pluginlibrary.component.ServiceProxy1;
import org.qiyi.pluginlibrary.component.ServiceProxy2;

import org.qiyi.pluginlibrary.component.InstrActivityProxyLandscape;
import org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent;
import org.qiyi.pluginlibrary.component.ServiceProxy;

public class ProxyComponentMappingByProcess {
    static final String PROXY_PROCESS = "com.qiyi.video:plugin";

    public interface MappingProcessIndex {
        /**
         * Get the current process's index return by host configuration rules
         *
         * @param processName
         * @return default return 0, if no mapping match
         */
        int getProcessIndex(String processName);

        /**
         * Get plugin's default process name
         *
         * @return
         */
        String getDefaultProcessName();
    }

    /**
     * This value should only set by host
     **/
    private static MappingProcessIndex sProcessMapping;

    /**
     * Set process mapping rules by host's configuration
     *
     * @param mapping
     */
    public static void setProcessMapping(MappingProcessIndex mapping) {
        sProcessMapping = mapping;
    }

    /**
     * Get plugin's default process name
     *
     * @return
     */
    public static String getDefaultPlugProcessName() {
        if (null != sProcessMapping) {
            return sProcessMapping.getDefaultProcessName();
        } else {
            return PROXY_PROCESS;
        }
    }

    /**
     * Get proxy activity name by process name
     *
     * @param isTranslucent
     * @param processName
     * @return
     */
    public static String mappingActivity(boolean isTranslucent, boolean isLandscape, boolean handleConfigChange, String processName) {
        if (null == sProcessMapping) {
            if (isTranslucent) {
                return InstrActivityProxyTranslucent.class.getName();
            } else if (isLandscape) {
                return InstrActivityProxyLandscape.class.getName();
            }
            if (handleConfigChange) {
                return InstrActivityProxyHandleConfigChange.class.getName();
            } else {
                return InstrActivityProxy.class.getName();
            }
        }

        int index = sProcessMapping.getProcessIndex(processName);
        String classSuffix = "";
        if (index > 0) {
            classSuffix = classSuffix + index;
        }
        if (isTranslucent) {
            return InstrActivityProxyTranslucent.class.getName() + classSuffix;
        } else if (isLandscape) {
            return InstrActivityProxyLandscape.class.getName() + classSuffix;
        }
        if (handleConfigChange) {
            return InstrActivityProxyHandleConfigChange.class.getName() + classSuffix;
        } else {
            return InstrActivityProxy.class.getName() + classSuffix;
        }
    }

    /**
     * Get proxy service name by process name
     *
     * @param processName
     * @return
     */
    public static String mappingService(String processName) {
        if (null == sProcessMapping) {
            return ServiceProxy.class.getName();
        }
        switch (sProcessMapping.getProcessIndex(processName)) {
        case 0:
            return ServiceProxy.class.getName();
        case 1:
            return ServiceProxy1.class.getName();
        case 2:
            return ServiceProxy2.class.getName();

        default:
            return ServiceProxy.class.getName();
        }
    }
}