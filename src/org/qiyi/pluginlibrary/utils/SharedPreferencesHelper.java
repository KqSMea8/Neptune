package org.qiyi.pluginlibrary.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
/**
 * 用于bi统计
 */
public class SharedPreferencesHelper {
	public static final String PREFERENCE_NAME = "bi4sdk";

	private static SharedPreferencesHelper instance;
	private static Context context;

	public static SharedPreferencesHelper getInstance(Context context) {
		if(instance == null){
			instance = new SharedPreferencesHelper(context);
		}
		return instance;
	}

	private SharedPreferencesHelper(Context mcontext) {
		if(mcontext == null){
			return;
		}
		context = mcontext.getApplicationContext();
	}

	@SuppressLint("InlinedApi")
	public static SharedPreferences getPreferences(){
		SharedPreferences spf = null;
		if(context == null) return null;
		if(hasHoneycomb()){
			spf = context.getSharedPreferences(PREFERENCE_NAME,Context.MODE_MULTI_PROCESS);
		}else{
			spf = context.getSharedPreferences(PREFERENCE_NAME,Context.MODE_PRIVATE);
		}
		return spf;
	}
	
	@TargetApi(11)
	private static boolean hasHoneycomb(){
		return Build.VERSION.SDK_INT >= 11;
	}
	
	//for bi
	public static final String IP = "IP";//ip地址
	public static final String LOG_DEBUG_KEY = "LOG_DEBUG_KEY";//debug开关
	public static final String SHUT_DOWN_TIME = "BI_SHUT_DOWN_TIME";//关机时间
	public static final String BI_UUID = "BI_UUID";//uuid
	public static final String BI_PLATFROM = "BI_PLATFROM";//平台信息
	public static final String BI_LOGIN_ID = "BI_LOGIN_ID";//用户登录ID
	public static final String BI_BE_KILLED = "BI_BE_KILLED";//表示服务是否被杀
	public static final String BI_SWITCH = "BI_SWITCH";//bi开关
	public static final String BI_FIRST_LAUCH = "BI_FIRST_LAUCH"; //BI第一次启动  
	public static final String BI_DELIVER_PERIOD = "BI_DELIVER_PERIOD";//投递周期
	public static final String BI_SEARCH_INFO_PERIOD = "BI_SEARCH_INFO_PERIOD";//用户信息扫描周期
    public static final String BI_LOCATION_LATI = "BI_LOCATION_LATI";//纬度
    public static final String BI_LOCATION_LONGTI = "BI_LOCATION_LONGTI";//经度

	public long getLongValue(String key) {
		if (key != null && !key.equals("")) {
			SharedPreferences sp = getPreferences();
			if(sp != null){
				return sp.getLong(key, 0);
			}
		}
		return 0;
	}

	public String getStringValue(String key) {
		if (key != null && !key.equals("")) {
			SharedPreferences sp = getPreferences();
			if(sp != null){
				return sp.getString(key, "");
			}
		}
		return null;
	}
	
	public String getStringValue(String key,String defValue) {
		if (key != null && !key.equals("")) {
			SharedPreferences sp = getPreferences();
			if(sp != null){
				return sp.getString(key, defValue);
			}
		}
		return defValue;
	}

	public int getIntValue(String key) {
		if (key != null && !key.equals("")) {
			SharedPreferences sp = getPreferences();
			if(sp != null){
				return sp.getInt(key, 0);
			}
		}
		return 0;
	}

	public boolean getBooleanValue(String key) {
		if (key != null && !key.equals("")) {
			SharedPreferences sp = getPreferences();
			if(sp != null){
				return sp.getBoolean(key, false);
			}
		}
		return true;
	}

	public float getFloatValue(String key) {
		if (key != null && !key.equals("")) {
			SharedPreferences sp = getPreferences();
			if(sp != null){
				return sp.getFloat(key, 0);
			}
		}
		return 0;
	}

	public void putStringValue(String key, String value) {
		if (key != null && !key.equals("")) {
			SharedPreferences sp = getPreferences();
			if(sp != null){
				Editor editor = sp.edit();
				editor.putString(key, value);
				commit(editor);
			}
		}
	}

	public void putIntValue(String key, int value) {
		if (key != null && !key.equals("")) {
			SharedPreferences sp = getPreferences();
			if(sp != null){
				Editor editor = sp.edit();
				editor.putInt(key, value);
				commit(editor);
			}
		}
	}

	public void putBooleanValue(String key, boolean value) {
		if (key != null && !key.equals("")) {
			
			SharedPreferences sp = getPreferences();
			if(sp != null){
				Editor editor = sp.edit();
				editor.putBoolean(key, value);
				commit(editor);
			}
		}
	}

	public void putLongValue(String key, long value) {
		if (key != null && !key.equals("")) {
			SharedPreferences sp = getPreferences();
			if(sp != null){
				Editor editor = sp.edit();
				editor.putLong(key, value);
				commit(editor);
			}
		}
	}

	public void putFloatValue(String key, Float value) {
		if (key != null && !key.equals("")) {
			SharedPreferences sp = getPreferences();
			if(sp != null){
				Editor editor = sp.edit();
				editor.putFloat(key, value);
				commit(editor);
			}
		}
	}
	
	 @SuppressLint("NewApi")
	public static void commit(Editor dateEditor ){
		if(dateEditor == null)return;
   		if(android.os.Build.VERSION.SDK_INT < 9){
   			dateEditor.commit();
   		}else{
   			dateEditor.apply();
   		}
   	}
}
