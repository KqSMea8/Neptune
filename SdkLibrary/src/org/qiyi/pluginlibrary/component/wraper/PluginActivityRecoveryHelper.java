package org.qiyi.pluginlibrary.component.wraper;

import android.annotation.NonNull;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;

import org.qiyi.pluginlibrary.component.RecoveryActivity0;
import org.qiyi.pluginlibrary.component.RecoveryActivity1;
import org.qiyi.pluginlibrary.component.RecoveryActivity2;

/**
 * 插件进程被回收以后，恢复阶段辅助工具类
 */
class PluginActivityRecoveryHelper {
    private Bundle mPendingICicle;
    private Intent mPendingIntent;
    private Bundle mPendingSavedInstanceState;

    private <T> T select(T saved, T input) {
        if (saved != null) {
            return saved;
        }
        return input;
    }

    Intent recoveryIntent(Intent intent) {
        Intent result = select(mPendingIntent, intent);
        mPendingIntent = null;
        return result;
    }

    Bundle recoveryIcicle(Bundle icicle) {
        Bundle result = select(mPendingICicle, icicle);
        mPendingICicle = null;
        return result;
    }

    void saveIntent(Intent intent) {
        mPendingIntent = new Intent(intent);
    }

    void saveIcicle(Bundle icicle) {
        mPendingICicle = icicle;
    }

    void saveSavedInstanceState(Bundle savedInstanceState) {
        mPendingSavedInstanceState = savedInstanceState;
    }

    void mockActivityOnRestoreInstanceStateIfNeed(Instrumentation instr, Activity activity) {
        if (mPendingSavedInstanceState != null) {
            instr.callActivityOnRestoreInstanceState(activity, mPendingSavedInstanceState);
            mPendingSavedInstanceState = null;
        }
    }

    /**
     * 为各个代理 Activity 选择相应进程的 RecoveryActivity
     *
     * @param proxyClassName 代理 Activity 名称
     * @return RecoveryActivity
     */
    String selectRecoveryActivity(@NonNull String proxyClassName) {
        char lastChar = proxyClassName.charAt(proxyClassName.length() - 1);
        switch (lastChar) {
            case '0':
                return RecoveryActivity0.class.getName();
            case '1':
                return RecoveryActivity1.class.getName();
            case '2':
                return RecoveryActivity2.class.getName();
            default:
                throw new IllegalStateException("can not find RecoveryActivity for " + proxyClassName);
        }
    }
}