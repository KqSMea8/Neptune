package org.qiyi.pluginlibrary.component.stackmgr;

import android.app.Activity;

import org.qiyi.pluginlibrary.utils.Util;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * 插件的Activity栈抽象, 和系统的{@link com.android.server.am.ActivityStack}类似
 */
public class PActivityStack {
    // taskAffinity
    private String taskName;

    private final LinkedList<Activity> mActivities;

    PActivityStack(String taskName) {
        this.taskName = taskName;
        mActivities = new LinkedList<>();
    }

    /**
     * 获取当前任务栈的名称
     * @return
     */
    public String getTaskName() {
        return taskName;
    }

    public LinkedList<Activity> getActivities() {
        return mActivities;
    }

    public int size() {
        return mActivities.size();
    }

    public synchronized boolean isEmpty() {
        return mActivities.isEmpty();
    }

    // 放入链表的前面
    public synchronized void push(Activity activity) {
        mActivities.addFirst(activity);
    }

    public synchronized void insertFirst(Activity activity) {
        mActivities.addLast(activity);
    }

    public synchronized boolean pop(Activity activity) {
        return mActivities.remove(activity);
    }

    public synchronized Activity getTop() {
        return mActivities.getFirst();
    }

    /**
     * 清空当前任务栈里的Activity
     */
    public void clear(boolean needFinish) {
        Iterator<Activity> iterator = mActivities.iterator();
        while (iterator.hasNext()) {
            Activity activity = iterator.next();
            if (activity != null && needFinish
                    && !Util.isFinished(activity)) {
                activity.finish();
            }
            iterator.remove();
        }
    }
}
