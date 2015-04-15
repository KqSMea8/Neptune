package org.qiyi.pluginlibrary.proxy.activity;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import org.qiyi.pluginlibrary.ProxyEnvironment;
import org.qiyi.pluginlibrary.ErrorType.ErrorType;
import org.qiyi.pluginlibrary.adapter.FragmentActivityProxyAdapter;
import org.qiyi.pluginlibrary.api.TargetActivator;
import org.qiyi.pluginlibrary.component.CMActivity;
import org.qiyi.pluginlibrary.component.CMFragmentActivity;
import org.qiyi.pluginlibrary.utils.JavaCalls;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
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
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
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

public class FragmentActivityProxy extends FragmentActivity implements FragmentActivityProxyAdapter {

    private CMFragmentActivity target;

    public void loadTargetActivity() {
        if (target == null && !super.isFinishing()) {
            Intent curIntent = getIntent();
            if (curIntent == null) {
                finish();
                return;
            }
            String targetClassName = curIntent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_ACTIVITY);
            String targetPackageName = curIntent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME);
            if (!ProxyEnvironment.hasInstance(targetPackageName)) {
                finish();

                if (targetClassName == null) {
                    targetClassName = "";
                }

                if (!TextUtils.isEmpty(targetPackageName)) {
                    Intent intent = new Intent(getIntent());
                    intent.setComponent(new ComponentName(targetPackageName, targetClassName));
                    TargetActivator.loadTargetAndRun(this, intent);
                }
                return;
            }

