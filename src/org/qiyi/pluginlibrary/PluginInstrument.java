package org.qiyi.pluginlibrary;

import java.lang.reflect.Method;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.qiyi.pluginlibrary.utils.ReflectionUtils;

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
public class PluginInstrument extends Instrumentation {
    private static ConcurrentMap<String, Vector<Method>> sMethods = new ConcurrentHashMap<String, Vector<Method>>(5);

    private String mPkgName;
    Instrumentation mHostInstr;
    ReflectionUtils mInstrumentRef;

    public PluginInstrument(Instrumentation pluginIn, String pkgName) {
        this.mHostInstr = pluginIn;
        mInstrumentRef = ReflectionUtils.on(pluginIn);
        mPkgName = pkgName;
    }

    /** @Override */
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target, Intent intent,
            int requestCode, Bundle options) {

        ActivityJumpUtil.handleStartActivityIntent(mPkgName, intent, requestCode, options, who);
        return mInstrumentRef.call("execStartActivity", sMethods, who, contextThread, token, target, intent, requestCode, options).get();

    }

    /** @Override */
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target, Intent intent,
            int requestCode) {
        ActivityJumpUtil.handleStartActivityIntent(mPkgName, intent, requestCode, null, who);
        return mInstrumentRef.call("execStartActivity", sMethods, who, contextThread, token, target, intent, requestCode).get();

    }

    /**
     * @Override For below android 6.0
     */
    public ActivityResult execStartActivityAsCaller(Context who, IBinder contextThread, IBinder token, Activity target, Intent intent,
            int requestCode, Bundle options, int userId) {
        ActivityJumpUtil.handleStartActivityIntent(mPkgName, intent, requestCode, null, who);
        return mInstrumentRef.call("execStartActivityAsCaller", sMethods, who, contextThread, token, target, intent, requestCode, options, userId)
                .get();
    }

    /**
     * @Override For android 6.0
     */
    public ActivityResult execStartActivityAsCaller(Context who, IBinder contextThread, IBinder token, Activity target, Intent intent,
            int requestCode, Bundle options, boolean ignoreTargetSecurity, int userId) {
        ActivityJumpUtil.handleStartActivityIntent(mPkgName, intent, requestCode, null, who);
        return mInstrumentRef.call("execStartActivityAsCaller", sMethods, who, contextThread, token, target, intent, requestCode, options,
                ignoreTargetSecurity, userId).get();
    }

    /** @Override */
    public void execStartActivitiesAsUser(Context who, IBinder contextThread, IBinder token, Activity target, Intent[] intents,
            Bundle options, int userId) {
        for (Intent intent : intents) {
            ActivityJumpUtil.handleStartActivityIntent(mPkgName, intent, 0, options, who);
        }
        mInstrumentRef.call("execStartActivitiesAsUser", sMethods, who, contextThread, token, target, intents, options, userId);
    }

    /**
     * @Override For below android 6.0, start activity from Fragment
     */
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Fragment target, Intent intent,
            int requestCode, Bundle options) {
        ActivityJumpUtil.handleStartActivityIntent(mPkgName, intent, requestCode, null, who);
        return mInstrumentRef.call("execStartActivity", sMethods, who, contextThread, token, target, intent, requestCode, options).get();
    }

    /**
     * @Override For android 6.0, start activity from Fragment
     */
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, String target, Intent intent,
            int requestCode, Bundle options) {
        ActivityJumpUtil.handleStartActivityIntent(mPkgName, intent, requestCode, null, who);
        return mInstrumentRef.call("execStartActivity", sMethods, who, contextThread, token, target, intent, requestCode, options).get();
    }
}
