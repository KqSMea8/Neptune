# Neptune

![license](http://img.shields.io/badge/license-Apache2.0-brightgreen.svg?style=flat)
![Release Version](https://img.shields.io/badge/release-2.5.0-red.svg)
![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)

**Neptune是爱奇艺研发的一套灵活，稳定，轻量级的插件化方案。它可以在数以百万级的Android设备上动态加载和运行插件apk。**

该框架支持了爱奇艺数十个独立业务的发展和需求，比如爱奇艺文学，电影票，爱奇艺直播等。

随着Android P的公测和发布，我们的框架遇到了非限制性私有SDK接口访问的限制。短时间内，我们及时进行了跟进测试和适配。现在Neptune已经完全兼容Android P，只有少数几个浅灰名单中的API被使用到。Neptune可以在Android P上无缝且稳定地运行。


# Android P兼容性

在Neptune项目中，只有在浅灰名单中的私有API会被访问，没有使用到深灰和黑名单中的API。

| API List | API Used cnt |
| :----    | :---- |
| Black list | 0 |
| Dark grey list | 0 |
| light grey list | 9 |

### 细节

```
Accessing hidden field Landroid/app/ActivityThread;->mInstrumentation:Landroid/app/Instrumentation; (light greylist, reflection)
Accessing hidden method Ldalvik/system/VMRuntime;->getCurrentInstructionSet()Ljava/lang/String; (light greylist, reflection)
Accessing hidden method Landroid/content/res/AssetManager;->addAssetPath(Ljava/lang/String;)I (light greylist, reflection)
Accessing hidden method Landroid/app/Instrumentation;->execStartActivity(Landroid/content/Context;Landroid/os/IBinder;Landroid/os/IBinder;Landroid/app/Activity;Landroid/content/Intent;ILandroid/os/Bundle;)Landroid/app/Instrumentation$ActivityResult; (light greylist, reflection)
Accessing hidden field Landroid/view/ContextThemeWrapper;->mResources:Landroid/content/res/Resources; (light greylist, reflection)
Accessing hidden field Landroid/app/Activity;->mApplication:Landroid/app/Application; (light greylist, reflection)
Accessing hidden field Landroid/content/ContextWrapper;->mBase:Landroid/content/Context; (light greylist, reflection)
Accessing hidden field Landroid/app/Activity;->mInstrumentation:Landroid/app/Instrumentation; (light greylist, reflection)
Accessing hidden field Landroid/app/Activity;->mActivityInfo:Landroid/content/pm/ActivityInfo; (light greylist, reflection)
```

除了`ActivityThread#mInstrumentation`和`AssetManager#addAssetPath()`必须要使用到。其他浅灰名单中的API，我们提供了另外的方式去规避风险。我们为插件开发提供常用的基类PluginActivity作为父类继承，在插件编译期通过Gradle Plugin动态修改插件Activity的父类。


# 支持的特性

| 特性 | 描述  |
| :------ | :-----: |
| 组件 | Activity/Service/Receiver |
| 主程序Manifest注册 | 不需要 |
| 共享宿主代码 | 支持 |
| 共享宿主资源 | 支持 |
| 资源隔离 | 支持 |
| 独立运行插件 | 支持 |
| Android特性 | 支持几乎所有 |
| 兼容性  | 几乎市面上所有ROM |
| 进程隔离 | 支持 |
| 插件之间相互依赖  | 支持 |
| 插件开发  | 接近原生APP开发 |
| 支持的Android版本 | API Level 14+ |

# Architecture

![plugin_arch](plugin_arch.png)

# Getting Started

## Host Project

在App模块的`build.gradle`中compile移入Neptune库

```Gradle
    compile 'org.qiyi.video:neptune:2.5.0'
```

在`Application#onCreate()`阶段初始化NoahDocker

```Java
public class XXXApplication extends Application {
    
    @Override
    public void onCreate() {
        NeptuneConfig config = new NeptuneConfig.NeptuneConfigBuilder()
                    .configSdkMode(NeptuneConfig.INSTRUMENTATION_MODE)
                    .enableDebug(BuildConfig.DEBUG)
                    .build();
        Neptune.init(this, config);
    }
}
```

更多细节和开发指南请参考wiki。

## Plugin Project

如果插件APP需要共享宿主APP的一些资源，你需要在插件工程根目录下的`build.gradle`中的`buildscript`块中添加如下依赖

```Gradle
dependencies {
    classpath  'com.iqiyi.tools.build:plugin-gradle:1.1.0'
}
```

在App模块的`build.gradle`中应用gradle插件并添加相应配置

```Gradle
apply plugin: 'com.qiyi.neptune.plugin'

neptune {
    pluginMode = true      // In plugin apk build mode
    packageId = 0x30       // The packge id of Resources
    hostDependencies = "{group1}:{artifact1};{group2}:{artifact2}" // host app resources dependencies
}
```

# Developer Guide

* [API文档见wiki](http://gitlab.qiyi.domain/mobile-android/baseline-sh/QYPlugin/wikis/home)
* [宿主APP的示例工程](samples/HostApp)
* [插件APP的示例工程](samples/PluginApp)
* [阅读SDKLibrary的源码](SdkLibrary)

# Contribution

我们真诚的欢迎任何有价值的PR提交，包括代码，建议和文档。

# License

Neptune is [Apache v2.0 Licensed](LICENSE.md).

