package com.qiyi.plugin.collector.dependence

import com.android.build.gradle.api.ApkVariant
import com.android.builder.model.AndroidLibrary
import com.android.utils.FileUtils
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap
import com.google.common.collect.Lists
import com.qiyi.plugin.collector.res.ResourceEntry
import com.qiyi.plugin.collector.res.StyleableEntry
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Represents a AAR dependence from Maven repository or Android library module
 *
 * @author zhengtao
 */
class AarDependenceInfo extends DependenceInfo {

    /**
     * Android library dependence in android build system, delegate of AarDependenceInfo
     * android gradle plugin below 2.3.3: refer to AndroidDependency
     * android gradle plugin 2.2.x or above 3.0.0: refer to AndroidLibrary
     */
    Object dependency

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

    File aarRSymbolFile

    AarDependenceInfo(String group, String artifact, String version, Object dependency) {
        super(group, artifact, version)
        this.dependency = dependency
        this.aarManifestFile = dependency.manifest
        this.aarRSymbolFile = dependency.symbolFile
    }

    String getName() {
        return dependency.name
    }

    @Override
    File getJarFile() {
        return dependency.jarFile
    }

    @Override
    DependenceType getDependenceType() {
        return DependenceType.AAR
    }

    File getManifest() {
        return dependency.manifest
    }

    File getSymbolFile() {
        return dependency.symbolFile
    }

    /**
     * Return collection of "resourceType:resourceName", parse from R symbol file
     * @return set of a combination of resource type and name
     */
    public Set<String> getResourceKeys() {

        def resKeys = [] as Set<String>

        def rSymbol = aarRSymbolFile
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

        String projectName = (dependency instanceof AndroidLibrary && dependency.project != null && !dependency.project.isEmpty()) ?
                dependency.project : artifact
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

            if (!aarManifestFile.exists()) {
                throw new GradleException("Android Library Project ${projectName} R.txt not found")
            }
        }

        return this
    }

    /**
     * 修复AGP 3.0.0, R.txt文件指向路径不对的问题
     * 修复AGP 3.1.0+, bundle目录不存在R.txt文件找不到的问题
     */
    public AarDependenceInfo fixRSymbol(Project project, ApkVariant apkVariant) {
        if (symbolFile.exists()) {
            aarRSymbolFile = symbolFile
            return this
        }

        String projectName = (dependency instanceof AndroidLibrary && dependency.project != null && !dependency.project.isEmpty()) ?
                dependency.project : artifact
        Project subProject = project.rootProject.findProject(projectName)
        if (subProject != null) {
            File bundlesDir = new File(subProject.buildDir, "intermediates/bundles")
            File targetSymbol = FileUtils.join(bundlesDir, apkVariant.buildType.name, "R.txt")
            if (targetSymbol.exists()) {
                aarRSymbolFile = targetSymbol
                return this
            }

            File symbolDir = new File(subProject.buildDir, "intermediates/symbols")
            targetSymbol = FileUtils.join(symbolDir, apkVariant.buildType.name)
            if (apkVariant.flavorName != null && apkVariant.flavorName != "") {
                targetSymbol = FileUtils.join(targetSymbol, apkVariant.flavorName)
            }
            targetSymbol = FileUtils.join(targetSymbol, "R.txt")
            if (targetSymbol.exists()) {
                aarRSymbolFile = targetSymbol
                return this
            }

            if (!aarRSymbolFile.exists()) {
                throw new GradleException("Android Library Project ${projectName} R.txt not found")
            }
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