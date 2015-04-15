package org.qiyi.pluginlibrary.adapter;

import org.qiyi.pluginlibrary.component.CMActivity;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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

public abstract interface ActivityProxyAdapter {

    public abstract CMActivity getTarget();

    public abstract Activity getActivity();

    public abstract View proxyOnCreateView(String paramString, Context paramContext, AttributeSet paramAttributeSet);
    
    public abstract View proxyOnCreateView(View parent,String paramString, Context paramContext, AttributeSet paramAttributeSet);
    
    public abstract void proxyAddContentView(View paramView, ViewGroup.LayoutParams paramLayoutParams);

    public abstract boolean proxyBindService(Intent paramIntent, ServiceConnection paramServiceConnection, int paramInt);
    public abstract void proxyUnbindService( ServiceConnection paramServiceConnection);

    public abstract void proxyCloseContextMenu();

    public abstract void proxyCloseOptionsMenu();

    public abstract PendingIntent proxyCreatePendingResult(int paramInt1, Intent paramIntent, int paramInt2);

    public abstract boolean proxyDispatchKeyEvent(KeyEvent paramKeyEvent);

    public abstract boolean proxyDispatchPopulateAccessibilityEvent(AccessibilityEvent paramAccessibilityEvent);

    public abstract boolean proxyDispatchTouchEvent(MotionEvent paramMotionEvent);

    public abstract boolean proxyDispatchTrackballEvent(MotionEvent paramMotionEvent);

    public abstract View proxyFindViewById(int paramInt);

    public abstract void proxyFinish();

    public abstract void proxyFinishActivity(int paramInt);

    public abstract void proxyFinishActivityFromChild(Activity paramActivity, int paramInt);

    public abstract void proxyFinishFromChild(Activity paramActivity);
    
    public abstract Context proxyGetApplicationContext();

    public abstract ComponentName proxyGetCallingActivity();

    public abstract SharedPreferences proxyGetSharedPreferences(String name, int mode);

    public abstract String proxyGetCallingPackage();

    public abstract int proxyGetChangingConfigurations();

    public abstract View proxyGetCurrentFocus();

    public abstract Intent proxyGetIntent();

    public abstract Object proxyGetLastNonConfigurationInstance();

    public abstract LayoutInflater proxyGetLayoutInflater();

    public abstract String proxyGetLocalClassName();

    public abstract MenuInflater proxyGetMenuInflater();

    public abstract PackageManager proxyGetPackageManager();

    public abstract SharedPreferences proxyGetPreferences(int paramInt);

    public abstract int proxyGetRequestedOrientation();

    public abstract Object proxyGetSystemService(String paramString);

    public abstract int proxyGetTaskId();

    public abstract int proxyGetWallpaperDesiredMinimumHeight();

    public abstract int proxyGetWallpaperDesiredMinimumWidth();

    public abstract Window proxyGetWindow();

    public abstract WindowManager proxyGetWindowManager();

    public abstract boolean proxyHasWindowFocus();

    public abstract boolean proxyIsFinishing();

    public abstract boolean proxyIsTaskRoot();

    public abstract boolean proxyMoveTaskToBack(boolean paramBoolean);

    public abstract void proxyOnActivityResult(int paramInt1, int paramInt2, Intent paramIntent);

    public abstract void proxyOnApplyThemeResource(Resources.Theme paramTheme, int paramInt, boolean paramBoolean);

    public abstract void proxyOnAttachedToWindow();

    public abstract void proxyOnBackPressed();

    public abstract void proxyOnChildTitleChanged(Activity paramActivity, CharSequence paramCharSequence);

    public abstract void proxyOnConfigurationChanged(Configuration paramConfiguration);

    public abstract void proxyOnContentChanged();

    public abstract boolean proxyOnContextItemSelected(MenuItem paramMenuItem);

    public abstract void proxyOnContextMenuClosed(Menu paramMenu);

    public abstract void proxyOnCreate(Bundle paramBundle);

    public abstract void proxyOnCreateContextMenu(ContextMenu paramContextMenu, View paramView,
            ContextMenu.ContextMenuInfo paramContextMenuInfo);

    public abstract boolean proxyOnCreatePanelMenu(int paramInt, Menu paramMenu);

    public abstract boolean proxyOnCreateThumbnail(Bitmap paramBitmap, Canvas paramCanvas);

    public abstract void proxyOnDestroy();

    public abstract void proxyOnDetachedFromWindow();

    public abstract boolean proxyOnKeyDown(int paramInt, KeyEvent paramKeyEvent);

    public abstract boolean proxyOnKeyLongPress(int paramInt, KeyEvent paramKeyEvent);

    public abstract boolean proxyOnKeyMultiple(int paramInt1, int paramInt2, KeyEvent paramKeyEvent);

    public abstract boolean proxyOnKeyUp(int paramInt, KeyEvent paramKeyEvent);

