package org.qiyi.pluginlibrary.listener;


/**
 * @author zhuchengjin
 *	用于插件状态更新时使用。
 */
public abstract class PluginStatusChangeListener{
	
	/**
	 * @param status,状态
	 * @param obj 传递数据参数
	 * 状态发生改变时回调
	 */
	public abstract void onPluginStatusChanged(int status, Object obj);
  
	/**
	 * @param errorType 错误类型
	 * @param errorCode 错误代码
	 * 发生错误时回调
	 */
	public abstract void onError(int errorType,int errorCode);
	
	
}
