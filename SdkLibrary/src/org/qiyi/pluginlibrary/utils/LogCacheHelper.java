/**
 *
 * Copyright 2018 iQIYI.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.qiyi.pluginlibrary.utils;

import org.qiyi.pluginlibrary.debug.PluginCenterDebugHelper;

import java.util.concurrent.ConcurrentLinkedQueue;

public class LogCacheHelper {

    private static ConcurrentLinkedQueue<String> mLogCache = new ConcurrentLinkedQueue<String>();
    private static final int MAX_LENGTH = 30;
    private static LogCacheHelper instance;
    private boolean isNeedPersistence = false;


    public static LogCacheHelper getInstance() {
        if (instance == null) {
            synchronized (LogCacheHelper.class) {
                if (instance == null) {
                    instance = new LogCacheHelper();
                }
            }
        }
        return instance;
    }

    public void setIsNeedPersistence(boolean isNeedPersistence){
        this.isNeedPersistence = isNeedPersistence;
    }

    /**
     * 将log日志加入cache，cache栈满后持久化
     *
     * @param log 逐条日志
     */
    public void addToCache(String log) {
        if (!isNeedPersistence) return;
        if (log != null) {
            mLogCache.add(log);
        }
        if (mLogCache.size() >= MAX_LENGTH) {
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < MAX_LENGTH; i++) {
                stringBuffer.append(mLogCache.poll());
            }
            if (stringBuffer.length() != 0) {
                PluginCenterDebugHelper.getInstance().savePluginLogInfo(stringBuffer);
            }
        }
    }

    /**
     * 将内存cache中的数据，全部持久化
     */
    public void pollAllCacheToFile() {
        if (mLogCache.size() > 0) {
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < mLogCache.size(); i++) {
                stringBuffer.append(mLogCache.poll());
            }
            if (stringBuffer.length() != 0) {
                PluginCenterDebugHelper.getInstance().savePluginLogInfo(stringBuffer);
            }
        }
    }
}
