package org.qiyi.pluginlibrary.constant;

/**
 * Author:yuanzeyao
 * Date:2017/7/3 17:07
 * Email:yuanzeyao@qiyi.com
 */

public interface IIntentConstant {
    /**代理插件包名的key*/
    public static final String EXTRA_TARGET_PACKAGNAME_KEY = "targe_package";
    /**代理插件组件类名的key*/
    public static final String EXTRA_TARGET_CLASS_KEY = "pluginapp_extra_target_activity"; //"targe_class";
    /**主动加载插件的广播*/
    public static final String EXTRA_VALUE_LOADTARGET_STUB = "target_stub";
    /**通知插件启动完毕(用于快捷方式)*/
    public static final String ACTION_PLUGIN_LOADED = "org.qiyi.pluginapp.ACTION_PLUGIN_LOADED";

    /**插件初始化完毕*/
    public static final String ACTION_PLUGIN_INIT = "org.qiyi.pluginapp.action.TARGET_INIT";

    /**解决多次bind一个service时，之会执行一次onBind的问题*/
    public static final String EXTRA_TARGET_CATEGORY = "pluginapp_service_category";
    /**bind Service时读取flag值的key*/
    public static final String BIND_SERVICE_FLAGS = "bind_service_flags";



    @Deprecated
    public static final String META_KEY_ACTIVITY_SPECIAL = "pluginapp_activity_special";
    @Deprecated
    public static final String PLUGIN_ACTIVITY_TRANSLUCENT = "Translucent";
    @Deprecated
    public static final String PLUGIN_ACTIVTIY_HANDLE_CONFIG_CHAGNE = "Handle_configuration_change";

    /**
     * 插件退出
     */
    public static final String ACTION_QUIT = "org.qiyi.pluginapp.action.QUIT";

    /**
     * 控制插件启动时的loading
     */
    public static final String EXTRA_SHOW_LOADING = "plugin_show_loading";
}
