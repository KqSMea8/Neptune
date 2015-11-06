// IPackageDeleteObserver.aidl
package org.qiyi.pluginlibrary.pm;

interface IPackageDeleteObserver {

    void packageDeleted(String packageName, int returnCode);

}
