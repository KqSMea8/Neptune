package org.qiyi.pluginlibrary.adapter;

import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

public abstract interface ListActivityProxyAdapter extends ActivityProxyAdapter {
    public abstract ListAdapter proxyGetListAdapter();

    public abstract ListView proxyGetListView();

    public abstract long proxyGetSelectedItemId();

    public abstract int proxyGetSelectedItemPosition();

    public abstract void proxyOnListItemClick(ListView paramListView, View paramView, int paramInt, long paramLong);

    public abstract void proxySetListAdapter(ListAdapter paramListAdapter);

    public abstract void proxySetSelection(int paramInt);
}
