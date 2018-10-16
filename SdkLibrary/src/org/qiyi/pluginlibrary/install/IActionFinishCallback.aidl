package org.qiyi.pluginlibrary.install;

interface IActionFinishCallback {

    void onActionComplete(String packageName, int resultCode);

    String getProcessName();
}
