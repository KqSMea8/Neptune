package org.qiyi.pluginlibrary.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.qiyi.pluginlibrary.ProxyEnvironment;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;

/**
 * @author zhuchengjin
 * 主要用于恢复插件中的sevice
 */
public class PlugServiceSp {

	private static final String PREFERENCE_NAME ="alive_service";
	
	private static PlugServiceSp instance;
	private SharedPreferences sp;

	public static PlugServiceSp getInstance(Context context) {
		if (instance == null && context != null) {
			instance = new PlugServiceSp(context);
		}
		return instance;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private PlugServiceSp(Context context) {
		if( android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
			sp = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_MULTI_PROCESS);//支持跨进程访问
		}else{
			sp = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
		}
	}
	
	/**
	 * @return
	 */
	public  Intent setParamIntent(){
		Intent paramIntent = new Intent();
		Map<String,?> map = sp.getAll();
		if(map == null) return paramIntent ;
		for(Map.Entry<String, ?> entry :map.entrySet()){
			String targetClassName = entry.getKey();
			String targetPackageName = (String)entry.getValue();
			paramIntent.putExtra(ProxyEnvironment.EXTRA_TARGET_SERVICE, targetClassName);
			paramIntent.putExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME,targetPackageName);
			return paramIntent;
		}
		return paramIntent;
	}
	
	/**
	 * @param paramIntent
	 * 保存启动的service
	 */
	public void saveAliveService(Intent paramIntent){
    	String  targetClassName = paramIntent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_SERVICE);
        String  targetPackageName = paramIntent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME);
    	Editor editor = sp.edit();
		editor.putString(targetClassName, targetPackageName).commit();
		
    }
	
	   /**
     * @param paramIntent
     * 删除关闭的service
     */
    public void removeAliveService(String targetClassName){
    	Editor editor = sp.edit();
        if(editor != null){
        	editor.remove(targetClassName).commit();
        }
    }
    
    
    /**
     * @return
     * 获取可运行的service
     */
    public List<Intent> getAliveService(){
    	Intent paramIntent = new Intent();
    	Map<String,?> map = sp.getAll();
    	List<Intent> intentLists = new ArrayList<Intent>();
    	if(map == null) return intentLists;
    	for(Map.Entry<String, ?> entry :map.entrySet()){
			String targetClassName = entry.getKey();
			String targetPackageName = (String)entry.getValue();
			paramIntent.putExtra(ProxyEnvironment.EXTRA_TARGET_SERVICE, targetClassName);
			paramIntent.putExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME,targetPackageName);
			intentLists.add(paramIntent);
		}
    	return intentLists;
    }
}
