package org.qiyi.pluginlibrary.proxy.activity;

import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;

/**
 * dialog样式的　activity
 */
public class DialogActivityProxy extends ActivityProxy {

	@Override
	protected void onCreate(Bundle bundle) {
		// TODO Auto-generated method stub
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		super.onCreate(bundle);
	}
}
