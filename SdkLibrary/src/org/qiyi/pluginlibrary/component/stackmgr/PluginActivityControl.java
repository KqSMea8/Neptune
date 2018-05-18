package org.qiyi.pluginlibrary.component.stackmgr;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.qiyi.pluginlibrary.error.ErrorType;
import org.qiyi.pluginlibrary.plugin.PluginActivityCallback;
import org.qiyi.pluginlibrary.runtime.PluginManager;
import org.qiyi.pluginlibrary.utils.ReflectionUtils;
import org.qiyi.pluginlibrary.exception.ReflectException;
import org.qiyi.pluginlibrary.utils.VersionUtils;

/**
 * 插件的控制器<br> 派发插件事件和控制插件生命周期
 */
public class PluginActivityControl implements PluginActivityCallback {
    public static ConcurrentMap<String, Vector<Method>> sMethods = new ConcurrentHashMap<String, Vector<Method>>();
    Activity mProxy;// 代理Activity
    Activity mPlugin;// 插件Activity
    ReflectionUtils mProxyRef;// 指向代理Activity的反射工具类
    ReflectionUtils mPluginRef;// 指向插件Activity的反射工具类
    Application mApplication;// 分派给插件的Application
    Instrumentation mHostInstr;

    /**
     * @param proxy  代理Activity
     * @param plugin 插件Activity
     * @param app    分派给插件的Application
     * @throws Exception
     */
    public PluginActivityControl(Activity proxy, Activity plugin, Application app, Instrumentation pluginInstr) throws Exception {
        if (null == proxy || null == plugin || null == app || null == pluginInstr) {
            throw new Exception("proxy, plugin, app, pluginInstr shouldn't be null! proxy: " + proxy + " plugin: " + plugin + " app: " + app
                    + " pluginInstr: " + pluginInstr);
        }
        mProxy = proxy;
        mPlugin = plugin;
        mApplication = app;

        mHostInstr = pluginInstr;

        // 使反射工具类指向相应的对象
        mProxyRef = ReflectionUtils.on(proxy);
        mPluginRef = ReflectionUtils.on(plugin);
    }

