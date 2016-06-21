package org.qiyi.pluginnew.context;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.qiyi.plugin.manager.ProxyEnvironmentNew;
import org.qiyi.pluginlibrary.ActivityJumpUtil;
import org.qiyi.pluginlibrary.PluginServiceWrapper;
import org.qiyi.pluginlibrary.plugin.InterfaceToGetHost;
import org.qiyi.pluginlibrary.utils.ContextUtils;
import org.qiyi.pluginlibrary.utils.ReflectionUtils;
import org.qiyi.pluginlibrary.utils.ResourcesToolForPlugin;
import org.qiyi.pluginlibrary.utils.Util;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

public abstract class CustomContextWrapper extends ContextWrapper implements InterfaceToGetHost {

    private static final String S_SHARED_PREFS =
            ContextUtils.isAndroidN() ? "sSharedPrefsCache" : "sSharedPrefs";

    protected static ConcurrentMap<String, Method> sMethods = new ConcurrentHashMap<String, Method>(2);

    public CustomContextWrapper(Context base) {
        super(base);
    }

    @Override
    public ClassLoader getClassLoader() {
        return getEnvironment().getDexClassLoader();
    }

    @Override
    public Context getApplicationContext() {
        return getEnvironment().getApplication();
    }

    @Override
    public Resources getResources() {
        return getEnvironment().getTargetResources();
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    @Override
    public ComponentName startService(Intent service) {
        Log.d(getLogTag(), "startService: " + service);
        ProxyEnvironmentNew env = getEnvironment();
        if (env != null) {
            env.remapStartServiceIntent(service);
        }
        return super.startService(service);
    }

    @Override
    public boolean stopService(Intent name) {
        Log.d(getLogTag(), "stopService: " + name);
        ProxyEnvironmentNew env = getEnvironment();
        if (env != null) {
            String actServiceClsName = name.getComponent().getClassName();
            PluginServiceWrapper plugin = ProxyEnvironmentNew.sAliveServices
                    .get(PluginServiceWrapper.getIndeitfy(getPluginPackageName(), actServiceClsName));
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
        Log.d(getLogTag(), "bindService: " + service);
        ProxyEnvironmentNew env = getEnvironment();
        if (env != null) {
            env.remapStartServiceIntent(service);
        }
        if (conn != null) {
            if (env != null && service != null && service.getComponent() != null) {
                String serviceClass = service.getStringExtra(ProxyEnvironmentNew.EXTRA_TARGET_SERVICE);
                String packageName = env.getTargetPackageName();
                if (!TextUtils.isEmpty(serviceClass) && !TextUtils.isEmpty(packageName)) {
                    ProxyEnvironmentNew.sAliveServiceConnection.put(packageName + "." + serviceClass, conn);
                }
            }
        }
        return super.bindService(service, conn, flags);
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
        if (ProxyEnvironmentNew.sAliveServiceConnection.keySet().contains(conn)) {
            ProxyEnvironmentNew.sAliveServiceConnection.remove(conn);
        }
        Log.d(getLogTag(), "unbindService: " + conn);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(ActivityJumpUtil.handleStartActivityIntent(getPluginPackageName(), intent, -1, null, this));
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        super.startActivity(ActivityJumpUtil.handleStartActivityIntent(getPluginPackageName(), intent, -1, options, this), options);
    }

    @Override
    public File getFilesDir() {

        File superFile = super.getFilesDir();

        if (getEnvironment() == null) {
            return superFile;
        }

        File fileDir = new File(getEnvironment().getTargetMapping().getDataDir() + "/files/");
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        return getEnvironment().getTargetAssetManager() == null ? superFile : fileDir;
    }

    @Override
    public File getCacheDir() {

        if (getEnvironment() == null) {
            return super.getCacheDir();
        }
        File cacheDir = new File(getEnvironment().getTargetMapping().getDataDir() + "/cache/");
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        return getEnvironment().getTargetAssetManager() == null ? super.getCacheDir() : cacheDir;
    }

    @Override
    public File getFileStreamPath(String name) {
        if (getEnvironment() == null) {
            return super.getFilesDir();
        }
        File fileDir = new File(getEnvironment().getTargetMapping().getDataDir() + "/files/" + name);
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        return getEnvironment().getTargetAssetManager() == null ? super.getFileStreamPath(name) : fileDir;
    }

    @Override
    public File getDir(String name, int mode) {
        if (getEnvironment() == null) {
            return super.getFilesDir();
        }
        File fileDir = new File(getEnvironment().getTargetMapping().getDataDir() + "/app_" + name + "/");
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        return getEnvironment().getTargetAssetManager() == null ? super.getDir(name, mode) : fileDir;

    }

    @Override
    public File getDatabasePath(String name) {
        File dir;
        File f;
        if (name.charAt(0) == File.separatorChar) {
            String dirPath = name.substring(0, name.lastIndexOf(File.separatorChar));
            dir = new File(dirPath);
            name = name.substring(name.lastIndexOf(File.separatorChar));
            f = new File(dir, name);
            return f;
        } else {
            if (getEnvironment() == null) {
                return super.getDatabasePath(name);
            }
            f = new File(getEnvironment().getTargetMapping().getDataDir() + "/databases/" + name);
            if (!f.exists()) {
                f.mkdir();
            }
        }

        return getEnvironment().getTargetAssetManager() == null ? super.getDatabasePath(name) : f;
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        if (getEnvironment() == null) {
            return super.openFileInput(name);
        }
        File f = makeFilename(getFilesDir(), name);
        return new FileInputStream(f);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        if (getEnvironment() == null) {
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
        if (getEnvironment() == null) {
            return super.deleteFile(name);
        }
        File fileDir = new File(getEnvironment().getTargetMapping().getDataDir() + "/files/" + name);
        return getEnvironment().getTargetAssetManager() == null ? super.deleteFile(name) : fileDir.delete();
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {
        File databaseDir = new File(getEnvironment().getTargetMapping().getDataDir() + "/databases/");
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
            File targetFile = new File(getEnvironment().getTargetMapping().getDataDir() + "/databases/" + name);
            if (!targetFile.exists()) {
                Util.moveFile(file, targetFile);
            }
            File bakFile = new File(dbPath, dbName + ".db-journal");
            File targetBakFile = new File(
                    getEnvironment().getTargetMapping().getDataDir() + "/databases/" + dbName + ".db-journal");
            if (bakFile.exists() && !targetBakFile.exists()) {
                Util.moveFile(bakFile, targetBakFile);
            }
        }
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory, DatabaseErrorHandler errorHandler) {
        File databaseDir = new File(getEnvironment().getTargetMapping().getDataDir() + "/databases/");
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
        File databaseDir = new File(getEnvironment().getTargetMapping().getDataDir() + "/databases/");
        if (!databaseDir.exists()) {
            databaseDir.mkdir();
        }
        return super.deleteDatabase(databaseDir.getAbsolutePath() + "/" + name);
    }

    @Override
    public String[] databaseList() {
        File databaseDir = new File(getEnvironment().getTargetMapping().getDataDir() + "/databases/");
        if (!databaseDir.exists()) {
            databaseDir.mkdir();
        }
        return databaseDir.list();
    }

    private File getSharedPrefsFile(String name) {
        File base = null;
        base = new File(getEnvironment().getTargetMapping().getDataDir() + "/shared_prefs/");
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

                    if (prefsFile.exists() && !prefsFile.canRead()) {

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
                if ((mode & Context.MODE_MULTI_PROCESS) != 0 || getEnvironment().getTargetMapping()
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
                    if ((mode & Context.MODE_MULTI_PROCESS) != 0 || getEnvironment().getTargetMapping()
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

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (getEnvironment() != null && getEnvironment().getTargetMapping() != null) {
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
        for (int i = 0; i < fileList.length; i++) {
            String file = fileList[i];
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
     * Override Oppo method in Context Resolve cann't start plugin on oppo
     * devices, true or false both OK, false as the temporary result
     *
     * @return
     */
    public boolean isOppoStyle() {
        return false;
    }

    /**
     * Get the context which start this plugin
     *
     * @return
     */
    @Override
    public Context getOriginalContext() {
        if (null != getEnvironment()) {
            return getEnvironment().getHostContext();
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
        if (null != getEnvironment()) {
            return getEnvironment().getHostResourceTool();
        }
        return null;
    }

    @Override
    public void exitApp() {
        if (null != getEnvironment()) {
            getEnvironment().quitApp(true);
        }
    }

    /**
     * Get proxy environment
     *
     * @return plugin's environment
     */
    protected abstract ProxyEnvironmentNew getEnvironment();

    /**
     * Get log tag
     *
     * @return log tag
     */
    protected abstract String getLogTag();
}
