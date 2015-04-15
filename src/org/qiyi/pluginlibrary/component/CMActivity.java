package org.qiyi.pluginlibrary.component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.qiyi.pluginlibrary.ProxyEnvironment;
import org.qiyi.pluginlibrary.adapter.ActivityProxyAdapter;
import org.qiyi.pluginlibrary.api.IPluginInvoke;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

/**
 * 虚拟的Activity类
 */
public class CMActivity extends CMContextWrapper implements LayoutInflater.Factory, KeyEvent.Callback,
        View.OnCreateContextMenuListener, ComponentCallbacks,IPluginInvoke {
    public static final int DEFAULT_KEYS_DIALER = 1;
    public static final int DEFAULT_KEYS_DISABLE = 0;
    public static final int DEFAULT_KEYS_SEARCH_GLOBAL = 4;
    public static final int DEFAULT_KEYS_SEARCH_LOCAL = 3;
    public static final int DEFAULT_KEYS_SHORTCUT = 2;
    public static final int RESULT_CANCELED = 0;
    public static final int RESULT_FIRST_USER = 1;
    public static final int RESULT_OK = -1;
    public Activity activity;
    private ActivityProxyAdapter proxyActivity;
    private boolean bOnCreateCalled = false;
    
    /** 宿主activity */
    protected Activity mParent;
    protected IPluginInvoke invoke;//与宿主通信
    
    /** 参考 {@link ActivityLifecycleCallbacks} */
    public static ActivityLifecycleCallbacks sActivityLifecycleCallbacks;
    
    /**
     * activity 生命周期回调。目前只有一个地方需要，就是 hostapp需要 activity的生命周期回调，来进行 activity task
     * 上的计数，用于判断是否还有activity存在。因为hostapp中的actiivty大都继承与同一个BaseActivity，我们的插件Activity
     * 则不是，对hostapp 依赖于这样的计数，无法解决。所以提供此回调。
     * 通过 {@link CMActivity#setActivityLifecycleCallbacks(ActivityLifecycleCallbacks)} 进行设置。
     */
    public interface ActivityLifecycleCallbacks {
        void onActivityCreated(Activity activity, Bundle savedInstanceState);
        void onActivityStarted(Activity activity);
        void onActivityResumed(Activity activity);
        void onActivityPaused(Activity activity);
        void onActivityStopped(Activity activity);
        void onActivitySaveInstanceState(Activity activity, Bundle outState);
        void onActivityDestroyed(Activity activity);
    }

    public CMActivity() {
        super(null);
    }
    
    // BEGIN: 自定义的接口

    /**
     * 获取真实Activity（代理）
     * 
     * @return ActivityProxy
     */
    public Activity getActivity() {
        return activity;
    }
    
    public Activity getHostActivity(){
    	if(ProxyEnvironment.mParent != null){
    		PluginDebugLog.log("plugin","ProxyEnvironment.mParent:"+ProxyEnvironment.mParent);
    		return ProxyEnvironment.mParent;
    	}
    	
    	return activity;
    }

    /**
     * 返回父Activity的CMActivity
     * 
     * @return CMActivity子类实例
     */
    public final Context getCMParent() {
        ActivityProxyAdapter adp = (ActivityProxyAdapter) activity.getParent();
        return adp.getTarget();
    }

    // END: 自定义的接口

    /** 参考 {@link ActivityLifecycleCallbacks} */
    public static void setActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        sActivityLifecycleCallbacks = callback;
    }

    public void addContentView(View paramView, ViewGroup.LayoutParams paramLayoutParams) {
        proxyActivity.proxyAddContentView(paramView, paramLayoutParams);
    }

    public boolean bindService(Intent paramIntent, ServiceConnection paramServiceConnection, int paramInt) {
        return proxyActivity.proxyBindService(paramIntent, paramServiceConnection, paramInt);
    }
    
    @Override
    public void unbindService(ServiceConnection conn) {
    	// TODO Auto-generated method stub
    	proxyActivity.proxyUnbindService(conn);
    }

    public void closeContextMenu() {
        proxyActivity.proxyCloseContextMenu();
    }

    public void closeOptionsMenu() {
        proxyActivity.proxyCloseOptionsMenu();
    }

    public PendingIntent createPendingResult(int paramInt1, Intent paramIntent, int paramInt2) {
        return proxyActivity.proxyCreatePendingResult(paramInt1, paramIntent, paramInt2);
    }

    public final void dismissDialog(int paramInt) {
        activity.dismissDialog(paramInt);
    }

    public boolean dispatchKeyEvent(KeyEvent paramKeyEvent) {
        return proxyActivity.proxyDispatchKeyEvent(paramKeyEvent);
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent paramAccessibilityEvent) {
        return proxyActivity.proxyDispatchPopulateAccessibilityEvent(paramAccessibilityEvent);
    }

    public boolean dispatchTouchEvent(MotionEvent paramMotionEvent) {
        return proxyActivity.proxyDispatchTouchEvent(paramMotionEvent);
    }

    public boolean dispatchTrackballEvent(MotionEvent paramMotionEvent) {
        return proxyActivity.proxyDispatchTrackballEvent(paramMotionEvent);
    }

    public View findViewById(int paramInt) {
        return proxyActivity.proxyFindViewById(paramInt);
    }

    public void finish() {
        proxyActivity.proxyFinish();
    }

    public void finishActivity(int paramInt) {
        proxyActivity.proxyFinishActivity(paramInt);
    }

    public void finishActivityFromChild(Activity paramActivity, int paramInt) {
        proxyActivity.proxyFinishActivityFromChild(paramActivity, paramInt);
    }

    public void finishFromChild(Activity paramActivity) {
        proxyActivity.proxyFinishFromChild(paramActivity);
    }

    public final Application getApplication() {
        return ProxyEnvironment.getInstance(getTargetPackageName()).getApplication();
    }

    public ComponentName getCallingActivity() {
        return proxyActivity.proxyGetCallingActivity();
    }

    public String getCallingPackage() {
        return proxyActivity.proxyGetCallingPackage();
    }

    public int getChangingConfigurations() {
        return proxyActivity.proxyGetChangingConfigurations();
    }

    public ComponentName getComponentName() {
        return activity.getComponentName();
    }

    public View getCurrentFocus() {
        return proxyActivity.proxyGetCurrentFocus();
    }

    public Intent getIntent() {
        return proxyActivity.proxyGetIntent();
    }

    public LayoutInflater getLayoutInflater() {
        return proxyActivity.proxyGetLayoutInflater();
    }

    public String getLocalClassName() {
        return proxyActivity.proxyGetLocalClassName();
    }

    public MenuInflater getMenuInflater() {
        return proxyActivity.proxyGetMenuInflater();
    }

    public String getPackageCodePath() {
        return ProxyEnvironment.getInstance(getTargetPackageName()).getTargetPath();
    }

    @Override
    public String getPackageName() {
        return activity.getPackageName();
    }

    public PackageManager getPackageManager() {
        return proxyActivity.proxyGetPackageManager();
    }

    public final Activity getParent() {
        return activity.getParent();
    }

    public SharedPreferences getPreferences(int paramInt) {
        return proxyActivity.proxyGetPreferences(paramInt);
    }

    public int getRequestedOrientation() {
        return proxyActivity.proxyGetRequestedOrientation();
    }

    public Object getSystemService(String paramString) {
        return proxyActivity.proxyGetSystemService(paramString);
    }

    public int getTaskId() {
        return proxyActivity.proxyGetTaskId();
    }

    public final CharSequence getTitle() {
        return activity.getTitle();
    }

    public final int getTitleColor() {
        return activity.getTitleColor();
    }

    public final int getVolumeControlStream() {
        return activity.getVolumeControlStream();
    }

    public int getWallpaperDesiredMinimumHeight() {
        return proxyActivity.proxyGetWallpaperDesiredMinimumHeight();
    }

    public int getWallpaperDesiredMinimumWidth() {
        return proxyActivity.proxyGetWallpaperDesiredMinimumWidth();
    }

    public Window getWindow() {
        return proxyActivity.proxyGetWindow();
    }

    public WindowManager getWindowManager() {
        return proxyActivity.proxyGetWindowManager();
    }

    public boolean hasWindowFocus() {
        return proxyActivity.proxyHasWindowFocus();
    }

    public final boolean isChild() {
        return activity.isChild();
    }

    public boolean isFinishing() {
        return proxyActivity.proxyIsFinishing();
    }

    public boolean isTaskRoot() {
        return proxyActivity.proxyIsTaskRoot();
    }

    @Deprecated
    public final Cursor managedQuery(Uri paramUri, String[] paramArrayOfString1, String paramString1,
            String[] paramArrayOfString2, String paramString2) {
        return activity.managedQuery(paramUri, paramArrayOfString1, paramString1, paramArrayOfString2, paramString2);
    }

    public boolean moveTaskToBack(boolean paramBoolean) {
        return proxyActivity.proxyMoveTaskToBack(paramBoolean);
    }

    protected void onActivityResult(int paramInt1, int paramInt2, Intent paramIntent) {
        proxyActivity.proxyOnActivityResult(paramInt1, paramInt2, paramIntent);
    }

    protected void onApplyThemeResource(Resources.Theme paramTheme, int paramInt, boolean paramBoolean) {
        proxyActivity.proxyOnApplyThemeResource(paramTheme, paramInt, paramBoolean);
    }

    public void onAttachedToWindow() {
        proxyActivity.proxyOnAttachedToWindow();
    }

    public void onBackPressed() {
        proxyActivity.proxyOnBackPressed();
    }

    protected void onChildTitleChanged(Activity paramActivity, CharSequence paramCharSequence) {
        proxyActivity.proxyOnChildTitleChanged(paramActivity, paramCharSequence);
    }

    public void onConfigurationChanged(Configuration paramConfiguration) {
        proxyActivity.proxyOnConfigurationChanged(paramConfiguration);
    }

    public void onContentChanged() {
        proxyActivity.proxyOnContentChanged();
    }

    public boolean onContextItemSelected(MenuItem paramMenuItem) {
        return proxyActivity.proxyOnContextItemSelected(paramMenuItem);
    }

    public void onContextMenuClosed(Menu paramMenu) {
        proxyActivity.proxyOnContextMenuClosed(paramMenu);
    }

    protected void onCreate(Bundle paramBundle) {
        if (activity.getParent() == null) { // 如果是子Activity，不进入栈管理
            ProxyEnvironment.getInstance(getTargetPackageName()).pushActivityToStack(activity);
        }
        bOnCreateCalled = true;
        
        // 设置屏幕方向
        int orientation = ProxyEnvironment.getInstance(getTargetPackageName()).getTargetActivityOrientation(
                this.getClass().getName());
        if (orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            setRequestedOrientation(orientation);
        }

        // 调用Activity 的 super.onCreate
        proxyActivity.proxyOnCreate(paramBundle);
        
        if (sActivityLifecycleCallbacks != null && !(this.getClass().equals(CMActivity.class))) {
            sActivityLifecycleCallbacks.onActivityCreated(activity, paramBundle);
        }
    }

    public void onCreateContextMenu(ContextMenu paramContextMenu, View paramView,
            ContextMenu.ContextMenuInfo paramContextMenuInfo) {
        proxyActivity.proxyOnCreateContextMenu(paramContextMenu, paramView, paramContextMenuInfo);
    }

    public CharSequence onCreateDescription() {
        return null;
    }

    protected Dialog onCreateDialog(int paramInt) {
        return null;
    }

    public boolean onCreatePanelMenu(int paramInt, Menu paramMenu) {
        return proxyActivity.proxyOnCreatePanelMenu(paramInt, paramMenu);
    }

    public View onCreatePanelView(int paramInt) {
        return null;
    }

    public boolean onCreateThumbnail(Bitmap paramBitmap, Canvas paramCanvas) {
        return proxyActivity.proxyOnCreateThumbnail(paramBitmap, paramCanvas);
    }

    public View onCreateView(String paramString, Context paramContext, AttributeSet paramAttributeSet) {
    	 return proxyActivity.proxyOnCreateView(paramString,paramContext,paramAttributeSet);
    }

    protected void onDestroy() {
        proxyActivity.proxyOnDestroy();

        // 排除掉 rootActiviti引用的MAActivity 实例
        if (this.getClass().equals(CMActivity.class)) {
            return;
        }

        if (activity.getParent() == null) { // 如果是子Activity，不进入栈管理
            ProxyEnvironment.getInstance(getTargetPackageName()).popActivityFromStack(activity);
        }

        if (sActivityLifecycleCallbacks != null) {
            sActivityLifecycleCallbacks.onActivityDestroyed(activity);
        }
    }

    public void onDetachedFromWindow() {
        proxyActivity.proxyOnDetachedFromWindow();
    }

    public boolean onKeyDown(int paramInt, KeyEvent paramKeyEvent) {
        return proxyActivity.proxyOnKeyDown(paramInt, paramKeyEvent);
    }

    public boolean onKeyLongPress(int paramInt, KeyEvent paramKeyEvent) {
        return proxyActivity.proxyOnKeyLongPress(paramInt, paramKeyEvent);
    }

    public boolean onKeyMultiple(int paramInt1, int paramInt2, KeyEvent paramKeyEvent) {
        return proxyActivity.proxyOnKeyMultiple(paramInt1, paramInt2, paramKeyEvent);
    }

    public boolean onKeyUp(int paramInt, KeyEvent paramKeyEvent) {
        return proxyActivity.proxyOnKeyUp(paramInt, paramKeyEvent);
    }

    public void onLowMemory() {
        proxyActivity.proxyOnLowMemory();
    }

    public boolean onMenuItemSelected(int paramInt, MenuItem paramMenuItem) {
        return proxyActivity.proxyOnMenuItemSelected(paramInt, paramMenuItem);
    }

    public boolean onMenuOpened(int paramInt, Menu paramMenu) {
        return proxyActivity.proxyOnMenuOpened(paramInt, paramMenu);
    }

    protected void onNewIntent(Intent paramIntent) {
    }

    public boolean onOptionsItemSelected(MenuItem paramMenuItem) {
        return proxyActivity.proxyOnOptionsItemSelected(paramMenuItem);
    }

    public void onOptionsMenuClosed(Menu paramMenu) {
        proxyActivity.proxyOnOptionsMenuClosed(paramMenu);
    }

    public void onPanelClosed(int paramInt, Menu paramMenu) {
        proxyActivity.proxyOnPanelClosed(paramInt, paramMenu);
    }

    protected void onPause() {
    	System.out.println("________cmactivity onPause");
        proxyActivity.proxyOnPause();
        
        // 排除掉 rootActiviti引用的MAActivity 实例
        if (sActivityLifecycleCallbacks != null && !(this.getClass().equals(CMActivity.class))) {
            sActivityLifecycleCallbacks.onActivityPaused(activity);
        }
    }

    protected void onPostCreate(Bundle paramBundle) {
        proxyActivity.proxyOnPostCreate(paramBundle);
    }

    protected void onPostResume() {
        proxyActivity.proxyOnPostResume();
    }

    public void onPrepareDialog(int paramInt, Dialog paramDialog) {
        proxyActivity.proxyOnPrepareDialog(paramInt, paramDialog);
    }

    public boolean onPrepareOptionsMenu(Menu paramMenu) {
        return proxyActivity.proxyOnPrepareOptionsMenu(paramMenu);
    }

    public boolean onPreparePanel(int paramInt, View paramView, Menu paramMenu) {
        return proxyActivity.proxyOnPreparePanel(paramInt, paramView, paramMenu);
    }

    protected void onRestart() {
        proxyActivity.proxyOnRestart();
    }

    protected void onRestoreInstanceState(Bundle paramBundle) {
        proxyActivity.proxyOnRestoreInstanceState(paramBundle);
    }

    protected void onResume() {
        proxyActivity.proxyOnResume();
        
        // 排除掉 rootActiviti引用的MAActivity 实例
        if (sActivityLifecycleCallbacks != null && !(this.getClass().equals(CMActivity.class))) {
            sActivityLifecycleCallbacks.onActivityResumed(activity);
        }
    }

    public Object onRetainNonConfigurationInstance() {
        return proxyActivity.proxyOnRetainNonConfigurationInstance();
    }

    protected void onSaveInstanceState(Bundle paramBundle) {
        proxyActivity.proxyOnSaveInstanceState(paramBundle);
        
        // 排除掉 rootActiviti引用的MAActivity 实例
        if (sActivityLifecycleCallbacks != null && !(this.getClass().equals(CMActivity.class))) {
            sActivityLifecycleCallbacks.onActivitySaveInstanceState(activity, paramBundle);
        }
    }

    public boolean onSearchRequested() {
        return proxyActivity.proxyOnSearchRequested();
    }

    protected void onStart() {
        proxyActivity.proxyOnStart();
        
        // 排除掉 rootActiviti引用的MAActivity 实例
        if (sActivityLifecycleCallbacks != null && !(this.getClass().equals(CMActivity.class))) {
            sActivityLifecycleCallbacks.onActivityStarted(activity);
        }
    }

    protected void onStop() {
        proxyActivity.proxyOnStop();
        
        // 排除掉 rootActiviti引用的MAActivity 实例
        if (sActivityLifecycleCallbacks != null && !(this.getClass().equals(CMActivity.class))) {
            sActivityLifecycleCallbacks.onActivityStopped(activity);
        }
    }

    protected void onTitleChanged(CharSequence paramCharSequence, int paramInt) {
        proxyActivity.proxyOnTitleChanged(paramCharSequence, paramInt);
    }

    public boolean onTouchEvent(MotionEvent paramMotionEvent) {
        return proxyActivity.proxyOnTouchEvent(paramMotionEvent);
    }

    public boolean onTrackballEvent(MotionEvent paramMotionEvent) {
        return proxyActivity.proxyOnTrackballEvent(paramMotionEvent);
    }

    public void onUserInteraction() {
        proxyActivity.proxyOnUserInteraction();
    }

    protected void onUserLeaveHint() {
    }

    public void onWindowAttributesChanged(WindowManager.LayoutParams paramLayoutParams) {
        proxyActivity.proxyOnWindowAttributesChanged(paramLayoutParams);
    }

    public void onWindowFocusChanged(boolean paramBoolean) {
        proxyActivity.proxyOnWindowFocusChanged(paramBoolean);
    }

    public void openContextMenu(View paramView) {
        proxyActivity.proxyOpenContextMenu(paramView);
    }

    public void openOptionsMenu() {
        proxyActivity.proxyOpenOptionsMenu();
    }

    public void overridePendingTransition(int paramInt1, int paramInt2) {
        proxyActivity.proxyOverridePendingTransition(paramInt1, paramInt2);
    }

    public void registerForContextMenu(View paramView) {
        proxyActivity.proxyRegisterForContextMenu(paramView);
    }

    public final void removeDialog(int paramInt) {
        activity.removeDialog(paramInt);
    }

    public final boolean requestWindowFeature(int paramInt) {
        return activity.requestWindowFeature(paramInt);
    }

    public final void runOnUiThread(Runnable paramRunnable) {
        activity.runOnUiThread(paramRunnable);
    }

    public void setActivityProxy(ActivityProxyAdapter paramActivityProxyAdapter) {
        activity = paramActivityProxyAdapter.getActivity();
        proxyActivity = paramActivityProxyAdapter;
    }

    public void setContentView(int paramInt) {
        proxyActivity.proxySetContentView(paramInt);
    }

    public void setContentView(View paramView) {
        proxyActivity.proxySetContentView(paramView);
    }

    public void setContentView(View paramView, ViewGroup.LayoutParams paramLayoutParams) {
        proxyActivity.proxySetContentView(paramView, paramLayoutParams);
    }

    public final void setDefaultKeyMode(int paramInt) {
        activity.setDefaultKeyMode(paramInt);
    }
    
    public final void setBackgroundDrawableResource(int paramInt){
    	
    }

    public final void setFeatureDrawable(int paramInt, Drawable paramDrawable) {
        activity.setFeatureDrawable(paramInt, paramDrawable);
    }

    public final void setFeatureDrawableAlpha(int paramInt1, int paramInt2) {
        activity.setFeatureDrawableAlpha(paramInt1, paramInt2);
    }

    public final void setFeatureDrawableResource(int paramInt1, int paramInt2) {
        activity.setFeatureDrawableResource(paramInt1, paramInt2);
    }

    public final void setFeatureDrawableUri(int paramInt, Uri paramUri) {
        activity.setFeatureDrawableUri(paramInt, paramUri);
    }

    public void setIntent(Intent paramIntent) {
        proxyActivity.proxySetIntent(paramIntent);
    }

    public final void setProgress(int paramInt) {
        activity.setProgress(paramInt);
    }

    public final void setProgressBarIndeterminate(boolean paramBoolean) {
        activity.setProgressBarIndeterminate(paramBoolean);
    }

    public final void setProgressBarIndeterminateVisibility(boolean paramBoolean) {
        activity.setProgressBarIndeterminateVisibility(paramBoolean);
    }

    public final void setProgressBarVisibility(boolean paramBoolean) {
        activity.setProgressBarVisibility(paramBoolean);
    }

    public void setRequestedOrientation(int paramInt) {
        proxyActivity.proxySetRequestedOrientation(paramInt);
    }

    public final void setResult(int paramInt) {
        activity.setResult(paramInt);
    }

    public final void setResult(int paramInt, Intent paramIntent) {
        activity.setResult(paramInt, paramIntent);
    }

    public final void setSecondaryProgress(int paramInt) {
        activity.setSecondaryProgress(paramInt);
    }

    public void setTitle(int paramInt) {
        proxyActivity.proxySetTitle(paramInt);
    }

    public void setTitle(CharSequence paramCharSequence) {
        proxyActivity.proxySetTitle(paramCharSequence);
    }

    public void setTitleColor(int paramInt) {
        proxyActivity.proxySetTitleColor(paramInt);
    }

    public void setVisible(boolean paramBoolean) {
        proxyActivity.proxySetVisible(paramBoolean);
    }

    public final void setVolumeControlStream(int paramInt) {
        activity.setVolumeControlStream(paramInt);
    }

    public final void showDialog(int paramInt) {
        activity.showDialog(paramInt);
    }

    public void startActivity(Intent paramIntent) {
        ProxyEnvironment.getInstance(getTargetPackageName()).remapStartActivityIntent(paramIntent);
        proxyActivity.proxyStartActivity(paramIntent);
    }

    public void startActivityForResult(Intent paramIntent, int paramInt) {
        ProxyEnvironment.getInstance(getTargetPackageName()).remapStartActivityIntent(paramIntent);
        proxyActivity.proxyStartActivityForResult(paramIntent, paramInt);
    }

    public void startActivityForResult(Intent paramIntent, int paramInt,Bundle options) {
        ProxyEnvironment.getInstance(getTargetPackageName()).remapStartActivityIntent(paramIntent);
        proxyActivity.proxyStartActivityForResult(paramIntent, paramInt,options);
    }
    
    public void startActivityFromChild(Activity paramActivity, Intent paramIntent, int paramInt) {
        proxyActivity.proxyStartActivityFromChild(paramActivity, paramIntent, paramInt);
    }

    public boolean startActivityIfNeeded(Intent paramIntent, int paramInt) {
        return proxyActivity.proxyStartActivityIfNeeded(paramIntent, paramInt);
    }

    public void startIntentSender(IntentSender paramIntentSender, Intent paramIntent, int paramInt1, int paramInt2,
            int paramInt3) throws IntentSender.SendIntentException {
        proxyActivity.proxyStartIntentSender(paramIntentSender, paramIntent, paramInt1, paramInt2, paramInt3);
    }

    public void startIntentSenderForResult(IntentSender paramIntentSender, int paramInt1, Intent paramIntent,
            int paramInt2, int paramInt3, int paramInt4) throws IntentSender.SendIntentException {
        proxyActivity.proxyStartIntentSenderForResult(paramIntentSender, paramInt1, paramIntent, paramInt2, paramInt3,
                paramInt4);
    }

    public void startManagingCursor(Cursor paramCursor) {
        proxyActivity.proxyStartManagingCursor(paramCursor);
    }

    public boolean startNextMatchingActivity(Intent paramIntent) {
        return proxyActivity.proxyStartNextMatchingActivity(paramIntent);
    }

    public void startSearch(String paramString, boolean paramBoolean1, Bundle paramBundle, boolean paramBoolean2) {
        proxyActivity.proxyStartSearch(paramString, paramBoolean1, paramBundle, paramBoolean2);
    }

    public ComponentName startService(Intent paramIntent) {
        ProxyEnvironment.getInstance(getTargetPackageName()).remapStartServiceIntent(paramIntent);
        return proxyActivity.proxyStartService(paramIntent);
    }

    public void stopManagingCursor(Cursor paramCursor) {
        proxyActivity.proxyStopManagingCursor(paramCursor);
    }

    public boolean stopService(Intent paramIntent) {
    	if(paramIntent != null){
    		String targetService = paramIntent.getComponent().getClassName();
    		if(ProxyEnvironment.aliveServiceMap.containsKey(targetService)){
    			ProxyEnvironment.aliveServiceMap.get(targetService).stopService(paramIntent);
    		}
    	}
        return proxyActivity.proxyStopService(paramIntent);
    }

    public void takeKeyEvents(boolean paramBoolean) {
        proxyActivity.proxyTakeKeyEvents(paramBoolean);
    }

    public void unregisterForContextMenu(View paramView) {
        proxyActivity.proxyUnregisterForContextMenu(paramView);
    }

    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        // TODO Auto-generated method stub
        return proxyActivity.registerReceiver(receiver, filter);
    }

    public void unregisterReceiver(BroadcastReceiver receiver) {
        // TODO Auto-generated method stub
        proxyActivity.unregisterReceiver(receiver);
    }

    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * public ActionMode onWindowStartingActionMode(Callback callback) { // TODO
     * Auto-generated method stub return null; }
     * 
     * public void onActionModeStarted(ActionMode mode) { // TODO Auto-generated
     * method stub
     * 
     * }
     * 
     * public void onActionModeFinished(ActionMode mode) { // TODO
     * Auto-generated method stub
     * 
     * }
     */
    @TargetApi(11)
    /**
     * ！！！api 11 之后的方法，使用此方法请注意
     * @param finish
     */
    public void setFinishOnTouchOutside(boolean finish) {
        proxyActivity.proxysetFinishOnTouchOutside(finish);
    }

    public SharedPreferences getSharedPreferences(String name, int mode) {
        // TODO 需要判断是否要改变data路径，先加前缀
        if (ProxyEnvironment.getInstance(getTargetPackageName()).isDataNeedPrefix()) {
            name = getTargetPackageName() + "_" + name;
        }
        return proxyActivity.proxyGetSharedPreferences(name, mode);
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        // TODO 加前缀导致as下载到手机内存bug，需要整体实现改变数据路径
        // name = getTargetPackageName() + "_" + name;
        return activity.openFileInput(name);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        // TODO 加前缀导致as下载到手机内存bug，需要整体实现改变数据路径
        // name = getTargetPackageName() + "_" + name;
        return activity.openFileOutput(name, mode);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {
        if (ProxyEnvironment.getInstance(getTargetPackageName()).isDataNeedPrefix()) {
            name = getTargetPackageName() + "_" + name;
        }
        return activity.openOrCreateDatabase(name, mode, factory);
    }

/*    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory,
            DatabaseErrorHandler errorHandler) {
        name = getTargetPackageName() + "_" + name;
        return activity.openOrCreateDatabase(name, mode, factory, errorHandler);
    }
*/
    @Override
    public File getFileStreamPath(String name) {
        // name = getTargetPackageName() + "_" + name;
        return activity.getFileStreamPath(name);
    }

    @Override
    public Context getApplicationContext() {

        // 在onCreate回调前，系统可能会getApplication，比如4.0上的Window.getCompatInfo，这时候需要返回真正的application
        if (bOnCreateCalled || CMActivity.class.getName().equals(this.getClass().getName())) {
            return super.getApplicationContext();
        } else {
            return proxyActivity.proxyGetApplicationContext();
        }
    }
    
    @Override
    public void sendBroadcast(Intent intent) {
        ProxyEnvironment.getInstance(getTargetPackageName()).remapReceiverIntent(intent);
        super.sendBroadcast(intent);
    }
    
    @Override
    public void sendBroadcast(Intent intent, String receiverPermission) {
        ProxyEnvironment.getInstance(getTargetPackageName()).remapReceiverIntent(intent);
        super.sendBroadcast(intent,receiverPermission);
    }

    
	@Override
	public void invoke(Object... obj) {
		// TODO Auto-generated method stub
		if(invoke !=null){
			invoke.invoke(obj);
			PluginDebugLog.log(TAG, "调用了组件 cmActivity");
		}
	}
	
	public void setPluginDebug(boolean isdebug){
		
	}
    /**
     * 如果是插件在外部调用，需要传递父类的Activity
     * @param activity
     */
    public void setParent(Activity activity,IPluginInvoke pluginInvoke) {
        mParent = activity;
        this.invoke = pluginInvoke;//与宿主通信的回调
    }
    
    @Override
    public Looper getMainLooper() {
    	// TODO Auto-generated method stub
    	Looper looper = null;
    	if(mParent != null){
    		looper = mParent.getMainLooper();
    	}else{
    		looper = proxyActivity.getProxyMainLooper();
    	}
    	return looper;
    }
    
	public FragmentManager getFragmentManager() {
		return proxyActivity.getProxyFragmentManager();
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		return proxyActivity.proxyonCreateOptionsMenu(menu);
	}
}
