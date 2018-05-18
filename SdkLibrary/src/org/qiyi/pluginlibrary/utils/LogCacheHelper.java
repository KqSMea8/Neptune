package org.qiyi.pluginlibrary.utils;

import org.qiyi.pluginlibrary.debug.PluginCenterDebugHelper;

import java.util.concurrent.ConcurrentLinkedQueue;

public class LogCacheHelper {

    private static ConcurrentLinkedQueue<String> mLogCache = new ConcurrentLinkedQueue<String>();
    private static final int MAX_LENGTH = 20;

    private static LogCacheHelper instance;

    public static LogCacheHelper getInstance(){
        if (instance == null){
            synchronized (LogCacheHelper.class){
                if (instance == null){
                    instance = new LogCacheHelper();
                }
            }
        }
        return instance;
    }

    public void addToCache(String log) {
        if (log != null){
            mLogCache.add(log);
        }
        if (mLogCache.size() >= MAX_LENGTH) {
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < MAX_LENGTH; i++) {
                stringBuffer.append(mLogCache.poll());
            }
            if (stringBuffer.length() != 0){
                PluginCenterDebugHelper.getInstance().savePluginLogInfo(PluginCenterDebugHelper.getInstance().getCurrentSystemTime(), stringBuffer);
            }
        }else if (mLogCache.size() > 0){
            if (!PluginDebugLog.isDebug()){
                StringBuffer stringBuffer = new StringBuffer();
                for (int i = 0; i < mLogCache.size(); i++) {
                    stringBuffer.append(mLogCache.poll());
                }
                if (stringBuffer.length() != 0){
                    PluginCenterDebugHelper.getInstance().savePluginLogInfo(PluginCenterDebugHelper.getInstance().getCurrentSystemTime(), stringBuffer);
                }
            }
        }
    }
}
