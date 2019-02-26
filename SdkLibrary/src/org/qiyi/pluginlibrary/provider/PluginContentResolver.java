package org.qiyi.pluginlibrary.provider;

import android.app.ActivityThread;
import android.content.Context;
import android.content.IContentProvider;
import android.net.Uri;
import android.text.TextUtils;

import org.qiyi.pluginlibrary.component.ContentProviderProxy1;
import org.qiyi.pluginlibrary.component.wraper.ContentResolverWrapper;
import org.qiyi.pluginlibrary.utils.ComponentFinder;
import org.qiyi.pluginlibrary.utils.PluginDebugLog;
import org.qiyi.pluginlibrary.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

/**
 * author: liuchun
 * date: 2019/1/28
 */
public class PluginContentResolver extends ContentResolverWrapper {
    private static final String TAG = "PluginContentResolver";
    // IContentProvider的binder实例
    private static IContentProvider sIContentProvider;

    private static synchronized IContentProvider getIContentProvider(Context context) {
        if (sIContentProvider == null) {
            hookIContentProviderAsNeeded(context);
        }
        return sIContentProvider;
    }

    private static void hookIContentProviderAsNeeded(Context context) {
        Uri uri = Uri.parse(ContentProviderProxy1.getUri(context));
        context.getContentResolver().call(uri, "wakeup", null, null);
        try {
            Field authority = null;
            Field provider = null;
            ActivityThread activityThread = ActivityThread.currentActivityThread();
            Map providerMap = ReflectionUtils.on(activityThread).get("mProviderMap");
            Iterator iterator = providerMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                Object key = entry.getKey();
                Object value = entry.getValue();
                String auth;
                if (key instanceof String) {
                    // 4.2以下版本
                    auth = (String) key;
                } else {
                    // ProviderKey
                    if (authority == null) {
                        authority = key.getClass().getDeclaredField("authority");
                        authority.setAccessible(true);
                    }
                    auth = (String) authority.get(key);
                }
                // 找到了代理ContentProvider的authority
                if (TextUtils.equals(auth, ContentProviderProxy1.getAuthority(context))) {
                    if (provider == null) {
                        provider = value.getClass().getDeclaredField("mProvider");
                        provider.setAccessible(true);
                    }
                    IContentProvider rawProvider = (IContentProvider) provider.get(value);
                    IContentProvider proxy = IContentProviderProxy.newInstance(context, rawProvider);
                    sIContentProvider = proxy;
                    PluginDebugLog.runtimeLog(TAG, "hookIContentProvider succeed : " + sIContentProvider);
                    break;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PluginContentResolver(Context context) {
        super(context);
    }

    @Override
    protected IContentProvider acquireProvider(Context context, String name) {
        if (ComponentFinder.resolveProviderInfo(context, name) != null) {
            return getIContentProvider(context);
        }
        return super.acquireProvider(context, name);
    }

    @Override
    protected IContentProvider acquireExistingProvider(Context context, String name) {
        if (ComponentFinder.resolveProviderInfo(context, name) != null) {
            return getIContentProvider(context);
        }
        return super.acquireExistingProvider(context, name);
    }

    @Override
    protected IContentProvider acquireUnstableProvider(Context context, String name) {
        if (ComponentFinder.resolveProviderInfo(context, name) != null) {
            return getIContentProvider(context);
        }
        return super.acquireUnstableProvider(context, name);
    }

    @Override
    public boolean releaseProvider(IContentProvider icp) {
        return true;
    }

    @Override
    public boolean releaseUnstableProvider(IContentProvider icp) {
        return true;
    }

    @Override
    public void unstableProviderDied(IContentProvider icp) {
    }

    @Override
    public void appNotRespondingViaProvider(IContentProvider icp) {
    }
}
