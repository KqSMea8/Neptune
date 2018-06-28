package org.qiyi.pluginlibrary.component;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.assist.AssistContent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import org.qiyi.pluginlibrary.HybirdPlugin;
import org.qiyi.pluginlibrary.component.stackmgr.PServiceSupervisor;
import org.qiyi.pluginlibrary.component.stackmgr.PluginActivityControl;
import org.qiyi.pluginlibrary.component.stackmgr.PluginServiceWrapper;
import org.qiyi.pluginlibrary.constant.IIntentConstant;
import org.qiyi.pluginlibrary.context.PluginContextWrapper;
import org.qiyi.pluginlibrary.error.ErrorType;
import org.qiyi.pluginlibrary.listenter.IResourchStaticsticsControllerManager;
import org.qiyi.pluginlibrary.plugin.InterfaceToGetHost;
import org.qiyi.pluginlibrary.pm.PluginPackageInfo;
import org.qiyi.pluginlibrary.pm.PluginPackageManagerNative;
import org.qiyi.pluginlibrary.pm.PluginPackageManagerService;
import org.qiyi.pluginlibrary.runtime.NotifyCenter;
import org.qiyi.pluginlibrary.runtime.PluginLoadedApk;
import org.qiyi.pluginlibrary.runtime.PluginManager;
import org.qiyi.pluginlibrary.utils.ComponetFinder;
import org.qiyi.pluginlibrary.utils.ErrorUtil;
import org.qiyi.pluginlibrary.utils.IntentUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.ReflectionUtils;
import org.qiyi.pluginlibrary.utils.ResourcesToolForPlugin;
import org.qiyi.pluginlibrary.utils.Util;
import org.qiyi.pluginlibrary.utils.VersionUtils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * :plugin1 Activity　代理
 */
public class InstrActivityProxy1 extends Activity implements InterfaceToGetHost {
    private static final String TAG = InstrActivityProxy1.class.getSimpleName();

    /**
     * 保留 savedInstanceState，在真正恢复的时候模拟恢复
     *
     * 已知问题：如果 savedInstanceState 包含自定义 class 会出错，考虑到代理方案可能会下线，暂不修复
     */
    private static Bundle sPendingSavedInstanceState;
    private PluginLoadedApk mLoadedApk;
    private PluginActivityControl mPluginContrl;
    private PluginContextWrapper mPluginContextWrapper;
    private String mPluginPackage = "";
    private volatile boolean mRestartCalled = false;
    private BroadcastReceiver mFinishSelfReceiver;
    /**
     * 等待 PluginPackageManagerService 连接成功后启动插件
     */
    private BroadcastReceiver mLaunchPluginReceiver;

