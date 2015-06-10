package org.qiyi.plugin.manager;

import org.qiyi.pluginlibrary.ProxyEnvironment;
import org.qiyi.pluginlibrary.api.ITargetLoadedCallBack;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

/**
 * 插件控制器
 * 
 */
public class TargetActivatorNew {

//    private static ITargetActivatorNew iTargetActivatorNew;

    /**
     * 加载并启动插件
     * @param context
     * @param intent 包含目标Component
     */
    public static void loadTargetAndRun(final Context context, final Intent intent) {

/*        if(iTargetActivatorNew != null){
            iTargetActivatorNew.loadTargetAndRun(context, intent);
        }*/

    	 ProxyEnvironmentNew.enterProxy(context, null,intent);
    }
    
    /**
     * 静默加载插件，异步加载，可以设置callback
     * 
     * @param context
     *            application Context
     * @param packageName
     *            插件包名
     * @param callback
     *            加载成功的回调
     */
    public static void loadTarget(final Context context, final String packageName, 
            final ITargetLoadedCallBack callback) {

        // 插件已经加载
        if (ProxyEnvironmentNew.isEnterProxy(packageName)) {
            callback.onTargetLoaded(packageName);
            return;
        }

        if (callback == null) {
            return;
        }

        BroadcastReceiver recv = new BroadcastReceiver() {
            public void onReceive(Context ctx, Intent intent) {
                String curPkg = intent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME);
                if (ProxyEnvironment.ACTION_TARGET_LOADED.equals(intent.getAction())
                        && TextUtils.equals(packageName, curPkg)) {
                	PluginDebugLog.log("plugin", "收到自定义的广播org.qiyi.pluginapp.action.TARGET_LOADED");
                    callback.onTargetLoaded(packageName);
                    context.getApplicationContext().unregisterReceiver(this);
                }
            };
        };
        PluginDebugLog.log("plugin", "注册自定义广播org.qiyi.pluginapp.action.TARGET_LOADED");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ProxyEnvironment.ACTION_TARGET_LOADED);
        context.getApplicationContext().registerReceiver(recv, filter);

        Intent intent = new Intent();
        intent.setAction(ProxyEnvironment.ACTION_TARGET_LOADED);
        intent.setComponent(new ComponentName(packageName, recv.getClass().getName()));
        ProxyEnvironmentNew.enterProxy(context,null,intent);
    }

/*    public static void setiTargetActivatorNew(ITargetActivatorNew iTargetActivatorNew) {
        TargetActivatorNew.iTargetActivatorNew = iTargetActivatorNew;
    }*/

/**
     * 加载并启动插件
     * 
     * @param context
     *            host的Activity
     * @param componentName
     *            目标Component
     * @param creator
     *            loading界面创建器
     */
//    public static void loadTargetAndRun(final Context context, final Intent intent, ILoadingViewCreator creator) {
//        ProxyEnvironment.putLoadingViewCreator(intent.getComponent().getPackageName(), creator);
//        loadTargetAndRun(context, intent);
//    }

    /**
     * 启动搭理service
     * @param context
     * @param intent
     */
//    public static void startSeviceProxy(final Context context, final Intent intent){
//    	loadTargetAndRun(context,intent);//启动service
//    }
    
    
//    public static void bindServiceProxy(final Context context,final Intent intent,ServiceConnection conn,int flags){
//        loadTargetAndRun(context,intent,conn,flags);//启动service
//    }
    
    
//    public static void loadTargetAndRun(final Context context, final Intent intent,ServiceConnection conn,int flags) {
//    	intent.putExtra(ProxyEnvironment.BIND_SERVICE_FLAGS, flags);
//        ProxyEnvironment.enterProxy(context,conn,intent);
//    }
    
    /**
     * 启动activity 并将activity 处理的结果返回
     *
     * 插件已全部改为运行在独立进城，先注释掉没有使用的方法
     *
     * @param intent
     * @param requestCode
     */
/*    public static void startActivityForResult(final Activity mParent,Intent intent , int requestCode){
    	PluginDebugLog.log("plugin", "startActivityForResult:" + requestCode);
    	intent.putExtra("requestCode", requestCode);
        loadTargetAndRun(mParent, intent);
//    	ProxyEnvironmentNew.enterProxy(mParent, null,intent);
    }*/
    

    /**
     * 加载并启动插件
     *
     * 插件已全部改为运行在独立进城，先注释掉没有使用的方法
     *
     * @param context
     *            host的Activity
     * @param componentName
     *            目标Component
     */
/*    private static void loadTargetAndRun(final Context context, final ComponentName componentName) {
        Intent intent = new Intent();
        intent.setComponent(componentName);
        loadTargetAndRun(context, intent);
    }*/

    /**
     * 加载并启动插件
     * 
     * @param context
     *            host的Activity
     * @param componentName
     *            目标Component
     * 
     * @param creator
     *            loading界面创建器
     */
//    public static void loadTargetAndRun(final Context context, final ComponentName componentName,
//            ILoadingViewCreator creator) {
//        ProxyEnvironment.putLoadingViewCreator(componentName.getPackageName(), creator);
//        loadTargetAndRun(context, componentName);
//    }

    /**
     * 加载并启动插件
     *
     * 插件已全部改为运行在独立进城，先注释掉没有使用的方法
     *
     * @param context
     *            host的application context
     * @param packageName
     *            插件包名
     */
