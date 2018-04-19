package org.qiyi.pluginlibrary.error;

/**
 * @author zhuchengjin
 * 一些异常情况, 错误码定义
 */
public class ErrorType {

    /** 错误的原因 */
    public static final String ERROR_REASON = "error_reason";
    /** 成功 */
    public static final int SUCCESS = 0;
    public static final int SUCCESS_DOWNLOADED = 1;  //下载成功
    public static final int SUCCESS_INSTALLED = 2;   //安装成功
    public static final int SUCCESS_LOADED = 3;      //插件加载成功
    /* 插件下载，校验等错误，应用层定义，区间字段：0~3000 */

    /** 安装错误，asset下文件不存在 */
    public static final int INSTALL_ERROR_ASSET_APK_NOT_FOUND = 4000;
    /** 安装错误，插件apk不存在 */
    public static final int INSTALL_ERROR_APK_NOT_EXIST = 4001;
    /** 安装错误，插件apk读取IOException */
    public static final int INSTALL_ERROR_FILE_IOEXCEPTION = 4002;
    /** 安装错误，插件apk从Asset目录拷贝到内置存储区失败 */
    public static final int INSTALL_ERROR_ASSET_APK_COPY_FAILED = 4003;
    /** 安装错误，插件apk拷贝操作失败，比如安装到sdcard */
    public static final int INSTALL_ERROR_APK_COPY_FAILED = 4004;
    /** 安装错误，插件apk的路径无效 */
    public static final int INSTALL_ERROR_FILE_PATH_ILLEGAL = 4005;
    /** 安装错误，插件apk解析失败 */
    public static final int INSTALL_ERROR_APK_PARSE_FAILED = 4006;
    /** 安装错误，插件包名与apk里包名不一致 */
    public static final int INSTALL_ERROR_PKG_NAME_NOT_MATCH = 4007;
    /** 安装错误，插件重命名失败 */
    public static final int INSTALL_ERROR_RENAME_FAILED = 4008;
    /** 安装错误，插件安装目录创建失败 */
    public static final int INSTALL_ERROR_MKDIR_FAILED = 4009;
    /** 安装错误，插件apk未签名 */
    public static final int INSTALL_ERROR_APK_NO_SIGNATURE = 4010;
    /** 安装错误，插件apk签名不一致 */
    public static final int INSTALL_ERROR_APK_SIGNATURE_NOT_MATCH = 4011;
    /** 安装错误，安装so库不存在 */
    public static final int INSTALL_ERROR_SO_NOT_EXIST = 4100;
    /** 安装错误，安装so库拷贝失败 */
    public static final int INSTALL_ERROR_SO_COPY_FAILED = 4101;
    /** 安装错误，安装so库时解压失败 */
    public static final int INSTALL_ERROR_SO_UNZIP_FAILED = 4102;
    /** 安装错误，安装dex文件不存在 */
    public static final int INSTALL_ERROR_DEX_NOT_EXIST = 4200;
    /** 安装错误，安装dex文件拷贝失败 */
    public static final int INSTALL_ERROR_DEX_COPY_FAILED = 4201;
    /** 安装错误，远程Service超时 */
    public static final int INSTALL_ERROR_CLIENT_TIME_OUT = 4300;



    /***
     * 下载成功
     */
    public static final int SUCCESS_DOWNLOAD = -0x0001;

    /***
     * 安装成功
     */
    public static final int SUCCESS_INSALL = -0x0002;

    /**
     * 内置插件
     */
    public static final int SUCCESS_INSTALL_BUILTIN = -0x1002;

    /***
     * 启动成功
     */
    public static final int SUCCESS_STARTUP = -0x0003;

    /***
     * 获取列表成功
     */
    public static final int SUCCESS_PLUGIN_LIST = -0x0004;

    /**
     * 上层插件下载异常
     */
    public static final int ERROR_CODE_NOWIFI = 0x0001;// 没有网络
    public static final int ERROR_CODE_DOWNLOAD = 0x0002;// 下载失败
    public static final int ERROR_CODE_INSTALL_SWITCH = 0x0003;// 开关未打开
    public static final int ERROR_CODE_INSTALL_VERIFY = 0x0004;// 校验失败
    public static final int ERROR_CODE_INSTALL_OTHER = 0x0005;// 其他安装失败的原因
    public static final int ERROR_CODE_START = 0x0006;// 启动失败
    public static final int ERROR_CODE_NETWORKEXCEPTION = 0x0007;// 网络异常
    public static final int ERROR_CODE_NODATA = 0x0008;// 没有数据
    public static final int ERROR_CODE_PARSE = 0x0009;// 解析失败