    private void initUiForRecovery() {
        BaseRecoveryActivity.IRecoveryUiCreator recoveryUiCreator = HybirdPlugin.getConfig().getRecoveryUiCreator();
        if (recoveryUiCreator != null) {
            setContentView(recoveryUiCreator.createContentView(this));
        } else {
            int size = (int) (50 * getResources().getDisplayMetrics().density + 0.5f);
            ProgressBar progressBar = new ProgressBar(this);
            FrameLayout frameLayout = new FrameLayout(this);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size, Gravity.CENTER);
            frameLayout.addView(progressBar, lp);
            setContentView(frameLayout);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 onCreate....");
        String pluginActivityName = null;
        String pluginPkgName = null;
        final String[] pkgAndCls = parsePkgAndClsFromIntent();
        if (pkgAndCls != null) {
            pluginPkgName = pkgAndCls[0];
            pluginActivityName = pkgAndCls[1];
        } else {
            PluginManager.deliver(this, false, pluginPkgName,
                    ErrorType.ERROR_CLIENT_GET_PKG_AND_CLS_FAIL);
            PluginDebugLog.log(TAG, "Pkg or activity is null in LActivityProxy, just return!");
            this.finish();
            return;
        }

        if (!tryToInitPluginLoadApk(pluginPkgName)) {
            boolean ppmsReady = PluginPackageManagerNative.getInstance(this).isConnected();
            if (ppmsReady) {
                this.finish();
                PluginDebugLog.log(TAG, "mPluginEnv is null in LActivityProxy, just return!");
            } else {
                initUiForRecovery();
                sPendingSavedInstanceState = savedInstanceState;

                mLaunchPluginReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Serializable serviceClass = intent.getSerializableExtra(IIntentConstant.EXTRA_SERVICE_CLASS);
                        if (PluginPackageManagerService.class.equals(serviceClass)) {
                            Intent pluginIntent = new Intent(getIntent());
                            pluginIntent.setComponent(new ComponentName(pkgAndCls[0], pkgAndCls[1]));
                            PluginManager.launchPlugin(context, pluginIntent, Util.getCurrentProcessName(InstrActivityProxy1.this));
                        }
                    }
                };
                registerReceiver(mLaunchPluginReceiver, new IntentFilter(IIntentConstant.ACTION_SERVICE_CONNECTED));

                mFinishSelfReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        InstrActivityProxy1.this.finish();
                    }
                };
                IntentFilter filter = new IntentFilter();
                filter.addAction(IIntentConstant.ACTION_START_PLUGIN_ERROR);
                filter.addAction(IIntentConstant.ACTION_PLUGIN_LOADED);
                registerReceiver(mFinishSelfReceiver, filter);
            }
            return;
        }
        if (!PluginManager.isPluginLoadedAndInit(pluginPkgName)) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(pluginPkgName,
                    IIntentConstant.EXTRA_VALUE_LOADTARGET_STUB));
            PluginManager.readyToStartSpecifyPlugin(this, null, intent, true);
        }

        NotifyCenter.notifyPluginStarted(this, getIntent());
        Activity mPluginActivity = loadPluginActivity(mLoadedApk, pluginActivityName);
        if (null == mPluginActivity) {
            PluginManager.deliver(this, false, pluginPkgName,
                    ErrorType.ERROR_CLIENT_FILL_PLUGIN_ACTIVITY_FAIL);
            PluginDebugLog.log(TAG, "Cann't get pluginActivityName class finish!");
            this.finish();
            return;
        }
        try {
            mPluginContrl = new PluginActivityControl(InstrActivityProxy1.this, mPluginActivity,
                    mLoadedApk.getPluginApplication(), mLoadedApk.getPluginInstrument());
        } catch (Exception e1) {
            PluginManager.deliver(this, false, pluginPkgName,
                    ErrorType.ERROR_CLIENT_CREATE_PLUGIN_ACTIVITY_CONTROL_FAIL);
            e1.printStackTrace();
            this.finish();
            return;
        }
        if (null != mPluginContrl) {
            mPluginContextWrapper = new PluginContextWrapper(InstrActivityProxy1.this.getBaseContext(), pluginPkgName);
            ActivityInfo actInfo = mLoadedApk.getActivityInfoByClassName(pluginActivityName);
            if (actInfo != null) {
                changeActivityInfo(this, pluginPkgName, actInfo);
            }
            mPluginContrl.dispatchProxyToPlugin(mLoadedApk.getPluginInstrument(), mPluginContextWrapper, pluginPkgName);
            int resTheme = mLoadedApk.getActivityThemeResourceByClassName(pluginActivityName);
            setTheme(resTheme);
            // Set plugin's default theme.
            mPluginActivity.setTheme(resTheme);

            try {
                callProxyOnCreate(savedInstanceState);
            } catch (Exception e) {
                ErrorUtil.throwErrorIfNeed(e);
                PluginManager.deliver(this, false, pluginPkgName,
                        ErrorType.ERROR_CLIENT_CALL_ON_CREATE_FAIL);
                this.finish();
                return;
            }
            mRestartCalled = false;  // onCreate()-->onStart()
        }
    }

    /**
     * 调用被代理的Activity的onCreate方法
     *
     * @param savedInstanceState
     */
    private void callProxyOnCreate(Bundle savedInstanceState) {
        boolean mockRestoreInstanceState = false;
        // 使用上一次的 savedInstance 进行恢复
        if (sPendingSavedInstanceState != null && savedInstanceState == null) {
            savedInstanceState = sPendingSavedInstanceState;
            savedInstanceState.setClassLoader(mLoadedApk.getPluginClassLoader());
            sPendingSavedInstanceState = null;
            mockRestoreInstanceState = true;
        }
        if (getParent() == null) {
            mLoadedApk.getActivityStackSupervisor().pushActivityToStack(this);
        }
        mPluginContrl.callOnCreate(savedInstanceState);

        // 模拟 onRestoreInstanceState
        if (mockRestoreInstanceState) {
            onRestoreInstanceState(savedInstanceState);
        }

        mPluginContrl.getPluginRef().set("mDecor", this.getWindow().getDecorView());
        PluginManager.dispatchPluginActivityCreated(mPluginPackage, mPluginContrl.getPlugin(), savedInstanceState);
        NotifyCenter.notifyPluginActivityLoaded(this.getBaseContext());
    }


    /**
     * 装载被代理的Activity
     *
     * @param mLoadedApk   插件的实例对象
     * @param activityName 需要被代理的Activity 类名
     * @return 成功则返回插件中被代理的Activity对象
     */
    private Activity loadPluginActivity(PluginLoadedApk mLoadedApk, String activityName) {
        try {
            Activity mActivity = (Activity) mLoadedApk.getPluginClassLoader()
                    .loadClass(activityName).newInstance();
            return mActivity;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 从Intent里面解析被代理的Activity的包名和组件名
     *
     * @return 成功则返回一个长度为2的String[], String[0]表示包名，String[1]表示类名
     * 失败则返回null
     */
    private String[] parsePkgAndClsFromIntent() {
        Intent mIntent = getIntent();
        if (mIntent == null) {
            return null;
        }

        //从action里面拿到pkg,并全局保存，然后还原action
        if (TextUtils.isEmpty(mPluginPackage)) {
            mPluginPackage = IntentUtils.getPluginPackage(mIntent);
        }
        IntentUtils.resetAction(mIntent);

        if (!TextUtils.isEmpty(mPluginPackage)) {
            if (mLoadedApk == null) {
                mLoadedApk = PluginManager.getPluginLoadedApkByPkgName(mPluginPackage);
            }
            if (mLoadedApk != null) {
                //解决插件中跳转自定义Bean对象失败的问题
                mIntent.setExtrasClassLoader(mLoadedApk.getPluginClassLoader());
            }
        }

        String[] result = new String[2];
        result[0] = IntentUtils.getTargetPackage(mIntent);
        result[1] = IntentUtils.getTargetClass(mIntent);
        if (!TextUtils.isEmpty(result[0]) && !TextUtils.isEmpty(result[1])) {
            PluginDebugLog.runtimeFormatLog(TAG, "pluginPkg:%s, pluginCls:%s", result[0], result[1]);
            return result;
        }
        return null;
    }


    /**
     * 尝试初始化PluginLoadedApk
     *
     * @param mPluginPackage
     * @return
     */
    private boolean tryToInitPluginLoadApk(String mPluginPackage) {
        if (!TextUtils.isEmpty(mPluginPackage) && null == mLoadedApk) {
            mLoadedApk = PluginManager.getPluginLoadedApkByPkgName(mPluginPackage);
        }
        if (null != mLoadedApk) {
            return true;
        }
        return false;
    }

    private boolean mNeedUpdateConfiguration = true;


    public PluginActivityControl getController() {
        return mPluginContrl;
    }

    @Override
    public Resources getResources() {
        if (mLoadedApk == null) {
            return super.getResources();
        }
        Resources mPluginResource = mLoadedApk.getPluginResource();
        return mPluginResource == null ? super.getResources()
                : mPluginResource;
    }

    @Override
    public void setTheme(int resid) {
        if (VersionUtils.hasNougat()) {
            String[] temp = parsePkgAndClsFromIntent();
            if (mNeedUpdateConfiguration && (temp != null || mLoadedApk != null)) {
                if (null != temp) {
                    tryToInitPluginLoadApk(temp[0]);
                }
                if (mLoadedApk != null && temp != null) {
                    ActivityInfo actInfo = mLoadedApk.getActivityInfoByClassName(temp[1]);
                    if (actInfo != null) {
                        int resTheme = actInfo.getThemeResource();
                        if (mNeedUpdateConfiguration) {
                            changeActivityInfo(InstrActivityProxy1.this, temp[0], actInfo);
                            super.setTheme(resTheme);
                            mNeedUpdateConfiguration = false;
                            return;
                        }
                    }
                }
            }
            super.setTheme(resid);
        } else {
            getTheme().applyStyle(resid, true);
        }
    }

    @Override
    public Resources.Theme getTheme() {
        if (mLoadedApk == null) {
            String[] temp = parsePkgAndClsFromIntent();
            if (null != temp) {
                tryToInitPluginLoadApk(temp[0]);
            }
        }
        return super.getTheme();
    }


    @Override
    public AssetManager getAssets() {
        if (mLoadedApk == null) {
            return super.getAssets();
        }
        AssetManager mPluginAssetManager = mLoadedApk.getPluginAssetManager();
        return mPluginAssetManager == null ? super.getAssets()
                : mPluginAssetManager;
    }

    @Override
    public File getFilesDir() {
        return mPluginContextWrapper.getFilesDir();
    }

    @Override
    public File getCacheDir() {
        return mPluginContextWrapper.getCacheDir();
    }

    @Override
    public File getExternalFilesDir(String type) {
        return mPluginContextWrapper.getExternalFilesDir(type);
    }

    @Override
    public File getExternalCacheDir() {
        return mPluginContextWrapper.getExternalCacheDir();
    }

    @Override
    public File getFileStreamPath(String name) {
        return mPluginContextWrapper.getFileStreamPath(name);
    }

    @Override
    public File getDir(String name, int mode) {
        return mPluginContextWrapper.getDir(name, mode);

    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        return mPluginContextWrapper.openFileInput(name);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return mPluginContextWrapper.openFileOutput(name, mode);
    }

    @Override
    public File getDatabasePath(String name) {
        return mPluginContextWrapper.getDatabasePath(name);
    }

    @Override
    public boolean deleteFile(String name) {
        return mPluginContextWrapper.deleteFile(name);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {

        return mPluginContextWrapper.openOrCreateDatabase(name, mode, factory);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory,
                                               DatabaseErrorHandler errorHandler) {
        return mPluginContextWrapper.openOrCreateDatabase(name, mode, factory, errorHandler);
    }

    @Override
    public boolean deleteDatabase(String name) {

        return mPluginContextWrapper.deleteDatabase(name);
    }

    @Override
    public String[] databaseList() {
        return mPluginContextWrapper.databaseList();
    }

    @Override
    public ClassLoader getClassLoader() {
        if (mLoadedApk == null) {
            return super.getClassLoader();
        }
        return mLoadedApk.getPluginClassLoader();
    }

    @Override
    public Context getApplicationContext() {
        if (null != mLoadedApk && null != mLoadedApk.getPluginApplication()) {
            return mLoadedApk.getPluginApplication();
        }
        return super.getApplicationContext();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 onResume....");
        if (getController() != null) {
            try {
                getController().callOnResume();
                PluginManager.dispatchPluginActivityResumed(mPluginPackage, mPluginContrl.getPlugin());
                IResourchStaticsticsControllerManager.onResume(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 onStart...., mRestartCalled: " + mRestartCalled);
        if (mRestartCalled) {
            // onStop()-->onRestart()-->onStart()，避免回调插件Activity#onStart方法两次
            mRestartCalled = false;
            return;
        }

        if (getController() != null) {
            try {
                getController().callOnStart();
                PluginManager.dispatchPluginActivityStarted(mPluginPackage, mPluginContrl.getPlugin());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 onPostCreate....");
        if (getController() != null) {
            try {
                getController().callOnPostCreate(savedInstanceState);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 onDestroy....");
        if (null == this.getParent() && mLoadedApk != null) {
            mLoadedApk.getActivityStackSupervisor().popActivityFromStack(this);
        }
        if (getController() != null) {

            try {
                getController().callOnDestroy();
                PluginManager.dispatchPluginActivityDestroyed(mPluginPackage, mPluginContrl.getPlugin());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mLaunchPluginReceiver != null) {
            unregisterReceiver(mLaunchPluginReceiver);
        }
        if (mFinishSelfReceiver != null) {
            unregisterReceiver(mFinishSelfReceiver);
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 onPause....");
        if (getController() != null) {

            try {
                getController().callOnPause();
                PluginManager.dispatchPluginActivityPaused(mPluginPackage, mPluginContrl.getPlugin());
                IResourchStaticsticsControllerManager.onPause(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 onBackPressed....");
        if (getController() != null) {
            try {
                getController().callOnBackPressed();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 onStop....");
        if (getController() != null) {
            try {
                getController().callOnStop();
                PluginManager.dispatchPluginActivityStopped(mPluginPackage, mPluginContrl.getPlugin());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 onRestart....");
        if (getController() != null) {
            try {
                getController().callOnRestart();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mRestartCalled = true;  //标记onRestart()被回调
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (getController() != null) {
            return getController().callOnKeyDown(keyCode, event);
        }

        return super.onKeyDown(keyCode, event);
    }


    public void startActivityForResult(Intent intent, int requestCode) {
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy startActivityForResult one....");
        if (mLoadedApk != null) {
            super.startActivityForResult(
                    ComponetFinder.switchToActivityProxy(mLoadedApk.getPluginPackageName(),
                            intent, requestCode, this),
                    requestCode);
        } else {
            super.startActivityForResult(intent, requestCode);
        }
    }

    @SuppressLint("NewApi")
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy startActivityForResult two....");
        if (mLoadedApk != null) {
            super.startActivityForResult(
                    ComponetFinder.switchToActivityProxy(mLoadedApk.getPluginPackageName(),
                            intent, requestCode, this),
                    requestCode, options);
        } else {
            super.startActivityForResult(intent, requestCode, options);
        }
    }

    @Override
    public ComponentName startService(Intent mIntent) {
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 startService....");
        if (mLoadedApk != null) {
            ComponetFinder.switchToServiceProxy(mLoadedApk, mIntent);
        }
        return super.startService(mIntent);
    }

    @Override
    public boolean stopService(Intent name) {
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 stopService....");
        if (mLoadedApk != null) {
            String mTargetServiceName = null;
            if (name.getComponent() != null) {
                mTargetServiceName = name.getComponent().getClassName();

            } else {
                PluginPackageInfo mInfo = mLoadedApk.getPluginPackageInfo();
                ServiceInfo mServiceInfo = mInfo.resolveService(name);
                if (mServiceInfo != null) {
                    mTargetServiceName = mServiceInfo.name;
                }
            }
            if (!TextUtils.isEmpty(mTargetServiceName)) {
                PluginServiceWrapper plugin = PServiceSupervisor.getServiceByIdentifer(
                        PluginServiceWrapper.getIndeitfy(mLoadedApk.getPluginPackageName(),
                                mTargetServiceName));
                if (plugin != null) {
                    plugin.updateServiceState(PluginServiceWrapper.PLUGIN_SERVICE_STOPED);
                    plugin.tryToDestroyService();
                    return true;
                }
            }

        }
        return super.stopService(name);
    }

    @Override
    public boolean bindService(Intent mIntent, ServiceConnection conn, int flags) {
        if (mLoadedApk != null) {
            ComponetFinder.switchToServiceProxy(mLoadedApk, mIntent);
        }
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 bindService...." + mIntent);
        return super.bindService(mIntent, conn, flags);
    }


    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return mPluginContextWrapper.getSharedPreferences(name, mode);
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        if (getController() != null) {
            getController().callDump(prefix, fd, writer, args);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mNeedUpdateConfiguration = true;
        if (getController() != null) {
            getController().callOnConfigurationChanged(newConfig);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (getController() != null) {
            getController().callOnPostResume();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        if (getController() != null) {
            getController().callOnDetachedFromWindow();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 onCreateView1:" + name);
        if (getController() != null) {
            return getController().callOnCreateView(name, context, attrs);
        }
        return super.onCreateView(name, context, attrs);
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 onCreateView2:" + name);
        if (getController() != null) {
            return getController().callOnCreateView(parent, name, context, attrs);
        }
        return super.onCreateView(parent, name, context, attrs);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 onNewIntent");
        if (getController() != null) {
            getController().callOnNewIntent(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 onActivityResult");
        if (getController() != null) {
            getController().getPluginRef().call("onActivityResult", PluginActivityControl.sMethods, requestCode, resultCode, data);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 onAttachFragment");
        if (getController() != null && getController().getPlugin() != null) {
            getController().getPlugin().onAttachFragment(fragment);
        }
    }

    @Override
    public View onCreatePanelView(int featureId) {

        if (getController() != null && getController().getPlugin() != null) {
            return getController().getPlugin().onCreatePanelView(featureId);
        } else {
            return super.onCreatePanelView(featureId);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        if (getController() != null && getController().getPlugin() != null) {
            getController().getPlugin().onOptionsMenuClosed(menu);
        }
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        super.onPanelClosed(featureId, menu);
        if (getController() != null && getController().getPlugin() != null) {
            getController().getPlugin().onPanelClosed(featureId, menu);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (getController() != null && getController().getPlugin() != null) {
            return getController().getPlugin().onKeyUp(keyCode, event);
        } else {
            return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public void onAttachedToWindow() {

        super.onAttachedToWindow();
        if (getController() != null && getController().getPlugin() != null) {
            getController().getPlugin().onAttachedToWindow();
        }
    }

    @Override
    public CharSequence onCreateDescription() {
        if (getController() != null && getController().getPlugin() != null) {
            return getController().getPlugin().onCreateDescription();
        } else {
            return super.onCreateDescription();
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (getController() != null && getController().getPlugin() != null) {
            return getController().getPlugin().onGenericMotionEvent(event);
        } else {
            return super.onGenericMotionEvent(event);
        }
    }

    @Override
    public void onContentChanged() {

        super.onContentChanged();
        if (getController() != null && getController().getPlugin() != null) {
            getController().getPlugin().onContentChanged();
        }
    }

    @Override
    public boolean onCreateThumbnail(Bitmap outBitmap, Canvas canvas) {
        if (getController() != null && getController().getPlugin() != null) {
            return getController().getPlugin().onCreateThumbnail(outBitmap, canvas);
        } else {
            return super.onCreateThumbnail(outBitmap, canvas);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 onRestoreInstanceState");
        if (getController() != null) {
            getController().callOnRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        PluginDebugLog.runtimeLog(TAG, "InstrActivityProxy1 onSaveInstanceState");
        if (getController() != null) {
            getController().callOnSaveInstanceState(outState);
            PluginManager.dispatchPluginActivitySaveInstanceState(mPluginPackage, mPluginContrl.getPlugin(), outState);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (getController() != null) {
            ReflectionUtils pluginRef = getController().getPluginRef();
            if (pluginRef != null) {
                // 6.0.1用mHasCurrentPermissionsRequest保证分发permission
                // result完成，根据现有
                // 的逻辑，第一次权限请求onRequestPermissionsResult一直是true，会影响之后的申请权限的
                // dialog弹出
                try {
                    pluginRef.set("mHasCurrentPermissionsRequest", false);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                pluginRef.call("onRequestPermissionsResult", PluginActivityControl.sMethods, requestCode, permissions, grantResults);
            }
        }
    }

    public void onStateNotSaved() {
        super.onStateNotSaved();
        if (getController() != null) {
            getController().getPluginRef().call("onStateNotSaved", PluginActivityControl.sMethods);
        }
    }

    public boolean onSearchRequested(SearchEvent searchEvent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getController() != null) {
                return getController().getPlugin().onSearchRequested(searchEvent);
            }
            return super.onSearchRequested(searchEvent);
        } else {
            return false;
        }
    }

    public boolean onSearchRequested() {
        if (getController() != null) {
            getController().getPlugin().onSearchRequested();
        }
        return super.onSearchRequested();
    }

    public void onProvideAssistContent(AssistContent outContent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            super.onProvideAssistContent(outContent);
            if (getController() != null) {
                getController().getPlugin().onProvideAssistContent(outContent);
            }
        }
    }

    /**
     * Get the context which start this plugin
     *
     * @return
     */
    @Override
    public Context getOriginalContext() {
        if (null != mLoadedApk) {
            return mLoadedApk.getHostContext();
        }
        return null;
    }

    /**
     * Get host resource
     *
     * @return host resource tool
     */
    @Override
    public ResourcesToolForPlugin getHostResourceTool() {
        if (null != mLoadedApk) {
            return mLoadedApk.getHostResourceTool();
        }
        return null;
    }

    @Override
    public void exitApp() {
        if (null != mLoadedApk) {
            mLoadedApk.quitApp(true);
        }
    }

    @Override
    public String getPluginPackageName() {
        if (null != mLoadedApk) {
            return mLoadedApk.getPluginPackageName();
        }
        return this.getPackageName();
    }

    public String dump() {
        String[] pkgCls = parsePkgAndClsFromIntent();
        if (null != pkgCls && pkgCls.length == 2) {
            return "Package&Cls is: " + this + " " + (pkgCls[0] + " " + pkgCls[1]) + " flg=0x"
                    + Integer.toHexString(getIntent().getFlags());
        } else {
            return "Package&Cls is: " + this + " flg=0x" + Integer.toHexString(getIntent().getFlags());
        }
    }

    public void dump(PrintWriter printWriter) {
        String[] pkgCls = parsePkgAndClsFromIntent();
        if (null != pkgCls && pkgCls.length == 2) {
            printWriter.print("Package&Cls is: " + this + " " + (pkgCls[0] + " " + pkgCls[1]) + " flg=0x"
                    + Integer.toHexString(getIntent().getFlags()));
            ;
        } else {
            printWriter.print("Package&Cls is: " + this + " flg=0x" + Integer.toHexString(getIntent().getFlags()));
        }
    }


    @Override
    public ApplicationInfo getApplicationInfo() {
        if (mPluginContextWrapper != null) {
            return mPluginContextWrapper.getApplicationInfo();
        }
        return super.getApplicationInfo();
    }

    @Override
    public String getPackageCodePath() {
        if (mPluginContextWrapper != null) {
            return mPluginContextWrapper.getPackageCodePath();
        }
        return super.getPackageCodePath();
    }

    private void changeActivityInfo(Activity activity, String pkgName, ActivityInfo mActivityInfo) {
        ActivityInfo origActInfo = null;
        try {
            Field field_mActivityInfo = Activity.class.getDeclaredField("mActivityInfo");
            field_mActivityInfo.setAccessible(true);
            origActInfo = (ActivityInfo) field_mActivityInfo.get(activity);
        } catch (Exception e) {
            PluginManager.deliver(activity, false, pkgName, ErrorType.ERROR_CLIENT_CHANGE_ACTIVITYINFO_FAIL);
            PluginDebugLog.log(TAG, e.getStackTrace());
            return;
        }
        PluginLoadedApk mLoadedApk = PluginManager.getPluginLoadedApkByPkgName(pkgName);


        if (null != mActivityInfo) {
            if (null != mLoadedApk && null != mLoadedApk.getPluginPackageInfo()) {
                mActivityInfo.applicationInfo = mLoadedApk.getPluginPackageInfo().getPackageInfo().applicationInfo;
            }
            if (origActInfo != null) {
                origActInfo.applicationInfo = mActivityInfo.applicationInfo;
                origActInfo.configChanges = mActivityInfo.configChanges;
                origActInfo.descriptionRes = mActivityInfo.descriptionRes;
                origActInfo.enabled = mActivityInfo.enabled;
                origActInfo.exported = mActivityInfo.exported;
                origActInfo.flags = mActivityInfo.flags;
                origActInfo.icon = mActivityInfo.icon;
                origActInfo.labelRes = mActivityInfo.labelRes;
                origActInfo.logo = mActivityInfo.logo;
                origActInfo.metaData = mActivityInfo.metaData;
                origActInfo.name = mActivityInfo.name;
                origActInfo.nonLocalizedLabel = mActivityInfo.nonLocalizedLabel;
                origActInfo.packageName = mActivityInfo.packageName;
                origActInfo.permission = mActivityInfo.permission;
                origActInfo.screenOrientation = mActivityInfo.screenOrientation;
                origActInfo.softInputMode = mActivityInfo.softInputMode;
                origActInfo.targetActivity = mActivityInfo.targetActivity;
                origActInfo.taskAffinity = mActivityInfo.taskAffinity;
                origActInfo.theme = mActivityInfo.theme;
            }
        }
        // Handle ActionBar title
        if (null != origActInfo) {
            if (origActInfo.nonLocalizedLabel != null) {
                activity.setTitle(origActInfo.nonLocalizedLabel);
            } else if (origActInfo.labelRes != 0) {
                activity.setTitle(origActInfo.labelRes);
            } else {
                if (origActInfo.applicationInfo != null) {
                    if (origActInfo.applicationInfo.nonLocalizedLabel != null) {
                        activity.setTitle(origActInfo.applicationInfo.nonLocalizedLabel);
                    } else if (origActInfo.applicationInfo.labelRes != 0) {
                        activity.setTitle(origActInfo.applicationInfo.labelRes);
                    } else {
                        activity.setTitle(origActInfo.applicationInfo.packageName);
                    }
                }
            }
        }
        if (null != mActivityInfo) {
            try {
                getWindow().setSoftInputMode(mActivityInfo.softInputMode);
            } catch (Exception e) {
                e.printStackTrace();
            }

            PluginDebugLog.log(TAG, "changeActivityInfo->changeTheme: " + " theme = " +
                    mActivityInfo.getThemeResource() + ", icon = " + mActivityInfo.getIconResource()
                    + ", logo = " + mActivityInfo.logo + ", labelRes" + mActivityInfo.labelRes);
        }
    }
}