/*    public static void loadTargetAndRun(final Context context, String packageName) {
        loadTargetAndRun(context, new ComponentName(packageName, ""));
    }*/

    /**
     * 加载并启动插件
     * 
     * @param context
     *            host的application context
     * @param packageName
     *            插件包名
     * @param creator
     *            插件loading界面的创建器
     */
//    public static void loadTargetAndRun(final Context context, String packageName, ILoadingViewCreator creator) {
//        ProxyEnvironment.putLoadingViewCreator(packageName, creator);
//        loadTargetAndRun(context, new ComponentName(packageName, ""));
//    }

    /**
     * 静默加载插件，异步加载
     *
     * 插件已全部改为运行在独立进城，先注释掉没有使用的方法
     *
     * @param context
     *            application Context
     * @param packageName
     *            插件包名
     */
/*    public static void loadTarget(final Context context, String packageName) {
        loadTargetAndRun(context, new ComponentName(packageName, ProxyEnvironment.EXTRA_VALUE_LOADTARGET_STUB));
    }*/
    
    /**
     * 静默加载插件，异步加载，可以设置callback
     *
     * 插件已全部改为运行在独立进城，先注释掉没有使用的方法
     *
     * @param context
     *            application Context
     * @param packageName
     *            插件包名
     * @param callback
     *            加载成功的回调
     */
/*    public static void loadTarget(final Context context, final String packageName,
            final ITargetLoadedCallBack callback) {

        // 插件已经加载
        if (ProxyEnvironmentNew.isEnterProxy(packageName)) {
            callback.onTargetLoaded(packageName);
            return;
        }

        if (callback == null) {
            loadTarget(context, packageName);
            return;
        }

        BroadcastReceiver recv = new BroadcastReceiver() {
            public void onReceive(Context ctx, Intent intent) {
                String curPkg = intent.getStringExtra(ProxyEnvironment.EXTRA_TARGET_PACKAGNAME);
                if (ProxyEnvironment.ACTION_TARGET_LOADED.equals(intent.getAction())
                        && TextUtils.equals(packageName, curPkg)) {
                	PluginDebugLog.log("plugin", "收到自定义的广播org.qiyi.pluginapp.action.TARGET_LOADED");
                    callback.onTargetLoaded(packageName);
                    context.getApplicationContext().unregisterReceiver(this);
                }
            }
        };
        PluginDebugLog.log("plugin", "注册自定义广播org.qiyi.pluginapp.action.TARGET_LOADED");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ProxyEnvironment.ACTION_TARGET_LOADED);
        context.getApplicationContext().registerReceiver(recv, filter);

        Intent intent = new Intent();
        intent.setAction(ProxyEnvironment.ACTION_TARGET_LOADED);
        intent.setComponent(new ComponentName(packageName, recv.getClass().getName()));
        ProxyEnvironmentNew.enterProxy(context,null,intent);
    }*/

    /**
     * 获取 package 对应的 classLoader。一般情况下不需要获得插件的classloader。 只有那种纯 jar
     * sdk形式的插件，需要获取classloader。 获取过程为异步回调的方式。此函数，存在消耗ui线程100ms-200ms级别。
     *
     * 插件已全部改为运行在独立进城，先注释掉没有使用的方法
     *
     * @param context
     *            application Context
     * @param packageName
     *            插件包名
     * @param callback
     *            回调，classloader 通过此异步回调返回给hostapp
     */
/*    public static void loadAndGetClassLoader(final Context context, final String packageName,
            final IGetClassLoaderCallback callback) {
        
        loadTarget(context, packageName, new ITargetLoadedCallBack() {

            @Override
            public void onTargetLoaded(String packageName) {
            	try {
            		ProxyEnvironmentNew.initProxyEnvironment(context, packageName);
            		ProxyEnvironmentNew targetEnv = ProxyEnvironmentNew.getInstance(packageName);
            		ClassLoader classLoader = targetEnv.getDexClassLoader();
            		
            		callback.getClassLoaderCallback(classLoader);
				} catch (Exception e) {
					// TODO: handle exception
				}

            }
        });

    }*/

    /**
     * 加载插件并创建插件内的View，View的Context是插件的Application Context
     * 
     * @param context
     *            host的 application context
     * @param packageName
     *            插件包名
     * @param viewClass
     *            view的类名
     * @param callback
     *            view创建成功的回调
     */
//    public static void loadAndCreateView(Context context, final String packageName, final String viewClass,
//            final ICreateViewCallBack callback) {
//
//        loadTarget(context, packageName, new ITargetLoadedCallBack() {
//
//            @Override
//            public void onTargetLoaded(String packageName) {
//                View view = null;
//                try {
//                    Class<?> targetClass = ProxyEnvironment.getInstance(packageName).getDexClassLoader()
//                            .loadClass(viewClass);
//                    Constructor<?> constructor = targetClass.getConstructor(new Class<?>[] { Context.class });
//                    view = (View) constructor.newInstance(ProxyEnvironment.getInstance(packageName).getApplication());
//                } catch (Exception e) {
//                    Log.e("TargetActivitor", "*** Create View Fail : \r\n" + e.getMessage());
//                }
//                callback.onViewCreated(packageName, view);
//            }
//        });
//
//    }

    /**
     * 注销插件App
     * 插件已全部改为运行在独立进城，先注释掉没有使用的方法
     * @param packageName
     *            插件包名
     */
/*    public static void unLoadTarget(String packageName) {
    	ProxyEnvironmentNew.exitProxy(packageName);
    }*/

    public interface ITargetActivatorNew {
        void loadTargetAndRun(final Context context, final Intent intent);
    }
}
