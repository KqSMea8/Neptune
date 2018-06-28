package com.qiyi.plugin.hooker

import com.android.build.gradle.AndroidGradleOptions
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.tasks.ManifestProcessorTask
import com.android.build.gradle.tasks.MergeResources
import com.android.build.gradle.tasks.ProcessAndroidResources
import com.android.sdklib.BuildToolInfo
import com.android.utils.FileUtils
import com.google.common.collect.ListMultimap
import com.google.common.io.Files
import com.qiyi.plugin.QYPluginExtension
import com.qiyi.plugin.aapt.Aapt
import com.qiyi.plugin.collector.ResourceCollector
import com.qiyi.plugin.collector.res.ResourceEntry
import com.qiyi.plugin.collector.res.StyleableEntry
import com.qiyi.plugin.dex.DexProcessor
import com.qiyi.plugin.utils.ZipUtil
import groovy.xml.Namespace
import groovy.xml.XmlUtil
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task


class TaskHookerManager {

    private Project project
    /** Android Config information */
    private AppExtension android

    private QYPluginExtension qyplugin

    public TaskHookerManager(Project project) {
        this.project = project
        android = project.extensions.findByType(AppExtension)
        qyplugin = project.extensions.findByType(QYPluginExtension)
    }


    public void registerTaskHooker() {
        project.afterEvaluate {
            android.applicationVariants.all { ApplicationVariantImpl appVariant ->

                def scope = appVariant.getVariantData().getScope()
                MergeResources mergeResTask
                if (pluginExt.isHigherAGP) {
                    mergeResTask = appVariant.getVariantData().mergeResourcesTask
                } else {
                    String mergeTaskName = scope.getMergeResourcesTask().name
                    mergeResTask = project.tasks.getByName(mergeTaskName) as MergeResources
                }

                String processResTaskName = pluginExt.isHigherAGP ?
                        scope.getProcessResourcesTask().name : scope.getGenerateRClassTask().name
                ProcessAndroidResources processResTask = project.tasks.getByName(processResTaskName) as ProcessAndroidResources

                ManifestProcessorTask manifestTask
                if (pluginExt.isHigherAGP) {
                    Object task = appVariant.getVariantData().getScope().manifestProcessorTask
                    if (task instanceof ManifestProcessorTask) {
                        manifestTask = (ManifestProcessorTask)task
                    } else {
                        // AGP 3.0.1 返回的是AndroidTask类型, AndroidTask类在3.1中被删除了，这里使用反射创建
                        try {
                            Class<?> clazz = Class.forName("com.android.build.gradle.internal.scope.AndroidTask")
                            if (clazz.isInstance(task)) {
                                String manifestTaskName = task.name
                                manifestTask = project.tasks.getByName(manifestTaskName) as ManifestProcessorTask
                            } else  {
                                throw new GradleException("ManifestProcessorTask unknown task type ${task.getClass().name}")
                            }
                        } catch (ClassNotFoundException e) {
                            throw new GradleException("com.android.build.gradle.internal.scope.AndroidTask not found")
                        }
                    }
                } else {
                    def outputScope = scope.getVariantData().getMainOutput().getScope()
                    String manifestTaskName = outputScope.getManifestProcessorTask().name
                    manifestTask = project.tasks.getByName(manifestTaskName) as ManifestProcessorTask
                }

                String varName = appVariant.name.capitalize()
                Task dexTask = project.tasks.findByName("transformClassesWithDexFor${varName}")
                if (dexTask == null) {
                    // if still null, may lower gradle
                    dexTask = project.tasks.findByName("dex${varName}")
                }

                hookMergeResourceTask(mergeResTask, processResTask)

                hookProcessResourceTask(processResTask, appVariant)

                hookManifestProcessTask(manifestTask)

                hookDexTask(dexTask)
            }
        }
    }

    /**
     * hook合并Resource的task，将public.xml文件拷贝到
     * build/intermediates/rese/merged/{variant.name}目录
     */
    private void hookMergeResourceTask(MergeResources mergeResTask, ProcessAndroidResources processResTask) {
        // 合并资源任务
        mergeResTask.doLast {
            if (isAapt2Enable(processResTask)) {
                println "${mergeResTask.name}.doLast aapt2 is enabled, compile public.xml to .flat file"
                handleAapt2(mergeResTask, processResTask)
            } else {
                println "${mergeResTask.name}.doLast aapt2 is disabled, use legacy aapt1"
                handleAapt(mergeResTask)
            }
        }
    }

