package com.zyhang.arouter.autowiredtransform

import com.android.SdkConstants
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOCase
import org.apache.commons.io.IOUtils
import org.apache.commons.io.filefilter.SuffixFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

import static org.objectweb.asm.Opcodes.ALOAD
import static org.objectweb.asm.Opcodes.DUP
import static org.objectweb.asm.Opcodes.INVOKESPECIAL
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL
import static org.objectweb.asm.Opcodes.NEW

class AutowiredTransform extends Transform {

    Project project
    String appPackage = 'com.zyhang.arouter.autowiredtransform.app'

    static final String fileNameSuffix = '$$ARouter$$Autowired'
    static final String fileNameSuffixClass = fileNameSuffix + SdkConstants.DOT_CLASS

    private Set<String> autowiredClasses = Collections.newSetFromMap(new ConcurrentHashMap<>())

    AutowiredTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return 'AutowiredTransform'
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        transformInvocation.outputProvider.deleteAll()
        transformInvocation.inputs.each { TransformInput input ->
            input.jarInputs.each { JarInput jarInput ->
                File src = jarInput.getFile()
                File dst = transformInvocation.getOutputProvider().getContentLocation(
                        jarInput.getName(), jarInput.getContentTypes(),
                        jarInput.getScopes(), Format.JAR)
                try {
                    scanAutowiredFromJar(src)
                    injectFromJar(src)
                    FileUtils.copyFile(src, dst)
                } catch (IOException e) {
                    throw new RuntimeException(e)
                }
            }
            autowiredClasses.clear()
            input.directoryInputs.each { DirectoryInput directoryInput ->
                File src = directoryInput.getFile()
                File dst = transformInvocation.getOutputProvider().getContentLocation(
                        directoryInput.getName(), directoryInput.getContentTypes(),
                        directoryInput.getScopes(), Format.DIRECTORY)
                try {
                    scanAutowiredFromDir(src)
                    injectFromDir(src)
                    FileUtils.copyDirectory(src, dst)
                } catch (IOException e) {
                    throw new RuntimeException(e)
                }
            }
            autowiredClasses.clear()
        }
    }

    void scanAutowiredFromJar(File src) {
        JarFile jarFile = new JarFile(src)
        Enumeration<JarEntry> entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement()
            String jarEntryName = jarEntry.getName()
            if (jarEntryName.endsWith(fileNameSuffixClass)) {
                String target = trimName(jarEntryName, 0)
                autowiredClasses.add(target)
            }
        }
        jarFile.close()
    }

    void scanAutowiredFromDir(File src) {
        File dir = new File(src, appPackage.replace('.', File.separatorChar))
        if (dir.exists() && dir.isDirectory()) {
            Collection<File> files = FileUtils.listFiles(dir,
                    new SuffixFileFilter(fileNameSuffixClass, IOCase.INSENSITIVE),
                    TrueFileFilter.INSTANCE)
            files.each { File file ->
                String target = trimName(file.getAbsolutePath(),
                        src.getAbsolutePath().length() + 1)
                        .replace(File.separatorChar, '/')
                autowiredClasses.add(target)
            }
        }
    }

    void injectFromJar(File src) {
        if (autowiredClasses.isEmpty()) {
            return
        }
        File optJar = new File(src.getParent(), src.getName() + ".opt")
        if (optJar.exists()) {
            optJar.delete()
        }
        JarFile jarFile = new JarFile(src)
        Enumeration<JarEntry> entries = jarFile.entries()
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(optJar))

        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement()
            String jarEntryName = jarEntry.getName()
            InputStream inputStream = jarFile.getInputStream(jarEntry)
            jarOutputStream.putNextEntry(new ZipEntry(jarEntryName))
            if (jarEntryName.endsWith(SdkConstants.DOT_CLASS) &&
                    autowiredClasses.contains(trimName(jarEntryName, 0)
                            .concat(fileNameSuffix))) {
                byte[] bytes = inject(jarFile.getInputStream(jarEntry))
                jarOutputStream.write(bytes)
            } else {
                jarOutputStream.write(IOUtils.toByteArray(inputStream))
            }
            inputStream.close()
            jarOutputStream.closeEntry()
        }
        jarOutputStream.close()
        jarFile.close()
        src.delete()
        optJar.renameTo(src)
    }

    void injectFromDir(File src) {
        if (autowiredClasses.isEmpty()) {
            return
        }
        File dir = new File(src, appPackage.replace('.', File.separatorChar))
        if (dir.exists() && dir.isDirectory()) {
            Collection<File> files = FileUtils.listFiles(dir,
                    new SuffixFileFilter(SdkConstants.DOT_CLASS, IOCase.INSENSITIVE),
                    TrueFileFilter.INSTANCE)
            for (File file : files) {
                if (autowiredClasses.contains(trimName(file.getAbsolutePath(),
                        src.getAbsolutePath().length() + 1)
                        .replace(File.separatorChar, '/')
                        .concat(fileNameSuffix))) {
                    InputStream inputStream = new FileInputStream(file)
                    byte[] bytes = inject(inputStream)
                    OutputStream outputStream = new FileOutputStream(file)
                    outputStream.write(bytes)
                    outputStream.close()
                    inputStream.close()
                }
            }
        }
    }

    static String trimName(String s, int start) {
        return s.substring(start, s.length() - SdkConstants.DOT_CLASS.length())
    }

    byte[] inject(InputStream inputStream) throws IOException {
        ClassReader cr = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
        ClassVisitor cv = new InjectVisitor(Opcodes.ASM5, cw)
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
        return cw.toByteArray()
    }

    static class InjectVisitor extends ClassVisitor implements Opcodes {

        String className, superName
        boolean findOnCreate

        InjectVisitor(int api, ClassVisitor classVisitor) {
            super(api, classVisitor)
        }

        @Override
        void visit(int version, int access, String name, String signature, String superName,
                   String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces)
            this.className = name
            this.superName = superName
        }

        @Override
        MethodVisitor visitMethod(int access, String name, String desc,
                                  String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions)
            if ("onCreate" == name && "(Landroid/os/Bundle;)V" == desc) {
                findOnCreate = true
                mv = new InjectMethodVisitor(ASM5, mv, className.concat(fileNameSuffix))
            }
            return mv
        }

        @Override
        void visitEnd() {
            if (!findOnCreate) {
                MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, "onCreate", "(Landroid/os/Bundle;)V", null, null)
                mv.visitCode()

                String injector = className.concat(fileNameSuffix)
                mv.visitTypeInsn(NEW, injector)
                mv.visitInsn(DUP)
                mv.visitMethodInsn(INVOKESPECIAL, injector, "<init>",
                        "()V", false)
                mv.visitVarInsn(ALOAD, 0)
                mv.visitMethodInsn(INVOKEVIRTUAL, injector, "inject",
                        "(Ljava/lang/Object;)V", false)

                mv.visitVarInsn(ALOAD, 0)
                mv.visitVarInsn(ALOAD, 1)
                mv.visitMethodInsn(INVOKESPECIAL, superName, "onCreate", "(Landroid/os/Bundle;)V", false)
                mv.visitInsn(RETURN)
                mv.visitMaxs(2, 2)
                mv.visitEnd()
                cv.visitEnd()
            }
            super.visitEnd()
        }
    }

    private static class InjectMethodVisitor extends MethodVisitor {
        String injector

        InjectMethodVisitor(int api, MethodVisitor methodVisitor, String injector) {
            super(api, methodVisitor)
            this.injector = injector
        }

        @Override
        void visitCode() {
            mv.visitTypeInsn(NEW, injector)
            mv.visitInsn(DUP)
            mv.visitMethodInsn(INVOKESPECIAL, injector, "<init>",
                    "()V", false)
            mv.visitVarInsn(ALOAD, 0)
            mv.visitMethodInsn(INVOKEVIRTUAL, injector, "inject",
                    "(Ljava/lang/Object;)V", false)

            super.visitCode()
        }
    }
}