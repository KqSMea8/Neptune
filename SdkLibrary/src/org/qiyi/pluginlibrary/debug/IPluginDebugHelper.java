package org.qiyi.pluginlibrary.debug;

import android.content.Context;

import java.util.List;

/**
 * Created by qipu on 2017/1/23.
 */
public interface IPluginDebugHelper {
    /**
     * 保存请求插件列表的url
     *
     * @param context   Context
     * @param time      保存时间
     * @param url       url
     */
    void savePluginRequestUrl(Context context, final String time, final String url);

    /**
     * 获取请求插件列表的Url
     *
     * @param context   Context
     * @return          url
     */
    List<String> getPluginRequestUrl(Context context);
    /**
     * 保存插件列表
     *
     * @param context   Context
     * @param time      保存插件列表的时间
     * @param plugins   插件列表
     */
    void savePluginList(Context context, final String time, final String plugins);

    /**
     * 获取存储的插件列表信息
     *
     * @param context   Context
     * @return          返回插件列表
     */
    List<String> getPluginList(Context context);

    /**
     * 获取某个插件的后端吐的信息
     *
     * @param pluginName        插件包名
     */
    String getPluginInfo(String pluginName);

    /**
     * 保存插件下载状态（只保存成功和失败）
     *
     * @param context           Context
     * @param time              保存信息时的时间
     * @param downloadState     下载状态
     */
    void savePlguinDownloadState(Context context, final String time, final String downloadState);

    /**
     * 获取插件下载状态信息
     *
     * @param context   Context
     * @return          返回插件下载状态的信息
     */
    List<String> getPluginDownloadState(Context context);

    /**
     * 保存插件安装状态（只保存成功和失败）
     *
     * @param context           Context
     * @param time              保存信息时的时间
     * @param installState      安装状态
     */
    void savePluginInstallState(Context context, final String time, final String installState);

    /**
     * 获得插件安装的状态信息
     *
     * @param context   Context
     * @return          返回插件安装状态信息
     */
    List<String> getPluginInstallState(Context context);

    /**
     * 保存运行的插件信息
     *
     * @param context       Context
     * @param time          保存信息时的时间
     * @param pluginInfo    插件信息
     */
    void saveRunningPluginInfo(Context context, final String time, final String pluginInfo);

    /**
     * 获得运行的插件信息
     *
     * @param context   Context
     * @return          返回插件运行的信息
     */
    List<String> getRunningPluginInfo(Context context);

    /**
     * 保存插件跳转信息
     *
     * @param context       Context
     * @param time          保存信息时的时间
     * @param intent        跳转信息
     */
    void savePluginActivityAndServiceJump(Context context, final String time, final String intent);

    /**
     * 获得插件跳转信息
     *
     * @param context   Context
     * @return          返回插件跳转信息
     */
    List<String> getPluginJumpInfo(Context context);

    /**
     * 获取系统当前时间
     *
     * @return  返回{"yyyy年MM月dd日: HH:mm:ss : "}格式的时间
     */
    String getCurrentSystemTime();

    /**
     * 保存部分log信息
     */
    void savePluginLogInfo(Context context, final String time, final StringBuffer logInfo);
}