    public abstract void proxyOnLowMemory();

    public abstract boolean proxyOnMenuItemSelected(int paramInt, MenuItem paramMenuItem);

    public abstract boolean proxyOnMenuOpened(int paramInt, Menu paramMenu);

    public abstract boolean proxyOnOptionsItemSelected(MenuItem paramMenuItem);

    public abstract void proxyOnOptionsMenuClosed(Menu paramMenu);

    public abstract void proxyOnPanelClosed(int paramInt, Menu paramMenu);

    public abstract void proxyOnPause();

    public abstract void proxyOnPostCreate(Bundle paramBundle);

    public abstract void proxyOnPostResume();

    public abstract void proxyOnPrepareDialog(int paramInt, Dialog paramDialog);

    public abstract boolean proxyOnPrepareOptionsMenu(Menu paramMenu);

    public abstract boolean proxyOnPreparePanel(int paramInt, View paramView, Menu paramMenu);

    public abstract void proxyOnRestart();

    public abstract void proxyOnRestoreInstanceState(Bundle paramBundle);

    public abstract void proxyOnResume();

    public abstract Object proxyOnRetainNonConfigurationInstance();

    public abstract void proxyOnSaveInstanceState(Bundle paramBundle);

    public abstract boolean proxyOnSearchRequested();

    public abstract void proxyOnStart();

    public abstract void proxyOnStop();

    public abstract void proxyOnTitleChanged(CharSequence paramCharSequence, int paramInt);

    public abstract boolean proxyOnTouchEvent(MotionEvent paramMotionEvent);

    public abstract boolean proxyOnTrackballEvent(MotionEvent paramMotionEvent);

    public abstract void proxyOnUserInteraction();

    public abstract void proxyOnWindowAttributesChanged(WindowManager.LayoutParams paramLayoutParams);

    public abstract void proxyOnWindowFocusChanged(boolean paramBoolean);

    public abstract void proxyOpenContextMenu(View paramView);

    public abstract void proxyOpenOptionsMenu();

    public abstract void proxyOverridePendingTransition(int paramInt1, int paramInt2);

    public abstract void proxyRegisterForContextMenu(View paramView);

    public abstract void proxySetContentView(int paramInt);

    public abstract void proxySetContentView(View paramView);

    public abstract void proxySetContentView(View paramView, ViewGroup.LayoutParams paramLayoutParams);

    public abstract void proxySetIntent(Intent paramIntent);

    public abstract void proxySetRequestedOrientation(int paramInt);

    public abstract void proxySetTitle(int paramInt);

    public abstract void proxySetTitle(CharSequence paramCharSequence);

    public abstract void proxySetTitleColor(int paramInt);

    public abstract void proxySetVisible(boolean paramBoolean);

    public abstract void proxyStartActivity(Intent paramIntent);

    public abstract void proxyStartActivityForResult(Intent paramIntent, int paramInt);
    
    public abstract void proxyStartActivityForResult(Intent paramIntent, int paramInt,Bundle options);

    public abstract void proxyStartActivityFromChild(Activity paramActivity, Intent paramIntent, int paramInt);

    public abstract boolean proxyStartActivityIfNeeded(Intent paramIntent, int paramInt);

    public abstract void proxyStartIntentSender(IntentSender paramIntentSender, Intent paramIntent, int paramInt1,
            int paramInt2, int paramInt3) throws IntentSender.SendIntentException;

    public abstract void proxyStartIntentSenderForResult(IntentSender paramIntentSender, int paramInt1,
            Intent paramIntent, int paramInt2, int paramInt3, int paramInt4) throws IntentSender.SendIntentException;

    public abstract void proxyStartIntentSenderFromChild(Activity paramActivity, IntentSender paramIntentSender,
            int paramInt1, Intent paramIntent, int paramInt2, int paramInt3, int paramInt4)
            throws IntentSender.SendIntentException;

    public abstract void proxyStartManagingCursor(Cursor paramCursor);

    public abstract boolean proxyStartNextMatchingActivity(Intent paramIntent);

    public abstract void proxyStartSearch(String paramString, boolean paramBoolean1, Bundle paramBundle,
            boolean paramBoolean2);

    public abstract ComponentName proxyStartService(Intent paramIntent);

    public abstract void proxyStopManagingCursor(Cursor paramCursor);

    public abstract boolean proxyStopService(Intent paramIntent);

    public abstract void proxyTakeKeyEvents(boolean paramBoolean);

    public abstract void proxyUnregisterForContextMenu(View paramView);

    public void proxysetFinishOnTouchOutside(boolean finish);

    public abstract Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter);

    public abstract void unregisterReceiver(BroadcastReceiver receiver);
    
    public abstract Looper getProxyMainLooper();
    
    public abstract FragmentManager getProxyFragmentManager();
    
    public abstract boolean proxyonCreateOptionsMenu(Menu menu);
}
