package org.qiyi.pluginlibrary.adapter;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

/**
 * 
 * @author chenyangkun
 * @since 2014-4-17
 */
public interface FragmentActivityProxyAdapter extends ActivityProxyAdapter {

    public abstract FragmentManager proxyGetSupportFragmentManager();

    public abstract void proxyOnAttachFragment(Fragment paramFragment);

    public abstract void proxyStartActivityFromFragment(Fragment paramFragment, Intent paramIntent, int paramInt);

    public abstract void proxysetFinishOnTouchOutside(boolean finish);

}
