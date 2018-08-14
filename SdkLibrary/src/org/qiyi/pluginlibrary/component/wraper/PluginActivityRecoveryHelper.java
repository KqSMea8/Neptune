package org.qiyi.pluginlibrary.component.wraper;

import android.annotation.NonNull;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;

import org.qiyi.pluginlibrary.component.TransRecoveryActivity0;
import org.qiyi.pluginlibrary.component.TransRecoveryActivity1;
import org.qiyi.pluginlibrary.component.TransRecoveryActivity2;

import java.util.UUID;

/**
 * 插件进程被回收以后，恢复阶段辅助工具类
 * <p>
 * 每个待恢复的 icicle 与 savedInstanceState 都会创建一个 id 保存在与之对应的 activity intent 里面进行传递。
 * 恢复的时候取出 id 即可找到对应的 icicle 与 savedInstanceState
 */
class PluginActivityRecoveryHelper {
    private static final String KEY_RECOVERY_ICICLE = "org.qiyi.PluginActivityRecoveryHelper.icicle";
    private static final String KEY_RECOVERY_SAVED_INSTANCE_STATE = "org.qiyi.PluginActivityRecoveryHelper.savedInstanceState";

    private ArrayMap<String, Bundle> mPendingIcicleMap = new ArrayMap<>();
    private ArrayMap<String, Bundle> mPendingSavedInstanceStateMap = new ArrayMap<>();

    private boolean shouldIgnore(Activity activity) {
        return activity == null || activity.getIntent() == null;
    }

    Bundle recoveryIcicle(Activity activity, Bundle icicle) {
        if (shouldIgnore(activity)) {
            return icicle;
        }
        String id = activity.getIntent().getStringExtra(KEY_RECOVERY_ICICLE);
        if (id != null) {
            Bundle mappedIcicle = mPendingIcicleMap.remove(id);
            if (mappedIcicle != null) {
                return mappedIcicle;
            }
        }
        return icicle;
    }

    void saveIcicle(Activity activity, Bundle icicle) {
        if (shouldIgnore(activity)) {
            return;
        }
        String id = UUID.randomUUID().toString();
        Intent intent = activity.getIntent();
        intent.putExtra(KEY_RECOVERY_ICICLE, id);
        mPendingIcicleMap.put(id, icicle);
    }

    void saveSavedInstanceState(Activity activity, Bundle savedInstanceState) {
        if (shouldIgnore(activity)) {
            return;
        }
        String id = UUID.randomUUID().toString();
        Intent intent = activity.getIntent();
        intent.putExtra(KEY_RECOVERY_SAVED_INSTANCE_STATE, id);
        mPendingSavedInstanceStateMap.put(id, savedInstanceState);
    }

    void mockActivityOnRestoreInstanceStateIfNeed(Instrumentation instr, Activity activity) {
        if (shouldIgnore(activity)) {
            return;
        }
        String id = activity.getIntent().getStringExtra(KEY_RECOVERY_SAVED_INSTANCE_STATE);
        if (id != null) {
            Bundle mappedSavedInstanceState = mPendingSavedInstanceStateMap.remove(id);
            if (mappedSavedInstanceState != null) {
                instr.callActivityOnRestoreInstanceState(activity, mappedSavedInstanceState);
            }
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
                return TransRecoveryActivity0.class.getName();
            case '1':
                return TransRecoveryActivity1.class.getName();
            case '2':
                return TransRecoveryActivity2.class.getName();
            default:
                throw new IllegalStateException("can not find RecoveryActivity for " + proxyClassName);
        }
    }
}
