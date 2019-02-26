package org.qiyi.pluginlibrary.component.wraper;

import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentProvider;

import org.qiyi.pluginlibrary.utils.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * author: liuchun
 * date: 2019/1/28
 */
public class ContentResolverWrapper extends ContentResolver {
    private static ConcurrentMap<String, Vector<Method>> sMethods = new ConcurrentHashMap<String, Vector<Method>>(5);

    private ContentResolver mBase;

    public ContentResolverWrapper(Context context) {
        super(context);
        mBase = context.getContentResolver();
    }

    /** @Override*/
    protected IContentProvider acquireProvider(Context context, String name) {
        //return mBase.acquireProvider(context, name);
        Class<?>[] paramTypes = new Class[]{Context.class, String.class};
        return ReflectionUtils.on(mBase).call("acquireProvider", sMethods,
                paramTypes, context, name).get();
    }

    /** @Override*/
    protected IContentProvider acquireExistingProvider(Context context, String name) {
        //return mBase.acquireExistingProvider(context, name);
        Class<?>[] paramTypes = new Class[]{Context.class, String.class};
        return ReflectionUtils.on(mBase).call("acquireExistingProvider", sMethods,
                paramTypes, context, name).get();
    }

    /** @Override*/
    public boolean releaseProvider(IContentProvider icp) {
        //return mBase.releaseProvider(icp);
        return ReflectionUtils.on(mBase).call("releaseProvider", sMethods,
                new Class<?>[]{IContentProvider.class}, icp).get();
    }

    /** @Override*/
    protected IContentProvider acquireUnstableProvider(Context context, String name) {
        //return mBase.acquireUnstableProvider(context, name);
        Class<?>[] paramTypes = new Class[]{Context.class, String.class};
        return ReflectionUtils.on(mBase).call("acquireUnstableProvider", sMethods,
                paramTypes, context, name).get();
    }

    /** @Override*/
    public boolean releaseUnstableProvider(IContentProvider icp) {
        //return mBase.releaseUnstableProvider(icp);
        return ReflectionUtils.on(mBase).call("releaseUnstableProvider", sMethods,
                new Class<?>[]{IContentProvider.class}, icp).get();
    }

    /** @Override*/
    public void unstableProviderDied(IContentProvider icp) {
        //return mBase.unstableProviderDied(icp);
        ReflectionUtils.on(mBase).call("unstableProviderDied", sMethods,
                new Class<?>[]{IContentProvider.class}, icp);
    }

    /** @Override*/
    public void appNotRespondingViaProvider(IContentProvider icp) {
        //return mBase.appNotRespondingViaProvider(icp);
        // TODO dark greylist in Android P
        ReflectionUtils.on(mBase).call("appNotRespondingViaProvider", sMethods,
                new Class<?>[]{IContentProvider.class}, icp);
    }
}
