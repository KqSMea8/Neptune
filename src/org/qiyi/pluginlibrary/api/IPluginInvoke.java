package org.qiyi.pluginlibrary.api;


/**
 * @author zhuchengjin
 *  插件化调用逻辑
 */
public interface IPluginInvoke {

	public static final String TAG = "plugin";
	public void invoke(Object... obj);
}
