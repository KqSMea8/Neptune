package org.qiyi.pluginlibrary.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Log;

/**
 *
 * @since
 */
public final class Util {
    private static final String TAG = PluginDebugLog.TAG;
    /** apk 中 lib 目录的前缀标示。比如 lib/x86/libshare_v2.so */
    private static final String APK_LIB_DIR_PREFIX = "lib/";
    /** lib中so后缀 */
    private static final String APK_LIB_SUFFIX = ".so";

    /** utility class private constructor */
    private Util() {
    }

    /**
     * 6.2扩展了几个方法
     */
    public static final String pluginSDKVersion = "6.2";

    public static String getPluginSDKVersion() {
        return pluginSDKVersion;
    }

    /**
     * 读取 apk 文件的最后修改时间（生成时间），通过编译命令编译出来的apk第一个 entry 为 META-INF/MANIFEST.MF
     * 所以我们只读取此文件的修改时间可以。
     *
     * 对于 eclipse 插件打包的 apk 不适用。文件 entry顺序不确定。
     *
     * @param fis
     * @throws IOException
     * @return 返回 {@link SimpleDateTime}
     */
    public static SimpleDateTime readApkModifyTime(InputStream fis) throws IOException {

        int LOCHDR = 30; // header 部分信息截止字节 // SUPPRESS CHECKSTYLE
        int LOCVER = 4; // 排除掉magic number 后的第四个字节，version部分 // SUPPRESS
                        // CHECKSTYLE
        int LOCTIM = 10; // 最后修改时间 第10个字节。 // SUPPRESS CHECKSTYLE

        byte[] hdrBuf = new byte[LOCHDR - LOCVER];

        // Read the local file header.
        byte[] magicNumer = new byte[4]; // SUPPRESS CHECKSTYLE magic number
        fis.read(magicNumer);
        fis.read(hdrBuf, 0, hdrBuf.length);

        int time = peekShort(hdrBuf, LOCTIM - LOCVER);
        int modDate = peekShort(hdrBuf, LOCTIM - LOCVER + 2);

        SimpleDateTime cal = new SimpleDateTime();
        /*
         * zip中的日期格式为 dos 格式，从 1980年开始计时。
         */
        cal.set(1980 + ((modDate >> 9) & 0x7f), ((modDate >> 5) & 0xf), // SUPPRESS
                                                                        // CHECKSTYLE
                                                                        // magic
                                                                        // number
                modDate & 0x1f, (time >> 11) & 0x1f, (time >> 5) & 0x3f, // SUPPRESS
                                                                         // CHECKSTYLE
                                                                         // magic
                                                                         // number
                (time & 0x1f) << 1); // SUPPRESS CHECKSTYLE magic number

        fis.skip(0);

        return cal;
    }

    /**
     * 从buffer数组中读取一个 short。
     *
     * @param buffer buffer数组
     * @param offset 偏移量，从这个位置读取一个short。
     * @return short值
     */
    private static int peekShort(byte[] buffer, int offset) {
        short result = (short) ((buffer[offset + 1] << 8) | (buffer[offset] & 0xff)); // SUPPRESS
                                                                                      // CHECKSTYLE
                                                                                      // magic
                                                                                      // number

        return result & 0xffff; // SUPPRESS CHECKSTYLE magic number
    }