    /**
     * hook aapt生成arsc和R.java文件的task，重写arsc文件剔除多余的文件
     */
    private void hookProcessResourceTask(ProcessAndroidResources processResTask,
                                         ApkVariant apkVariant) {
        if (!pluginExt.stripResource) {
            println "No need to strip host resources from plugin arsc file"
            return
        }
        // 处理资源任务
        processResTask.doLast { ProcessAndroidResources par ->
            // rewrite resource
            println "${processResTask.name} doLast execute start, rewrite generated arsc file"
            reWriteArscFile(processResTask, apkVariant)
        }
    }

    /**
     * hook Manifest插入特定的信息
     */
    private void hookManifestProcessTask(ManifestProcessorTask manifestProcessorTask) {

        if (!pluginExt.stripResource) {
            println "No need to strip host resources, so do not modify manifest"
            return
        }

        manifestProcessorTask.doLast {
            File manifest
            try {
                manifest = manifestProcessorTask.manifestOutputFile
            } catch (Throwable tr) {
                tr.printStackTrace()
                // AGP 3.0.0, changed
                println manifestProcessorTask.manifestOutputDirectory
                manifest = new File(manifestProcessorTask.manifestOutputDirectory, "AndroidManifest.xml")
            }

            if (!manifest.exists()) {
                throw new GradleException("AndroidManifest.xml not exist for ManifestProcessTask")
            }

            def android = new Namespace('http://schemas.android.com/apk/res/android', 'android')

            def root = new XmlParser().parse(manifest)
            def app = root.application[0]
            def meta = new Node(app, "meta-data")
            meta.attributes().put(android.name, "pluginapp_res_merge")
            meta.attributes().put(android.value, "true")
            // rewrite the manifest file
            XmlUtil.serialize(root, new FileOutputStream(manifest))
        }
    }

    /**
     * hook dex生成的task，重写java class文件
     */
    private void hookDexTask(Task dexTask) {
        if (!pluginExt.dexModify || dexTask == null) {
            println "dexTask is null or dexModify is disabled, ${pluginExt.dexModify}"
            return
        }

        dexTask.doFirst {
            println "${dexTask.name} doFirst execute start, modify Activity classes"
            DexProcessor processor = new DexProcessor()
            String intermediatesPath = FileUtils.join(project.buildDir, "intermediates")
            dexTask.inputs.files.files.each { file ->
                if (!file.absolutePath.startsWith(intermediatesPath)) {
                    return
                }

                println "${dexTask.name} task input file: ${file}"
                if (file.isDirectory()) {
                    processor.processDir(file)
                } else if (file.name.endsWith(".jar")) {
                    processor.processJar(file)
                } else if (file.name.endsWith(".class")) {
                    processor.processClass(file)
                } else {
                    println "${dexTask.name} task, other input file ${file}"
                }
            }
        }
    }

