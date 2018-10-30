package org.qiyi.pluginlibrary.debug;

import android.content.Context;

import java.util.Collections;
import java.util.List;

public class PluginDebugCacheProxy implements IPluginDebugHelper {
    private IPluginDebugHelper proxy = null;

    private static class InnerHolder {
        private static PluginDebugCacheProxy sProxy = new PluginDebugCacheProxy();
    }

    public static PluginDebugCacheProxy getInstance() {
        return InnerHolder.sProxy;
    }

    public void setProxy(IPluginDebugHelper proxy) {
        this.proxy = proxy;
    }

    @Override
    public void savePluginRequestUrl(Context context, String url) {
        if (proxy != null) {
            proxy.savePluginRequestUrl(context, url);
        }
    }

    @Override
    public List<String> getPluginRequestUrl(Context context) {
        if (proxy != null) {
            return proxy.getPluginRequestUrl(context);
        }
        return Collections.emptyList();
    }

    @Override
    public void savePluginList(Context context, String plugins) {
        if (proxy != null) {
            proxy.savePluginList(context, plugins);
        }
    }

    @Override
    public List<String> getPluginList(Context context) {
        if (proxy != null) {
            return proxy.getPluginList(context);
        }
        return Collections.emptyList();
    }

    @Override
    public void savePluginDownloadState(Context context, String downloadState) {
        if (proxy != null) {
            proxy.savePluginDownloadState(context, downloadState);
        }
    }

    @Override
    public List<String> getPluginDownloadState(Context context) {
        if (proxy != null) {
            return proxy.getPluginDownloadState(context);
        }
        return Collections.emptyList();
    }

    @Override
    public void savePluginInstallState(Context context, String installState) {
        if (proxy != null) {
            proxy.savePluginInstallState(context, installState);
        }
    }

    @Override
    public List<String> getPluginInstallState(Context context) {
        if (proxy != null) {
            return proxy.getPluginInstallState(context);
        }
        return Collections.emptyList();
    }

    @Override
    public void saveRunningPluginInfo(Context context, String pluginInfo) {
        if (proxy != null) {
            proxy.saveRunningPluginInfo(context, pluginInfo);
        }
    }

    @Override
    public List<String> getRunningPluginInfo(Context context) {
        if (proxy != null) {
            return proxy.getRunningPluginInfo(context);
        }
        return Collections.emptyList();
    }

    @Override
    public void savePluginActivityAndServiceJump(Context context, String intent) {
        if (proxy != null) {
            proxy.savePluginActivityAndServiceJump(context, intent);
        }
    }

    @Override
    public List<String> getPluginJumpInfo(Context context) {
        if (proxy != null) {
            return proxy.getPluginJumpInfo(context);
        }
        return Collections.emptyList();
    }

    @Override
    public String getPluginInfo(Context context, String pluginName) {
        if (proxy != null) {
            return proxy.getPluginInfo(context, pluginName);
        }
        return "";
    }

    @Override
    public void savePluginLogBuffer(Context context, String logTag, String logMsg) {
        if (proxy != null) {
            proxy.savePluginLogBuffer(context, logTag, logMsg);
        }
    }

    @Override
    public List<String> getPluginLogBuffer(Context context, String logTag) {
        if (proxy != null) {
            return proxy.getPluginLogBuffer(context, logTag);
        }
        return Collections.emptyList();
    }
}
