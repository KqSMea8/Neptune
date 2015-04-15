package org.qiyi.pluginlibrary.component.service;


/**
 * @author zhuchengjin
 * 自启动service组件
 */
public abstract class CMSelfLaunchService extends CMService {
	
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}
	 /**
	 * @return
	 * 判断service 是否正在运行
	 */
	public boolean isServiceRunning(){
    	return false;
    }
}
