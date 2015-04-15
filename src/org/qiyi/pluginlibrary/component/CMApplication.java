package org.qiyi.pluginlibrary.component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.qiyi.pluginlibrary.ProxyEnvironment;
import org.qiyi.pluginlibrary.proxy.PackageMangerProxy;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.view.Display;

/**
 * 插件的application
 */
public class CMApplication extends Application {

    /** TAG */
    public static final String TAG = "PluginApplication";

    /** application代理 （host的application） */
    private Application applicationProxy;
    /** 插件应用的包名 */
    private String mTargetPacakgeName = null;

    /**
     * 设置host的application
     * 
     * @param proxy host application
     */
    public void setApplicationProxy(Application proxy) {
        applicationProxy = proxy;

        // 兼容 联想K860 4.0手机上的crash问题，
        // 这个手机的dialog创建过程会访问application的mLoadedApk，不设置会抛空指针异常 
        Field loadedApkField = null;
        try {
            loadedApkField = proxy.getClass().getField("mLoadedApk");
            Object obj = loadedApkField.get(proxy);
            loadedApkField.set(this, obj);
        } catch (NoSuchFieldException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }

        // 拷贝一份自己的BaseContext，OuterContext设置为Aplication
        Context context = null;
        try {
            Class<?> contextImpl = Class.forName("android.app.ContextImpl");
            try {
                Constructor<?> constructor = contextImpl.getConstructor(new Class<?>[] { contextImpl });
                constructor.setAccessible(true);
                context = (Context) constructor.newInstance(proxy.getBaseContext());
            } catch (NoSuchMethodException e) {

                // 4.4.4 Rom上ContextImpl的构造方法已经没有了，只有静态方法createAppContext
                Class<?> activityThread = Class.forName("android.app.ActivityThread");
                Class<?> loadedApk = Class.forName("android.app.LoadedApk");
                Field mainThread = contextImpl.getDeclaredField("mMainThread");
                mainThread.setAccessible(true);
                Method ctxCreator = contextImpl.getDeclaredMethod("createAppContext", new Class<?>[] { activityThread,
                        loadedApk });
                ctxCreator.setAccessible(true);
                context = (Context) ctxCreator.invoke(contextImpl,
                        new Object[] { mainThread.get(proxy.getBaseContext()), loadedApkField.get(proxy) });
            }
            Method setOutCtx = contextImpl.getDeclaredMethod("setOuterContext", new Class<?>[] { Context.class });
            setOutCtx.setAccessible(true);
            setOutCtx.invoke(context, this);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        if (PluginDebugLog.isDebug()) {
            PluginDebugLog.log(TAG, "--- BaseContext : " + context);
        }

        if (context == null) {
            if (PluginDebugLog.isDebug()) {
            	PluginDebugLog.log(TAG, "*** Oops, BaseContext create fail! ");
            }
            context = proxy.getBaseContext();
        }

        attachBaseContext(context);
    }

    /**
     * @return the applicationProxy
     */
    public Application getApplicationProxy() {
        return applicationProxy;
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public Resources getResources() {
        ProxyEnvironment localProxyEnvironment = ProxyEnvironment.getInstance(mTargetPacakgeName);
        if (localProxyEnvironment.getTargetResources() != null) {
            return localProxyEnvironment.getTargetResources();
        }
        return applicationProxy.getResources();
    }

    /** 上次获取的PacakageManager，免得每次都获取 */
    private PackageManager lastPm;
    /** 真正的ProxyManager */
    private PackageMangerProxy proxyPm;

    @Override
    public PackageManager getPackageManager() {
        PackageManager pm = applicationProxy.getPackageManager();
        if (pm != lastPm && pm != null) {
            lastPm = pm;
            proxyPm = new PackageMangerProxy(pm);
            proxyPm.setPackageName(getPackageName());
            proxyPm.setTargetPackageName(mTargetPacakgeName);
        }

        return proxyPm;
    }

    /**
     * 设置插件包名
     * 
     * @param packageName
     *            包名
     */
    public void setTargetPackageName(String packageName) {
        mTargetPacakgeName = packageName;
    }

    /**
     * 获取插件包名
     * 
     * @return 包名
     */
    public String getTargetPackageName() {
        return mTargetPacakgeName;
    }

    @Override
    public String getPackageName() {
        return applicationProxy.getPackageName();
    }

    @Override
    public Object getSystemService(String name) {
        /*
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            return mBaseActivity.getLayoutInflater();
        }*/
        return super.getSystemService(name);
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        // TODO 需要判断是否要改变data路径，先加前缀
        if (ProxyEnvironment.getInstance(mTargetPacakgeName).isDataNeedPrefix()) {
            name = mTargetPacakgeName + "_" + name;
        }
        return applicationProxy.getSharedPreferences(name, mode);
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return applicationProxy.bindService(service, conn, flags);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return super.registerReceiver(receiver, filter);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission,
            Handler scheduler) {
        return super.registerReceiver(receiver, filter, broadcastPermission, scheduler);
    }

    @Override
    public int checkCallingOrSelfPermission(String permission) {
        return applicationProxy.checkCallingOrSelfPermission(permission);
    }

    @Override
    public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
        return applicationProxy.checkCallingOrSelfUriPermission(uri, modeFlags);
    }

    @Override
    public int checkCallingPermission(String permission) {
        return applicationProxy.checkCallingPermission(permission);
    }

    @Override
    public int checkCallingUriPermission(Uri uri, int modeFlags) {
        return applicationProxy.checkCallingUriPermission(uri, modeFlags);
    }

    @Override
    public int checkPermission(String permission, int pid, int uid) {
        return applicationProxy.checkPermission(permission, pid, uid);
    }

    @Override
    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
        return applicationProxy.checkUriPermission(uri, pid, uid, modeFlags);
    }

