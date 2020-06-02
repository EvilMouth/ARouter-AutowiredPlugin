package com.zyhang.arouter.autowiredtransform

import com.android.SdkConstants
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class AutowiredWeaver {
    private static final String TAG = 'AutowiredWeaver'

    // xxx.xxx.xxx.class
    private Set<String> autowiredClasses = Collections.newSetFromMap(new ConcurrentHashMap<>())

    void scan(TransformInvocation transformInvocation) {
        transformInvocation.inputs.each { TransformInput input ->
            input.jarInputs.each { JarInput jarInput ->
                File src = jarInput.file
                JarFile jarFile = new JarFile(src)
                Enumeration<JarEntry> entries = jarFile.entries()
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement()
                    String fullQualifiedClassName = Utils.fullQualifiedClassName(jarEntry)
                    if (fullQualifiedClassName.endsWith(Utils.fileNameSuffixClass)) {
                        autowiredClasses.add(fullQualifiedClassName)
                        println(TAG + ' scan: ' + fullQualifiedClassName)
                    }
                }
                jarFile.close()
            }
            input.directoryInputs.each { DirectoryInput directoryInput ->
                File src = directoryInput.file
                if (src.isDirectory()) {
                    src.eachFileRecurse { File file ->
                        String fullQualifiedClassName = Utils.fullQualifiedClassName(src, file)
                        if (fullQualifiedClassName.endsWith(Utils.fileNameSuffixClass)) {
                            autowiredClasses.add(fullQualifiedClassName)
                            println(TAG + ' scan: ' + fullQualifiedClassName)
                        }
                    }
                }
            }
        }
    }

    void weaveJar(File inputJar, File outputJar) throws IOException {
        ZipFile inputZip = new ZipFile(inputJar)
        ZipOutputStream outputZip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(outputJar.toPath())))
        Enumeration<? extends ZipEntry> inEntries = inputZip.entries()
        while (inEntries.hasMoreElements()) {
            ZipEntry entry = inEntries.nextElement()
            InputStream originalFile = new BufferedInputStream(inputZip.getInputStream(entry))
            ZipEntry outEntry = new ZipEntry(entry.getName())
            byte[] newEntryContent
            String fullQualifiedClassName = Utils.fullQualifiedClassName(outEntry)
            if (isWeavableClass(fullQualifiedClassName)) {
                newEntryContent = weaveSingleClassToByteArray(fullQualifiedClassName, originalFile)
            } else {
                newEntryContent = IOUtils.toByteArray(originalFile)
            }
            CRC32 crc32 = new CRC32()
            crc32.update(newEntryContent)
            outEntry.setCrc(crc32.getValue())
            outEntry.setMethod(ZipEntry.STORED)
            outEntry.setSize(newEntryContent.length)
            outEntry.setCompressedSize(newEntryContent.length)
            outEntry.setLastAccessTime(FileTime.fromMillis(0))
            outEntry.setLastModifiedTime(FileTime.fromMillis(0))
            outEntry.setCreationTime(FileTime.fromMillis(0))
            outputZip.putNextEntry(outEntry)
            outputZip.write(newEntryContent)
            outputZip.closeEntry()
        }
        outputZip.flush()
        outputZip.close()
    }

    void weaveSingleClassToFile(File dir, File inputFile, File outputFile) throws IOException {
        String fullQualifiedClassName = Utils.fullQualifiedClassName(dir, inputFile)
        if (isWeavableClass(fullQualifiedClassName)) {
            FileUtils.touch(outputFile)
            InputStream inputStream = new FileInputStream(inputFile)
            byte[] bytes = weaveSingleClassToByteArray(fullQualifiedClassName, inputStream)
            FileOutputStream fos = new FileOutputStream(outputFile)
            fos.write(bytes)
            fos.close()
            inputStream.close()
        } else {
            if (inputFile.isFile()) {
                FileUtils.touch(outputFile)
                FileUtils.copyFile(inputFile, outputFile)
            }
        }
    }

    private byte[] weaveSingleClassToByteArray(String fullQualifiedClassName, InputStream inputStream) throws IOException {
        if (Utils.matchView(autowiredClasses, fullQualifiedClassName)) {
            ClassReader classReader = new ClassReader(IOUtils.toByteArray(inputStream))
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
            ClassVisitor classVisitor = new AutowiredClassVisitor(classWriter)
            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
            return classWriter.toByteArray()
        }
        return IOUtils.toByteArray(inputStream)
    }

    private static boolean isWeavableClass(String fullyQualifiedClassName) {
        return fullyQualifiedClassName.endsWith(SdkConstants.DOT_CLASS) &&
                !fullyQualifiedClassName.startsWith("R\$") &&
                fullyQualifiedClassName != 'R.class' &&
                fullyQualifiedClassName != 'BuildConfig.class'
    }
}