package com.qiyi.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApkVariant
import com.android.builder.model.Version
import com.qiyi.plugin.dex.RClassTransform
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

        def android = project.extensions.getByType(AppExtension)
        boolean highAGP = Utils.compareVersion(Version.ANDROID_GRADLE_PLUGIN_VERSION, "3.0.0") >= 0
        println "current AGP version ${Version.ANDROID_GRADLE_PLUGIN_VERSION}, isHigherAGP=${highAGP}"

        project.afterEvaluate {
            // init plugin extension
            android.applicationVariants.each { ApkVariant variant ->

                pluginExt.with {
                    isHigherAGP = highAGP
                    packageName = variant.applicationId
                    packagePath = packageName.replace('.'.charAt(0), File.separatorChar)
                }
            }

            checkConfig()
        }

        if (highAGP) {
            // 注册修改Class的Transform
            android.registerTransform(new RClassTransform(project))
        }

        // 注册hook task相关任务
        TaskHookerManager taskHooker = new TaskHookerManager(project)
        taskHooker.registerTaskHooker()
    }


    private void checkConfig() {
        if (!pluginExt.pluginMode) {
            // not in plugin compile mode, close all the feature
            pluginExt.stripResource = false
            pluginExt.dexModify = false
        }

        if (pluginExt.packageId <= 0x01 || pluginExt.packageId > 0x7F) {
            throw new GradleException("invalid package Id 0x${Integer.toHexString(pluginExt.packageId)}")
        }

        if (pluginExt.packageId != 0x7F && pluginExt.pluginMode) {
            pluginExt.stripResource = true
        }

        String parameters = "plugin config parameters: pluginMode=${pluginExt.pluginMode}, packageId=0x${Integer.toHexString(pluginExt.packageId)}, stripResource=${pluginExt.stripResource}, dexModify=${pluginExt.dexModify}"
        println parameters
    }
}