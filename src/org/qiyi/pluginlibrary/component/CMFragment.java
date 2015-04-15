package org.qiyi.pluginlibrary.component;

import org.qiyi.pluginlibrary.proxy.activity.FragmentActivityProxy;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;


/**
 * 
 * 虚拟Fragment
 */
public class CMFragment extends Fragment {

    /**
     * 获取Fragment对应的Activity
     * 
     * @return Activity实例
     */
    public Context getTargetActivity() {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity instanceof FragmentActivityProxy) {
            return ((FragmentActivityProxy) fragmentActivity).getCMActivity();
        }
        return fragmentActivity;
    }

}
