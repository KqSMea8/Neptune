package org.qiyi.pluginlibrary.debug;

import android.content.Context;

import java.util.List;


public interface IPluginDebugHelper {
    /**
     * 保存请求插件列表的url
     *
     * @param context Context
     * @param url     url
     */
    void savePluginRequestUrl(Context context, String url);

    /**
     * 获取请求插件列表的Url
     *
     * @param context Context
     * @return url
     */
    List<String> getPluginRequestUrl(Context context);

    /**
     * 保存插件列表
     *
     * @param context Context
     * @param plugins 插件列表
     */
    void savePluginList(Context context, String plugins);

    /**
     * 获取存储的插件列表信息
     *
     * @param context Context
     * @return 返回插件列表
     */
    List<String> getPluginList(Context context);

    /**
     * 获取某个插件的后端吐的信息
     *
     * @param context Context
     * @param pluginName 插件包名
     */
    String getPluginInfo(Context context, String pluginName);

    /**
     * 保存插件下载状态（只保存成功和失败）
     *
     * @param context       Context
     * @param downloadState 下载状态
     */
    void savePluginDownloadState(Context context, String downloadState);

    /**
     * 获取插件下载状态信息
     *
     * @param context Context
     * @return 返回插件下载状态的信息
     */
    List<String> getPluginDownloadState(Context context);

    /**
     * 保存插件安装状态（只保存成功和失败）
     *
     * @param context      Context
     * @param installState 安装状态
     */
    void savePluginInstallState(Context context, String installState);

    /**
     * 获得插件安装的状态信息
     *
     * @param context Context
     * @return 返回插件安装状态信息
     */
    List<String> getPluginInstallState(Context context);

    /**
     * 保存运行的插件信息
     *
     * @param context    Context
     * @param pluginInfo 插件信息
     */
    void saveRunningPluginInfo(Context context, String pluginInfo);

    /**
     * 获得运行的插件信息
     *
     * @param context Context
     * @return 返回插件运行的信息
     */
    List<String> getRunningPluginInfo(Context context);

    /**
     * 保存插件跳转信息
     *
     * @param context Context
     * @param intent  跳转信息
     */
    void savePluginActivityAndServiceJump(Context context, String intent);

    /**
     * 获得插件跳转信息
     *
     * @param context Context
     * @return 返回插件跳转信息
     */
    List<String> getPluginJumpInfo(Context context);

    /**
     * 保存插件logBuffer信息
     *
     * @param context Context
     * @param logTag log的Tag
     * @param logMsg 插件调试log信息
     */
    void savePluginLogBuffer(Context context, String logTag, String logMsg);

    /**
     * 获取插件logBuffer信息
     *
     * @param context Context
     * @param logTag log的TAG
     * @return 返回插件调试log信息
     */
    List<String> getPluginLogBuffer(Context context, String logTag);
}
