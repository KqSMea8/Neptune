package org.qiyi.pluginlibrary.context;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;

import org.qiyi.pluginlibrary.PServiceSupervisor;
import org.qiyi.pluginlibrary.PluginServiceWrapper;
import org.qiyi.pluginlibrary.constant.IIntentConstant;
import org.qiyi.pluginlibrary.plugin.InterfaceToGetHost;
import org.qiyi.pluginlibrary.plugin.TargetMapping;
import org.qiyi.pluginlibrary.runtime.PluginLoadedApk;
import org.qiyi.pluginlibrary.utils.ComponetFinder;
import org.qiyi.pluginlibrary.utils.ContextUtils;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.ReflectionUtils;
import org.qiyi.pluginlibrary.utils.ResourcesToolForPlugin;
import org.qiyi.pluginlibrary.utils.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class CustomContextWrapper extends ContextWrapper implements InterfaceToGetHost {
    private static final String TAG = "CustomContextWrapper";

    private static final String S_SHARED_PREFS =
            ContextUtils.isAndroidN() || ContextUtils.isAndroidO() ? "sSharedPrefsCache" : "sSharedPrefs";

    protected static ConcurrentMap<String, Vector<Method>> sMethods = new ConcurrentHashMap<String, Vector<Method>>(2);

    protected ApplicationInfo mApplicationInfo = null;

    public CustomContextWrapper(Context base) {
        super(base);
    }

    @Override
    public ClassLoader getClassLoader() {
        return getPluginLoadedApk().getPluginClassLoader();
    }

    @Override
    public Context getApplicationContext() {
        return getPluginLoadedApk().getPluginApplication();
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        if (mApplicationInfo == null) {
            mApplicationInfo = super.getApplicationInfo();
            PluginLoadedApk mLoadedApk = getPluginLoadedApk();
            if (mLoadedApk != null && getPluginMapping() != null) {
                TargetMapping targetMapping = getPluginMapping();
                if (targetMapping.usePluginApplicationInfo()) {
                    mApplicationInfo.dataDir = targetMapping.getDataDir();
                    PluginDebugLog.log(TAG, "change data dir: " + mApplicationInfo.dataDir);
                    mApplicationInfo.nativeLibraryDir = targetMapping.getnativeLibraryDir();
                    PluginDebugLog.log(TAG, "change native lib path: " +
                            mApplicationInfo.nativeLibraryDir);
                }
            }
        }
        return mApplicationInfo;
    }

    @Override
    public Resources getResources() {
        return getPluginLoadedApk().getPluginResource();
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    @Override
    public ComponentName startService(Intent service) {
        PluginDebugLog.log(getLogTag(), "startService: " + service);
        PluginLoadedApk mLoadedApk = getPluginLoadedApk();
        if (mLoadedApk != null) {
            ComponetFinder.findSuitableServiceByIntent(mLoadedApk,service);
        }
        return super.startService(service);
    }

    @Override
    public boolean stopService(Intent name) {
        PluginDebugLog.log(getLogTag(), "stopService: " + name);
        PluginLoadedApk mLoadedApk = getPluginLoadedApk();
        if (mLoadedApk != null) {
            String actServiceClsName = "";
            if(name.getComponent() != null){
                actServiceClsName = name.getComponent().getClassName();
            }else{
                ServiceInfo mServiceInfo = getPluginMapping().resolveService(name);
                if(mServiceInfo != null){
                    actServiceClsName = mServiceInfo.name;
                }
            }

            PluginServiceWrapper plugin = PServiceSupervisor
                    .getServiceByIdentifer(PluginServiceWrapper.getIndeitfy(getPluginPackageName(), actServiceClsName));
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
        PluginDebugLog.log(getLogTag(), "bindService: " + service);
        PluginLoadedApk mLoadedApk = getPluginLoadedApk();
        if (mLoadedApk != null) {
            ComponetFinder.findSuitableServiceByIntent(mLoadedApk,service);
        }
        if (conn != null) {
            if (mLoadedApk != null && service != null) {
                String serviceClass = service.getStringExtra(IIntentConstant.EXTRA_TARGET_CLASS_KEY);
                String packageName = mLoadedApk.getPluginPackageName();
                if (!TextUtils.isEmpty(serviceClass) && !TextUtils.isEmpty(packageName)) {
                    PServiceSupervisor.addServiceConnectionByIdentifer(packageName + "." + serviceClass, conn);
                }
            }
        }
        return super.bindService(service, conn, flags);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
        PServiceSupervisor.removeServiceConnection(conn);
        PluginDebugLog.log(getLogTag(), "unbindService: " + conn);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(ComponetFinder.findSuitableActivityByIntent(getPluginPackageName(), intent, -1, this));
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        super.startActivity(ComponetFinder.findSuitableActivityByIntent(getPluginPackageName(), intent, -1, this), options);
    }

    @Override
    public File getFilesDir() {
        File superFile = super.getFilesDir();
        PluginLoadedApk mLoadedApk = getPluginLoadedApk();
        if (mLoadedApk == null) {
            return superFile;
        }

        File fileDir = new File(getPluginMapping().getDataDir() + "/files/");
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        return mLoadedApk.getPluginAssetManager() == null ? superFile : fileDir;
    }

    @Override
    public File getCacheDir() {
        PluginLoadedApk mLoadedApk = getPluginLoadedApk();
        if (mLoadedApk == null) {
            return super.getCacheDir();
        }
        File cacheDir = new File(getPluginMapping().getDataDir() + "/cache/");
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        return mLoadedApk.getPluginAssetManager() == null ? super.getCacheDir() : cacheDir;
    }

    @Override
    public File getFileStreamPath(String name) {
        PluginLoadedApk mLoadedApk = getPluginLoadedApk();
        if (mLoadedApk == null) {
            return super.getFilesDir();
        }
        File file = new File(getPluginMapping().getDataDir() + "/files/" + name);
        return mLoadedApk.getPluginAssetManager() == null ? super.getFileStreamPath(name) : file;
    }

    @Override
    public File getDir(String name, int mode) {
        PluginLoadedApk mLoadedApk = getPluginLoadedApk();
        if (mLoadedApk == null) {
            return super.getFilesDir();
        }
        File fileDir = new File(getPluginMapping().getDataDir() + "/app_" + name + "/");
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        return mLoadedApk.getPluginAssetManager() == null ? super.getDir(name, mode) : fileDir;

    }

    @Override
    public File getDatabasePath(String name) {
        PluginLoadedApk mLoadedApk = getPluginLoadedApk();
        File dir;
        File f;
        if (name.charAt(0) == File.separatorChar) {
            String dirPath = name.substring(0, name.lastIndexOf(File.separatorChar));
            dir = new File(dirPath);
            name = name.substring(name.lastIndexOf(File.separatorChar));
            f = new File(dir, name);
            return f;
        } else {
            if (mLoadedApk == null) {
                return super.getDatabasePath(name);
            }
            f = new File(getPluginMapping().getDataDir() + "/databases/" + name);
            if (!f.exists()) {
                f.mkdir();
            }
        }

        return mLoadedApk.getPluginAssetManager() == null ? super.getDatabasePath(name) : f;
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        if (getPluginLoadedApk() == null) {
            return super.openFileInput(name);
        }
        File f = makeFilename(getFilesDir(), name);
        return new FileInputStream(f);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        if (getPluginLoadedApk() == null) {
            return super.openFileOutput(name, mode);
        }
        final boolean append = (mode & MODE_APPEND) != 0;
        File f = makeFilename(getFilesDir(), name);
        try {
            FileOutputStream fos = new FileOutputStream(f, append);
            return fos;
        } catch (FileNotFoundException e) {

        }
        File parent = f.getParentFile();
        parent.mkdir();
        FileOutputStream fos = new FileOutputStream(f, append);
        return fos;
    }

    private File makeFilename(File base, String name) {
        if (name.indexOf(File.separatorChar) < 0) {
            return new File(base, name);
        }
        throw new IllegalArgumentException("File " + name + "contains a path separator");
    }

    @Override
    public boolean deleteFile(String name) {
        PluginLoadedApk mLoadedApk = getPluginLoadedApk();
        if (mLoadedApk == null) {
            return super.deleteFile(name);
        }
        File fileDir = new File(getPluginMapping().getDataDir() + "/files/" + name);
        return mLoadedApk.getPluginAssetManager() == null ? super.deleteFile(name) : fileDir.delete();
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {
        File databaseDir = new File(getPluginMapping().getDataDir() + "/databases/");
        if (!databaseDir.exists()) {
            databaseDir.mkdir();
        }
        // backup database for old version start
        checkBackupDB(name);
        // backup database for old version end
        return super.openOrCreateDatabase(databaseDir.getAbsolutePath() + "/" + name, mode, factory);
    }

    /**
     * this is move DB from /data/data/packageName/database to
     * /data/data/package/app_pluginapp/pluginpackage/databases if the app is
     * upgrade,we need backup and recover the db for user,
     *
     * @param name db name
     */
    private void checkBackupDB(String name) {

        if (name.lastIndexOf(".") == -1) {
            return; // if migrate file is not db file,ignore it.
        }

        String dbName = name.substring(0, name.lastIndexOf("."));

        String dbPath = "/data/data/" + this.getPackageName() + "/databases/";
        File file = new File(dbPath, name);
        if (file.exists()) {
            File targetFile = new File(getPluginMapping().getDataDir() + "/databases/" + name);
            if (!targetFile.exists()) {
                Util.moveFile(file, targetFile);
            }
            File bakFile = new File(dbPath, dbName + ".db-journal");
            File targetBakFile = new File(
                    getPluginMapping().getDataDir() + "/databases/" + dbName + ".db-journal");
            if (bakFile.exists() && !targetBakFile.exists()) {
                Util.moveFile(bakFile, targetBakFile);
            }
        }
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory, DatabaseErrorHandler errorHandler) {
        File databaseDir = new File(getPluginMapping().getDataDir() + "/databases/");
        if (!databaseDir.exists()) {
            databaseDir.mkdir();
        }
        // backup database for old version start
        checkBackupDB(name);
        // backup database for old version end

        return super.openOrCreateDatabase(databaseDir.getAbsolutePath() + "/" + name, mode, factory, errorHandler);
    }

    @Override
    public boolean deleteDatabase(String name) {
        File databaseDir = new File(getPluginMapping().getDataDir() + "/databases/");
        if (!databaseDir.exists()) {
            databaseDir.mkdir();
        }
        return super.deleteDatabase(databaseDir.getAbsolutePath() + "/" + name);
    }

    @Override
    public String[] databaseList() {
        File databaseDir = new File(getPluginMapping().getDataDir() + "/databases/");
        if (!databaseDir.exists()) {
            databaseDir.mkdir();
        }
        return databaseDir.list();
    }

    private File getSharedPrefsFile(String name) {
        File base = null;
        base = new File(getPluginMapping().getDataDir() + "/shared_prefs/");
        if (!base.exists()) {
            base.mkdir();
        }
        return new File(base, name + ".xml");
    }

    private static File makeBackupFile(File prefsFile) {
        return new File(prefsFile.getPath() + ".bak");
    }

    private SharedPreferences getSharedPreferecesForPlugin(String name, int mode) {
        try {
            Object sp = null;
            if (android.os.Build.VERSION.SDK_INT <= 10) {
                // now the plugin don't support 2.3,but if it will support in
                // the furture,we can use this.
                Class<?> SharedPreferencesImpl = Class.forName("android.app.ContextImpl$SharedPreferencesImpl");
                Constructor<?> constructor = SharedPreferencesImpl.getDeclaredConstructor(File.class, int.class);
                constructor.setAccessible(true);
                Class<?> clazz = Class.forName("android.app.ContextImpl");
                Field sSharedPrefs = clazz.getDeclaredField(S_SHARED_PREFS);
                File prefsFile;
                sSharedPrefs.setAccessible(true);
                boolean needInitialLoad = false;
                HashMap<String, Object> oSharedPrefs = (HashMap<String, Object>) sSharedPrefs.get(this.getBaseContext());
                synchronized (oSharedPrefs) {
                    sp = oSharedPrefs.get(name);
                    Method hasFileChangedUnexpectedly = SharedPreferencesImpl.getDeclaredMethod("hasFileChangedUnexpectedly");
                    boolean mHasFileChangedUnexpectedly = (Boolean) hasFileChangedUnexpectedly.invoke(sp);
                    if (sp != null && !mHasFileChangedUnexpectedly) {
                        return (SharedPreferences) sp;
                    }
                    prefsFile = getSharedPrefsFile(name);
                    if (sp == null) {
                        sp = constructor.newInstance(prefsFile, mode);
                        oSharedPrefs.put(name, sp);
                        needInitialLoad = true;
                    }
                }
                synchronized (sp) {
                    Method isLoaded = SharedPreferencesImpl.getDeclaredMethod("isLoaded");
                    boolean isLoadResult = (Boolean) isLoaded.invoke(sp);
                    if (needInitialLoad && isLoadResult) {
                        return (SharedPreferences) sp;
                    }
                    File backup = makeBackupFile(prefsFile);
                    if (backup.exists()) {
                        prefsFile.delete();
                        backup.renameTo(prefsFile);
                    }

                    Map map = null;
                    Class<?> fileUtilsClass = Class.forName("android.os.FileUtils");

                    Class<?> fileStatusClass = Class.forName("android.os.FileUtils$FileStatus");
                    Constructor<?> fileStatusConstructor = fileStatusClass.getConstructor();
                    Object FileStatus = fileStatusConstructor.newInstance();

                    Method getFileStatus = fileUtilsClass.getDeclaredMethod("getFileStatus", String.class, fileStatusClass);
                    boolean getFileStatusResult = (Boolean) getFileStatus.invoke(FileStatus, prefsFile.getPath(), FileStatus);
                    if (getFileStatusResult && prefsFile.canRead()) {
                        try {
                            FileInputStream str = new FileInputStream(prefsFile);
                            Class<?> xmlUtilClass = Class.forName("com.android.internal.util.XmlUtils");
                            map = (Map) xmlUtilClass.getDeclaredMethod("readMapXml", FileInputStream.class)
                                    .invoke(xmlUtilClass.newInstance(), str);
                            str.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    SharedPreferencesImpl.getMethod("replace", Map.class, fileStatusClass).invoke(sp, map, FileStatus);
                }
                return (SharedPreferences) sp;
            } else if (android.os.Build.VERSION.SDK_INT <= 18) {

                Class<?> SharedPreferencesImpl = Class.forName("android.app.SharedPreferencesImpl");
                Constructor<?> constructor = SharedPreferencesImpl.getDeclaredConstructor(File.class, int.class);
                constructor.setAccessible(true);
                HashMap<String, Object> oSharedPrefs = ReflectionUtils.on(this.getBaseContext())
                        .<HashMap<String, Object>> get(S_SHARED_PREFS);
                // HashMap<String, Object> oSharedPrefs = ()
                // JavaCalls.getField(this.getBaseContext(), "sSharedPrefs");

                synchronized (oSharedPrefs) {
                    sp = oSharedPrefs.get(name);
                    if (sp == null) {
                        File prefsFile = getSharedPrefsFile(name);
                        sp = constructor.newInstance(prefsFile, mode);
                        oSharedPrefs.put(name, sp);
                    }
                }
                if ((mode & Context.MODE_MULTI_PROCESS) != 0 || getPluginMapping()
                        .getPackageInfo().applicationInfo.targetSdkVersion < android.os.Build.VERSION_CODES.HONEYCOMB) {
                    ReflectionUtils.on(sp).call("startReloadIfChangedUnexpectedly", sMethods);
                    // JavaCalls.invokeMethod(sp,
                    // "startReloadIfChangedUnexpectedly", null, null);
                }
            } else {
                Class<?> clazz = Class.forName("android.app.ContextImpl");
                Class<?> SharedPreferencesImpl = Class.forName("android.app.SharedPreferencesImpl");
                Constructor<?> constructor = SharedPreferencesImpl.getDeclaredConstructor(File.class, int.class);
                constructor.setAccessible(true);
                // ArrayMap<String, ArrayMap<String, Object>> oSharedPrefs =
                // (ArrayMap<String, ArrayMap<String, Object>>)
                // JavaCalls.getField(this.getBaseContext(), "sSharedPrefs");
                ArrayMap<String, ArrayMap<String, Object>> oSharedPrefs = ReflectionUtils.on(this.getBaseContext())
                        .<ArrayMap<String, ArrayMap<String, Object>>> get(S_SHARED_PREFS);
                synchronized (clazz) {
                    if (oSharedPrefs == null) {
                        oSharedPrefs = new ArrayMap<String, ArrayMap<String, Object>>();
                    }

                    final String packageName = getPackageName();
                    ArrayMap<String, Object> packagePrefs = oSharedPrefs.get(packageName);
                    if (packagePrefs == null) {
                        packagePrefs = new ArrayMap<String, Object>();
                        oSharedPrefs.put(packageName, packagePrefs);
                    }

                    sp = packagePrefs.get(name);
                    if (sp == null) {
                        File prefsFile = getSharedPrefsFile(name);
                        sp = constructor.newInstance(prefsFile, mode);
                        packagePrefs.put(name, sp);
                        return (SharedPreferences) sp;
                    }
                    if ((mode & Context.MODE_MULTI_PROCESS) != 0 || getPluginMapping()
                            .getPackageInfo().applicationInfo.targetSdkVersion < android.os.Build.VERSION_CODES.HONEYCOMB) {
                        ReflectionUtils.on(sp).call("startReloadIfChangedUnexpectedly", sMethods);
                        // JavaCalls.invokeMethod(sp,
                        // "startReloadIfChangedUnexpectedly", null, null);
                    }
                }
            }

            return (SharedPreferences) sp;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
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
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (getPluginLoadedApk() != null && getPluginMapping() != null) {
            backupSharedPreference(name);
            SharedPreferences sp = getSharedPreferecesForPlugin(name, mode);
            if (sp != null) {
                return sp;
            }
        }
        return super.getSharedPreferences(name, mode);
    }

    private void backupSharedPreference(String name) {
        String sharePath = "/data/data/" + this.getPackageName() + "/shared_prefs/";
        File sFile = new File(sharePath);
        String[] fileList = sFile.list();
        if (fileList == null)
            return;
        for (String file : fileList) {
            if (file != null && (file.equals(name + ".xml") || file.contains("_" + name + ".xml"))) {
                File oriFile = new File(sharePath + file);
                File tarFile = getSharedPrefsFile(name);
                if (oriFile.exists() && !tarFile.exists()) {
                    Util.moveFile(oriFile, tarFile, false);
                }
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
        PluginLoadedApk mLoadedApk = getPluginLoadedApk();
        if (null != mLoadedApk) {
            return mLoadedApk.getHostContext();
        }
        return null;
    }

    @Override
    public String getPackageCodePath() {
        if (getPluginLoadedApk() != null) {
            TargetMapping targetMapping = getPluginMapping();
            if (targetMapping != null && targetMapping.usePluginCodePath()) {
                PackageInfo packageInfo = targetMapping.getPackageInfo();
                if (packageInfo != null && packageInfo.applicationInfo != null) {
                    String sourceDir = packageInfo.applicationInfo.sourceDir;
                    if (!TextUtils.isEmpty(sourceDir)) {
                        return sourceDir;
                    }
                }
            }
        }
        return super.getPackageCodePath();
    }

    /**
     * Get host resource
     *
     * @return host resource tool
     */
    @Override
    public ResourcesToolForPlugin getHostResourceTool() {
        PluginLoadedApk mLoadedApk = getPluginLoadedApk();
        if (null != mLoadedApk) {
            return mLoadedApk.getHostResourceTool();
        }
        return null;
    }

    public TargetMapping getPluginMapping(){
        PluginLoadedApk mLoadedApk = getPluginLoadedApk();
        if(mLoadedApk != null){
            return mLoadedApk.getPluginMapping();
        }
        return null;
    }

    @Override
    public void exitApp() {
        PluginLoadedApk mLoadedApk = getPluginLoadedApk();
        if (null != mLoadedApk) {
            mLoadedApk.quitApp(true);
        }
    }

    /**
     * Get proxy environment
     *
     * @return plugin's environment
     */
    protected abstract PluginLoadedApk getPluginLoadedApk();

    /**
     * Get log tag
     *
     * @return log tag
     */
    protected abstract String getLogTag();


}