    public void dispatchProxyToPlugin(Instrumentation pluginInstr, Context contextWrapper, String packageName) {
        if (mPlugin == null || mPlugin.getBaseContext() != null || pluginInstr == null) {
            return;
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                // Android O的attach方法在7.0的参数基础上增加了android.view.ViewRootImpl$ActivityConfigCallback这样一个参数，这个参数是PhoneWindow的成员变量
                // 8.0, android.os.Build.VERSION_CODES.O
                callAttachV26(pluginInstr);
            } else if (android.os.Build.VERSION.SDK_INT >= 24) {
                // Android N的attach方法在6.0的参数基础上增加了一个Window的参数
                // 7.0, android.os.Build.VERSION_CODES.N
                callAttachV24(pluginInstr);
            } else if (android.os.Build.VERSION.SDK_INT >= 22) {
                // Android 5.1的attach方法在5.0基础上增加了一个String mReferrer参数
                // 5.1 and 6.0, android.os.Build.VERSION_CODES.LOLLIPOP_MR1
                // android.os.Build.VERSION_CODES.M
                callAttachV22(pluginInstr);
            } else if (android.os.Build.VERSION.SDK_INT >= 21) {
                // Android 5.0的attach方法在4.x基础上增加了一个IVoiceInteractor mVoiceInteractor参数
                // 5.0, android.os.Build.VERSION_CODES.LOLLIPOP;
                callAttachV21(pluginInstr);
            } else if (android.os.Build.VERSION.SDK_INT >= 14){
                // 4.x, android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH~KITKAT
                callAttachV14(pluginInstr);
            } else {
                // 2.x, android.os.Build.VERSION_CODES.GINGERBREAD
                callAttachV4(pluginInstr);
            }

            mPluginRef.set("mWindow", mProxy.getWindow());
            mPluginRef.set("mWindowManager", mProxy.getWindow().getWindowManager());
            mPlugin.getWindow().setCallback(mPlugin);
            ReflectionUtils.on(mProxy.getBaseContext()).call("setOuterContext", sMethods, mPlugin);

        } catch (ReflectException e) {
            PluginManager.deliver(mProxy, false, packageName, ErrorType.ERROR_CLIENT_DISPATCH_PROXY_TO_PLUGIN_FAIL);
            e.printStackTrace();
        }

    }


    /**
     * 反射调用Android O上的Activity#attach()方法
     * Android O的attach方法在7.0的参数基础上增加了android.view.ViewRootImpl$ActivityConfigCallback参数
     * <p>
     * <href>http://androidxref.com/8.0.0_r4/xref/frameworks/base/core/java/android/app/Activity.java#6902</href>
     */
    private void callAttachV26(Instrumentation pluginInstr) {
        try {
            mPluginRef.call(
                    // 方法名
                    "attach",
                    sMethods,
                    // Context context
                    // contextWrapper,
                    mProxy,
                    // ActivityThread aThread
                    mProxyRef.get("mMainThread"),
                    // Instrumentation instr
                    pluginInstr,
                    // IBinder token
                    mProxyRef.get("mToken"),
                    // int ident
                    mProxyRef.get("mIdent"),
                    // Application application
                    mApplication == null ? mProxy.getApplication() : mApplication,
                    // Intent intent
                    mProxy.getIntent(),
                    // ActivityInfo info
                    mProxyRef.get("mActivityInfo"),
                    // CharSequence title
                    mProxy.getTitle(),
                    // Activity parent
                    mProxy.getParent(),
                    // String id
                    mProxyRef.get("mEmbeddedID"),
                    // NonConfigurationInstances
                    // lastNonConfigurationInstances
                    mProxy.getLastNonConfigurationInstance(),
                    // Configuration config
                    mProxyRef.get("mCurrentConfig"),
                    // String mReferrer
                    mProxyRef.get("mReferrer"),
                    // IVoiceInteractor mVoiceInteractor
                    mProxyRef.get("mVoiceInteractor"),
                    // Window window
                    mProxy.getWindow(),
                    // android.view.ViewRootImpl$ActivityConfigCallback activityConfigCallback, 这个参数在PhoneWindow中
                    // 由于后面会替换插件Activity的Window对象，此处可以传null
                    null);
        } catch (ReflectException re) {
            re.printStackTrace();
            callAttachV24(pluginInstr);
        }
    }

    /**
     * 反射调用Android N上的Activity#attach()方法
     * Android N的attach方法在6.0的参数基础上增加了一个Window的参数
     * <p>
     * <href>http://androidxref.com/7.0.0_r1/xref/frameworks/base/core/java/android/app/Activity.java#6593</href>
     */
    private void callAttachV24(Instrumentation pluginInstr) {
        try {
            mPluginRef.call(
                    // 方法名
                    "attach",
                    sMethods,
                    // Context context
                    // contextWrapper,
                    mProxy,
                    // ActivityThread aThread
                    mProxyRef.get("mMainThread"),
                    // Instrumentation instr
                    pluginInstr,
                    // IBinder token
                    mProxyRef.get("mToken"),
                    // int ident
                    mProxyRef.get("mIdent"),
                    // Application application
                    mApplication == null ? mProxy.getApplication() : mApplication,
                    // Intent intent
                    mProxy.getIntent(),
                    // ActivityInfo info
                    mProxyRef.get("mActivityInfo"),
                    // CharSequence title
                    mProxy.getTitle(),
                    // Activity parent
                    mProxy.getParent(),
                    // String id
                    mProxyRef.get("mEmbeddedID"),
                    // NonConfigurationInstances
                    // lastNonConfigurationInstances
                    mProxy.getLastNonConfigurationInstance(),
                    // Configuration config
                    mProxyRef.get("mCurrentConfig"),
                    // String mReferrer
                    mProxyRef.get("mReferrer"),
                    // IVoiceInteractor mVoiceInteractor
                    mProxyRef.get("mVoiceInteractor"),
                    // Window window
                    mProxy.getWindow());
        } catch (ReflectException re) {
            re.printStackTrace();
            callAttachV22(pluginInstr);
        }
    }

    /**
     * 反射调用Android 5.1 and 6.0上的Activity#attach()方法
     * Android 5.1的attach方法在5.0基础上增加了一个String mReferrer参数
     * <p>
     * <href>http://androidxref.com/5.1.0_r1/xref/frameworks/base/core/java/android/app/Activity.java#5922</href>
     */
    private void callAttachV22(Instrumentation pluginInstr) {
        try {
            mPluginRef.call(
                    // 方法名
                    "attach",
                    sMethods,
                    // Context context
                    // contextWrapper,
                    mProxy,
                    // ActivityThread aThread
                    mProxyRef.get("mMainThread"),
                    // Instrumentation instr
                    pluginInstr,
                    // IBinder token
                    mProxyRef.get("mToken"),
                    // int ident
                    mProxyRef.get("mIdent"),
                    // Application application
                    mApplication == null ? mProxy.getApplication() : mApplication,
                    // Intent intent
                    mProxy.getIntent(),
                    // ActivityInfo info
                    mProxyRef.get("mActivityInfo"),
                    // CharSequence title
                    mProxy.getTitle(),
                    // Activity parent
                    mProxy.getParent(),
                    // String id
                    mProxyRef.get("mEmbeddedID"),
                    // NonConfigurationInstances
                    // lastNonConfigurationInstances
                    mProxy.getLastNonConfigurationInstance(),
                    // Configuration config
                    mProxyRef.get("mCurrentConfig"),
                    // String mReferrer
                    mProxyRef.get("mReferrer"),
                    // IVoiceInteractor mVoiceInteractor
                    mProxyRef.get("mVoiceInteractor"));
        } catch (ReflectException re) {
            re.printStackTrace();
            callAttachV21(pluginInstr);
        }
    }

    /**
     * 反射调用Android 5.0上的Activity#attach()方法
     * Android 5.0的attach方法在4.x基础上增加了一个IVoiceInteractor mVoiceInteractor参数
     * <p>
     * <href>http://androidxref.com/5.0.0_r2/xref/frameworks/base/core/java/android/app/Activity.java#5866</href>
     */
    private void callAttachV21(Instrumentation pluginInstr) {
        try {
            mPluginRef.call(
                    // 方法名
                    "attach",
                    sMethods,
                    // Context context
                    // contextWrapper,
                    mProxy,
                    // ActivityThread aThread
                    mProxyRef.get("mMainThread"),
                    // Instrumentation instr
                    pluginInstr,
                    // IBinder token
                    mProxyRef.get("mToken"),
                    // int ident
                    mProxyRef.get("mIdent"),
                    // Application application
                    mApplication == null ? mProxy.getApplication() : mApplication,
                    // Intent intent
                    mProxy.getIntent(),
                    // ActivityInfo info
                    mProxyRef.get("mActivityInfo"),
                    // CharSequence title
                    mProxy.getTitle(),
                    // Activity parent
                    mProxy.getParent(),
                    // String id
                    mProxyRef.get("mEmbeddedID"),
                    // NonConfigurationInstances
                    // lastNonConfigurationInstances
                    mProxy.getLastNonConfigurationInstance(),
                    // Configuration config
                    mProxyRef.get("mCurrentConfig"),
                    // IVoiceInteractor mVoiceInteractor
                    mProxyRef.get("mVoiceInteractor"));
        } catch (ReflectException re) {
            re.printStackTrace();
            callAttachV14(pluginInstr);
        }
    }

    /**
     * 反射调用4.x上的Activity#attach()方法
     * <p>
     * <href>http://androidxref.com/4.0.3_r1/xref/frameworks/base/core/java/android/app/Activity.java#4409</href>
     */
    private void callAttachV14(Instrumentation pluginInstr) {
        try {
            mPluginRef.call(
                    // 方法名
                    "attach",
                    sMethods,
                    // Context context
                    // contextWrapper,
                    mProxy,
                    // ActivityThread aThread
                    mProxyRef.get("mMainThread"),
                    // Instrumentation instr
                    pluginInstr,
                    // IBinder token
                    mProxyRef.get("mToken"),
                    // int ident
                    mProxyRef.get("mIdent"),
                    // Application application
                    mApplication == null ? mProxy.getApplication() : mApplication,
                    // Intent intent
                    mProxy.getIntent(),
                    // ActivityInfo info
                    mProxyRef.get("mActivityInfo"),
                    // CharSequence title
                    mProxy.getTitle(),
                    // Activity parent
                    mProxy.getParent(),
                    // String id
                    mProxyRef.get("mEmbeddedID"),
                    // NonConfigurationInstances
                    // lastNonConfigurationInstances
                    mProxy.getLastNonConfigurationInstance(),
                    // Configuration config
                    mProxyRef.get("mCurrentConfig"));
        } catch (ReflectException re) {
            re.printStackTrace();
            callAttachV4(pluginInstr);
        }
    }

    /**
     * 反射调用2.x上的Activity#attach()方法
     * <href>http://androidxref.com/2.3.6/xref/frameworks/base/core/java/android/app/Activity.java#3739</href>
     */
    private void callAttachV4(Instrumentation pluginInstr) {
        mPluginRef.call(
                // 方法名
                "attach",
                sMethods,
                // Context context
                // contextWrapper,
                mProxy,
                // ActivityThread aThread
                mProxyRef.get("mMainThread"),
                // Instrumentation instr
                pluginInstr,
                // IBinder token
                mProxyRef.get("mToken"),
                // int ident
                mProxyRef.get("mIdent"),
                // Application application
                mApplication == null ? mProxy.getApplication() : mApplication,
                // Intent intent
                mProxy.getIntent(),
                // ActivityInfo info
                mProxyRef.get("mActivityInfo"),
                // CharSequence title
                mProxy.getTitle(),
                // Activity parent
                mProxy.getParent(),
                // String id
                mProxyRef.get("mEmbeddedID"),
                // Object lastNonConfigurationInstances
                mProxy.getLastNonConfigurationInstance(),
                // HashMap<String, Object> lastNonConfigurationChildInstances
                null,
                // Configuration config
                mProxyRef.get("mCurrentConfig"));
    }

    /**
     * @return 插件的Activity
     */
    public Activity getPlugin() {
        return mPlugin;
    }

    /**
     * 设置插件的Activity
     *
     * @param plugin
     */
    public void setPlugin(Activity plugin) {
        mPlugin = plugin;
        mProxyRef = ReflectionUtils.on(plugin);
    }

    /**
     * 得到代理的Activity
     *
     * @return
     */
    public Activity getProxy() {
        return mProxy;
    }

    /**
     * 设置代理的Activity
     *
     * @param proxy
     */
    public void setProxy(Activity proxy) {
        mProxy = proxy;
        mProxyRef = ReflectionUtils.on(proxy);
    }

    /**
     * @return 代理Activity的反射工具类
     */
    public ReflectionUtils getProxyRef() {
        return mProxyRef;
    }

    /**
     * @return 插件Activity的反射工具类
     */
    public ReflectionUtils getPluginRef() {
        return mPluginRef;
    }

    /**
     * 执行插件的onCreate方法
     *
     * @param saveInstance
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void callOnCreate(Bundle saveInstance) {
        if (null != mHostInstr) {
            mHostInstr.callActivityOnCreate(mPlugin, saveInstance);
        }
    }

    @Override
    public void callOnPostCreate(Bundle savedInstanceState) {
        if (null != mHostInstr) {
            mHostInstr.callActivityOnPostCreate(mPlugin, savedInstanceState);
        }
    }

    /**
     * 执行插件的onStart方法
     *
     * @see android.app.Activity#onStart()
     */
    @Override
    public void callOnStart() {
        if (null != getPluginRef()) {
            getPluginRef().call("performStart", sMethods);
        }
    }

    /**
     * 执行插件的onResume方法
     *
     * @see android.app.Activity#onResume()
     */
    @Override
    public void callOnResume() {
        if (null != getPluginRef()) {
            getPluginRef().call("performResume", sMethods);
        }
    }

    /**
     * 执行插件的onDestroy方法
     *
     * @see android.app.Activity#onDestroy()
     */
    @Override
    public void callOnDestroy() {
        if (null != mHostInstr) {
            mHostInstr.callActivityOnDestroy(mPlugin);
        }
    }

    /**
     * 执行插件的onStop方法
     *
     * @see android.app.Activity#onStop()
     */
    @Override
    public void callOnStop() {
        if (null != getPluginRef()) {
            if (VersionUtils.hasNougat()) {
                // 此处强制写false可能带来一些风险，暂时没有其他的方法处理
                getPluginRef().call("performStop", sMethods, false);
            } else {
                getPluginRef().call("performStop", sMethods);
            }
        }
    }

    /**
     * 执行插件的onRestart方法
     *
     * @see android.app.Activity#onRestart()
     */
    @Override
    public void callOnRestart() {
        if (null != getPluginRef()) {
            getPluginRef().call("performRestart", sMethods);
        }
    }

    /**
     * 执行插件的onSaveInstanceState方法
     *
     * @param outState
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void callOnSaveInstanceState(Bundle outState) {
        if (null != mHostInstr) {
            mHostInstr.callActivityOnSaveInstanceState(mPlugin, outState);
        }
    }

    /**
     * 执行插件的onRestoreInstanceState方法
     *
     * @param savedInstanceState
     * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
     */
    @Override
    public void callOnRestoreInstanceState(Bundle savedInstanceState) {
        if (null != mHostInstr) {
            mHostInstr.callActivityOnRestoreInstanceState(mPlugin, savedInstanceState);
        }
    }

    /**
     * 执行插件的onStop方法
     *
     * @see android.app.Activity#onStop()
     */
    @Override
    public void callOnPause() {
        if (null != getPluginRef()) {
            getPluginRef().call("performPause", sMethods);
        }
    }

    /**
     * 执行插件的onBackPressed方法
     *
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void callOnBackPressed() {
        if (null != mPlugin) {
            mPlugin.onBackPressed();
        } else if (null != mProxy) {
            mProxy.onBackPressed();
        }
    }

    /**
     * 执行插件的onKeyDown方法
     *
     * @param keyCode
     * @param event
     * @return
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean callOnKeyDown(int keyCode, KeyEvent event) {
        if (null != mPlugin) {
            return mPlugin.onKeyDown(keyCode, event);
        } else if (null != mProxy) {
            return mProxy.onKeyDown(keyCode, event);
        }
        return false;
    }

    // Finals ADD 修复Fragment BUG
    @Override
    public void callDump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (null != mPlugin) {
                mPlugin.dump(prefix, fd, writer, args);
            } else if (null != mProxy) {
                mProxy.dump(prefix, fd, writer, args);
            }
        }
    }

    @Override
    public void callOnConfigurationChanged(Configuration newConfig) {
        if (null != mPlugin) {
            mPlugin.onConfigurationChanged(newConfig);
        } else if (null != mProxy) {
            mProxy.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void callOnPostResume() {
        getPluginRef().call("onPostResume", sMethods);
    }

    @Override
    public void callOnDetachedFromWindow() {
        if (null != mPlugin) {
            mPlugin.onDetachedFromWindow();
        } else if (null != mProxy) {
            mProxy.onDetachedFromWindow();
        }
    }

    @Override
    public View callOnCreateView(String name, Context context, AttributeSet attrs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (null != mPlugin) {
                return mPlugin.onCreateView(name, context, attrs);
            } else if (null != mProxy) {
                return mProxy.onCreateView(name, context, attrs);
            }
        }
        return null;
    }

    @Override
    public View callOnCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (null != mPlugin) {
                return mPlugin.onCreateView(parent, name, context, attrs);
            } else if (null != mProxy) {
                return mProxy.onCreateView(parent, name, context, attrs);
            }
        }
        return null;
    }

    @Override
    public void callOnNewIntent(Intent intent) {
        if (null != mHostInstr) {
            mHostInstr.callActivityOnNewIntent(mPlugin, intent);
        }
    }

    @Override
    public void callOnActivityResult(int requestCode, int resultCode, Intent data) {
        getPluginRef().call("onActivityResult", sMethods, requestCode, resultCode, data);
    }
}