    /**
     * 客户端异常
     */
    public static final int ERROR_CLIENT = 0x1000;
    /** 客户端未知异常 */
    public static final int ERROR_CLIENT_UNKNOWN = 0x1001;
    /** 客户端校验失败 */
    public static final int ERROR_CLIENT_SIGNATURE_NOT_MATCH = 0x1002;
    /** 客户端未签名 */
    public static final int ERROR_CLIENT_NO_SIGNATURE = 0x1003;
    /** apk 客户端解析失败 */
    public static final int ERROR_CLIENT_PARSE_ERROR = 0x1004;
    /** 安装文件拷贝失败 */
    public static final int ERROR_CLIENT_COPY_ERROR = 0x1005;
    /** 安装超时 */
    public static final int ERROR_CLIENT_TIME_OUT = 0x1006;
    /** 插件包名和配置的包名不匹配 */
    public static final int ERROR_CLIENT_PACKAGE_NAME_NOT_MATCH = 0x1100;

    /** 插件apk文件未找到 */
    public static final int ERROR_CLIENT_FILE_NOTFOUND = 0x1007;
    /** 关闭流异常 */
    public static final int ERROR_CLIENT_CLOSE_IOEXCEPTION = 0x1008;

    /** 插件未加载，未找到插件运行的环境 */
    public static final int ERROR_CLIENT_NOT_LOAD = 0x1009;
    /** 初始化插件application 失败 */
    public static final int ERROR_CLIENT_INIT_PLUG_APP = 0x1010;
    /** load 启动插件类失败 */
    public static final int ERROR_CLIENT_LOAD_START = 0x1011;
    /** 插件非apk文件 */
    public static final int ERROR_CLIENT_LOAD_NO_APK = 0x1012;
    /** 初始化插件app信息失败 */
    public static final int ERROR_CLIENT_LOAD_INIT_APK_FAILE = 0x1013;
    /** 初始化资源失败 */
    public static final int ERROR_CLIENT_LOAD_INIT_RESOURCE_FAILE = 0x1014;
    /** 为获取到包名 */
    public static final int ERROR_CLIENT_LOAD_NO_PAKNAME = 0x1015;
    /** 后台加载插件init Target */
    public static final int ERROR_CLIENT_LOAD_INIT_TARGET = 0x1016;
    /** 初始化target异常 */
    public static final int ERROR_CLIENT_LOAD_INIT_EXCEPTION = 0x1017;

    /** 初始化target异常 */
    public static final int ERROR_CLIENT_LOAD_INIT_EXCEPTION_INSTANTIATION = 0x1018;
    /** 初始化target异常 */
    public static final int ERROR_CLIENT_LOAD_INIT_EXCEPTION_ILLEGALACCESS = 0x1019;
    /** 初始化target异常 */
    public static final int ERROR_CLIENT_LOAD_INIT_EXCEPTION_CLASSNOTFOUND = 0x101A;

    /** 文件目录创建失败 */
    public static final int ERROR_CLIENT_LOAD_CREATE_ROOT_DIR = 0x101C;
    /** 环境创建失败 */
    public static final int ERROR_CLIENT_ENVIRONMENT_NULL = 0x101D;
    /** 包名为空 */
    public static final int ERROR_CLIENT_LOAD_CREATE_FILE_NULL = 0x101F;
    /** 文件目录创建失败 */
    public static final int ERROR_CLIENT_LOAD_INIT_ENVIRONMENT_FAIL = 0x1020;

    /** 新插件方案，为Activity创建dex文件失败 */
    public static final int ERROR_CLIENT_CREATE_ACTIVITY_DEX_FAIL = ERROR_CLIENT_LOAD_INIT_ENVIRONMENT_FAIL + 1;
    /** 新插件方案，attach base to application失败 */
    public static final int ERROR_CLIENT_SET_APPLICATION_BASE_FAIL = ERROR_CLIENT_CREATE_ACTIVITY_DEX_FAIL + 1;
    /** 新插件方案，重写instrumentation失败 */
    public static final int ERROR_CLIENT_CHANGE_INSTRUMENTATION_FAIL = ERROR_CLIENT_SET_APPLICATION_BASE_FAIL + 1;

