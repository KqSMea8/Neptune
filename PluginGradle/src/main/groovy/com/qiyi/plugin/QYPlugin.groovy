package com.qiyi.plugin


import com.android.build.gradle.api.ApkVariant
import com.android.builder.Version
import com.qiyi.plugin.hooker.TaskHookerManager
import com.qiyi.plugin.utils.Utils
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class QYPlugin implements Plugin<Project> {
    Project project
    /** plugin extension */
    QYPluginExtension pluginExt

    @Override
    void apply(Project project) {

        if (!project.plugins.hasPlugin("com.android.application")) {
            throw new GradleException("com.android.application not found, QYPlugin can be only apply to android application module")
        }

        pluginExt = project.extensions.create("qyplugin", QYPluginExtension)
        this.project = project

        project.afterEvaluate {
            // init plugin extension
            project.android.applicationVariants.each { ApkVariant variant ->

                pluginExt.with {
                    isHigherAGP = Utils.compareVersion(Version.ANDROID_GRADLE_PLUGIN_VERSION, "3.0.0") >= 0
                    packageName = variant.applicationId
                    packagePath = packageName.replace('.'.charAt(0), File.separatorChar)
                }
            }
            // gather public.xml files
            project.android.sourceSets.main.res.srcDirs.each { File dir ->
                File xml = new File(dir, "values/public.xml")
                if (xml.exists()) {
                    pluginExt.publicXml(xml)
                }
            }

            println "current AGP version ${Version.ANDROID_GRADLE_PLUGIN_VERSION}, isHigherAGP=${pluginExt.isHigherAGP}"
        }
        // 注册hook task相关任务
        TaskHookerManager taskHooker = new TaskHookerManager(project)
        taskHooker.registerTaskHooker()
    }
}