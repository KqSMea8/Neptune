package org.qiyi.pluginlibrary.constant;

/**
 * Author:yuanzeyao
 * Date:2017/7/3 17:07
 * Email:yuanzeyao@qiyi.com
 */

public interface IIntentConstant {
    /**是否插件中的组件*/
    public static final String EXTRA_TARGET_IS_PLUGIN_KEY = "target_is_plugin";
    /**代理插件包名的key*/
    public static final String EXTRA_TARGET_PACKAGE_KEY = "target_package";
    /**代理插件组件类名的key*/
    public static final String EXTRA_TARGET_CLASS_KEY = "target_class";
    /**主动加载插件的广播*/
    public static final String EXTRA_VALUE_LOADTARGET_STUB = "target_stub";
    /**通知插件启动完毕(用于快捷方式) */
    public static final String ACTION_PLUGIN_LOADED = "org.qiyi.pluginapp.ACTION_PLUGIN_LOADED";
    /**插件启动失败*/
    public static final String ACTION_START_PLUGIN_ERROR = "org.qiyi.pluginapp.ACTION_START_PLUGIN_ERROR";
    /**插件初始化完毕*/
    public static final String ACTION_PLUGIN_INIT = "org.qiyi.pluginapp.action.TARGET_INIT";

    /**解决多次bind一个service时，之会执行一次onBind的问题*/
    public static final String EXTRA_TARGET_CATEGORY = "pluginapp_service_category";
    /**bind Service时读取flag值的key*/
    public static final String BIND_SERVICE_FLAGS = "bind_service_flags";
    /**通知 Service 绑定成功，可以进行进程通信了*/
    public static final String ACTION_SERVICE_CONNECTED = "org.qiyi.pluginapp.ACTION_SERVICE_CONNECTED";

    @Deprecated
    public static final String META_KEY_ACTIVITY_SPECIAL = "pluginapp_activity_special";
    @Deprecated
    public static final String PLUGIN_ACTIVITY_TRANSLUCENT = "Translucent";
    @Deprecated
    public static final String PLUGIN_ACTIVTIY_HANDLE_CONFIG_CHAGNE = "Handle_configuration_change";
    /**Service退出*/
    public static final String ACTION_QUIT = "org.qiyi.pluginapp.action.QUIT";
    /**控制插件启动loading*/
    public static final String EXTRA_SHOW_LOADING = "plugin_show_loading";

    /**安装完的插件的*/
    public static final String EXTRA_PKG_NAME = "package_name";
    /**安装插件的apk的源路径*/
    public static final String EXTRA_SRC_FILE = "install_src_file";
    /**插件的安装地址*/
    public static final String EXTRA_DEST_FILE = "install_dest_file";
    /**被安装的插件的信息*/
    public static final String EXTRA_PLUGIN_INFO = "plugin_info";
    /**ACTION_SERVICE_CONNECTED Broadcast 中的 service 类型*/
    public static final String EXTRA_SERVICE_CLASS = "service_class";
    public static final String TASK_AFFINITY_CONTAINER = ":container1";
}
