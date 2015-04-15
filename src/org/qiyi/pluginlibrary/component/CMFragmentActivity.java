package org.qiyi.pluginlibrary.component;

import org.qiyi.pluginlibrary.ProxyEnvironment;
import org.qiyi.pluginlibrary.adapter.FragmentActivityProxyAdapter;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;


public class CMFragmentActivity extends CMActivity {

    private FragmentActivityProxyAdapter proxyActivity;

    public FragmentManager getSupportFragmentManager() {
        return proxyActivity.proxyGetSupportFragmentManager();
    }

    public void setActivityProxy(FragmentActivityProxyAdapter paramFramtentActivityProxyAdapter) {
        super.setActivityProxy(paramFramtentActivityProxyAdapter);
        this.proxyActivity = paramFramtentActivityProxyAdapter;
    }

    public void onAttachFragment(Fragment paramFragment) {
        proxyActivity.proxyOnAttachFragment(paramFragment);
    }

    public void startActivityFromFragment(Fragment paramFragment, Intent paramIntent, int paramInt) {
    	ProxyEnvironment.getInstance(getTargetPackageName()).remapStartActivityIntent(paramIntent);
        proxyActivity.proxyStartActivityFromFragment(paramFragment, paramIntent, paramInt);
    }

}
