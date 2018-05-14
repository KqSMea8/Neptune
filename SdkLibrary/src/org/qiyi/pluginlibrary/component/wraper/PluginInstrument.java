package org.qiyi.pluginlibrary.component.wraper;

import java.lang.reflect.Method;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.qiyi.pluginlibrary.utils.ComponetFinder;
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
 * 用于Hook插件Activity中Instrumentation
 *
 * @see android.app.Activity#startActivity(android.content.Intent)
 */
public class PluginInstrument extends Instrumentation {
    private static final String TAG = "PluginInstrument";

    private static ConcurrentMap<String, Vector<Method>> sMethods = new ConcurrentHashMap<String, Vector<Method>>(5);

    private String mPkgName;
    Instrumentation mHostInstr;
    private ReflectionUtils mInstrumentRef;

    /**
     * 插件的Instrumentation
     */
    public PluginInstrument(Instrumentation hostInstr) {
        this(hostInstr, "");
    }

    public PluginInstrument(Instrumentation hostInstr, String pkgName) {
        mHostInstr = hostInstr;
        mInstrumentRef = ReflectionUtils.on(hostInstr);
        mPkgName = pkgName;
    }

    /**
     * 如果是PluginInstrumentation，拆装出原始的HostInstr
     *
     * @param instrumentation
     * @return
     */
    public static Instrumentation unwrap(Instrumentation instrumentation) {
        if (instrumentation instanceof PluginInstrument) {
            return ((PluginInstrument)instrumentation).mHostInstr;
        }
        return instrumentation;
    }

    /**
     * @Override
     */
    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {

        ComponetFinder.switchToActivityProxy(mPkgName, intent, requestCode, who);
        try {
            return mInstrumentRef.call("execStartActivity", sMethods, who, contextThread, token, target, intent, requestCode, options).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @Override
     */
    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode) {
        ComponetFinder.switchToActivityProxy(mPkgName, intent, requestCode, who);
        try {
            return mInstrumentRef.call("execStartActivity", sMethods, who, contextThread, token, target, intent, requestCode).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @Override For below android 6.0
     */
    public ActivityResult execStartActivityAsCaller(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options, int userId) {
        ComponetFinder.switchToActivityProxy(mPkgName, intent, requestCode, who);
        try {
            return mInstrumentRef.call("execStartActivityAsCaller", sMethods, who, contextThread, token, target, intent, requestCode, options, userId)
                    .get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @Override For android 6.0
     */
    public ActivityResult execStartActivityAsCaller(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options,
            boolean ignoreTargetSecurity, int userId) {
        ComponetFinder.switchToActivityProxy(mPkgName, intent, requestCode, who);
        try {
            return mInstrumentRef.call("execStartActivityAsCaller", sMethods, who, contextThread, token, target, intent, requestCode, options,
                    ignoreTargetSecurity, userId).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @Override
     */
    public void execStartActivitiesAsUser(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent[] intents, Bundle options, int userId) {
        for (Intent intent : intents) {
            ComponetFinder.switchToActivityProxy(mPkgName, intent, 0, who);
        }
        try {
            mInstrumentRef.call("execStartActivitiesAsUser", sMethods, who, contextThread, token, target, intents, options, userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @Override For below android 6.0, start activity from Fragment
     */
    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Fragment target,
            Intent intent, int requestCode, Bundle options) {
        ComponetFinder.switchToActivityProxy(mPkgName, intent, requestCode, who);
        try {
            return mInstrumentRef.call("execStartActivity", sMethods, who, contextThread, token, target, intent, requestCode, options).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @Override For android 6.0, start activity from Fragment
     */
    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, String target,
            Intent intent, int requestCode, Bundle options) {
        ComponetFinder.switchToActivityProxy(mPkgName, intent, requestCode, who);
        try {
            return mInstrumentRef.call("execStartActivity", sMethods, who, contextThread, token, target, intent, requestCode, options).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
