package org.qiyi.pluginnew;

import org.qiyi.pluginlibrary.component.InstrActivityProxy;
import org.qiyi.pluginlibrary.component.InstrActivityProxy1;
import org.qiyi.pluginlibrary.component.InstrActivityProxy2;
import org.qiyi.pluginlibrary.component.InstrActivityProxy3;
import org.qiyi.pluginlibrary.component.InstrActivityProxy4;
import org.qiyi.pluginlibrary.component.InstrActivityProxy5;
import org.qiyi.pluginlibrary.component.InstrActivityProxy6;
import org.qiyi.pluginlibrary.component.InstrActivityProxy7;
import org.qiyi.pluginlibrary.component.InstrActivityProxy8;
import org.qiyi.pluginlibrary.component.InstrActivityProxy9;
import org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent;
import org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent1;
import org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent2;
import org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent3;
import org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent4;
import org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent5;
import org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent6;
import org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent7;
import org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent8;
import org.qiyi.pluginlibrary.component.InstrActivityProxyTranslucent9;
import org.qiyi.pluginlibrary.component.ServiceProxy1;
import org.qiyi.pluginlibrary.component.ServiceProxy2;
import org.qiyi.pluginlibrary.component.ServiceProxy3;
import org.qiyi.pluginlibrary.component.ServiceProxy4;
import org.qiyi.pluginlibrary.component.ServiceProxy5;
import org.qiyi.pluginlibrary.component.ServiceProxy6;
import org.qiyi.pluginlibrary.component.ServiceProxy7;
import org.qiyi.pluginlibrary.component.ServiceProxy8;
import org.qiyi.pluginlibrary.component.ServiceProxy9;
import org.qiyi.pluginnew.service.ServiceProxyNew;

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

	/** This value should only set by host **/
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
	public static String mappingActivity(boolean isTranslucent, String processName) {
		if (null == sProcessMapping) {
			if (isTranslucent) {
				return InstrActivityProxyTranslucent.class.getName();
			} else {
				return InstrActivityProxy.class.getName();
			}
		}

		switch (sProcessMapping.getProcessIndex(processName)) {
		case 0:
			if (isTranslucent) {
				return InstrActivityProxyTranslucent.class.getName();
			} else {
				return InstrActivityProxy.class.getName();
			}
		case 1:
			if (isTranslucent) {
				return InstrActivityProxyTranslucent1.class.getName();
			} else {
				return InstrActivityProxy1.class.getName();
			}
		case 2:
			if (isTranslucent) {
				return InstrActivityProxyTranslucent2.class.getName();
			} else {
				return InstrActivityProxy2.class.getName();
			}
		case 3:
			if (isTranslucent) {
				return InstrActivityProxyTranslucent3.class.getName();
			} else {
				return InstrActivityProxy3.class.getName();
			}
		case 4:
			if (isTranslucent) {
				return InstrActivityProxyTranslucent4.class.getName();
			} else {
				return InstrActivityProxy4.class.getName();
			}
		case 5:
			if (isTranslucent) {
				return InstrActivityProxyTranslucent5.class.getName();
			} else {
				return InstrActivityProxy5.class.getName();
			}
		case 6:
			if (isTranslucent) {
				return InstrActivityProxyTranslucent6.class.getName();
			} else {
				return InstrActivityProxy6.class.getName();
			}
		case 7:
			if (isTranslucent) {
				return InstrActivityProxyTranslucent7.class.getName();
			} else {
				return InstrActivityProxy7.class.getName();
			}
		case 8:
			if (isTranslucent) {
				return InstrActivityProxyTranslucent8.class.getName();
			} else {
				return InstrActivityProxy8.class.getName();
			}
		case 9:
			if (isTranslucent) {
				return InstrActivityProxyTranslucent9.class.getName();
			} else {
				return InstrActivityProxy9.class.getName();
			}

		default:
			if (isTranslucent) {
				return InstrActivityProxyTranslucent.class.getName();
			} else {
				return InstrActivityProxy.class.getName();
			}
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
			return ServiceProxyNew.class.getName();
		}
		switch (sProcessMapping.getProcessIndex(processName)) {
		case 0:
			return ServiceProxyNew.class.getName();
		case 1:
			return ServiceProxy1.class.getName();
		case 2:
			return ServiceProxy2.class.getName();
		case 3:
			return ServiceProxy3.class.getName();
		case 4:
			return ServiceProxy4.class.getName();
		case 5:
			return ServiceProxy5.class.getName();
		case 6:
			return ServiceProxy6.class.getName();
		case 7:
			return ServiceProxy7.class.getName();
		case 8:
			return ServiceProxy8.class.getName();
		case 9:
			return ServiceProxy9.class.getName();
		default:
			return ServiceProxyNew.class.getName();
		}
	}
}