    @Override
    public int checkUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid,
            int modeFlags) {
        return applicationProxy.checkUriPermission(uri, readPermission, writePermission, pid, uid, modeFlags);
    }

    @Override
    public void clearWallpaper() throws IOException {
        applicationProxy.clearWallpaper();
    }

    @Override
    public Context getBaseContext() {
        return applicationProxy.getBaseContext();
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    @Override
    public ContentResolver getContentResolver() {
        return applicationProxy.getContentResolver();
    }

    @Override
    public Looper getMainLooper() {
        return applicationProxy.getMainLooper();
    }

    @Override
    public void setTheme(int resid) {
        applicationProxy.setTheme(resid);
    }

    @Override
    public Theme getTheme() {
        ProxyEnvironment localProxyEnvironment = ProxyEnvironment.getInstance(mTargetPacakgeName);
        Theme theme = localProxyEnvironment.getTargetTheme();
        if (theme != null) {
            return theme;
        } else {
            return applicationProxy.getTheme();
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return applicationProxy.getClassLoader();
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return applicationProxy.getApplicationInfo();
    }

    @Override
    public String getPackageResourcePath() {
        return applicationProxy.getPackageResourcePath();
    }

    @Override
    public String getPackageCodePath() {
        return applicationProxy.getPackageCodePath();
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        // TODO 加前缀导致as下载到手机内存bug，需要整体实现改变数据路径
        // name = mTargetPacakgeName + "_" + name;
        return applicationProxy.openFileInput(name);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        // TODO 加前缀导致as下载到手机内存bug，需要整体实现改变数据路径
        // name = mTargetPacakgeName + "_" + name;
        return applicationProxy.openFileOutput(name, mode);
    }

    @Override
    public boolean deleteFile(String name) {
        return applicationProxy.deleteFile(name);
    }

    @Override
    public File getFileStreamPath(String name) {
        /*
        if (ProxyEnvironment.getInstance(mTargetPacakgeName).isDataNeedPrefix()) {
            name = mTargetPacakgeName + "_" + name;
        }*/
        return applicationProxy.getFileStreamPath(name);
    }

    @Override
    public String[] fileList() {
        return applicationProxy.fileList();
    }

    @Override
    public File getFilesDir() {
        return applicationProxy.getFilesDir();
    }

    @Override
    public File getCacheDir() {
        return applicationProxy.getCacheDir();
    }

    @Override
    public File getDir(String name, int mode) {
        return applicationProxy.getDir(name, mode);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {
        if (ProxyEnvironment.getInstance(mTargetPacakgeName).isDataNeedPrefix()) {
            name = mTargetPacakgeName + "_" + name;
        }
        return applicationProxy.openOrCreateDatabase(name, mode, factory);
    }

    @Override
    public boolean deleteDatabase(String name) {
        return applicationProxy.deleteDatabase(name);
    }

    @Override
    public File getDatabasePath(String name) {
        if (ProxyEnvironment.getInstance(mTargetPacakgeName).isDataNeedPrefix()) {
            name = mTargetPacakgeName + "_" + name;
        }
        return applicationProxy.getDatabasePath(name);
    }

    @Override
    public String[] databaseList() {
        return applicationProxy.databaseList();
    }

    @Override
    public Drawable getWallpaper() {
        return applicationProxy.getWallpaper();
    }

    @Override
    public Drawable peekWallpaper() {
        return applicationProxy.peekWallpaper();
    }

    @Override
    public int getWallpaperDesiredMinimumWidth() {
        return applicationProxy.getWallpaperDesiredMinimumWidth();
    }

    @Override
    public int getWallpaperDesiredMinimumHeight() {
        return applicationProxy.getWallpaperDesiredMinimumHeight();
    }

    @Override
    public void setWallpaper(Bitmap bitmap) throws IOException {
        applicationProxy.setWallpaper(bitmap);
    }

    @Override
    public void setWallpaper(InputStream data) throws IOException {
        applicationProxy.setWallpaper(data);
    }

    @Override
    public void startActivity(Intent intent) {
        ProxyEnvironment.getInstance(mTargetPacakgeName).remapStartActivityIntent(intent);
        applicationProxy.startActivity(intent);
    }

    @Override
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues,
            int extraFlags) throws SendIntentException {
        applicationProxy.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags);
    }

    @Override
    public void sendBroadcast(Intent intent) {
        ProxyEnvironment.getInstance(mTargetPacakgeName).remapReceiverIntent(intent);
        applicationProxy.sendBroadcast(intent);
    }

    @Override
    public void sendBroadcast(Intent intent, String receiverPermission) {
        ProxyEnvironment.getInstance(mTargetPacakgeName).remapReceiverIntent(intent);
        applicationProxy.sendBroadcast(intent, receiverPermission);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
        applicationProxy.sendOrderedBroadcast(intent, receiverPermission);
    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver,
            Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        applicationProxy.sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode,
                initialData, initialExtras);
    }

    @Override
    public void sendStickyBroadcast(Intent intent) {
        applicationProxy.sendStickyBroadcast(intent);
    }

    @Override
    public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, Handler scheduler,
            int initialCode, String initialData, Bundle initialExtras) {
        applicationProxy.sendStickyOrderedBroadcast(intent, resultReceiver, scheduler, initialCode, initialData,
                initialExtras);
    }

    @Override
    public void removeStickyBroadcast(Intent intent) {
        applicationProxy.removeStickyBroadcast(intent);
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        super.unregisterReceiver(receiver);
    }

    /**
     * TODO: root下载安装,第一次安装成功,第二次安装失败,发现用applicationProxy service没有起来.
     * 这个问题比较诡异.用activity的验证没问题
     */
    @Override
    public ComponentName startService(Intent service) {
        ProxyEnvironment.getInstance(mTargetPacakgeName).remapStartServiceIntent(service);
        return applicationProxy.startService(service);
    }

    @Override
    public boolean stopService(Intent name) {
        ProxyEnvironment.getInstance(mTargetPacakgeName).remapStartServiceIntent(name);
        return applicationProxy.stopService(name);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        applicationProxy.unbindService(conn);
    }

    @Override
    public boolean startInstrumentation(ComponentName className, String profileFile, Bundle arguments) {
        return applicationProxy.startInstrumentation(className, profileFile, arguments);
    }

    @Override
    public void enforcePermission(String permission, int pid, int uid, String message) {
        applicationProxy.enforcePermission(permission, pid, uid, message);
    }

    @Override
    public void enforceCallingPermission(String permission, String message) {
        applicationProxy.enforceCallingPermission(permission, message);
    }

    @Override
    public void enforceCallingOrSelfPermission(String permission, String message) {
        applicationProxy.enforceCallingOrSelfPermission(permission, message);
    }

    @Override
    public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {
        applicationProxy.grantUriPermission(toPackage, uri, modeFlags);
    }

    @Override
    public void revokeUriPermission(Uri uri, int modeFlags) {
        applicationProxy.revokeUriPermission(uri, modeFlags);
    }

    @Override
    public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags, String message) {
        applicationProxy.enforceUriPermission(uri, pid, uid, modeFlags, message);
    }

    @Override
    public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {
        applicationProxy.enforceCallingUriPermission(uri, modeFlags, message);
    }

    @Override
    public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {
        applicationProxy.enforceCallingOrSelfUriPermission(uri, modeFlags, message);
    }

    @Override
    public void enforceUriPermission(Uri uri, String readPermission, String writePermission, int pid, int uid,
            int modeFlags, String message) {
        applicationProxy.enforceUriPermission(uri, readPermission, writePermission, pid, uid, modeFlags, message);
    }

    @Override
    public Context createPackageContext(String packageName, int flags) throws NameNotFoundException {
        return applicationProxy.createPackageContext(packageName, flags);
    }

    @Override
    public boolean isRestricted() {
        return applicationProxy.isRestricted();
    }

    /**
     * @param name
     * @param mode
     * @param factory
     * @param errorHandler
     * @return
     * api 11 新增接口 调用时请注意
     */
    @Override
    @TargetApi(11)
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory,
            DatabaseErrorHandler errorHandler) {
        if (ProxyEnvironment.getInstance(mTargetPacakgeName).isDataNeedPrefix()) {
            name = mTargetPacakgeName + "_" + name;
        }
        return applicationProxy.openOrCreateDatabase(name, mode, factory, errorHandler);
    }

    /**
     * @param callback
     *  api 14 
     */
    @Override
    @TargetApi(14)
    public void registerComponentCallbacks(ComponentCallbacks callback) {
        applicationProxy.registerComponentCallbacks(callback);
    }

    @Override
    @TargetApi(14)
    public void unregisterComponentCallbacks(ComponentCallbacks callback) {
        applicationProxy.unregisterComponentCallbacks(callback);
    }

    @Override
    @TargetApi(14)
    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        applicationProxy.registerActivityLifecycleCallbacks(callback);
    }

    @Override
    @TargetApi(14)
    public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        applicationProxy.unregisterActivityLifecycleCallbacks(callback);
    }

    @Override
    @TargetApi(17)
    public Context createConfigurationContext(Configuration overrideConfiguration) {
        return applicationProxy.createConfigurationContext(overrideConfiguration);
    }

    @Override
    @TargetApi(17)
    public Context createDisplayContext(Display display) {
        return applicationProxy.createDisplayContext(display);
    }

    @Override
    public File getExternalFilesDir(String type) {
        return applicationProxy.getExternalFilesDir(type);
    }

    @Override
    @TargetApi(11)
    public File getObbDir() {
        return applicationProxy.getObbDir();
    }

    @Override
    public File getExternalCacheDir() {
        return applicationProxy.getExternalCacheDir();
    }

    @Override
    @TargetApi(17)
    public void removeStickyBroadcastAsUser(Intent intent, UserHandle user) {
        applicationProxy.removeStickyBroadcastAsUser(intent, user);
    }

    @Override
    @TargetApi(17)
    public void sendBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission) {
        applicationProxy.sendBroadcastAsUser(intent, user, receiverPermission);
    }

    @Override
    @TargetApi(17)
    public void sendBroadcastAsUser(Intent intent, UserHandle user) {
        applicationProxy.sendBroadcastAsUser(intent, user);
    }

    @Override
    @TargetApi(17)
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission,
            BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData,
            Bundle initialExtras) {
        applicationProxy.sendOrderedBroadcastAsUser(intent, user, receiverPermission, resultReceiver, scheduler,
                initialCode, initialData, initialExtras);
    }

    @Override
    @TargetApi(17)
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {
        applicationProxy.sendStickyBroadcastAsUser(intent, user);
    }

    @Override
    @TargetApi(17)
    public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver,
            Handler scheduler, int initialCode, String initialData, Bundle initialExtras) {
        applicationProxy.sendStickyOrderedBroadcastAsUser(intent, user, resultReceiver, scheduler, initialCode,
                initialData, initialExtras);
    }

	/**
	 * 使用此方法请注意 api 11 新增方法。低版本需要自己实现
	 * @param intents
	 * @param options
	 */
	@Override
	@TargetApi(16)
    public void startActivities(Intent[] intents, Bundle options) {
		if(Build.VERSION.SDK_INT >=  16){
    		applicationProxy.startActivities(intents, options);
    	}
    }

    @Override
    @TargetApi(11)
    public void startActivities(Intent[] intents) {
    	if(Build.VERSION.SDK_INT >=  11){
    		applicationProxy.startActivities(intents);
    	}
    }

    @Override
    @TargetApi(16)
    public void startActivity(Intent intent, Bundle options) {
    	if(Build.VERSION.SDK_INT >=  16){
    		applicationProxy.startActivity(intent, options);
    	}
    }

    @Override
    @TargetApi(16)
    public void startIntentSender(IntentSender intent, Intent fillInIntent, int flagsMask, int flagsValues,
            int extraFlags, Bundle options) throws SendIntentException {
    	if(Build.VERSION.SDK_INT >=  16){
    		applicationProxy.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags, options);
    	}
    }
    

}
