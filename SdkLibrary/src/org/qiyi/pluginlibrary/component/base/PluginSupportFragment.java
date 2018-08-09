package org.qiyi.pluginlibrary.component.base;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentHostCallback;
import android.support.v4.app.PluginFragmentHostCallback;

import org.qiyi.pluginlibrary.component.wraper.ActivityWrapper;
import org.qiyi.pluginlibrary.constant.IIntentConstant;
import org.qiyi.pluginlibrary.context.PluginContextWrapper;
import org.qiyi.pluginlibrary.runtime.PluginLoadedApk;
import org.qiyi.pluginlibrary.runtime.PluginManager;
import org.qiyi.pluginlibrary.utils.FragmentPluginHelper;
import org.qiyi.pluginlibrary.utils.ReflectionUtils;

/**
 * 插件的基类 Fragment，所有插件的 Fragment 都应该继承自此类，以保证 Fragment 可以在宿主里面展示。
 */
public class PluginSupportFragment extends Fragment implements IPluginBase {
    private static final String TAG = "PluginSupportFragment";
    private String mPluginPackageName;
    /**
     * 是否在宿主运行，true 在宿主运行， false 在插件独立运行
     */
    private boolean mInHost;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // 如果当前 ClassLoader 与 context 的 classLoader 不一致，则为宿主运行
        mInHost = getClass().getClassLoader() != context.getClassLoader();
        if (mInHost) {
            initPackageName();
            modifyHostContextForPlugin();
        }
    }

    private void modifyHostContextForPlugin() {
        if (mPluginPackageName != null) {
            PluginLoadedApk loadedApk = PluginManager.getPluginLoadedApkByPkgName(mPluginPackageName);
            if (loadedApk != null) {
                Object host = ReflectionUtils.on(this).get("mHost");
                if (!(host instanceof PluginFragmentHostCallback)) {
                    // 替换 mHost, 不能直接修改原来的 mHost，因为它在各个 Fragment 间共享
                    FragmentActivity activity = ReflectionUtils.on(host).get("mActivity");
                    ActivityWrapper wrapper = new ActivityWrapper(activity, mPluginPackageName);
                    PluginFragmentHostCallback newHost = new PluginFragmentHostCallback((FragmentHostCallback<FragmentActivity>) host, wrapper);
                    ReflectionUtils.on(this).set("mHost", newHost);
                }
            }
        }
    }

    private void initPackageName() {
        if (getArguments() != null) {
            mPluginPackageName = getArguments().getString(IIntentConstant.EXTRA_TARGET_PACKAGE_KEY);
        }
        /*
        // 插件 Fragment 需要创建自己的 Child Fragment，会丢失 package name，
        // 如果需要这样的场景需求，可以使用下面的方式获取 package name
        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader instanceof PluginClassLoader) {
            mPluginPackageName = ((PluginClassLoader) classLoader).getPackageName();
        }
        */
    }

    @Override
    public String getPluginPackageName() {
        return mPluginPackageName;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mInHost) {
            FragmentPluginHelper.disableViewSaveInstanceRecursively(getView());
        }
        super.onSaveInstanceState(outState);
    }
}
