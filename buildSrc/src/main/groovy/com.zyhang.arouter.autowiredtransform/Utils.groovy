package com.zyhang.arouter.autowiredtransform

import com.android.SdkConstants

import java.util.zip.ZipEntry

class Utils {

    static final String fileNameSuffix = '$$ARouter$$Autowired'
    static final String fileNameSuffixClass = fileNameSuffix + SdkConstants.DOT_CLASS

    static String fullQualifiedClassName(ZipEntry entry) {
        return entry.getName().replace("/", ".")
    }

    static String fullQualifiedClassName(File dir, File src) {
        String dirPath = dir.absolutePath
        if (!dirPath.endsWith(File.separator)) {
            dirPath = dirPath + File.separator
        }
        return src.absolutePath.replace(dirPath, "")
                .replace(File.separator, ".")
    }

    static boolean matchView(Collection<String> collection, String fullQualifiedClassName) {
        if (fullQualifiedClassName.endsWith(SdkConstants.DOT_CLASS)) {
            String target = trimClass(fullQualifiedClassName).concat(fileNameSuffix).concat(SdkConstants.DOT_CLASS)
            return collection.contains(target)
        }
        return false
    }

    static String trimClass(String className) {
        return className.substring(0, className.length() - SdkConstants.DOT_CLASS.length())
    }
}