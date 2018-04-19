## 插件中心常见问题

#### 我的插件有一个新版本3.0，希望只在基线版本7.9和以后的版本让用户使用。如何做到？

有两种途径可以解决
1. 在基线7.9发版时告知基线，修改配置在基线代码里的对应插件的新版本号3.0。 （联系人：袁泽瑶、刘纯）
2. 修改插件中心后台配置给7.9的插件的配置项里的最低版本号。（联系人：王兆仲）

#### 我的插件发布了新版本3.0，设置了对应的基线最低版本为7.9，是否会有兼容性问题？

新版本插件升级3.0，配置基线最低版本号为7.9。带来的影响：

用户已安装的旧版本的爱奇艺app升级到基线7.9后，只能使用新版本3.0的插件。旧版本的插件状态显示不可用，需要升级到新版本。
目前所有插件在wifi网会自动静默安装/升级。
非wifi网下。用户点击插件的入口，如未安装，会得到手动升级的提示。
旧版本的基线app（比如7.8），已安装旧版本插件（比如2.9），插件的使用不受影响。不存在兼容性问题。
插件在独立包中使用基线allclass中的Fresco注意事项。
插件在独立包中使用allclass中的fresco的时候除了依赖allclass以外，还需要在工程中加入下面两个文件：

attrs_fresco.xml
libimagepipeline.so

#### 插件中序列化对象注意事项：

在插件中类序列化的时候如果使用的是Serializable而不是Parcelable，会导致在Android版本5.0以下的手机上出现崩溃，

崩溃的原因就是反序列化的时候类找不到，这个是Android5.0以下版本的BUG，在Android5.0以下版本setExtrasClassLoader()

对于用Serializable实现的序列化数据不起作用。所以插件中序列化对象需要使用Parcelable实现。



#### 无法加载本地插件


加载本地插件的前提条件如下：

1、爱奇艺App具有读写sd卡权限(Android 6.0+  系统清理数据时，会把权限回收)
2、如果已经安装过线上插件，那么需要卸载之前插件或者清空数据(如果启动过插件，那么需要退出爱奇艺App)
3、在sd卡根目录按照如下规则放置两个文件，pluginpackagename.apk 和pluginpackagename.log(pluginpackagename是插件包名)，以秀场为例:com.iqiyi.ishow.apk和com.iqiyi.ishow.log
4、爱奇艺App 的debug开关打开(这个debug并非Debug包和Releas包的意思，而是AndroidManifist中Application节点的android:debuggable 的值)。

此开关是通过jenkins控制，如果发现自己使用的debuggable熟悉为false，那么可以自己手动触发打包，确保debug_control选择open即可

jenkins打包地址：http://10.13.43.127:8888/job/manual_android_qiyivideo_trunck_build/build