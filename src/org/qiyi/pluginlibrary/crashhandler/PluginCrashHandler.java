package org.qiyi.pluginlibrary.crashhandler;

import java.lang.Thread.UncaughtExceptionHandler;

import org.qiyi.pluginlibrary.utils.PluginDebugLog;


/**
 * @author zhuchengjin
 * 当程序发生Uncaught异常的时候,有该类来接管程序,并记录发送错误报告
 */
public class PluginCrashHandler implements UncaughtExceptionHandler {

	public static final String TAG = "pluginCrash";
	
	//系统默认的uncaughtException 处理异常
	public Thread.UncaughtExceptionHandler mDefaultHandler ;
	
	//异常处理类
	private static PluginCrashHandler _instance;
	
	private  static synchronized PluginCrashHandler getInstance(){
		if(_instance == null){
			_instance = new PluginCrashHandler();
		}
		return _instance;
	}
	
	/**
	 * 初始化崩溃信息的log
	 */
	public static void initCrashHandler(){
		getInstance().init();
	}
	
	private void init(){
		// 获取系统默认的UncaughtException处理器
		mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		// 设置该CrashHandler为程序的默认处理器
		Thread.setDefaultUncaughtExceptionHandler(this);
	}
	
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		if (!handleException(ex) && mDefaultHandler != null) {
			// 如果用户没有处理则让系统默认的异常处理器来处理
			mDefaultHandler.uncaughtException(thread, ex);
		} else {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}
	
	// 收集错误信息
	private boolean handleException(Throwable ex) {
		if (ex == null) {
			return false;
		}
		//debug模式下显示崩溃提示信息
		getErrorInfo(ex);
		return true;
	}

	// 整理错误信息
	private void getErrorInfo(Throwable ex) {
		// 异常对象
		Throwable throwable = ex.getCause();
		Throwable currentThrowable = null;
		if (throwable == null) {
			currentThrowable = ex;
		} else {
			while (throwable != null) {
				Throwable temp = throwable.getCause();
				if (temp == null) {
					currentThrowable = throwable;
					break;
				} else {
					throwable = temp;
				}

			}
		}
		StackTraceElement[] stack = currentThrowable.getStackTrace();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < stack.length; i++) {
			StackTraceElement ste = stack[i];
			if (i == 0) {
				buffer.append("Caused by: ");
				buffer.append(ste.toString() + "\n");

			} else {
				buffer.append("\tat " + ste.toString() + "\n");
			}
		}
		PluginDebugLog.log(TAG, "plugin崩溃信息 = " + buffer.toString());
	}
}
