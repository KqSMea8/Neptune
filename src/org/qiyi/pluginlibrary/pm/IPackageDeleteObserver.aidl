// IPackageDeleteObserver.aidl
package org.qiyi.pluginlibrary.pm;

interface IPackageDeleteObserver {

    oneway void packageDeleted(String packageName, int returnCode);

}
