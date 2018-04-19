package com.qiyi.plugin


class QYPluginExtension {
    /** Host or Plugin project compile */
    boolean pluginMode = true
    /** Android Gradle Plugin version larger than 3.0.0 */
    boolean isHigherAGP
    /** Custom defined resource package Id */
    int packageId = 0x7f
    /** Variant application id */
    String packageName
    /** Package path for java classes */
    String packagePath
    /** File of split R.java */
    File splitRJavaFile
    /** File of public.xml from host project */
    List<File> publicXmlFiles = new ArrayList<>()
    /** host Symbol file - R.txt */
    File hostSymbolFile
    /** host dependence - aar module*/
    String hostDependencies

    public void publicXml(String path) {
        publicXmlFiles.add(new File(path))
    }

    public void publicXml(File file) {
        publicXmlFiles.add(file)
    }
}
