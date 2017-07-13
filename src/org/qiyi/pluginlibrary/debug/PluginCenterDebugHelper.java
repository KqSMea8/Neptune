package org.qiyi.pluginlibrary.debug;

import android.content.Context;

import java.util.List;

/**
 * Created by qipu on 2017/1/23.
 */
public class PluginCenterDebugHelper {

    private IPluginDebugHelper iPluginDebugHelper = null;
    private Context context = null;

    private PluginCenterDebugHelper() {

    }

    private static class SingletonHolder {
        static PluginCenterDebugHelper mInstance = new PluginCenterDebugHelper();
    }

    public static PluginCenterDebugHelper getInstance() {
        return SingletonHolder.mInstance;
    }

    /**
     * 初始化
     *
     * @param ctx       Context
     * @param helper    IPluginDebugHelper接口实现实例
     */
    public void init(Context ctx, IPluginDebugHelper helper) {
        if (null != ctx && null != helper) {
            context = ctx.getApplicationContext();
            this.iPluginDebugHelper = helper;
        }
    }

//    /**
//     * 设置IPluginDebugHelper的实现类
//     *
//     * @param iPluginDebugHelper    IPluginDebugHelper的实现类
//     */
//    public void setIPluginDebugHelper(IPluginDebugHelper iPluginDebugHelper) {
//        iPluginDebugHelper = iPluginDebugHelper;
//    }

    /**
     * 保存请求插件列表的url
     *
     * @param time      保存时间
     * @param url       url
     */
    public void savePluginRequestUrl(final String time, final String url) {
        iPluginDebugHelper.savePluginRequestUrl(context, time, url);
    }

    /**
     * 获取插件请求url
     *
     * @return      url
     */
    public List<String> getPluginRequestUrl() {
        return iPluginDebugHelper.getPluginRequestUrl(context);
    }

    /**
     * 保存插件列表
     *
     * @param time      保存时间
     * @param plugins   插件列表
     */
    public void savePluginList(final String time, final String plugins) {
        iPluginDebugHelper.savePluginList(context, time, plugins);
    }

    /**
     * 获取存储的插件列表信息
     */
    public List<String> getPluginList() {
        return iPluginDebugHelper.getPluginList(context);
    }

    /**
     * 获取某个插件的后端吐的信息
     *
     * @param pluginName        插件包名
     */
    public String getPluginInfo(String pluginName) {
        return iPluginDebugHelper.getPluginInfo(pluginName);
    }

    /**
     * 保存插件下载状态（只保存成功和失败）
     *
     * @param time              保存信息时的时间
     * @param downloadState     下载状态
     */
    public void savePlguinDownloadState(final String time, final String downloadState) {
        iPluginDebugHelper.savePlguinDownloadState(context, time, downloadState);

    }

    /**
     * 获取插件下载状态信息
     */
    public List<String> getPluginDownloadState(){
        return iPluginDebugHelper.getPluginDownloadState(context);
    }

    /**
     * 保存插件安装状态（只保存成功和失败）
     *
     * @param time              保存信息时的时间
     * @param installState      安装状态
     */
    public void savePluginInstallState(final String time, final String installState) {
        iPluginDebugHelper.savePluginInstallState(context, time, installState);
    }

    /**
     * 获得插件安装的状态信息
     */
    public List<String> getPluginInstallState() {
        return iPluginDebugHelper.getPluginInstallState(context);
    }

    /**
     * 保存运行的插件信息
     *
     * @param time          保存信息时的时间
     * @param pluginInfo    插件信息
     */
    public void saveRunningPluginInfo(final String time, final String pluginInfo) {
        iPluginDebugHelper.saveRunningPluginInfo(context, time, pluginInfo);
    }

    /**
     * 获得运行的插件信息
     */
    public List<String> getRunningPluginInfo() {
        return iPluginDebugHelper.getRunningPluginInfo(context);
    }

    /**
     * 保存插件跳转信息
     *
     * @param time          保存信息时的时间
     * @param intent        跳转信息
     */
    public void savePluginActivityAndServiceJump(final String time, final String intent) {
        iPluginDebugHelper.savePluginActivityAndServiceJump(context, time, intent);
    }

    /**
     * 获得插件跳转信息
     */
    public List<String> getPluginJumpInfo() {
        return iPluginDebugHelper.getPluginJumpInfo(context);
    }

    /**
     * 获取系统当前时间
     *
     * @return  返回{"yyyy年MM月dd日: HH:mm:ss : "}格式的时间
     */
    public String getCurrentSystemTime() {
        return iPluginDebugHelper.getCurrentSystemTime();
    }
}
