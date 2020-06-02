package com.zyhang.arouter.autowiredtransform

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.ide.common.internal.WaitableExecutor
import org.apache.commons.io.FileUtils

class AutowiredTransform extends Transform {

    private String appPackage = 'com.zyhang.arouter.autowiredtransform.app'
    private AutowiredWeaver autowiredWeaver
    private WaitableExecutor executor

    AutowiredTransform(String applicationId) {
        appPackage = applicationId.replace('.', '/')
        autowiredWeaver = new AutowiredWeaver()
        executor = WaitableExecutor.useGlobalSharedThreadPool()
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
        return true
    }

    @Override
    void transform(TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {
        long ms = System.currentTimeMillis()
        boolean isIncremental = transformInvocation.incremental
        if (!isIncremental) {
            transformInvocation.outputProvider.deleteAll()
        }
        // scan
        autowiredWeaver.scan(transformInvocation)
        // transform
        transformInvocation.inputs.each { TransformInput input ->
            input.jarInputs.each { JarInput jarInput ->
                File src = jarInput.file
                File dest = transformInvocation.outputProvider.getContentLocation(
                        jarInput.name, jarInput.contentTypes,
                        jarInput.scopes, Format.JAR)
                if (isIncremental) {
                    switch (jarInput.status) {
                        case Status.NOTCHANGED:
                            break
                        case Status.ADDED:
                        case Status.CHANGED:
                            transformJar(src, dest)
                            break
                        case Status.REMOVED:
                            if (dest.exists()) {
                                FileUtils.forceDelete(dest)
                            }
                            break
                    }
                } else {
                    transformJar(src, dest)
                }
            }
            input.directoryInputs.each { DirectoryInput directoryInput ->
                File src = directoryInput.file
                File dest = transformInvocation.outputProvider.getContentLocation(
                        directoryInput.name, directoryInput.contentTypes,
                        directoryInput.scopes, Format.DIRECTORY)
                FileUtils.forceMkdir(dest)
                String srcDirPath = src.absolutePath
                String destDirPath = dest.absolutePath
                if (isIncremental) {
                    directoryInput.changedFiles.each {
                        File inputFile = it.key
                        Status status = it.value
                        String destFilePath = inputFile.absolutePath.replace(srcDirPath, destDirPath)
                        File destFile = new File(destFilePath)
                        switch (status) {
                            case Status.NOTCHANGED:
                                break
                            case Status.REMOVED:
                                if (destFile.exists()) {
                                    destFile.delete()
                                }
                                break
                            case Status.ADDED:
                            case Status.CHANGED:
                                FileUtils.touch(destFile)
                                transformSingleFile(src, inputFile, destFile)
                                break
                        }
                    }
                } else {
                    transformDir(src, dest)
                }
            }
        }

        executor.waitForTasksWithQuickFail(true)
        println('AutowiredTransform cost ' + (System.currentTimeMillis() - ms) + 'ms')
    }

    private void transformJar(File srcJar, File destJar) {
        executor.execute {
            autowiredWeaver.weaveJar(srcJar, destJar)
        }
    }

    private void transformDir(File inputDir, File outputDir) throws IOException {
        def inputDirPath = inputDir.absolutePath
        def outputDirPath = outputDir.absolutePath
        if (inputDir.isDirectory()) {
            inputDir.eachFileRecurse { file ->
                executor.execute {
                    def filePath = file.absolutePath
                    def outputFile = new File(filePath.replace(inputDirPath, outputDirPath))
                    autowiredWeaver.weaveSingleClassToFile(inputDir, file, outputFile)
                }
            }
        }
    }

    private void transformSingleFile(File dir, File inputFile, File outputFile) {
        executor.execute {
            autowiredWeaver.weaveSingleClassToFile(dir, inputFile, outputFile)
        }
    }
}