    /**
     * 处理aapt编译之后的产物
     * 1. 解压resource_{variant.name}.ap_文件到目录，该文件是一个Zip包，包含编译后的AndroidManifest、res目录和resources.arsc文件
     * 2. 收集插件apk的全量资源和宿主的资源，计算出最终需要保留在插件apk里的资源，根据packageId给插件独有的资源重新分配id
     * 3. 从插件res目录中删除宿主的资源，修改资源id和关联的xml文件
     * 4. 从resource_{variant_name}.ap_压缩文件中删除有变动的资源，然后通过aapt add命令重新添加进该文件
     * 5. 重新生成R.java，该文件含有全量的资源id
     *
     * @param par
     * @param variant
     */
    private void reWriteArscFile(ProcessAndroidResources par, ApkVariant variant) {

        File apFile
        if (pluginExt.isHigherAGP) {
            apFile = new File(par.resPackageOutputFolder, "resources-${variant.name}.ap_")
        } else {
            apFile = par.packageOutputFile
        }
        def resourcesDir = new File(apFile.parentFile, Files.getNameWithoutExtension(apFile.name))
        /** clean up last build resources */
        resourcesDir.deleteDir()
        /** Unzip resourece-${variant.name}.ap_ to resourceDir */
        project.copy {
            from project.zipTree(apFile)
            into resourcesDir

            include 'AndroidManifest.xml'
            include 'resources.arsc'
            include 'res/**/*'
        }
        /** collect host resource and plugin resources */
        ResourceCollector resourceCollector = new ResourceCollector(project, par, variant)
        resourceCollector.collect()

        def retainedTypes = convertResourcesForAapt(resourceCollector.pluginResources)
        def retainedStyleables = convertStyleablesForAapt(resourceCollector.pluginStyleables)
        def resIdMap = resourceCollector.resIdMap

        def rSymbolFile = pluginExt.isHigherAGP ? par.textSymbolOutputFile : new File(par.textSymbolOutputDir, 'R.txt')
        def libRefTable = ["${pluginExt.packageId}": (pluginExt.isHigherAGP ? par.originalApplicationId : par.packageForR)]

        def filteredResources = [] as HashSet<String>
        def updatedResources = [] as HashSet<String>

        def aapt = new Aapt(resourcesDir, rSymbolFile, android.buildToolsRevision)

        /** Delete host resources, must do it before aapt#filterPackage */
        aapt.filterResources(retainedTypes, filteredResources)
        /** Modify the arsc file, and replace ids of related xml files */
        aapt.filterPackage(retainedTypes, retainedStyleables, pluginExt.packageId, resIdMap, libRefTable, updatedResources)

        /**
         * Delete filtered entries (host resources) and then add updated resources into resourece-${variant-name}.ap_
         * Cause there is no 'aapt upate; command supported, so for the updated resources
         * we also delete first and run 'aapt add' later
         */
        ZipUtil.with(apFile).deleteAll(filteredResources + updatedResources)
        /**
         * Re-add updated entries
         * $ aapt add resources.ap_ file1 file2
         */
        project.exec {
            executable par.buildTools.getPath(BuildToolInfo.PathId.AAPT)
            workingDir resourcesDir
            args 'add', apFile.path
            args updatedResources
            // store the output instead of printing to the console
            standardOutput = new ByteArrayOutputStream()
        }

        updateRJava(aapt, par.sourceOutputDir, variant, resourceCollector)
    }

    /**
     * We use the third party library to modify the ASRC file,
     * this method used to transform resource data into the structure of the library
     * @param pluginResources Map of plugin resources
     */
    def static convertResourcesForAapt(ListMultimap<String, ResourceEntry> pluginResources) {
        def retainedTypes = []
        retainedTypes.add(0, [name: 'placeholder', id: Aapt.ID_NO_ATTR, entries: []])//attr 占位

        pluginResources.keySet().each { resType ->
            def firstEntry = pluginResources.get(resType).get(0)
            def typeEntry = [type   : "int", name: resType,
                             id     : parseTypeIdFromResId(firstEntry.resourceId),
                             _id    : parseTypeIdFromResId(firstEntry.newResourceId),
                             entries: []]

            pluginResources.get(resType).each { resEntry ->
                typeEntry.entries.add([
                        name: resEntry.resourceName,
                        id  : parseEntryIdFromResId(resEntry.resourceId),
                        _id : parseEntryIdFromResId(resEntry.newResourceId),
                        v   : resEntry.resourceId, _v: resEntry.newResourceId,
                        vs  : resEntry.hexResourceId, _vs: resEntry.hexNewResourceId])
            }

            if (resType == 'attr') {
                retainedTypes.set(0, typeEntry)
            } else {
                retainedTypes.add(typeEntry)
            }
        }

        return retainedTypes
    }

    /**
     * Transform styleable data into the structure of the aapt library
     * @param pluginStyleables Map of plugin styleables
     */
    def static convertStyleablesForAapt(List<StyleableEntry> pluginStyleables) {
        def retainedStyleables = []
        pluginStyleables.each { styleableEntry ->
            retainedStyleables.add([vtype: styleableEntry.valueType,
                                    type : 'styleable',
                                    key  : styleableEntry.name,
                                    idStr: styleableEntry.value])
        }
        return retainedStyleables
    }

    /**
     * Parse the type part of a android resource id
     */
    def static parseTypeIdFromResId(int resourceId) {
        resourceId >> 16 & 0xFF
    }

