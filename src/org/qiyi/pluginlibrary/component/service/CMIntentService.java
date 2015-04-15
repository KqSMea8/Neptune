package org.qiyi.pluginlibrary.component.service;


import android.content.Intent;

public abstract class CMIntentService extends CMService {
    abstract public void onHandleIntent(Intent paramIntent);
}
