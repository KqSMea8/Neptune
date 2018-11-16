package org.qiyi.pluginlibrary.runtime;

import android.content.Intent;
import android.content.ServiceConnection;

import java.lang.ref.WeakReference;

/**
 * Intent调起插件的请求, 缓存Intent和ServiceConnection对象
 * bindService时需要传递ServiceConnection
 */
public class IntentRequest {
    private Intent mIntent;  // intent
    private WeakReference<ServiceConnection> mScRef; // ServiceConnection

    public IntentRequest(Intent intent, ServiceConnection sc) {
        this.mIntent = intent;
        this.mScRef = new WeakReference<>(sc);
    }

    public Intent getIntent() {
        return mIntent;
    }

    public ServiceConnection getServiceConnection() {
        return mScRef.get();
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + mIntent.hashCode();
        ServiceConnection sc = mScRef.get();
        if (sc != null) {
            result = 31 * result + sc.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IntentRequest) {
            IntentRequest other = (IntentRequest)obj;
            if (!this.mIntent.equals(other.mIntent)) {
                return false;
            }
            ServiceConnection sc = this.getServiceConnection();
            ServiceConnection osc = other.getServiceConnection();
            return sc != null ? sc.equals(osc) : osc != null;
        }
        return false;
    }

    @Override
    public String toString() {
        String result = mIntent.toString();
        ServiceConnection sc = getServiceConnection();
        if (sc != null) {
            result += ", sc=" + sc.toString();
        }
        return result;
    }
}
