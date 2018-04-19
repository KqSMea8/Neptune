package com.qiyi.plugin.utils

class Utils {

    public static int compareVersion(String v1, String v2) {
        String[] va1 = v1.split("\\.")
        String[] va2 = v2.split("\\.")

        int idx = 0
        int minLen = Math.max(va1.length, va2.length)
        int diff = 0
        while (idx < minLen
                && (diff = va1[idx].length() - va2[idx].length()) == 0
                && (diff = va1[idx].compareTo(va2[idx])) == 0) {
            ++idx
        }

        return (diff != 0) ? diff : va1.length - va2.length
    }
}
