# NoahDocker [征名]

NoahDocker是一个灵活，稳定，轻量级的插件化方案，有爱奇艺移动端基线Team研发。它可以在数以百万级的Android设备上动态加载和运行插件apk。框架支持了爱奇艺20个多独立业务的发展，比如爱奇艺文学，电影票，爱奇艺直播等。

随着Android P即将发布，我们的框架遇到了非限制性SDK接口访问的挑战。短时间内，我们及时进行了跟进和适配，现在NoahDocker可以无缝运行在Android P设备上，目前只有一个Hook点（ActivityThread中的Instrumentation）。

When Android P is arriving, we meet the non-sdk strict challenge on P. And now, NoahDocker can run on Android P devices seamless with only *ONE* hook (Instrumentaion, same idea with other great plugin frameworks).


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

在App模块的`build.gradle`中compile移入NoahDocker库

```Gradle
    compile 'com.qiyi.video:noahdocker:xxx'
```

在`Application#onCreate()`阶段初始化NoahDocker

```Java
@Override
public void onCreate() {
    HybirdPluginConfig config = new HybirdPluginConfig.HybirdPluginConfigBuilder()
                .configSdkMode(1)
                .useNewCLMode(useNewFeature)
                .useNewResMode(useNewFeature)
                .useNewCompResolveMode(useNewFeature)
                .build();
    HybirdPlugin.init(this, config);

    PluginDebugLog.setIsDebug(BuildConfig.DEBUG);
}
```

更多细节和开发指南请参考wiki。

## Plugin Project

如果插件APP需要共享宿主APP的一些资源，你需要在插件工程根目录下的`build.gradle`中的`buildscript`块中添加如下依赖

```Gradle
dependencies {
    classpath  'com.iqiyi.tools.build:plugin-gradle:1.0.6'
}
```

在App模块的`build.gradle`中应用gradle插件并添加相应配置

```Gradle
apply plugin: 'com.qiyi.plugin'

qyplugin {
    pluginMode = true      // In plugin apk build mode
    packageId = 0x30       // The packge id of Resources
    hostDependencies = "{group1}:{artifact1};{group2}:{artifact2}" // host app resources dependencies
}
```

# Developer Guide

* API文档见wiki
* 宿主APP的示例工程
* 插件APP的示例工程
* 阅读SDKLibray的源码

# Contribution

我们真诚的欢迎任何有价值的PR提交，包括代码，建议和文档。

# License

NoahDocker is [Apache v2.0 Licensed](LICENSE.md).