    /**
     * 3.0 Instrumentation插件方案，InstrActivityProxy1 onCreate 时
     * intent中无pluginMessage
     */
    public static final int ERROR_CLIENT_GET_PKG_AND_CLS_FAIL = ERROR_CLIENT_CHANGE_INSTRUMENTATION_FAIL + 1;
    /**
     * 3.0 Instrumentation插件方案，InstrActivityProxy尝试初始化ProxyEnvironmentNew时失败（
     * 只有ProxyEnvironmentNew为null的时候会）
     */
    public static final int ERROR_CLIENT_TRY_TO_INIT_ENVIRONMENT_FAIL = ERROR_CLIENT_GET_PKG_AND_CLS_FAIL + 1;
    /** 3.0 Instrumentation插件方案，InstrActivityProxy1 fillPluginActivity 发生异常 */
    public static final int ERROR_CLIENT_FILL_PLUGIN_ACTIVITY_FAIL = ERROR_CLIENT_TRY_TO_INIT_ENVIRONMENT_FAIL + 1;
    /**
     * 3.0 Instrumentation插件方案，InstrActivityProxy1 new PluginActivityControl 发生异常
     */
    public static final int ERROR_CLIENT_CREATE_PLUGIN_ACTIVITY_CONTROL_FAIL = ERROR_CLIENT_FILL_PLUGIN_ACTIVITY_FAIL + 1;
    /**
     * 3.0 Instrumentation插件方案，PluginActivityControl dispatchProxyToPlugin 发生异常
     */
    public static final int ERROR_CLIENT_DISPATCH_PROXY_TO_PLUGIN_FAIL = ERROR_CLIENT_CREATE_PLUGIN_ACTIVITY_CONTROL_FAIL + 1;
    /**
     * 3.0 Instrumentation or 2.0插件方案，ActivityOverrider changeActivityInfo 发生异常
     */
    public static final int ERROR_CLIENT_CHANGE_ACTIVITYINFO_FAIL = ERROR_CLIENT_DISPATCH_PROXY_TO_PLUGIN_FAIL + 1;
    /** 3.0 Instrumentation，PluginActivityControl callOnCreate 发生异常 */
    public static final int ERROR_CLIENT_CALL_ON_CREATE_FAIL = ERROR_CLIENT_CHANGE_ACTIVITYINFO_FAIL + 1;
    /** 反射工具类异常，ReflectionUtils call方法发生异常 */
    public static final int ERROR_CLIENT_REFLECTIONUTILS_CALL = ERROR_CLIENT_CALL_ON_CREATE_FAIL + 1;
    // Class loader创建失败
    public static final int ERROR_CLIENT_CREATE_CLSlOADER = ERROR_CLIENT_REFLECTIONUTILS_CALL + 1;
    /**
     * 网络异常
     */
    public static final int ERROR_NETWORK = 0x2000;
    /** 网络不可用 */
    public static final int ERROR_NETWORK_UNUSABLE = 0x2001;
    /** 网络连接失败 */
    public static final int ERROR_NETWORK_CONNECT_ERROR = 0x2002;
    /** 网络解析失败 */
    public static final int ERROR_NETWORK_PARSE_ERROR = 0x2003;
    /** 文件大小和sp中大小不一致 */
    public static final int ERROR_NETWORK_FILESIZE_DIFFER = 0x2004;
    /** 没有可见网络 */
    public static final int ERROR_NETWORK_NO_VISIBLE = 0x2005;
    /** 暂停 */
    public static final int ERROR_NETWORK_PAUSE_ERROR = 0x2006;
    /** 没有网络输入流 */
    public static final int ERROR_NETWORK_NO_INPUTSTREAM = 0x2007;
    /** 连接超时 */
    public static final int ERROR_NETWORK_CONNECTION_TIMEOUNT = 0x2008;
    /** SOCKET超时 */
    public static final int ERROR_NETWORK_SOCKET_TIMEOUNT = 0x2009;
    /** 没有网络输入流 */
    public static final int ERROR_NETWORK_IO_ERROR = 0x200A;
    /** 协议不正确 */
    public static final int ERROR_NETWORK_URL_MALFORMED = 0x200B;
    /** 列表获取失败 */
    public static final int ERROR_NETWORK_PLUGIN_LIST_FAIL = 0x200C;
    /** 列表获取失败 */
    public static final int ERROR_NETWORK_RESPONSE_CODE_ERROR = 0x200D;
    /** 未知网络错误 */
    public static final int ERROR_NETWORK_UNKNOW = 0x200E;
    /** 版本升级 */
    public static final int ERROR_UPDATE_VERSION = 0x2010;

    /**
     * 服务端异常
     */
    public static final int ERROR_SERVER = 0x3000;
    /** 参数错误 */
    public static final int ERROR_SERVER_PARAMETER_ERROR = 0x3001;
    /** 内部错误 */
    public static final int ERROR_SERVER_BACKEND_ERROR = 0x3002;
    /** 认证失败 */
    public static final int ERROR_SERVER_INVALID_APP_NAME = 0x3003;
}
