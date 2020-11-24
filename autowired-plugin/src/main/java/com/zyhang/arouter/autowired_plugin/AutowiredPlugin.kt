package com.zyhang.arouter.autowired_plugin

import com.android.build.gradle.AppExtension
import com.ss.android.ugc.bytex.common.CommonPlugin
import com.ss.android.ugc.bytex.common.TransformConfiguration
import org.gradle.api.Project
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by zyhang on 2020/11/23.16:35
 */
class AutowiredPlugin : CommonPlugin<AutowiredExtension, AutowiredContext>() {

    private val targetClasses = Collections.newSetFromMap<String>(ConcurrentHashMap())

    override fun getContext(
        project: Project?,
        android: AppExtension?,
        extension: AutowiredExtension?
    ): AutowiredContext = AutowiredContext(project, android, extension)

    override fun traverse(relativePath: String, node: ClassNode) {
        super.traverse(relativePath, node)

        if (node.name.endsWith(SUFFIX)) {
            // collect activity/fragment class
            targetClasses.add(
                node.name.removeSuffix(SUFFIX).also {
                    context.logger.i("collect $it")
                }
            )
        }
    }

    override fun transform(relativePath: String, node: ClassNode): Boolean {
        if (targetClasses.contains(node.name)) {
            context.logger.i("transform ${node.name}")

            val injector = "${node.name}$SUFFIX"
            node.methods.find {
                it.name == "onCreate" && it.desc == "(Landroid/os/Bundle;)V"
            }?.let {
                context.logger.i("inject $injector in onCreate(Bundle)")

                val mv = MethodNode()
                mv.visitTypeInsn(Opcodes.NEW, injector)
                mv.visitInsn(Opcodes.DUP)
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, injector, "<init>", "()V", false)
                mv.visitVarInsn(Opcodes.ALOAD, 0)
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    injector,
                    "inject",
                    "(Ljava/lang/Object;)V",
                    false
                )
                it.instructions.insert(mv.instructions)
                return true
            }

            // if find no onCreate(Bundle), then create it and inject
            context.logger.i("create onCreate(Bundle), and inject $injector in it")

            val mv: MethodVisitor = node.visitMethod(
                Opcodes.ACC_PUBLIC, "onCreate",
                "(Landroid/os/Bundle;)V", null, null
            )
            mv.visitCode()
            mv.visitTypeInsn(Opcodes.NEW, injector)
            mv.visitInsn(Opcodes.DUP)
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, injector, "<init>", "()V", false)
            mv.visitVarInsn(Opcodes.ALOAD, 0)
            mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                injector,
                "inject",
                "(Ljava/lang/Object;)V",
                false
            )

            mv.visitVarInsn(Opcodes.ALOAD, 0)
            mv.visitVarInsn(Opcodes.ALOAD, 1)
            mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL, node.superName, "onCreate", "(Landroid/os/Bundle;)V",
                false
            )
            mv.visitInsn(Opcodes.RETURN)
            mv.visitMaxs(2, 2)
            mv.visitEnd()
            return true
        }
        return super.transform(relativePath, node)
    }

    override fun transformConfiguration(): TransformConfiguration {
        return object : TransformConfiguration {
            override fun consumesFeatureJars(): Boolean = extension.isConsumesFeatureJars()
        }
    }

    companion object {
        private const val SUFFIX = "\$\$ARouter\$\$Autowired"
    }
}