    /**
     * Parse the entry part of a android resource id
     */
    def static parseEntryIdFromResId(int resourceId) {
        resourceId & 0xFFFF
    }

    /**
     * Because the resource ID has changed, we need to regenerate the R.java file,
     * include the all resources R, plugin resources R, and R files of retained aars
     *
     * @param aapt Class to expand aapt function
     * @param sourceOutputDir Directory of R.java files generated by aapt
     *
     */
    def updateRJava(Aapt aapt, File sourceOutputDir, BaseVariant apkVariant, ResourceCollector resourceCollector) {

        sourceOutputDir.deleteDir()
        // update app module R.java file
        def packagePath = apkVariant.applicationId.replace('.'.charAt(0), File.separatorChar)
        def rSourceFile = new File(sourceOutputDir, "${packagePath}${File.separator}R.java")
        aapt.generateRJava(rSourceFile, apkVariant.applicationId, resourceCollector.allResources, resourceCollector.allStyleables)

        def splitRSourceFile = new File(sourceOutputDir.parentFile, "plugin${File.separator}${packagePath}${File.separator}R.java")
        aapt.generateRJava(splitRSourceFile, apkVariant.applicationId, resourceCollector.pluginResources, resourceCollector.pluginStyleables)
        pluginExt.splitRJavaFile = splitRSourceFile

        // update aar library module R.java file
       resourceCollector.retainedAarLibs.each {
            def aarPackage = it.package
            def rJavaFile = new File(sourceOutputDir, "${aarPackage.replace('.'.charAt(0), File.separatorChar)}${File.separator}R.java")
            aapt.generateRJava(rJavaFile, aarPackage, it.aarResources, it.aarStyleables)
        }

        resourceCollector.dump()
    }


    private void handleAapt2(MergeResources mergeResTask, ProcessAndroidResources processResTask) {
        File aapt2File = getAapt2File(processResTask)
        int i = 0
        android.sourceSets.main.res.srcDirs.each { File resDir ->
            File srcFile = new File(resDir, 'values/public.xml')
            if (srcFile.exists()) {
                def name = i++ == 0 ? "public.xml" : "public_${i}.xml"
                File dstFile = new File(resDir, "values/${name}")
                srcFile.renameTo(dstFile)
                String[] commands = [
                        aapt2File.absolutePath, 'compile', '--legacy', '-o', mergeResTask.outputDir, dstFile.path
                ]
                commands.execute()
            }
        }
    }

    private void handleAapt(MergeResources mergeResTask) {
        project.copy {
            int i = 0
            from(project.android.sourceSets.main.res.srcDirs) {
                include 'values/public.xml'
                rename 'public.xml', (i++ == 0 ? "public.xml" : "public_${i}.xml")
            }
            into(mergeResTask.outputDir)
        }
    }


    private boolean isAapt2Enable(ProcessAndroidResources processResTask) {
        if (processResTask) {
            try {
                // Add in AGP-3.0.0
                return processResTask.isAapt2Enabled()
            } catch (Throwable t) {
                t.printStackTrace()
            }
        }

        return AndroidGradleOptions.isAapt2Enabled(project)
    }

    private File getAapt2File(ProcessAndroidResources task) {
        String path = null
        try {
            def buildToolInfo = task.getBuilder().getBuildToolInfo()
            Map paths = buildToolInfo.mPaths
            def entry = paths.find { key, value ->
                (key.name().equalsIgnoreCase('AAPT2') || key.name().equalsIgnoreCase('DAEMON_AAPT2')) &&
                        key.isPresentIn(buildToolInfo.revision)
            }
            path = entry?.value
        } catch (Exception ignore) {
        }
        if (path == null) {
            path = "${project.android.sdkDirectory}${File.separator}" +
                    "build-tools${File.separator}" +
                    "${project.android.buildToolsVersion}${File.separator}" +
                    "aapt2${Os.isFamily(Os.FAMILY_WINDOWS) ? '.exe' : ''}"
        }
        println "aapt2Path: $path"
        File aapt2File = new File(path)
        if (!aapt2File.exists()) {
            throw new GradleException('aapt2 is missing')
        }
        return aapt2File
    }


    private QYPluginExtension getPluginExt() {
        return project.qyplugin
    }
}