            try {
            	
            	Class<?> className = ProxyEnvironment.getInstance(targetPackageName).getDexClassLoader().loadClass(targetClassName);
            	if(className != null && className.newInstance() instanceof CMFragmentActivity){
            		target = ((CMFragmentActivity)className.asSubclass(CMFragmentActivity.class).newInstance());
            		target.setActivityProxy(this);
            		target.setTargetPackagename(targetPackageName);
            		setTheme(ProxyEnvironment.getInstance(targetPackageName).getTargetActivityThemeResource(targetClassName));
            	}
            } catch (InstantiationException e) {
                e.printStackTrace();
                ProxyEnvironment.deliverPlug(false,targetPackageName , ErrorType.ERROR_CLIENT_LOAD_INIT_EXCEPTION_INSTANTIATION);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                ProxyEnvironment.deliverPlug(false,targetPackageName , ErrorType.ERROR_CLIENT_LOAD_INIT_EXCEPTION_ILLEGALACCESS);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                ProxyEnvironment.deliverPlug(false,targetPackageName , ErrorType.ERROR_CLIENT_LOAD_INIT_EXCEPTION_CLASSNOTFOUND);
            } catch(Exception e){
            	e.printStackTrace();
            	target = null;
            	ProxyEnvironment.deliverPlug(false,targetPackageName,ErrorType.ERROR_CLIENT_LOAD_INIT_EXCEPTION);
            }
        }
    }

    public void addContentView(View paramView, ViewGroup.LayoutParams paramLayoutParams) {
        if (target != null) {
            target.addContentView(paramView, paramLayoutParams);
        } else {
            super.addContentView(paramView, paramLayoutParams);
        }
    }

    public boolean bindService(Intent paramIntent, ServiceConnection paramServiceConnection, int paramInt) {
        if (target != null) {
            return target.bindService(paramIntent, paramServiceConnection, paramInt);
        } else {
            return super.bindService(paramIntent, paramServiceConnection, paramInt);
        }
    }

    public void closeContextMenu() {
        if (target != null) {
            target.closeContextMenu();
        } else {
            super.closeContextMenu();
        }
    }

    public void closeOptionsMenu() {
        if (target != null) {
            target.closeOptionsMenu();
        } else {
            super.closeOptionsMenu();
        }
    }

    public PendingIntent createPendingResult(int paramInt1, Intent paramIntent, int paramInt2) {
        if (target != null) {
            return target.createPendingResult(paramInt1, paramIntent, paramInt2);
        } else {
            return super.createPendingResult(paramInt1, paramIntent, paramInt2);
        }
    }

    @SuppressLint("NewApi")
	public boolean dispatchGenericMotionEvent(MotionEvent paramMotionEvent) {
        if (target != null) {
            return target.dispatchGenericMotionEvent(paramMotionEvent);
        } else {
            return super.dispatchGenericMotionEvent(paramMotionEvent);
        }
    }

    public boolean dispatchKeyEvent(KeyEvent paramKeyEvent) {
        if (target != null) {
            return target.dispatchKeyEvent(paramKeyEvent);
        } else {
            return super.dispatchKeyEvent(paramKeyEvent);
        }
    }

    @SuppressLint("NewApi")
	public boolean dispatchKeyShortcutEvent(KeyEvent paramKeyEvent) {
        if (target != null) {
            return target.dispatchKeyShortcutEvent(paramKeyEvent);
        } else {
            return super.dispatchKeyShortcutEvent(paramKeyEvent);
        }
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent paramAccessibilityEvent) {
        if (target != null) {
            return target.dispatchPopulateAccessibilityEvent(paramAccessibilityEvent);
        } else {
            return super.dispatchPopulateAccessibilityEvent(paramAccessibilityEvent);
        }
    }

    public boolean dispatchTouchEvent(MotionEvent paramMotionEvent) {
        if (target != null) {
            return target.dispatchTouchEvent(paramMotionEvent);
        } else {
            return super.dispatchTouchEvent(paramMotionEvent);
        }
    }

    public boolean dispatchTrackballEvent(MotionEvent paramMotionEvent) {
        if (target != null) {
            return target.dispatchTrackballEvent(paramMotionEvent);
        } else {
            return super.dispatchTrackballEvent(paramMotionEvent);
        }
    }

    // TODO
    public void dump(String paramString, FileDescriptor paramFileDescriptor, PrintWriter paramPrintWriter,
            String[] paramArrayOfString) {
    }

    public View findViewById(int paramInt) {
        if (target != null) {
            return target.findViewById(paramInt);
        } else {
            return super.findViewById(paramInt);
        }
    }

    public void finish() {
        if (target != null) {
            target.finish();
        } else {
            super.finish();
        }
    }

    public void finishActivity(int paramInt) {
        if (target != null) {
            target.finishActivity(paramInt);
        } else {
            super.finishActivity(paramInt);
        }
    }

    public void finishActivityFromChild(Activity paramActivity, int paramInt) {
        if (target != null) {
            target.finishActivityFromChild(paramActivity, paramInt);
        } else {
            super.finishActivityFromChild(paramActivity, paramInt);
        }
    }

    public void finishFromChild(Activity paramActivity) {
        if (target != null) {
            target.finishFromChild(paramActivity);
        } else {
            super.finishFromChild(paramActivity);
        }
    }

    public Activity getActivity() {
        return this;
    }

    public AssetManager getAssets() {
        if (target != null) {
            return target.getAssets();
        } else {
            return super.getAssets();
        }
    }

    public ComponentName getCallingActivity() {
        if (target != null) {
            return target.getCallingActivity();
        } else {
            return super.getCallingActivity();
        }
    }

    public String getCallingPackage() {
        if (target != null) {
            return target.getCallingPackage();
        }
        return super.getCallingPackage();
    }

    public int getChangingConfigurations() {
        return -1;
    }

    public ClassLoader getClassLoader() {
        if (target != null) {
            return target.getClassLoader();
        } else {
            return super.getClassLoader();
        }
    }

    public View getCurrentFocus() {
        if (target != null) {
            return target.getCurrentFocus();
        } else {
            return super.getCurrentFocus();
        }
    }

    public Intent getIntent() {
        CMActivity localIASFragmentActivity = target;
        if (localIASFragmentActivity != null) {
            return localIASFragmentActivity.getIntent();
        }
        return super.getIntent();
    }

    public LayoutInflater getLayoutInflater() {
        CMActivity localIASFragmentActivity = target;
        if (localIASFragmentActivity != null) {
            return localIASFragmentActivity.getLayoutInflater();
        }
        return super.getLayoutInflater();
    }

    public String getLocalClassName() {
        CMActivity localIASFragmentActivity = target;
        if (localIASFragmentActivity != null) {
            return localIASFragmentActivity.getLocalClassName();
        }
        return super.getLocalClassName();
    }

    public MenuInflater getMenuInflater() {
        if (target != null) {
            return target.getMenuInflater();
        } else {
            return super.getMenuInflater();
        }
    }

    public PackageManager getPackageManager() {
        if (target != null) {
            return target.getPackageManager();
        } else {
            return super.getPackageManager();
        }
    }

    public SharedPreferences getPreferences(int paramInt) {
        if (target != null) {
            return target.getPreferences(paramInt);
        } else {
            return super.getPreferences(paramInt);
        }
    }

    public int getRequestedOrientation() {
        if (target != null) {
            return target.getRequestedOrientation();
        } else {
            return super.getRequestedOrientation();
        }
    }

    @Override
    public Resources getResources() {
        if (target != null) {
            return target.getResources();
        } else {
            return super.getResources();
        }
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (target != null) {
            return target.getSharedPreferences(name, mode);
        } else {
            return super.getSharedPreferences(name, mode);
        }
    }

    public Object getSystemService(String paramString) {
        if (target != null)
            return target.getSystemService(paramString);
        return super.getSystemService(paramString);
    }

    public int getTaskId() {
        CMActivity localIASFragmentActivity = target;
        if (localIASFragmentActivity != null)
            return localIASFragmentActivity.getTaskId();
        return super.getTaskId();
    }

    public int getWallpaperDesiredMinimumHeight() {
        CMActivity localIASFragmentActivity = target;
        if (localIASFragmentActivity != null)
            return localIASFragmentActivity.getWallpaperDesiredMinimumHeight();
        return super.getWallpaperDesiredMinimumHeight();
    }

    public int getWallpaperDesiredMinimumWidth() {
        CMActivity localIASFragmentActivity = target;
        if (localIASFragmentActivity != null)
            return localIASFragmentActivity.getWallpaperDesiredMinimumWidth();
        return super.getWallpaperDesiredMinimumWidth();
    }

    public Window getWindow() {
        CMActivity localIASFragmentActivity = target;
        if (localIASFragmentActivity != null)
            return localIASFragmentActivity.getWindow();
        return super.getWindow();
    }

    public WindowManager getWindowManager() {
        CMActivity localIASFragmentActivity = target;
        if (localIASFragmentActivity != null)
            return localIASFragmentActivity.getWindowManager();
        return super.getWindowManager();
    }

    public boolean hasWindowFocus() {
        if (target != null) {
            return target.hasWindowFocus();
        } else {
            return super.hasWindowFocus();
        }
    }

    public boolean isFinishing() {
        if (target != null) {
            return target.isFinishing();
        } else {
            return super.isFinishing();
        }
    }

    public boolean isTaskRoot() {
        CMActivity localIASFragmentActivity = target;
        if (localIASFragmentActivity != null)
            return localIASFragmentActivity.isTaskRoot();
        return super.isTaskRoot();
    }

    public boolean moveTaskToBack(boolean paramBoolean) {
        CMActivity localIASFragmentActivity = target;
        if (localIASFragmentActivity != null)
            return localIASFragmentActivity.moveTaskToBack(paramBoolean);
        return super.moveTaskToBack(paramBoolean);
    }

    /*
     * public void onActionModeFinished(ActionMode paramActionMode) {
     * target.onActionModeFinished(paramActionMode); }
     * 
     * public void onActionModeStarted(ActionMode paramActionMode) {
     * target.onActionModeStarted(paramActionMode); }
     */
    protected void onActivityResult(int paramInt1, int paramInt2, Intent paramIntent) {
        if (target != null) {
            JavaCalls.invokeMethod(target, "onActivityResult", new Class[] { int.class, int.class, Intent.class },
                    new Object[] { paramInt1, paramInt2, paramIntent });
        } else {
            super.onActivityResult(paramInt1, paramInt2, paramIntent);
        }
    }

    protected void onApplyThemeResource(Resources.Theme paramTheme, int paramInt, boolean paramBoolean) {
        if (target != null) {
            JavaCalls.invokeMethod(target, "onApplyThemeResource", new Class[] { Resources.Theme.class, int.class,
                    boolean.class }, new Object[] { paramTheme, paramInt, paramBoolean });
        } else {
            super.onApplyThemeResource(paramTheme, paramInt, paramBoolean);
        }
    }

    public void onAttachedToWindow() {
        if (target != null)
            target.onAttachedToWindow();
        else {
            super.onAttachedToWindow();
        }
    }

    public void onBackPressed() {
        if (target != null) {
            target.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    protected void onChildTitleChanged(Activity paramActivity, CharSequence paramCharSequence) {
        if (target != null)
            JavaCalls.invokeMethod(target, "onChildTitleChanged", new Class[] { Activity.class, CharSequence.class },
                    new Object[] { paramActivity, paramCharSequence });
        else {
            super.onChildTitleChanged(paramActivity, paramCharSequence);
        }
    }

    public void onConfigurationChanged(Configuration paramConfiguration) {
        if (target != null) {
            target.onConfigurationChanged(paramConfiguration);
        } else {
            super.onConfigurationChanged(paramConfiguration);
        }
    }

    public void onContentChanged() {
        if (target != null) {
            target.onContentChanged();
        } else {
            super.onContentChanged();
        }
    }

    public boolean onContextItemSelected(MenuItem paramMenuItem) {
        if (target != null) {
            return target.onContextItemSelected(paramMenuItem);
        } else {
            return super.onContextItemSelected(paramMenuItem);
        }
    }

    public void onContextMenuClosed(Menu paramMenu) {
        if (target != null) {
            target.onContextMenuClosed(paramMenu);
        } else {
            super.onContextMenuClosed(paramMenu);
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        // 横竖屏切换会重新走onCreate，释放target
        target = null;
        loadTargetActivity();
        if (target != null) {
            JavaCalls.invokeMethod(target, "onCreate", new Class[] { Bundle.class }, new Object[] { bundle });
        } else {
            super.onCreate(bundle);
        }
    }

    public void onCreateContextMenu(ContextMenu paramContextMenu, View paramView,
            ContextMenu.ContextMenuInfo paramContextMenuInfo) {
        if (target != null) {
            target.onCreateContextMenu(paramContextMenu, paramView, paramContextMenuInfo);
        } else {
            super.onCreateContextMenu(paramContextMenu, paramView, paramContextMenuInfo);
        }
    }

    public CharSequence onCreateDescription() {
        CMActivity localIASFragmentActivity = target;
        if (localIASFragmentActivity != null)
            return localIASFragmentActivity.onCreateDescription();
        return super.onCreateDescription();
    }

    protected Dialog onCreateDialog(int paramInt) {
        if (target != null) {
            return (Dialog) JavaCalls.invokeMethod(target, "onCreateDialog", new Class[] { int.class },
                    new Object[] { paramInt });
        } else {
            return super.onCreateDialog(paramInt);
        }
    }

    public boolean onCreatePanelMenu(int paramInt, Menu paramMenu) {
        if (target != null) {
            return target.onCreatePanelMenu(paramInt, paramMenu);
        } else {
            return super.onCreatePanelMenu(paramInt, paramMenu);
        }
    }

    public View onCreatePanelView(int paramInt) {
        if (target != null) {
            return target.onCreatePanelView(paramInt);
        } else {
            return super.onCreatePanelView(paramInt);
        }
    }

    public boolean onCreateThumbnail(Bitmap paramBitmap, Canvas paramCanvas) {
        if (target != null)
            return target.onCreateThumbnail(paramBitmap, paramCanvas);
        return super.onCreateThumbnail(paramBitmap, paramCanvas);
    }

    public View onCreateView(String paramString, Context paramContext, AttributeSet paramAttributeSet) {
        if (target != null) {
            return target.onCreateView(paramString, paramContext, paramAttributeSet);
        } else {
            return super.onCreateView(paramString, paramContext, paramAttributeSet);
        }
    }

    protected void onDestroy() {
        if (target != null) {
            JavaCalls.invokeMethod(target, "onDestroy", new Class[] {}, new Object[] {});
        } else {
            super.onDestroy();
        }
    }

    public void onDetachedFromWindow() {
        if (target != null) {
            target.onDetachedFromWindow();
        } else {
            super.onDetachedFromWindow();
        }
    }

    public boolean onKeyDown(int paramInt, KeyEvent paramKeyEvent) {
        if (target != null) {
            return target.onKeyDown(paramInt, paramKeyEvent);
        } else {
            return super.onKeyDown(paramInt, paramKeyEvent);
        }
    }

    public boolean onKeyLongPress(int paramInt, KeyEvent paramKeyEvent) {
        if (target != null) {
            return target.onKeyLongPress(paramInt, paramKeyEvent);
        } else {
            return super.onKeyLongPress(paramInt, paramKeyEvent);
        }
    }

    public boolean onKeyMultiple(int paramInt1, int paramInt2, KeyEvent paramKeyEvent) {
        if (target != null) {
            return target.onKeyMultiple(paramInt1, paramInt2, paramKeyEvent);
        } else {
            return super.onKeyMultiple(paramInt1, paramInt2, paramKeyEvent);
        }
    }

    public boolean onKeyUp(int paramInt, KeyEvent paramKeyEvent) {
        if (target != null) {
            return target.onKeyUp(paramInt, paramKeyEvent);
        } else {
            return super.onKeyUp(paramInt, paramKeyEvent);
        }
    }

    public void onLowMemory() {
        if (target != null) {
            target.onLowMemory();
        } else {
            super.onLowMemory();
        }
    }

    public boolean onMenuItemSelected(int paramInt, MenuItem paramMenuItem) {
        if (target != null) {
            return target.onMenuItemSelected(paramInt, paramMenuItem);
        } else {
            return super.onMenuItemSelected(paramInt, paramMenuItem);
        }
    }

    public boolean onMenuOpened(int paramInt, Menu paramMenu) {
        if (target != null) {
            return target.onMenuOpened(paramInt, paramMenu);
        } else {
            return super.onMenuOpened(paramInt, paramMenu);
        }
    }

    protected void onNewIntent(Intent paramIntent) {
        if (target != null) {
            JavaCalls.invokeMethod(target, "onNewIntent", new Class[] { Intent.class }, new Object[] { paramIntent });
        } else {
            super.onNewIntent(paramIntent);
        }
    }

    public boolean onOptionsItemSelected(MenuItem paramMenuItem) {
        if (target != null) {
            return target.onOptionsItemSelected(paramMenuItem);
        } else {
            return super.onOptionsItemSelected(paramMenuItem);
        }
    }

    public void onOptionsMenuClosed(Menu paramMenu) {
        if (target != null) {
            target.onOptionsMenuClosed(paramMenu);
        } else {
            super.onOptionsMenuClosed(paramMenu);
        }
    }

    public void onPanelClosed(int paramInt, Menu paramMenu) {
        if (target != null) {
            target.onPanelClosed(paramInt, paramMenu);
        } else {
            super.onPanelClosed(paramInt, paramMenu);
        }
    }

    protected void onPause() {
        if (target != null) {
            JavaCalls.invokeMethod(target, "onPause", new Class[] {}, new Object[] {});
        } else {
            super.onPause();
        }
    }

    protected void onPostCreate(Bundle paramBundle) {
        if (target != null) {
            JavaCalls.invokeMethod(target, "onPostCreate", new Class[] { Bundle.class }, new Object[] { paramBundle });
        } else {
            super.onPostCreate(paramBundle);
        }
    }

    protected void onPostResume() {
        if (target != null) {
            JavaCalls.invokeMethod(target, "onPostResume", new Class[] {}, new Object[] {});
        } else {
            super.onPostResume();
        }
    }

    protected void onPrepareDialog(int paramInt, Dialog paramDialog) {
        if (target != null) {
            target.onPrepareDialog(paramInt, paramDialog);
        } else {
            super.onPrepareDialog(paramInt, paramDialog);
        }
    }

    public boolean onPrepareOptionsMenu(Menu paramMenu) {
        if (target != null) {
            return target.onPrepareOptionsMenu(paramMenu);
        } else {
            return super.onPrepareOptionsMenu(paramMenu);
        }
    }

    public boolean onPreparePanel(int paramInt, View paramView, Menu paramMenu) {
        if (target != null) {
            return target.onPreparePanel(paramInt, paramView, paramMenu);
        } else {
            return super.onPreparePanel(paramInt, paramView, paramMenu);
        }
    }

    protected void onRestart() {
        if (target != null) {
            JavaCalls.invokeMethod(target, "onRestart", new Class[] {}, new Object[] {});
        } else {
            super.onRestart();
        }
    }

    protected void onRestoreInstanceState(Bundle paramBundle) {
        if (target != null)
            JavaCalls.invokeMethod(target, "onRestoreInstanceState", new Class[] { Bundle.class },
                    new Object[] { paramBundle });
        else {
            super.onRestoreInstanceState(paramBundle);
        }
    }

    protected void onResume() {
        if (target != null) {
            JavaCalls.invokeMethod(target, "onResume", new Class[] {}, new Object[] {});
        } else {
            super.onResume();
        }
    }

    protected void onSaveInstanceState(Bundle paramBundle) {
        if (target != null) {
            JavaCalls.invokeMethod(target, "onSaveInstanceState", new Class[] { Bundle.class },
                    new Object[] { paramBundle });
        } else {
            super.onSaveInstanceState(paramBundle);
        }
    }

    public boolean onSearchRequested() {
        if (target != null) {
            return target.onSearchRequested();
        } else {
            return super.onSearchRequested();
        }
    }

    protected void onStart() {
        if (target != null) {
            JavaCalls.invokeMethod(target, "onStart", new Class[] {}, new Object[] {});
        } else {
            super.onStart();
        }
    }

    protected void onStop() {
        if (target != null) {
            JavaCalls.invokeMethod(target, "onStop", new Class[] {}, new Object[] {});
        } else {
            super.onStop();
        }
    }

    protected void onTitleChanged(CharSequence paramCharSequence, int paramInt) {
        if (target != null) {
            JavaCalls.invokeMethod(target, "onTitleChanged", new Class[] { CharSequence.class, int.class },
                    new Object[] { paramCharSequence, paramInt });
        } else {
            super.onTitleChanged(paramCharSequence, paramInt);
        }
    }

    public boolean onTouchEvent(MotionEvent paramMotionEvent) {
        if (target != null) {
            return target.onTouchEvent(paramMotionEvent);
        } else {
            return super.onTouchEvent(paramMotionEvent);
        }
    }

    public boolean onTrackballEvent(MotionEvent paramMotionEvent) {
        if (target != null) {
            return target.onTrackballEvent(paramMotionEvent);
        } else {
            return super.onTrackballEvent(paramMotionEvent);
        }
    }

    public void onUserInteraction() {
        if (target != null) {
            target.onUserInteraction();
        } else {
            super.onUserInteraction();
        }
    }

    protected void onUserLeaveHint() {
        // target.onUserLeaveHint();
        super.onUserLeaveHint();
    }

    public void onWindowAttributesChanged(WindowManager.LayoutParams paramLayoutParams) {
        if (target != null) {
            target.onWindowAttributesChanged(paramLayoutParams);
        } else {
            super.onWindowAttributesChanged(paramLayoutParams);
        }
    }

    public void onWindowFocusChanged(boolean paramBoolean) {
        if (target != null) {
            target.onWindowFocusChanged(paramBoolean);
        } else {
            super.onWindowFocusChanged(paramBoolean);
        }
    }

    /*
     * public ActionMode onWindowStartingActionMode(ActionMode.Callback
     * paramCallback) { return target.onWindowStartingActionMode(paramCallback);
     * }
     */
    public void openContextMenu(View paramView) {
        if (target != null) {
            target.openContextMenu(paramView);
        } else {
            super.openContextMenu(paramView);
        }
    }

    public void openOptionsMenu() {
        if (target != null) {
            target.openOptionsMenu();
        } else {
            super.openOptionsMenu();
        }
    }

    public void overridePendingTransition(int paramInt1, int paramInt2) {
        if (target != null) {
            target.overridePendingTransition(paramInt1, paramInt2);
        } else {
            super.overridePendingTransition(paramInt1, paramInt2);
        }
    }

    public void proxyAddContentView(View paramView, ViewGroup.LayoutParams paramLayoutParams) {
        super.addContentView(paramView, paramLayoutParams);
    }

    public boolean proxyBindService(Intent paramIntent, ServiceConnection paramServiceConnection, int paramInt) {
        // ProxyEnvironment.getInstance().remapIntent(this, paramIntent);
        return super.bindService(paramIntent, paramServiceConnection, paramInt);
    }
    
    public void proxyUnbindService(ServiceConnection paramServiceConnection) {
    	// ProxyEnvironment.getInstance().remapIntent(this, paramIntent);
    	super.unbindService(paramServiceConnection);
    }

    public void proxyCloseContextMenu() {
        super.closeContextMenu();
    }

    public void proxyCloseOptionsMenu() {
        super.closeOptionsMenu();
    }

    public PendingIntent proxyCreatePendingResult(int paramInt1, Intent paramIntent, int paramInt2) {
        return super.createPendingResult(paramInt1, paramIntent, paramInt2);
    }

    public boolean proxyDispatchKeyEvent(KeyEvent paramKeyEvent) {
        return super.dispatchKeyEvent(paramKeyEvent);
    }

    public boolean proxyDispatchPopulateAccessibilityEvent(AccessibilityEvent paramAccessibilityEvent) {
        return super.dispatchPopulateAccessibilityEvent(paramAccessibilityEvent);
    }

    public boolean proxyDispatchTouchEvent(MotionEvent paramMotionEvent) {
        return super.dispatchTouchEvent(paramMotionEvent);
    }

    public boolean proxyDispatchTrackballEvent(MotionEvent paramMotionEvent) {
        return super.dispatchTrackballEvent(paramMotionEvent);
    }

    public View proxyFindViewById(int paramInt) {
        return super.findViewById(paramInt);
    }

    public void proxyFinish() {
        super.finish();
    }

    public void proxyFinishActivity(int paramInt) {
        super.finishActivity(paramInt);
    }

    public void proxyFinishActivityFromChild(Activity paramActivity, int paramInt) {
        super.finishActivityFromChild(paramActivity, paramInt);
    }

    public void proxyFinishFromChild(Activity paramActivity) {
        super.finishFromChild(paramActivity);
    }

    public ComponentName proxyGetCallingActivity() {
        // return ProxyEnvironment.getInstance().mapComponentName(
        // super.getCallingActivity());
        return null;
    }

    public String proxyGetCallingPackage() {
        return super.getCallingPackage();
    }

    public int proxyGetChangingConfigurations() {
        return super.getChangingConfigurations();
    }

    public View proxyGetCurrentFocus() {
        return super.getCurrentFocus();
    }

    public Intent proxyGetIntent() {
        Intent localIntent = super.getIntent();
        // ProxyEnvironment.getInstance().unmapIntent(this, localIntent);
        return localIntent;
    }

    public Object proxyGetLastNonConfigurationInstance() {
        return super.getLastNonConfigurationInstance();
    }

    public LayoutInflater proxyGetLayoutInflater() {
        return super.getLayoutInflater();
    }

    public String proxyGetLocalClassName() {
        return super.getLocalClassName();
    }

    public MenuInflater proxyGetMenuInflater() {
        return super.getMenuInflater();
    }

    public PackageManager proxyGetPackageManager() {
        return super.getPackageManager();
    }

    public SharedPreferences proxyGetPreferences(int paramInt) {
        return super.getPreferences(paramInt);
    }

    public int proxyGetRequestedOrientation() {
        return super.getRequestedOrientation();
    }

    public Object proxyGetSystemService(String paramString) {
        return super.getSystemService(paramString);
    }

    public int proxyGetTaskId() {
        return super.getTaskId();
    }

    public int proxyGetWallpaperDesiredMinimumHeight() {
        return super.getWallpaperDesiredMinimumHeight();
    }

    public int proxyGetWallpaperDesiredMinimumWidth() {
        return super.getWallpaperDesiredMinimumWidth();
    }

    public Window proxyGetWindow() {
        return super.getWindow();
    }

    public WindowManager proxyGetWindowManager() {
        return super.getWindowManager();
    }

    public boolean proxyHasWindowFocus() {
        return super.hasWindowFocus();
    }

    public boolean proxyIsFinishing() {
        return super.isFinishing();
    }

    public boolean proxyIsTaskRoot() {
        return super.isTaskRoot();
    }

    public boolean proxyMoveTaskToBack(boolean paramBoolean) {
        return super.moveTaskToBack(paramBoolean);
    }

    public void proxyOnActivityResult(int paramInt1, int paramInt2, Intent paramIntent) {
        super.onActivityResult(paramInt1, paramInt2, paramIntent);
    }

    public void proxyOnApplyThemeResource(Resources.Theme paramTheme, int paramInt, boolean paramBoolean) {
        super.onApplyThemeResource(paramTheme, paramInt, paramBoolean);
    }

    public void proxyOnAttachedToWindow() {
        super.onAttachedToWindow();
    }

    public void proxyOnBackPressed() {
        super.onBackPressed();
    }

    public void proxyOnChildTitleChanged(Activity paramActivity, CharSequence paramCharSequence) {
        super.onChildTitleChanged(paramActivity, paramCharSequence);
    }

    public void proxyOnConfigurationChanged(Configuration paramConfiguration) {
        super.onConfigurationChanged(paramConfiguration);
    }

    public void proxyOnContentChanged() {
        super.onContentChanged();
    }

    public boolean proxyOnContextItemSelected(MenuItem paramMenuItem) {
        return super.onContextItemSelected(paramMenuItem);
    }

    public void proxyOnContextMenuClosed(Menu paramMenu) {
        super.onContextMenuClosed(paramMenu);
    }

    public void proxyOnCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
    }

    public void proxyOnCreateContextMenu(ContextMenu paramContextMenu, View paramView,
            ContextMenu.ContextMenuInfo paramContextMenuInfo) {
        super.onCreateContextMenu(paramContextMenu, paramView, paramContextMenuInfo);
    }

    public boolean proxyOnCreatePanelMenu(int paramInt, Menu paramMenu) {
        return super.onCreatePanelMenu(paramInt, paramMenu);
    }

    public boolean proxyOnCreateThumbnail(Bitmap paramBitmap, Canvas paramCanvas) {
        return super.onCreateThumbnail(paramBitmap, paramCanvas);
    }

    public void proxyOnDestroy() {
        super.onDestroy();
    }

    public void proxyOnDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public boolean proxyOnKeyDown(int paramInt, KeyEvent paramKeyEvent) {
        return super.onKeyDown(paramInt, paramKeyEvent);
    }

    public boolean proxyOnKeyLongPress(int paramInt, KeyEvent paramKeyEvent) {
        return super.onKeyLongPress(paramInt, paramKeyEvent);
    }

    public boolean proxyOnKeyMultiple(int paramInt1, int paramInt2, KeyEvent paramKeyEvent) {
        return super.onKeyMultiple(paramInt1, paramInt2, paramKeyEvent);
    }

    public boolean proxyOnKeyUp(int paramInt, KeyEvent paramKeyEvent) {
        return super.onKeyUp(paramInt, paramKeyEvent);
    }

    public void proxyOnLowMemory() {
        super.onLowMemory();
    }

    public boolean proxyOnMenuItemSelected(int paramInt, MenuItem paramMenuItem) {
        return super.onMenuItemSelected(paramInt, paramMenuItem);
    }

    public boolean proxyOnMenuOpened(int paramInt, Menu paramMenu) {
        return super.onMenuOpened(paramInt, paramMenu);
    }

    public boolean proxyOnOptionsItemSelected(MenuItem paramMenuItem) {
        return super.onOptionsItemSelected(paramMenuItem);
    }

    public void proxyOnOptionsMenuClosed(Menu paramMenu) {
        super.onOptionsMenuClosed(paramMenu);
    }

    public void proxyOnPanelClosed(int paramInt, Menu paramMenu) {
        super.onPanelClosed(paramInt, paramMenu);
    }

    public void proxyOnPause() {
        super.onPause();
    }

    public void proxyOnPostCreate(Bundle paramBundle) {
        super.onPostCreate(paramBundle);
    }

    public void proxyOnPostResume() {
        super.onPostResume();
    }

    public void proxyOnPrepareDialog(int paramInt, Dialog paramDialog) {
        super.onPrepareDialog(paramInt, paramDialog);
    }

    public boolean proxyOnPrepareOptionsMenu(Menu paramMenu) {
        return super.onPrepareOptionsMenu(paramMenu);
    }

    public boolean proxyOnPreparePanel(int paramInt, View paramView, Menu paramMenu) {
        return super.onPreparePanel(paramInt, paramView, paramMenu);
    }

    public void proxyOnRestart() {
        super.onRestart();
    }

    public void proxyOnRestoreInstanceState(Bundle paramBundle) {
        super.onRestoreInstanceState(paramBundle);
    }

    public void proxyOnResume() {
        super.onResume();
    }

    public Object proxyOnRetainNonConfigurationInstance() {
        return super.onRetainNonConfigurationInstance();
    }

    public void proxyOnSaveInstanceState(Bundle paramBundle) {
        super.onSaveInstanceState(paramBundle);
    }

    public boolean proxyOnSearchRequested() {
        return super.onSearchRequested();
    }

    public void proxyOnStart() {
        super.onStart();
    }

    public void proxyOnStop() {
        super.onStop();
    }

    public void proxyOnTitleChanged(CharSequence paramCharSequence, int paramInt) {
        super.onTitleChanged(paramCharSequence, paramInt);
    }

    public boolean proxyOnTouchEvent(MotionEvent paramMotionEvent) {
        return super.onTouchEvent(paramMotionEvent);
    }

    public boolean proxyOnTrackballEvent(MotionEvent paramMotionEvent) {
        return super.onTrackballEvent(paramMotionEvent);
    }

    public void proxyOnUserInteraction() {
        super.onUserInteraction();
    }

    public void proxyOnWindowAttributesChanged(WindowManager.LayoutParams paramLayoutParams) {
        super.onWindowAttributesChanged(paramLayoutParams);
    }

    public void proxyOnWindowFocusChanged(boolean paramBoolean) {
        super.onWindowFocusChanged(paramBoolean);
    }

    public void proxyOpenContextMenu(View paramView) {
        super.openContextMenu(paramView);
    }

    public void proxyOpenOptionsMenu() {
        super.openOptionsMenu();
    }

    public void proxyOverridePendingTransition(int paramInt1, int paramInt2) {
        super.overridePendingTransition(paramInt1, paramInt2);
    }

    public void proxyRegisterForContextMenu(View paramView) {
        super.registerForContextMenu(paramView);
    }

    public void proxySetContentView(int paramInt) {
        super.setContentView(paramInt);
    }

    public void proxySetContentView(View paramView) {
        super.setContentView(paramView);
    }

    public void proxySetContentView(View paramView, ViewGroup.LayoutParams paramLayoutParams) {
        super.setContentView(paramView, paramLayoutParams);
    }

    public void proxySetIntent(Intent paramIntent) {
        super.setIntent(paramIntent);
    }

    public void proxySetRequestedOrientation(int paramInt) {
        super.setRequestedOrientation(paramInt);
    }

    public void proxySetTitle(int paramInt) {
        super.setTitle(paramInt);
    }

    public void proxySetTitle(CharSequence paramCharSequence) {
        super.setTitle(paramCharSequence);
    }

    public void proxySetTitleColor(int paramInt) {
        super.setTitleColor(paramInt);
    }

    public void proxySetVisible(boolean paramBoolean) {
        super.setVisible(paramBoolean);
    }

    public void proxyStartActivity(Intent paramIntent) {
        super.startActivity(paramIntent);
    }

    public void proxyStartActivityForResult(Intent paramIntent, int paramInt) {
        super.startActivityForResult(paramIntent, paramInt);
    }

    public void proxyStartActivityFromChild(Activity paramActivity, Intent paramIntent, int paramInt) {
        super.startActivityFromChild(paramActivity, paramIntent, paramInt);
    }

    public boolean proxyStartActivityIfNeeded(Intent paramIntent, int paramInt) {
        return super.startActivityIfNeeded(paramIntent, paramInt);
    }

    public void proxyStartIntentSender(IntentSender paramIntentSender, Intent paramIntent, int paramInt1,
            int paramInt2, int paramInt3) throws IntentSender.SendIntentException {
        super.startIntentSender(paramIntentSender, paramIntent, paramInt1, paramInt2, paramInt3);
    }

    public void proxyStartIntentSenderForResult(IntentSender paramIntentSender, int paramInt1, Intent paramIntent,
            int paramInt2, int paramInt3, int paramInt4) throws IntentSender.SendIntentException {
        super.startIntentSenderForResult(paramIntentSender, paramInt1, paramIntent, paramInt2, paramInt3, paramInt4);
    }

    public void proxyStartIntentSenderFromChild(Activity paramActivity, IntentSender paramIntentSender, int paramInt1,
            Intent paramIntent, int paramInt2, int paramInt3, int paramInt4) throws IntentSender.SendIntentException {
        super.startIntentSenderFromChild(paramActivity, paramIntentSender, paramInt1, paramIntent, paramInt2,
                paramInt3, paramInt4);
    }

    public void proxyStartManagingCursor(Cursor paramCursor) {
        super.startManagingCursor(paramCursor);
    }

    public boolean proxyStartNextMatchingActivity(Intent paramIntent) {
        return super.startNextMatchingActivity(paramIntent);
    }

    public void proxyStartSearch(String paramString, boolean paramBoolean1, Bundle paramBundle, boolean paramBoolean2) {
        super.startSearch(paramString, paramBoolean1, paramBundle, paramBoolean2);
    }

    public ComponentName proxyStartService(Intent paramIntent) {
        return super.startService(paramIntent);
    }

    public void proxyStopManagingCursor(Cursor paramCursor) {
        super.stopManagingCursor(paramCursor);
    }

    public boolean proxyStopService(Intent paramIntent) {
        return super.stopService(paramIntent);
    }

    public void proxyTakeKeyEvents(boolean paramBoolean) {
        super.takeKeyEvents(paramBoolean);
    }

    public void proxyUnregisterForContextMenu(View paramView) {
        super.unregisterForContextMenu(paramView);
    }

    public void registerForContextMenu(View paramView) {
        if (target != null) {
            target.registerForContextMenu(paramView);
        } else {
            super.registerForContextMenu(paramView);
        }
    }

    public void setContentView(int paramInt) {
        if (target != null) {
            target.setContentView(paramInt);
        } else {
            super.setContentView(paramInt);
        }
    }

    public void setContentView(View paramView) {
        if (target != null) {
            target.setContentView(paramView);
        } else {
            super.setContentView(paramView);
        }
    }

    public void setContentView(View paramView, ViewGroup.LayoutParams paramLayoutParams) {
        if (target != null) {
            target.setContentView(paramView, paramLayoutParams);
        } else {
            super.setContentView(paramView, paramLayoutParams);
        }
    }

    public void setIntent(Intent paramIntent) {
        if (target != null) {
            target.setIntent(paramIntent);
        } else {
            super.setIntent(paramIntent);
        }
    }

    public void setRequestedOrientation(int paramInt) {
        if (target != null) {
            target.setRequestedOrientation(paramInt);
        } else {
            super.setRequestedOrientation(paramInt);
        }
    }

    @Override
    public Theme getTheme() {
        if (target != null) {
            return target.getTheme();
        } else {
            return super.getTheme();
        }
    }

    @Override
    public void setTheme(int resid) {
        if (target != null) {
            target.setTheme(resid);
        } else {
            super.setTheme(resid);
        }
    }

    public void setTitle(int paramInt) {
        if (target != null) {
            target.setTitle(paramInt);
        } else {
            super.setTitle(paramInt);
        }
    }

    public void setTitle(CharSequence paramCharSequence) {
        if (target != null) {
            target.setTitle(paramCharSequence);
        } else {
            super.setTitle(paramCharSequence);
        }
    }

    public void setTitleColor(int paramInt) {
        if (target != null) {
            target.setTitleColor(paramInt);
        } else {
            super.setTitleColor(paramInt);
        }
    }

    public void setVisible(boolean paramBoolean) {
        if (target != null) {
            target.setVisible(paramBoolean);
        } else {
            super.setVisible(paramBoolean);
        }
    }

    public void startActivity(Intent paramIntent) {
        if (target != null) {
            target.startActivity(paramIntent);
        } else {
            super.startActivity(paramIntent);
        }
    }

    public void startActivityForResult(Intent paramIntent, int paramInt) {
        if (target != null) {
            target.startActivityForResult(paramIntent, paramInt);
        } else {
            super.startActivityForResult(paramIntent, paramInt);
        }
    }
    
    @SuppressLint("NewApi")
	public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
    	if (target != null) {
            target.startActivityForResult(intent, requestCode,options);
        } else {
            super.startActivityForResult(intent, requestCode,options);
        }
    }

    public void startActivityFromChild(Activity paramActivity, Intent paramIntent, int paramInt) {
        if (target != null) {
            target.startActivityFromChild(paramActivity, paramIntent, paramInt);
        } else {
            super.startActivityFromChild(paramActivity, paramIntent, paramInt);
        }
    }

    public boolean startActivityIfNeeded(Intent paramIntent, int paramInt) {
        if (target != null) {
            return target.startActivityIfNeeded(paramIntent, paramInt);
        } else {
            return super.startActivityIfNeeded(paramIntent, paramInt);
        }
    }

    public void startIntentSender(IntentSender paramIntentSender, Intent paramIntent, int paramInt1, int paramInt2,
            int paramInt3) throws IntentSender.SendIntentException {
        if (target != null) {
            target.startIntentSender(paramIntentSender, paramIntent, paramInt1, paramInt2, paramInt3);
        } else {
            super.startIntentSender(paramIntentSender, paramIntent, paramInt1, paramInt2, paramInt3);
        }
    }

    public void startIntentSenderForResult(IntentSender paramIntentSender, int paramInt1, Intent paramIntent,
            int paramInt2, int paramInt3, int paramInt4) throws IntentSender.SendIntentException {
        if (target != null) {
            target.startIntentSenderForResult(paramIntentSender, paramInt1, paramIntent, paramInt2, paramInt3,
                    paramInt4);
        } else {
            super.startIntentSenderForResult(paramIntentSender, paramInt1, paramIntent, paramInt2, paramInt3, paramInt4);
        }
    }

    public void startManagingCursor(Cursor paramCursor) {
        if (target != null) {
            target.startManagingCursor(paramCursor);
        } else {
            super.startManagingCursor(paramCursor);
        }
    }

    public boolean startNextMatchingActivity(Intent paramIntent) {
        CMActivity localIASFragmentActivity = target;
        if (localIASFragmentActivity != null) {
            return localIASFragmentActivity.startNextMatchingActivity(paramIntent);
        }
        return super.startNextMatchingActivity(paramIntent);
    }

    public void startSearch(String paramString, boolean paramBoolean1, Bundle paramBundle, boolean paramBoolean2) {
        if (target != null) {
            target.startSearch(paramString, paramBoolean1, paramBundle, paramBoolean2);
        } else {
            super.startSearch(paramString, paramBoolean1, paramBundle, paramBoolean2);
        }
    }

    public ComponentName startService(Intent paramIntent) {
        if (target != null) {
            return target.startService(paramIntent);
        } else {
            return super.startService(paramIntent);
        }
    }

    @SuppressWarnings("deprecation")
	public void stopManagingCursor(Cursor paramCursor) {
        if (target != null) {
            target.stopManagingCursor(paramCursor);
        } else {
            super.stopManagingCursor(paramCursor);
        }
    }

    public boolean stopService(Intent paramIntent) {
        if (target != null) {
            return target.stopService(paramIntent);
        } else {
            return super.stopService(paramIntent);
        }
    }

    public void takeKeyEvents(boolean paramBoolean) {
        if (target != null) {
            target.takeKeyEvents(paramBoolean);
        } else {
            super.takeKeyEvents(paramBoolean);
        }
    }

    public void unregisterForContextMenu(View paramView) {
        if (target != null) {
            target.unregisterForContextMenu(paramView);
        } else {
            super.unregisterForContextMenu(paramView);
        }
    }

    public CMActivity getCMActivity() {
        return target;
    }

    @SuppressLint("NewApi")
	@Override
    public void proxysetFinishOnTouchOutside(boolean finish) {
        super.setFinishOnTouchOutside(finish);
    }

    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        // TODO Auto-generated method stub
        return super.registerReceiver(receiver, filter);
    }

    public void unregisterReceiver(BroadcastReceiver receiver) {
        // TODO Auto-generated method stub
        super.unregisterReceiver(receiver);
    }

    @Override
    public CMActivity getTarget() {
        return target;
    }

    @Override
    public SharedPreferences proxyGetSharedPreferences(String name, int mode) {
        return super.getSharedPreferences(name, mode);
    }

    @Override
    public Context getApplicationContext() {
        if (target != null) {
            return target.getApplicationContext();
        } else {
            return super.getApplicationContext();
        }
    }

    @Override
    public Context proxyGetApplicationContext() {
        return super.getApplicationContext();
    } // ActivityProxy END

    @Override
    public FragmentManager getSupportFragmentManager() {
        if (target != null) {
            return target.getSupportFragmentManager();
        } else {
            return super.getSupportFragmentManager();
        }
    }

    @Override
    public FragmentManager proxyGetSupportFragmentManager() {
        return super.getSupportFragmentManager();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (target != null) {
            target.onAttachFragment(fragment);
        } else {
            super.onAttachFragment(fragment);
        }
    }

    @Override
    public void proxyOnAttachFragment(Fragment paramFragment) {
        super.onAttachFragment(paramFragment);
    }

    @Override
    public void startActivityFromFragment(Fragment fragment, Intent intent, int requestCode) {
        if (target != null) {
            target.startActivityFromFragment(fragment, intent, requestCode);
        } else {
            super.startActivityFromFragment(fragment, intent, requestCode);
        }
    }

    @Override
    public void proxyStartActivityFromFragment(Fragment paramFragment, Intent paramIntent, int paramInt) {
        super.startActivityFromFragment(paramFragment, paramIntent, paramInt);
    }

	@Override
	public Looper getProxyMainLooper() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public View proxyOnCreateView(String paramString, Context paramContext,
			AttributeSet paramAttributeSet) {
		// TODO Auto-generated method stub
		return super.onCreateView(paramString, paramContext, paramAttributeSet);
	}

	@SuppressLint("NewApi")
	@Override
	public View proxyOnCreateView(View parent,String paramString, Context paramContext,
			AttributeSet paramAttributeSet) {
		// TODO Auto-generated method stub
		return super.onCreateView(parent,paramString, paramContext, paramAttributeSet);
	}

	@SuppressLint("NewApi")
	@Override
	public void proxyStartActivityForResult(Intent paramIntent, int paramInt,
			Bundle options) {
		// TODO Auto-generated method stub
		super.startActivityForResult(paramIntent, paramInt, options);
	}

	@SuppressLint("NewApi")
	@Override
	public android.app.FragmentManager getProxyFragmentManager() {
		// TODO Auto-generated method stub
		return super.getFragmentManager();
	}

	@Override
	public boolean proxyonCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		return super.onCreateOptionsMenu(menu);
	}
	
	
}
