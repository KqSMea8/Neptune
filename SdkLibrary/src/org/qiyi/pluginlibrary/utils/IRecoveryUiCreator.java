package org.qiyi.pluginlibrary.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;

import org.qiyi.pluginlibrary.R;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * 恢复插件 Activity 时的准备阶段 UI，由宿主提供
 */
public interface IRecoveryUiCreator {
    View createContentView(Context context);

    /**
     * 默认的 RecoveryUi
     */
    class DefaultRecoveryUiCreator implements IRecoveryUiCreator {
        @Override
        public View createContentView(Context context) {
            TextView textView = new TextView(context);
            textView.setText(R.string.under_recovery);
            FrameLayout frameLayout = new FrameLayout(context);
            LayoutParams lp = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER);
            frameLayout.addView(textView, lp);
            return frameLayout;
        }
    }
}