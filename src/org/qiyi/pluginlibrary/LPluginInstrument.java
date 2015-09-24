package org.qiyi.pluginlibrary;

import org.qiyi.pluginlibrary.utils.ReflectionUtils;
import org.qiyi.pluginnew.ActivityJumpUtil;

import android.app.Activity;
import android.app.Fragment;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

/**
 * 负责转移插件的跳转目标<br>
 *
 * @see android.app.Activity#startActivity(android.content.Intent)
 */
public class LPluginInstrument extends Instrumentation {

    private static final String TAG = LPluginInstrument.class.getSimpleName();
    private String mPkgName;
    Instrumentation mHostInstr;
    ReflectionUtils instrumentRef;

    public LPluginInstrument(Instrumentation pluginIn, String pkgName) {
        this.mHostInstr = pluginIn;
        instrumentRef = ReflectionUtils.on(pluginIn);
        mPkgName = pkgName;
    }

    /** @Override */
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token,
            Activity target, Intent intent, int requestCode, Bundle options) {

        ActivityJumpUtil.handleStartActivityIntent(mPkgName, intent, requestCode, options, who);
        return instrumentRef.call("execStartActivity", who, contextThread, token, target, intent,
                requestCode, options).get();

    }

    /** @Override */
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token,
            Activity target, Intent intent, int requestCode) {
        ActivityJumpUtil.handleStartActivityIntent(mPkgName, intent, requestCode, null, who);
        return instrumentRef.call("execStartActivity", who, contextThread, token, target, intent,
                requestCode).get();

    }

    /** @Override */
    public ActivityResult execStartActivityAsCaller(Context who, IBinder contextThread,
            IBinder token, Activity target, Intent intent, int requestCode, Bundle options,
            int userId) {
        ActivityJumpUtil.handleStartActivityIntent(mPkgName, intent, requestCode, null, who);
        return instrumentRef.call("execStartActivityAsCaller", who, contextThread, token, target,
                intent, requestCode, options, userId).get();
    }

    /** @Override */
    public void execStartActivitiesAsUser(Context who, IBinder contextThread, IBinder token,
            Activity target, Intent[] intents, Bundle options, int userId) {
        for (Intent intent : intents) {
            ActivityJumpUtil.handleStartActivityIntent(mPkgName, intent, 0, options, who);
        }
        instrumentRef.call("execStartActivitiesAsUser", who, contextThread, token, target, intents,
                options, userId);
    }

    /** @Override */
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token,
                Fragment target, Intent intent, int requestCode, Bundle options) {
        ActivityJumpUtil.handleStartActivityIntent(mPkgName, intent, requestCode, null, who);
        return instrumentRef.call("execStartActivity",
                who, contextThread, token, target, intent, requestCode, options).get();
    }
}
