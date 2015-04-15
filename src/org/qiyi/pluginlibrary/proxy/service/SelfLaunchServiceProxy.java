package org.qiyi.pluginlibrary.proxy.service;

import org.qiyi.pluginlibrary.ProxyEnvironment;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import android.content.Intent;
import android.os.IBinder;

/**
 * @author zhuchengjin
 *
 *	自唤起service
 */
public class SelfLaunchServiceProxy extends ServiceProxy{

	private static final String TAG = "plugin";
	
//	private ScreenOffBroadcastReceiver mRegisterReceiver;
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		PluginDebugLog.log(TAG, "SelfLaunchServiceProxy>>>>>onCreate()");
	}
	
	@Override
	public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2) {
		// TODO Auto-generated method stub
		PluginDebugLog.log(TAG, "SelfLaunchServiceProxy>>>>>onStartCommand():"+(paramIntent == null? "null":paramIntent));
		if(paramIntent == null){
			paramIntent = new Intent();
			paramIntent.putExtra(ProxyEnvironment.EXTRA_TARGET_SERVICE, "tv.pps.bi.task.ListenService");
			paramIntent.putExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME, "tv.pps.bi.biplugin");
		}
		return super.onStartCommand(paramIntent, paramInt1, paramInt2);
	}


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		PluginDebugLog.log(TAG, "SelfLaunchServiceProxy>>>>>onBind()");
		return super.onBind(intent);
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		PluginDebugLog.log(TAG, "SelfLaunchServiceProxy>>>>>onDestroy()");
//		unRegisterScreenOffReceiver(getApplicationContext());//注销广播
//		SharedPreferencesHelper sp = SharedPreferencesHelper.getInstance(this.getApplicationContext());
//		boolean flag = sp.getBooleanValue(SharedPreferencesHelper.BI_SWITCH);//bi开关控制
//		if(flag){//开关打开再启动service
//			ManagerService.startServiceProxy(getApplicationContext());//再次启动service
//		}
	}
	
	/**
	 * 注册锁屏接收器
	 * 
	 * @param paramContext
	 */
//	private void registerScreenOffReceiver(Context paramContext) {
//		IntentFilter localIntentFilter = new IntentFilter();
//		localIntentFilter.addAction("android.intent.action.SCREEN_OFF");
//		localIntentFilter.addAction("android.intent.action.SCREEN_ON");
//		if (mRegisterReceiver == null) {
//			mRegisterReceiver = new ScreenOffBroadcastReceiver();
//		}
//		try {
//			paramContext.registerReceiver(mRegisterReceiver, localIntentFilter);
//		} catch (Exception localException) {
//			localException.printStackTrace();
//		}
//	}
	
	/**
	 * 注销锁屏接收器
	 * 
	 * @param paramContext
	 */
//	private void unRegisterScreenOffReceiver(Context paramContext) {
//		try {
//			if(mRegisterReceiver==null)return;
//			paramContext.unregisterReceiver(mRegisterReceiver);
//			mRegisterReceiver = null;
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
}
