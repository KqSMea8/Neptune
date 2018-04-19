package com.qiyi.plugin.collector.dependence

import com.android.build.gradle.api.ApkVariant
import com.android.builder.dependency.level2.AndroidDependency
import com.android.utils.FileUtils
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap
import com.google.common.collect.Lists
import com.qiyi.plugin.collector.res.ResourceEntry
import com.qiyi.plugin.collector.res.StyleableEntry
import org.gradle.api.Project

/**
 * Represents a AAR dependence from Maven repository or Android library module
 *
 * @author zhengtao
 */
class AarDependenceInfo extends DependenceInfo {

    /**
     * Android library dependence in android build system, delegate of AarDependenceInfo
     */
    @Delegate AndroidDependency dependency

    /**
     * All resources(e.g. drawable, layout...) this library can access
     * include resources of self-project and dependence(direct&transitive) project
     */
    ListMultimap<String, ResourceEntry> aarResources = ArrayListMultimap.create()
    /**
     * All styleables this library can access, like "aarResources"
     */
    List<StyleableEntry> aarStyleables = Lists.newArrayList()

    File aarManifestFile

    AarDependenceInfo(String group, String artifact, String version, AndroidDependency dependency) {
        super(group, artifact, version)
        this.dependency = dependency
        this.aarManifestFile = manifest
    }

    @Override
    File getJarFile() {
        return dependency.jarFile
    }

    @Override
    DependenceType getDependenceType() {
        return DependenceType.AAR
    }

    /**
     * Return collection of "resourceType:resourceName", parse from R symbol file
     * @return set of a combination of resource type and name
     */
    public Set<String> getResourceKeys() {

        def resKeys = [] as Set<String>

        def rSymbol = symbolFile
        if (rSymbol.exists()) {
            rSymbol.eachLine { line ->
                if (!line.empty) {
                    def tokenizer = new StringTokenizer(line)
                    def valueType = tokenizer.nextToken()
                    def resType = tokenizer.nextToken()       // resource type (attr/string/color etc.)
                    def resName = tokenizer.nextToken()       // resource name

                    resKeys.add("${resType}:${resName}")
                }
            }
        }

        return resKeys
    }

    /**
     * 修复AGP 3.0.0+， bundle目录下没有AndroidManifest.xml的问题
     */
    public AarDependenceInfo fixAarManifest(Project project, ApkVariant apkVariant) {
        if (manifest.exists()) {
            aarManifestFile = manifest
            return this
        }

        String projectName = artifact
        Project subProject = project.rootProject.findProject(projectName)
        if (subProject != null) {
            File manifestsDir = new File(subProject.buildDir, "intermediates/manifests")
            File targetManifest = FileUtils.join(manifestsDir, "full", apkVariant.buildType.name)
            if (apkVariant.flavorName != null && apkVariant.flavorName != "") {
                targetManifest = FileUtils.join(targetManifest, apkVariant.flavorName)
            }
            targetManifest = FileUtils.join(targetManifest, "AndroidManifest.xml")
            if (targetManifest.exists()) {
                aarManifestFile = targetManifest
                return this
            }
            targetManifest = FileUtils.join(manifestsDir, "aapt", apkVariant.buildType.name)
            if (apkVariant.flavorName != null && apkVariant.flavorName != "") {
                targetManifest = FileUtils.join(targetManifest, apkVariant.flavorName)
            }
            targetManifest = FileUtils.join(targetManifest, "AndroidManifest.xml")
            if (targetManifest.exists()) {
                aarManifestFile = targetManifest
                return this
            }
            aarManifestFile = subProject.android.sourceSets.main.manifest.srcFile
        }

        return this
    }


    /**
     * Return the package name of this library, parse from manifest file
     * manifest file are obtained by delegating to "dependency"
     * @return package name of this library
     */
    public String getPackage() {
        def xmlManifest = new XmlParser().parse(aarManifestFile)
        return xmlManifest.@package
    }
}