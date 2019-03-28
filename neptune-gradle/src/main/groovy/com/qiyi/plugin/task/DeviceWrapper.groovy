package com.qiyi.plugin.task

import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.ddmlib.AdbCommandRejectedException
import com.android.ddmlib.CollectingOutputReceiver
import com.android.ddmlib.IDevice
import com.android.ddmlib.InstallException
import com.android.ddmlib.MultiLineReceiver
import com.android.ddmlib.NullOutputReceiver
import com.android.ddmlib.ShellCommandUnresponsiveException
import com.android.ddmlib.SyncException
import com.android.ddmlib.TimeoutException
import com.android.sdklib.AndroidVersion
import com.qiyi.plugin.QYPluginExtension
import org.gradle.api.Project
import org.gradle.util.VersionNumber

import java.util.concurrent.TimeUnit

class DeviceWrapper {

    private static final long INSTALL_TIMEOUT_MINUTES

    static {
        String installTimeout = System.getenv("ADB_INSTALL_TIMEOUT")
        long time = 4
        if (installTimeout != null) {
            try {
                time = Long.parseLong(installTimeout)
            } catch (NumberFormatException e) {
                // use default value
            }
        }
        INSTALL_TIMEOUT_MINUTES = time
    }

    private IDevice device
    private Project project
    private ApplicationVariantImpl variant
    private QYPluginExtension pluginExt

    DeviceWrapper(IDevice device, Project project, ApplicationVariantImpl variant) {
        this.device = device
        this.project = project
        this.variant = variant
        this.pluginExt = project.extensions.findByType(QYPluginExtension.class)
    }

    private String getHostPackage() {
        return pluginExt.hostPackageName
    }

    private String getPluginPackage() {
        return pluginExt.packageName
    }

    private String getPluginVersion() {
        return pluginExt.versionName
    }

    private boolean shouldLaunchPlugin() {
        return pluginExt.launchPlugin
    }

    private String getHostAbi() {
        return pluginExt.hostAbi
    }

    public void installPackage(File apkFile, boolean reinstall)
            throws InstallException {

        println "ready to install plugin apk ${apkFile} to host APP ${hostPackage}"
        String apkPath = apkFile.getAbsolutePath()
        try {
            // 检测宿主App是否安装到设备上
            checkHostAppInstalled()
            // 推送插件相关文件到sdcard上
            preInstallPlugin(apkFile)
            // 推送插件APK到设备上
            String remoteFilePath = device.syncPackageToDevice(apkPath)
            // 安装插件到宿主APP目录
            installRemotePackage(remoteFilePath, reinstall)
            // 删除远程临时文件
            device.removeRemotePackage(remoteFilePath)
            // 杀进程，重启客户端
            restartHostApp()
            // 通过schema跳转插件
            enterPlugin()
        } catch (IOException e) {
            throw new InstallException(e)
        } catch (AdbCommandRejectedException e) {
            throw new InstallException(e)
        } catch (TimeoutException e) {
            throw new InstallException(e)
        } catch (SyncException e) {
            throw new InstallException(e)
        }
    }

    /**
     * 校验宿主APP是否已经安装到设备上
     * @param device
     * @throws InstallException
     */
    void checkHostAppInstalled() throws InstallException {
        try {
            ListPackageReceiver receiver = new ListPackageReceiver()
            String cmd = "pm list packages"
            device.executeShellCommand(cmd, receiver, INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)

            if (!receiver.isPackageInstalled(hostPackage)) {
                throw new InstallException("host package " + hostPackage + " not installed in device")
            }
        } catch (TimeoutException e) {
            throw new InstallException(e)
        } catch (AdbCommandRejectedException e) {
            throw new InstallException(e)
        } catch (ShellCommandUnresponsiveException e) {
            throw new InstallException(e)
        } catch (IOException e) {
            throw new InstallException(e)
        }
    }

    /**
     * 推送插件apk和log文件到存储卡, 开启存储卡权限
     */
    void preInstallPlugin(File apkFile) {
        try {
            grantStoragePermission()
            String remotePath = "/sdcard/${pluginPackage}.apk"
            device.pushFile(apkFile.absolutePath, remotePath)

            File logFile = new File(apkFile.getParent(), "${pluginPackage}.log")
            logFile.createNewFile()
            logFile.withPrintWriter { pw ->
                pw.write("${pluginPackage}")
            }
            String remoteLogPath = "/sdcard/${pluginPackage}.log"
            device.pushFile(logFile.absolutePath, remoteLogPath)
        } catch (TimeoutException e) {
            throw new InstallException(e)
        } catch (AdbCommandRejectedException e) {
            throw new InstallException(e)
        } catch (ShellCommandUnresponsiveException e) {
            throw new InstallException(e)
        } catch (IOException e) {
            throw new InstallException(e)
        }
    }