    /**
     * Copy data from a source stream to destFile. Return true if succeed,
     * return false if failed.
     *
     * @param inputStream source file inputstream
     * @param destFile destFile
     *
     * @return success return true
     */
    public static boolean copyToFile(InputStream inputStream, File destFile) {
        PluginDebugLog.log(TAG, "copyToFile:" + inputStream + "," + destFile);
        if (inputStream == null || destFile == null) {
            return false;
        }

        try {
            if (destFile.exists()) {
                destFile.delete();
            }
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(destFile);
                byte[] buffer = new byte[4096]; // SUPPRESS CHECKSTYLE
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                if (null != out) {
                    try {
                        out.flush();
                        out.getFD().sync();
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            PluginDebugLog.log(TAG, "拷贝成功");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            PluginDebugLog.log(TAG, "拷贝失败");
            return false;
        }
    }

    /**
     * Copy data from a source stream to destFile. Return true if succeed,
     * return false if failed.
     *
     * @param srcFile source file
     * @param destFile destFile
     *
     * @return success return true
     */
    public static boolean copyToFile(File srcFile, File destFile) {
        PluginDebugLog.log(TAG, "copyToFile:" + srcFile + "," + destFile);
        if (srcFile == null || !srcFile.exists() || destFile == null) {
            return false;
        }
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(srcFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            inputStream = null;
        }
        boolean result = false;
        if (null != inputStream) {
            result = copyToFile(inputStream, destFile);
        }
        try {
            if (null != inputStream) {
                inputStream.close();
            }
        } catch (Exception e) {
        }
        return result;
    }

    /**
     * 安装 apk 中的 so 库。
     *
     * @param apkFilePath
     * @param libDir lib目录。
     */
    @SuppressWarnings("resource")
    @SuppressLint("NewApi")
    public static boolean installNativeLibrary(String apkFilePath, String libDir) {
        PluginDebugLog.log("plugin", "apkFilePath: " + apkFilePath + " libDir: " + libDir);
        boolean installResult = false;
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(apkFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (zipFile == null) {
            PluginDebugLog.log("plugin", "apkfile: " + apkFilePath + "doesn't exist when try to install native lib");
            return false;
        }

        if (installNativeLibrary(zipFile, libDir, Build.CPU_ABI) || installNativeLibrary(zipFile, libDir, Build.CPU_ABI2)) {
            installResult = true;
        } else {
            PluginDebugLog.log("plugin", "can't install native lib of " + apkFilePath + "as no matched ABI");
        }

        try {
            zipFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return installResult;
    }

    private static boolean installNativeLibrary(ZipFile apk, String libDir, String abi) {
        PluginDebugLog.log("plugin", "start to extract native lib for ABI: " + abi);
        boolean installResult = false;
        Enumeration<? extends ZipEntry> entries = apk.entries();
        ZipEntry entry;
        while (entries.hasMoreElements()) {
            entry = entries.nextElement();
            String name = entry.getName();
            if (!name.startsWith(APK_LIB_DIR_PREFIX + abi) || !name.endsWith(APK_LIB_SUFFIX)) {
                continue;
            }

            int lastSlash = name.lastIndexOf("/");
            InputStream entryInputStream = null;
            try {
                entryInputStream = apk.getInputStream(entry);
                String soFileName = name.substring(lastSlash);
                PluginDebugLog.log("plugin", "libDir: " + libDir + " soFileName: " + soFileName);
                installResult = copyToFile(entryInputStream, new File(libDir, soFileName));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (entryInputStream != null) {
                        entryInputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return installResult;
    }




    /**
     * Write byte array to file
     *
     * @param data source byte array
     * @param target the target file to write
     * @return success or not
     */
    public static boolean writeToFile(byte[] data, File target) {
        if (data == null || target == null) {
            PluginDebugLog.log(TAG, "writeToFile failed will null check!");
            return false;
        }
        FileOutputStream fo = null;
        ReadableByteChannel src = null;
        FileChannel out = null;
        try {
            src = Channels.newChannel(new ByteArrayInputStream(data));
            fo = new FileOutputStream(target);
            out = fo.getChannel();
            out.transferFrom(src, 0, data.length);
            PluginDebugLog.log(TAG, "write to file: " + target.getAbsolutePath() + " success!");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            PluginDebugLog.log(TAG, "write to file: " + target.getAbsolutePath() + " failed!");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            PluginDebugLog.log(TAG, "write to file: " + target.getAbsolutePath() + " failed!");
            return false;
        } finally {
            if (src != null) {
                try {
                    src.close();
                } catch (IOException e) {
                }

            }
            if (out != null) {
                try {
                    out.force(true);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
            if (fo != null) {
                try {
                    fo.flush();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    fo.close();
                } catch (IOException e) {
                }
            }
        }
        return true;
    }

    /**
     * Deletes a directory recursively.
     *
     * @param directory directory to delete
     * @throws IOException in case deletion is unsuccessful
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (directory == null) {
            PluginDebugLog.log(TAG, "deleteDirectory pkgName is empty return");
            return;
        }

        if (!directory.exists()) {
            return;
        }

        cleanDirectory(directory);
        if (!directory.delete()) {
            String message = "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory directory to clean
     * @throws IOException in case cleaning is unsuccessful
     */
    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) { // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }

        if (null != exception) {
            throw exception;
        }
    }

    /**
     * Deletes a file. If file is a directory, delete it and all
     * sub-directories. <p> The difference between File.delete() and this method
     * are: <ul> <li>A directory to be deleted does not have to be empty.</li>
     * <li>You get exceptions when a file or directory cannot be deleted.
     * (java.io.File methods returns a boolean)</li> </ul>
     *
     * @param file file or directory to delete, must not be <code>null</code>
     * @throws NullPointerException if the directory is <code>null</code>
     * @throws FileNotFoundException if the file was not found
     * @throws IOException in case deletion is unsuccessful
     */
    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent) {
                    throw new FileNotFoundException("File does not exist: " + file);
                }
                String message = "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }

    /**
     * 比较两个签名是否相同
     *
     * @param s1
     * @param s2
     * @return
     */
    public static int compareSignatures(Signature[] s1, Signature[] s2) {
        if (s1 == null) {
            return s2 == null ? PackageManager.SIGNATURE_NEITHER_SIGNED : PackageManager.SIGNATURE_FIRST_NOT_SIGNED;
        }
        if (s2 == null) {
            return PackageManager.SIGNATURE_SECOND_NOT_SIGNED;
        }
        HashSet<Signature> set1 = new HashSet<Signature>();
        for (Signature sig : s1) {
            set1.add(sig);
        }
        HashSet<Signature> set2 = new HashSet<Signature>();
        for (Signature sig : s2) {
            set2.add(sig);
        }
        // Make sure s2 contains all signatures in s1.
        if (set1.equals(set2)) {
            return PackageManager.SIGNATURE_MATCH;
        }
        return PackageManager.SIGNATURE_NO_MATCH;
    }

    private static void copyFile(File sourceFile, File targetFile) throws IOException {

        BufferedInputStream inBuff = null;

        BufferedOutputStream outBuff = null;

        try {
            FileInputStream input = new FileInputStream(sourceFile);
            FileOutputStream output = new FileOutputStream(targetFile);
            inBuff = new BufferedInputStream(input);
            outBuff = new BufferedOutputStream(output);
            byte[] b = new byte[1024 * 5];
            int len;
            while ((len = inBuff.read(b)) != -1) {
                outBuff.write(b, 0, len);
            }

            outBuff.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != inBuff) {
                try {
                    inBuff.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (null != outBuff) {
                try {
                    outBuff.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void moveFile(File sourceFile, File targetFile) {
        moveFile(sourceFile, targetFile, true);
    }

    public static void moveFile(File sourceFile, File targetFile, boolean needDeleteSource) {
        try {
            copyFile(sourceFile, targetFile);
            if (needDeleteSource) {
                sourceFile.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Judge activity is resume or not by reflection
     *
     * @param activity
     * @return
     */
    public static boolean isResumed(Activity activity) {
        boolean result = true;
        try {
            Class<?> clazz = Class.forName("android.app.Activity");
            Method isResumed = clazz.getDeclaredMethod("isResumed");
            result = (Boolean) isResumed.invoke(activity);
        } catch (ClassNotFoundException e) {
            if (PluginDebugLog.isDebug()) {
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
            if (PluginDebugLog.isDebug()) {
                e.printStackTrace();
            }
        } catch (IllegalAccessException e) {
            if (PluginDebugLog.isDebug()) {
                e.printStackTrace();
            }
        } catch (IllegalArgumentException e) {
            if (PluginDebugLog.isDebug()) {
                e.printStackTrace();
            }
        } catch (InvocationTargetException e) {
            if (PluginDebugLog.isDebug()) {
                e.printStackTrace();
            }
        }
        return result;
    }


    /**
     * 判断当前手机的指令集
     */
    private static String currentInstructionSet = null;
    public static String getCurrentInstructionSet() throws Exception {
        if (currentInstructionSet != null) {
            return currentInstructionSet;
        }
        Class<?> clazz = Class.forName("dalvik.system.VMRuntime");
        Method currentGet = clazz.getDeclaredMethod("getCurrentInstructionSet");

        currentInstructionSet = (String) currentGet.invoke(null);
        Log.d(TAG, "getCurrentInstructionSet:" + currentInstructionSet);
        return currentInstructionSet;
    }
}
