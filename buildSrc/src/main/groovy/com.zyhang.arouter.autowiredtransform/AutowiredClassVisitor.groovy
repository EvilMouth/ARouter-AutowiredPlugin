package com.zyhang.arouter.autowiredtransform

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class AutowiredClassVisitor extends ClassVisitor implements Opcodes {

    private static final String TAG = 'AutowiredClassVisitor'

    private String className, superName
    private boolean findOnCreate

    AutowiredClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM7, classVisitor)
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
            mv = new InjectMethodVisitor(mv)
        }
        return mv
    }

    @Override
    void visitEnd() {
        if (!findOnCreate) {
            println(TAG + ' create onCreate and inject: ' + className)
            MethodVisitor mv = cv.visitMethod(ACC_PROTECTED, "onCreate", "(Landroid/os/Bundle;)V", null, null)
            mv.visitCode()

            inject(mv)

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

    private class InjectMethodVisitor extends MethodVisitor {

        InjectMethodVisitor(MethodVisitor methodVisitor) {
            super(Opcodes.ASM7, methodVisitor)
        }

        @Override
        void visitCode() {
            println(TAG + ' inject in onCreate: ' + className)
            inject(mv)
            super.visitCode()
        }
    }

    private void inject(MethodVisitor mv) {
        String injector = className.concat(Utils.fileNameSuffix)
        mv.visitTypeInsn(NEW, injector)
        mv.visitInsn(DUP)
        mv.visitMethodInsn(INVOKESPECIAL, injector, "<init>", "()V", false)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitMethodInsn(INVOKEVIRTUAL, injector, "inject", "(Ljava/lang/Object;)V", false)
    }
}