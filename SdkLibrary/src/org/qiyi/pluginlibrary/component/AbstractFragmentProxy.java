package org.qiyi.pluginlibrary.component;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.qiyi.pluginlibrary.constant.IIntentConstant;
import org.qiyi.pluginlibrary.error.ErrorType;
import org.qiyi.pluginlibrary.listenter.IPluginElementLoadListener;
import org.qiyi.pluginlibrary.pm.PluginPackageManagerNative;
import org.qiyi.pluginlibrary.runtime.PluginManager;
import org.qiyi.pluginlibrary.utils.FragmentPluginHelper;

/**
 * 代理加载插件 Fragment 过程，这样宿主可以不需要关心 Fragment 的异步加载逻辑
 */
public abstract class AbstractFragmentProxy extends Fragment {
    private FragmentPluginHelper mPluginHelper;
    @Nullable
    private Fragment mPluginFragment;

    protected abstract View onCreateUi(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    protected abstract void onLoadPluginFragmentSuccess(FragmentManager fragmentManager, Fragment fragment, String packageName);

    protected abstract void onLoadPluginFragmentFail(int errorType, String packageName);

    @Nullable
    @Override
    public final View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mPluginHelper = new FragmentPluginHelper(getChildFragmentManager());
        View view = onCreateUi(inflater, container, savedInstanceState);
        loadPluginFragment();
        return view;
    }

    protected void loadPluginFragment() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            String packageName = arguments.getString(IIntentConstant.EXTRA_TARGET_PACKAGE_KEY);
            String className = arguments.getString(IIntentConstant.EXTRA_TARGET_CLASS_KEY);
            final Context hostContext = getContext().getApplicationContext();
            if (PluginPackageManagerNative.getInstance(hostContext).isPackageInstalled(packageName)) {
                PluginManager.createFragment(hostContext, packageName, className, getArguments(), new IPluginElementLoadListener<Fragment>() {
                    @Override
                    public void onSuccess(Fragment fragment, String packageName) {
                        mPluginFragment = fragment;
                        if (isAdded()) {
                            onLoadPluginFragmentSuccess(getChildFragmentManager(), fragment, packageName);
                        }
                    }

                    @Override
                    public void onFail(int errorType, String packageName) {
                        if (isAdded()) {
                            onLoadPluginFragmentFail(errorType, packageName);
                        }
                    }
                });
            } else {
                if (isAdded()) {
                    onLoadPluginFragmentFail(ErrorType.ERROR_CLIENT_PLUGIN_NOT_INSTALL, packageName);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mPluginHelper.beforeOnSaveInstanceState();
        super.onSaveInstanceState(outState);
        mPluginHelper.afterOnSaveInstanceState();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (mPluginFragment != null) {
            mPluginFragment.setUserVisibleHint(isVisibleToUser);
        }
    }

    @Override
    public boolean getUserVisibleHint() {
        if (mPluginFragment != null) {
            return mPluginFragment.getUserVisibleHint();
        }
        return super.getUserVisibleHint();
    }
}
