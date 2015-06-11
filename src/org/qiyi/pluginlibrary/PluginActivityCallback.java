package org.qiyi.pluginlibrary;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public interface PluginActivityCallback {

	void callOnCreate(Bundle saveInstance);

	void callOnStart();

	void callOnResume();

	void callOnDestroy();

	void callOnStop();

	void callOnRestart();

	void callOnSaveInstanceState(Bundle outState);

	void callOnRestoreInstanceState(Bundle savedInstanceState);

	void callOnPause();

	void callOnBackPressed();

	boolean callOnKeyDown(int keyCode, KeyEvent event);

	void callDump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args);

	void callOnPostResume();

	void callOnDetachedFromWindow();

	View callOnCreateView(String name, Context context, AttributeSet attrs);

	View callOnCreateView(View parent, String name, Context context, AttributeSet attrs);

	void callOnNewIntent(Intent intent);

	void callOnConfigurationChanged(Configuration newConfig);

	void callOnActivityResult(int requestCode, int resultCode, Intent data);
}