    /**
     * 给宿主APP授予存储卡权限
     */
    void grantStoragePermission() {
        try {
            NullOutputReceiver receiver = new NullOutputReceiver()
            // Android 8.0以上设备读写权限使用命令行需要单独授权
            String cmd = "pm grant ${hostPackage} android.permission.READ_EXTERNAL_STORAGE"
            device.executeShellCommand(cmd, receiver, INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            cmd = "pm grant ${hostPackage} android.permission.WRITE_EXTERNAL_STORAGE"
            device.executeShellCommand(cmd, receiver, INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        } catch (TimeoutException e) {
            throw new InstallException(e)
        } catch (AdbCommandRejectedException e) {
            throw new InstallException(e)
        } catch (ShellCommandUnresponsiveException e) {
            throw new InstallException(e)
        } catch (IOException e) {
            throw new InstallException(e)
        }
    }

    String getPluginApkName() throws InstallException {
        try {
            CollectingOutputReceiver receiver = new CollectingOutputReceiver()

            String cmd = "run-as ${hostPackage} ls app_pluginapp"
            println "ls plugin apk name cmd: " + cmd
            device.executeShellCommand(cmd, receiver, INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)

            String result = receiver.getOutput().trim()
            if (result.contains("Package \'${hostPackage}\' is not debuggable")) {
                throw new InstallException("Host App is not debuggable, please install debuggable apk")
            } else if (result.contains("Permission denied")) {
                throw new InstallException("No permission into Host App /data/data/${hostPackage} directory")
            }

            String[] splits = result.split("\\s+")
            for (String ss : splits) {
                if (ss.startsWith(pluginPackage) && ss.endsWith(".apk")) {
                    return ss
                }
            }
            return "${pluginPackage}.${pluginVersion}.apk"
        } catch (TimeoutException e) {
            throw new InstallException(e)
        } catch (AdbCommandRejectedException e) {
            throw new InstallException(e)
        } catch (ShellCommandUnresponsiveException e) {
            throw new InstallException(e)
        } catch (IOException e) {
            throw new InstallException(e)
        }
    }

    void installRemotePackage(String remoteFilePath, boolean reInstall) throws InstallException {
        try {
            println "remote apk file path: ${remoteFilePath}"
            String apkName = getPluginApkName()
            NullOutputReceiver receiver = new NullOutputReceiver()
            // 覆盖原有的apk
            String targetFile = "/data/data/${hostPackage}/app_pluginapp/${apkName}"
            String cmd = "run-as ${hostPackage} cp ${remoteFilePath} ${targetFile}"
            println "copy plugin apk into host app directory cmd: " + cmd
            device.executeShellCommand(cmd, receiver, INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            // 安装native so库
            installNativeLibrary()
            // 删除dex相关文件
            deleteDexFiles(apkName)
            // 执行dex2oat优化
            execDex2Oat(apkName)
        } catch (TimeoutException e) {
            throw new InstallException(e)
        } catch (AdbCommandRejectedException e) {
            throw new InstallException(e)
        } catch (ShellCommandUnresponsiveException e) {
            throw new InstallException(e)
        } catch (IOException e) {
            throw new InstallException(e)
        }
    }

    void installNativeLibrary() {
        // 删除设备上的lib目录
        deleteSoFiles()
        // 安装编译完成的so文件
        File jniLibs = getJniLibsDirectory()

        if (jniLibs.exists()) {
            File[] soFiles = jniLibs.listFiles(new FileFilter() {
                @Override
                boolean accept(File pathname) {
                    return pathname.isFile() && pathname.name.endsWith(".so")
                }
            })
            if (soFiles == null || soFiles.length <= 0) {
                println "this plugin apk do not have native library"
                return
            }

            soFiles.each { File soLib ->
                installNativeLibrary(soLib)
            }
        } else {
            println "${jniLibs.absolutePath} not exist"
        }
    }

    void installNativeLibrary(File soLib) {
        try {
            // 推送到设备上
            String remotePath = "/data/local/tmp/lib/${soLib.name}"
            device.pushFile(soLib.absolutePath, remotePath)
            // 拷贝到宿主目录
            NullOutputReceiver receiver = new NullOutputReceiver()
            String targetPath = "/data/data/${hostPackage}/app_pluginapp/${pluginPackage}/lib/${soLib.name}"
            String cmd = "run-as ${hostPackage} cp ${remotePath} ${targetPath}"
            println "copy plugin native library to host app directory cmd: " + cmd
            device.executeShellCommand(cmd, receiver, INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        } catch (TimeoutException e) {
            throw new InstallException(e)
        } catch (AdbCommandRejectedException e) {
            throw new InstallException(e)
        } catch (ShellCommandUnresponsiveException e) {
            throw new InstallException(e)
        } catch (IOException e) {
            throw new InstallException(e)
        }
    }


    File getJniLibsDirectory() {
        String path
        String prefix = "intermediates/transforms/mergeJniLibs/${variant.buildType.name}"
        if (pluginExt.agpVersion >= VersionNumber.parse("3.0")) {
            // AGP 3.0.0+
            path = "${prefix}/0/lib/${hostAbi}"
        } else {
            // AGP 2.3
            path = "${prefix}/folders/2000/1f/main/lib/${hostAbi}"
        }

        File jniLibs = new File(project.buildDir, path)

        println "project jniLibs directory is ${jniLibs.absolutePath}"
        return jniLibs
    }


    void deleteSoFiles() {
        try {
            CollectingOutputReceiver receiver = new CollectingOutputReceiver()

            String cmd = "run-as ${hostPackage} ls app_pluginapp/${pluginPackage}/lib"
            println "ls native libraries in plugin cmd: " + cmd
            device.executeShellCommand(cmd, receiver, INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)

            String result = receiver.getOutput().trim()

            if (result.contains("Package \'${hostPackage}\' is not debuggable")) {
                throw new InstallException("Host App is not debuggable, please install debuggable apk")
            } else if (result.contains("Permission denied")) {
                throw new InstallException("No permission into Host App /data/data/${hostPackage} directory")
            }

            NullOutputReceiver nullReceiver = new NullOutputReceiver()
            String[] splits = result.split("\\s+")
            for (String ss : splits) {
                if (ss != null && ss.length() > 0) {
                    String path = "/data/data/${hostPackage}/app_pluginapp/${pluginPackage}/lib/${ss}"
                    cmd = "run-as ${hostPackage} rm ${path}"
                    println "delete native libraries related files in plugin cmd: " + cmd
                    device.executeShellCommand(cmd, nullReceiver, INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                }
            }

        } catch (TimeoutException e) {
            e.printStackTrace()
        } catch (AdbCommandRejectedException e) {
            e.printStackTrace()
        } catch (ShellCommandUnresponsiveException e) {
            e.printStackTrace()
        } catch (IOException e) {
            e.printStackTrace()
        }
    }

    void deleteDexFiles(String apkName) {
        List<String> toDeleted = new ArrayList<>()
        // 删除原有的dex文件
        String rootDir = "/data/data/${hostPackage}/app_pluginapp"
        String dexName = apkName.replace('.apk', '.dex')

        String targetDex = "${rootDir}/${pluginPackage}/${dexName}"
        toDeleted.add(targetDex)

        // 删除prof和odex，vdex文件
        String prof = "${rootDir}/${apkName}.prof"
        toDeleted.add(prof)

        String odexName = apkName.replace('.apk', '.odex')
        String vdexName = apkName.replace('.apk', '.vdex')
        String odex = "${rootDir}/oat/arm/${odexName}"
        String vdex = "${rootDir}/oat/arm/${vdexName}"
        toDeleted.add(odex)
        toDeleted.add(vdex)

        for (String path : toDeleted) {
            String cmd = "run-as ${hostPackage} rm ${path}"
            println "delete dex related file cmd: " + cmd
            device.executeShellCommand(cmd, new NullOutputReceiver(), INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        }
    }

    void execDex2Oat(String apkName) {
        AndroidVersion version = getAndroidVersion()
        if (version.apiLevel < 21) {
            println "Device below Android Lollipop not exec dexopt optimize, api version ${version}"
            return
        }

        String dexName = apkName.replace('.apk', '.dex')
        String odexName = apkName.replace(".apk", ".odex")
        String apkFile = "/data/data/${hostPackage}/app_pluginapp/${apkName}"
        String oatFile = "/data/data/${hostPackage}/app_pluginapp/${pluginPackage}/${dexName}"
        if (version.apiLevel >= 26) {
            oatFile = "/data/data/${hostPackage}/app_pluginapp/oat/arm/${odexName}"
        }

        println "exec dex2oat to optimize the apk ${apkFile}"

        StringBuilder sb = new StringBuilder("dex2oat")
        if (version.apiLevel >= 24) {
            sb.append(" --runtime-arg")
              .append(" -classpath")
              .append(" --runtime-arg")
              .append(" &")
        }
        sb.append(" --dex-file=")
          .append(apkFile)
          .append(" --oat-file=")
          .append(oatFile)
          .append(" --instruction-set=arm")
        if (version.apiLevel >= 26) {
            sb.append(" --compiler-filter=quicken")
        } else {
            sb.append(" --compiler-filter=interpret-only")
        }

        String cmd = sb.toString()
        println "dex2oat cmd: " + cmd
        device.executeShellCommand(cmd, new NullOutputReceiver(), INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)
    }

    void restartHostApp() {
        try {
            println "restartHostApp exec ......"
            NullOutputReceiver receiver = new NullOutputReceiver()
            String cmd = "am force-stop ${hostPackage}"
            device.executeShellCommand(cmd, receiver, INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)

            if ("com.qiyi.video" == hostPackage || "tv.pps.mobile" == hostPackage) {
                cmd = "am start -n ${hostPackage}/${hostPackage}.WelcomeActivity"
            } else {
                cmd = "am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER ${hostPackage}"
            }
            device.executeShellCommand(cmd, receiver, INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        } catch (TimeoutException e) {
            throw new InstallException(e)
        } catch (AdbCommandRejectedException e) {
            throw new InstallException(e)
        } catch (ShellCommandUnresponsiveException e) {
            throw new InstallException(e)
        } catch (IOException e) {
            throw new InstallException(e)
        }
    }

    void enterPlugin() {
        if (!shouldLaunchPlugin()) {
            return
        }

        try {
            // 休眠5s等待主APP启动进入首页
            Thread.sleep(5000)
            // 通过schema跳转拉起插件首页
            String suffix = pluginPackage.replace(".", "_")
            String deeplink = "iqiyi://mobile/register_business/${suffix}"
            if ("com.qiyi.video" == hostPackage || "tv.pps.mobile" == hostPackage) {
                println "ready to launch plugin ......"
                NullOutputReceiver receiver = new NullOutputReceiver()
                String cmd = "am start -a android.intent.action.VIEW -d ${deeplink} ${hostPackage}"
                device.executeShellCommand(cmd, receiver, INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            } else {
                println "No support enter plugin for ${hostPackage}"
            }
        } catch (TimeoutException e) {
            throw new InstallException(e)
        } catch (AdbCommandRejectedException e) {
            throw new InstallException(e)
        } catch (ShellCommandUnresponsiveException e) {
            throw new InstallException(e)
        } catch (IOException e) {
            throw new InstallException(e)
        }
    }

    AndroidVersion getAndroidVersion() {
        device.getProperties()
        AndroidVersion version = device.getVersion()
        if (version.apiLevel < 14) {
            // 通过ADB shell读取build.prop文件
            try {
                String cmd = "cat /system/build.prop"
                BuildPropReceiver receiver = new BuildPropReceiver()
                device.executeShellCommand(cmd, receiver, INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES)

                String buildApi = receiver.getProperty(IDevice.PROP_BUILD_API_LEVEL)
                String codeName = receiver.getProperty(IDevice.PROP_BUILD_CODENAME)
                if (buildApi != null && buildApi.length() > 0) {
                    version = new AndroidVersion(Integer.parseInt(buildApi), codeName)
                }
            } catch (Exception e) {
                e.printStackTrace()
            }
        }

        return version
    }


    static final class BuildPropReceiver extends MultiLineReceiver {

        Map<String, String> properties

        BuildPropReceiver() {
            properties = new HashMap<>()
        }

        @Override
        void processNewLines(String[] lines) {
            for (String line : lines) {
                if (line == null || line.length() <= 0
                    || line.startsWith("#")
                    || line.startsWith("import")
                    || !line.contains("=")) {
                    continue
                }
                StringTokenizer tokenizer = new StringTokenizer(line.trim(), "=")
                String key = tokenizer.nextToken()
                String value = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : ""
                if (key.length() > 0 && value.length() >= 0) {
                    properties.put(key, value)
                }
            }
        }

        String getProperty(String key) {
            return properties.get(key)
        }

        @Override
        boolean isCancelled() {
            return false
        }
    }

    static final class ListPackageReceiver extends MultiLineReceiver {

        private Set<String> packageLists

        ListPackageReceiver() {
            packageLists = new HashSet<>()
        }

        @Override
        void processNewLines(String[] lines) {
            for (String line : lines) {
                if (line == null || line.length() <= 0) {
                    continue
                }
                //package:com.qrd.omadownload
                line = line.trim()
                if (line.startsWith("package:")) {
                    StringTokenizer tokenizer = new StringTokenizer(line, ":")
                    String pkg = tokenizer.nextToken()
                    String name = tokenizer.nextToken()
                    if (pkg.length() > 0 && name.length() > 0) {
                        packageLists.add(name)
                    }
                }
            }
        }

        @Override
        boolean isCancelled() {
            return false
        }

        boolean isPackageInstalled(String pkgName) {
            return packageLists.contains(pkgName)
        }
    }
}
