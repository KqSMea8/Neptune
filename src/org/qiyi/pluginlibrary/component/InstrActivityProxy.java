package org.qiyi.pluginlibrary.component;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.assist.AssistContent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;

import org.qiyi.pluginlibrary.ActivityJumpUtil;
import org.qiyi.pluginlibrary.ActivityOverrider;
import org.qiyi.pluginlibrary.ErrorType.ErrorType;
import org.qiyi.pluginlibrary.PServiceSupervisor;
import org.qiyi.pluginlibrary.PluginActivityControl;
import org.qiyi.pluginlibrary.PluginServiceWrapper;
import org.qiyi.pluginlibrary.ServiceJumpUtil;
import org.qiyi.pluginlibrary.context.CMContextWrapperNew;
import org.qiyi.pluginlibrary.listenter.IResourchStaticsticsControllerManager;
import org.qiyi.pluginlibrary.manager.ProxyEnvironment;
import org.qiyi.pluginlibrary.manager.ProxyEnvironmentManager;
import org.qiyi.pluginlibrary.plugin.InterfaceToGetHost;
import org.qiyi.pluginlibrary.utils.ContextUtils;
import org.qiyi.pluginlibrary.utils.IntentUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.ReflectionUtils;
import org.qiyi.pluginlibrary.utils.ResourcesToolForPlugin;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class InstrActivityProxy extends Activity implements InterfaceToGetHost {

    private static final String TAG = InstrActivityProxy.class.getSimpleName();

    private ProxyEnvironment mPluginEnv;
    private PluginActivityControl mPluginContrl;
    CMContextWrapperNew mPluginContextWrapper;
    private String mProxyPkg = "";

    /**
     * 装载插件的Activity
     *
     * @param actClsName
     */
    private Activity fillPluginActivity(ProxyEnvironment env, String actClsName) {
        try {
            Activity myPlugin = (Activity) env.getDexClassLoader().loadClass(actClsName).newInstance();
            return myPlugin;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String[] getPkgAndCls() {
        Intent mIntent = getIntent();
        if (null == mIntent) {
            return null;
        }

        //从action里面拿到pkg,并全局保存，然后还原action
        if(TextUtils.isEmpty(mProxyPkg)){
            mProxyPkg = IntentUtils.getPluginPackage(mIntent);
        }
        IntentUtils.resetAction(mIntent);

        if(!TextUtils.isEmpty(mProxyPkg)){
            if(mPluginEnv == null){
                mPluginEnv = ProxyEnvironmentManager.getEnvByPkgName(mProxyPkg);
            }
            if(mPluginEnv!=null){
                mIntent.setExtrasClassLoader(mPluginEnv.getDexClassLoader());
            }
        }
        final Bundle pluginMessage = mIntent.getExtras();
        String[] result = new String[2];
        if (null != pluginMessage) {
            result[1] = pluginMessage.getString(ActivityJumpUtil.EXTRA_TARGET_ACTIVITY);
            result[0] = pluginMessage.getString(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME);
            if (!TextUtils.isEmpty(result[0]) && !TextUtils.isEmpty(result[1])) {
                PluginDebugLog.runtimeFormatLog(TAG,"pluginPkg:%s,pluginCls:%s",result[0],result[1]);
                return result;
            }
        }
        return null;
    }

    /**
     * Try to init proxy environment
     *
     * @param pkgName
     */
    private boolean tryToInitEnvironment(String pkgName) {
        if (!TextUtils.isEmpty(pkgName) && null == mPluginEnv) {
            // in multiple process environment plugin doesn't know which process
            // to
            // launch environment, so one plugin cann't start other plugin.
            // if (!ProxyEnvironmentNew.hasInstance(pkgName)) {
            // String installMethod = "";
            // CMPackageInfo pkgInfo =
            // CMPackageManagerImpl.getInstance(this).getPackageInfo(pkgName);
            // if (null != pkgInfo && pkgInfo.pluginInfo != null) {
            // installMethod = pkgInfo.pluginInfo.mPluginInstallMethod;
            // } else {
            // ProxyEnvironmentNew.deliverPlug(false, pkgName,
            // ErrorType.ERROR_CLIENT_TRY_TO_INIT_ENVIRONMENT_FAIL);
            // Log.e(TAG, "Cann't get pkginfo for: " + pkgName);
            // return false;
            // }
            // PluginDebugLog.log("plugin", "doInBackground:" + pkgName + ",
            // installMethod: "
            // + installMethod);
            // ProxyEnvironmentNew.initProxyEnvironment(InstrActivityProxy.this,
            // pkgName, installMethod);
            // }
            mPluginEnv = ProxyEnvironmentManager.getEnvByPkgName(pkgName);
        }
        if (null != mPluginEnv) {
            return true;
        }
        return false;
    }

    private boolean mNeedUpdateConfiguration = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy onCreate....");
        String pluginActivityName = null;
        String pluginPkgName = null;
        String[] pkgAndCls = getPkgAndCls();
        if (pkgAndCls != null) {
            pluginPkgName = pkgAndCls[0];
            pluginActivityName = pkgAndCls[1];
        } else {
            ProxyEnvironmentManager.deliverPlug(this, false, pluginPkgName,
                    ErrorType.ERROR_CLIENT_GET_PKG_AND_CLS_FAIL);
            PluginDebugLog.log(TAG, "Pkg or activity is null in LActivityProxy, just return!");
            this.finish();
            return;
            // throw new
            // PluginCreateFailedException("Please put the Plugin Path!");
        }

        if (!tryToInitEnvironment(pluginPkgName)) {
            this.finish();
            PluginDebugLog.log(TAG, "mPluginEnv is null in LActivityProxy, just return!");
            return;
        }
        if (!ProxyEnvironmentManager.isEnterProxy(pluginPkgName)) {
            Intent i = new Intent();
            i.setComponent(new ComponentName(pluginPkgName,
                    ProxyEnvironment.EXTRA_VALUE_LOADTARGET_STUB));
            ProxyEnvironment.launchIntent(InstrActivityProxy.this, null, i);
        }
        ContextUtils.notifyHostPluginStarted(this, getIntent());
        Activity plugin = fillPluginActivity(mPluginEnv, pluginActivityName);
        if (null == plugin) {
            ProxyEnvironmentManager.deliverPlug(this, false, pluginPkgName,
                    ErrorType.ERROR_CLIENT_FILL_PLUGIN_ACTIVITY_FAIL);
            PluginDebugLog.log(TAG, "Cann't get pluginActivityName class finish!");
            this.finish();
            return;
        }
        try {
            mPluginContrl = new PluginActivityControl(InstrActivityProxy.this, plugin,
                    mPluginEnv.getApplication(), mPluginEnv.mPluginInstrument);
        } catch (Exception e1) {
            ProxyEnvironmentManager.deliverPlug(this, false, pluginPkgName,
                    ErrorType.ERROR_CLIENT_CREATE_PLUGIN_ACTIVITY_CONTROL_FAIL);
            e1.printStackTrace();
            this.finish();
            return;
        }
        if (null != mPluginContrl) {
            mPluginContextWrapper = new CMContextWrapperNew(InstrActivityProxy.this.getBaseContext(), pluginPkgName);
            ActivityInfo actInfo = mPluginEnv.findActivityByClassName(pluginActivityName);
            if (actInfo != null) {
                ActivityOverrider.changeActivityInfo(this, pluginPkgName, pluginActivityName);
            }
            mPluginContrl.dispatchProxyToPlugin(mPluginEnv.mPluginInstrument, mPluginContextWrapper, pluginPkgName);
            int resTheme = mPluginEnv.getTargetActivityThemeResource(pluginActivityName);
            setTheme(resTheme);
            // Set plugin's default theme.
            plugin.setTheme(resTheme);
            try {
                if (getParent() == null) {
                    mPluginEnv.getActivityStackSupervisor().pushActivityToStack(this);
                }
                mPluginContrl.callOnCreate(savedInstanceState);
                mPluginContrl.getPluginRef().set("mDecor", this.getWindow().getDecorView());
                ProxyEnvironment.sendPluginLoadedBroadcast(InstrActivityProxy.this.getBaseContext());
            } catch (Exception e) {
                ProxyEnvironmentManager.deliverPlug(this, false, pluginPkgName,
                        ErrorType.ERROR_CLIENT_CALL_ON_CREATE_FAIL);
                processError(e);
                this.finish();
                return;
            }
        }
    }

    private void processError(Exception e) {
        e.printStackTrace();
    }

    public PluginActivityControl getController() {
        return mPluginContrl;
    }

    @Override
    public Resources getResources() {
        if (mPluginEnv == null) {
            return super.getResources();
        }
        return mPluginEnv.getTargetResources() == null ? super.getResources()
                : mPluginEnv.getTargetResources();
    }

    @Override
    public void setTheme(int resid) {
        if (ContextUtils.isAndroidN() || ContextUtils.isAndroidO()) {
            String[] temp = getPkgAndCls();
            if (mNeedUpdateConfiguration && (temp != null || mPluginEnv != null)) {
                tryToInitEnvironment(temp[0]);
                if (mPluginEnv != null) {
                    ActivityInfo actInfo = mPluginEnv.findActivityByClassName(temp[1]);
                    if (actInfo != null) {
                        int resTheme = actInfo.getThemeResource();
                        if (mNeedUpdateConfiguration) {
                            ActivityOverrider.changeActivityInfo(InstrActivityProxy.this, temp[0], temp[1]);
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
    public Theme getTheme() {
        if (mPluginEnv == null) {
            String[] temp = getPkgAndCls();
            if (null != temp) {
                tryToInitEnvironment(temp[0]);
            }
        }
        return super.getTheme();
    }

    /**
     * Override Oppo method in Context Resolve cann't start plugin on oppo
     * devices, true or false both OK, false as the temporary result
     *
     * @return
     */
    public boolean isOppoStyle() {
        return false;
    }

    @Override
    public AssetManager getAssets() {
        if (mPluginEnv == null) {
            return super.getAssets();
        }
        return mPluginEnv.getTargetAssetManager() == null ? super.getAssets()
                : mPluginEnv.getTargetAssetManager();
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
    public File getFileStreamPath(String name) {
        return mPluginContextWrapper.getFileStreamPath(name);
    }

    @Override
    public File getDir(String name, int mode) {
        return mPluginContextWrapper.getDir(name, mode);

    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        // TODO Auto-generated method stub
        return mPluginContextWrapper.openFileInput(name);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        // TODO Auto-generated method stub
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
        if (mPluginEnv == null) {
            return super.getClassLoader();
        }
        return mPluginEnv.getDexClassLoader();
    }

    @Override
    public Context getApplicationContext() {
        if (null != mPluginEnv && null != mPluginEnv.getApplication()) {
            return mPluginEnv.getApplication();
        }
        return super.getApplicationContext();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy onResume....");
        if (getController() != null) {
            try {
                getController().callOnResume();
                IResourchStaticsticsControllerManager.onResume(this);
            } catch (Exception e) {
                processError(e);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy onStart....");
        if (getController() != null) {
            try {
                getController().callOnStart();
            } catch (Exception e) {
                processError(e);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (getController() != null) {
            try {
                getController().callOnPostCreate(savedInstanceState);
            } catch (Exception e) {
                processError(e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy onDestroy....");
        if (null == this.getParent() && mPluginEnv != null) {
            mPluginEnv.getActivityStackSupervisor().popActivityFromStack(this);
        }
        if (getController() != null) {

            try {
                getController().callOnDestroy();
                // LCallbackManager.callAllOnDestroy();
            } catch (Exception e) {
                processError(e);
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy onPause....");
        if (getController() != null) {

            try {
                getController().callOnPause();
                IResourchStaticsticsControllerManager.onPause(this);
                // LCallbackManager.callAllOnPause();
            } catch (Exception e) {
                processError(e);
            }
        }
    }

    @Override
    public void onBackPressed() {
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy onBackPressed....");
        if (getController() != null) {
            try {
                getController().callOnBackPressed();
                // LCallbackManager.callAllOnBackPressed();
            } catch (Exception e) {
                processError(e);
            }

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy onStop....");
        if (getController() != null) {
            try {
                getController().callOnStop();
                // LCallbackManager.callAllOnStop();
            } catch (Exception e) {
                processError(e);
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy onRestart....");
        if (getController() != null) {
            try {
                getController().callOnRestart();
                // LCallbackManager.callAllOnRestart();
            } catch (Exception e) {
                processError(e);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (getController() != null) {
            // LCallbackManager.callAllOnKeyDown(keyCode, event);
            return getController().callOnKeyDown(keyCode, event);

        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public ComponentName startService(Intent service) {
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy startService....");
        if (mPluginEnv != null) {
            ServiceJumpUtil.remapStartServiceIntent(mPluginEnv, service);
        }
        return super.startService(service);
    }

    @Override
    public boolean stopService(Intent name) {
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy stopService....");
        if (mPluginEnv != null) {
            String actServiceClsName = name.getComponent().getClassName();
            PluginServiceWrapper plugin = PServiceSupervisor.getServiceByIdentifer(
                    PluginServiceWrapper.getIndeitfy(mPluginEnv.getTargetPackageName(), actServiceClsName));
            if (plugin != null) {
                plugin.updateStartStatus(PluginServiceWrapper.PLUGIN_SERVICE_STOPED);
                plugin.tryToDestroyService(name);
                return true;
            }
        }
        return super.stopService(name);
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        if (mPluginEnv != null) {
            ServiceJumpUtil.remapStartServiceIntent(mPluginEnv, service);
        }
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy bindService...."+service);
        return super.bindService(service, conn, flags);
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy startActivityForResult one....");
        if (mPluginEnv != null) {
            super.startActivityForResult(
                    ActivityJumpUtil.handleStartActivityIntent(mPluginEnv.getTargetPackageName(), intent, requestCode, null, this),
                    requestCode);
        } else {
            super.startActivityForResult(intent, requestCode);
        }
    }

    @SuppressLint("NewApi")
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy startActivityForResult two....");
        if (mPluginEnv != null) {
            super.startActivityForResult(
                    ActivityJumpUtil.handleStartActivityIntent(mPluginEnv.getTargetPackageName(), intent, requestCode, options, this),
                    requestCode, options);
        } else {
            super.startActivityForResult(intent, requestCode, options);
        }
    }

    // public void startActivityFromFragment(Fragment fragment, Intent intent,
    // int requestCode) {
    // // TODO Auto-generated method stub
    // super.startActivityFromFragment(fragment, intent, requestCode);
    // }
    //
    // @Override
    // public void startActivityFromFragment(Fragment fragment, Intent intent,
    // int requestCode,
    // Bundle options) {
    // // TODO Auto-generated method stub
    // super.startActivityFromFragment(fragment, intent, requestCode, options);
    // }

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
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy onCreateView");
        if (getController() != null) {
            return getController().callOnCreateView(name, context, attrs);
        }
        return super.onCreateView(name, context, attrs);
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy onCreateView");
        if (getController() != null) {
            return getController().callOnCreateView(parent, name, context, attrs);
        }
        return super.onCreateView(parent, name, context, attrs);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy onNewIntent");
        if (getController() != null) {
            getController().callOnNewIntent(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy onActivityResult");
        if (getController() != null) {
            getController().getPluginRef().call("onActivityResult", PluginActivityControl.sMethods, requestCode, resultCode, data);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy onAttachFragment");
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
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy onRestoreInstanceState");
        if (getController() != null) {
            getController().callOnRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        PluginDebugLog.runtimeLog(TAG,"InstrActivityProxy onSaveInstanceState");
        if (getController() != null) {
            getController().callOnSaveInstanceState(outState);
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
        if (null != mPluginEnv) {
            return mPluginEnv.getHostContext();
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
        if (null != mPluginEnv) {
            return mPluginEnv.getHostResourceTool();
        }
        return null;
    }

    @Override
    public void exitApp() {
        if (null != mPluginEnv) {
            mPluginEnv.quitApp(true);
        }
    }

    @Override
    public String getPluginPackageName() {
        if (null != mPluginEnv) {
            return mPluginEnv.getTargetPackageName();
        }
        return this.getPackageName();
    }

    public String dump() {
        String[] pkgCls = getPkgAndCls();
        if (null != pkgCls && pkgCls.length == 2) {
            return "Package&Cls is: " + this + " " + (pkgCls != null ? pkgCls[0] + " " + pkgCls[1] : "") + " flg=0x"
                    + Integer.toHexString(getIntent().getFlags());
        } else {
            return "Package&Cls is: " + this + " flg=0x" + Integer.toHexString(getIntent().getFlags());
        }
    }

    public void dump(PrintWriter printWriter){
        String[] pkgCls = getPkgAndCls();
        if (null != pkgCls && pkgCls.length == 2) {
            printWriter.print("Package&Cls is: " + this + " " + (pkgCls != null ? pkgCls[0] + " " + pkgCls[1] : "") + " flg=0x"
                    + Integer.toHexString(getIntent().getFlags())); ;
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
}
