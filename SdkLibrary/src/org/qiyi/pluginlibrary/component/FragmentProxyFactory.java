package org.qiyi.pluginlibrary.component;

import android.annotation.NonNull;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.qiyi.pluginlibrary.HybirdPlugin;
import org.qiyi.pluginlibrary.R;
import org.qiyi.pluginlibrary.constant.IIntentConstant;
import org.qiyi.pluginlibrary.utils.ErrorUtil;

/**
 * 工厂类，负责创建 FragmentProxy，根据 HybirdPlugin 配置，决定具体的 FragmentProxy 类型
 */
public class FragmentProxyFactory {

    /**
     * 根据 packageName 与 classname 创建 FragmentProxy
     *
     * @param packageName 插件包名
     * @param className   插件 Fragment 类名
     * @return FragmentProxy
     */
    @NonNull
    public static Fragment create(String packageName, String className) {
        Class<? extends AbstractFragmentProxy> clazz = HybirdPlugin.getConfig().getFragmentProxyClass();
        Fragment fragment;
        if (clazz == null) {
            fragment = new DefaultFragmentProxy();
        } else {
            try {
                fragment = clazz.newInstance();
            } catch (Throwable e) {
                ErrorUtil.throwErrorIfNeed(e);
                fragment = new DefaultFragmentProxy();
            }
        }
        Bundle bundle = new Bundle();
        bundle.putString(IIntentConstant.EXTRA_TARGET_PACKAGE_KEY, packageName);
        bundle.putString(IIntentConstant.EXTRA_TARGET_CLASS_KEY, className);
        fragment.setArguments(bundle);
        return fragment;
    }

    /**
     * 默认的 FragmentProxy，一般接入方在有定制 UI 需求，需要自己继承 AbstractFragmentProxy 实现
     */
    public static class DefaultFragmentProxy extends AbstractFragmentProxy {
        private View loadingView;
        private View errorView;

        @Override
        protected View onCreateView(LayoutInflater inflater, ViewGroup container) {
            View view = inflater.inflate(R.layout.fragment_proxy_default, container, false);
            loadingView = view.findViewById(R.id.loading_view);
            errorView = view.findViewById(R.id.error_view);
            return view;
        }

        @Override
        protected void onLoadPluginFragmentSuccess(FragmentManager fragmentManager, Fragment fragment, String packageName) {
            fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit();
            loadingView.setVisibility(View.GONE);
            errorView.setVisibility(View.GONE);
        }

        @Override
        protected void onLoadPluginFragmentFail(String packageName) {
            loadingView.setVisibility(View.GONE);
            errorView.setVisibility(View.VISIBLE);
        }
    }
}
