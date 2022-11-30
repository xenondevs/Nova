@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package xyz.xenondevs.nova.transformer.patch

import jdk.internal.reflect.Reflection
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.nova.transformer.ReversibleMethodTransformer
import kotlin.reflect.jvm.javaMethod

internal object FieldFilterPatch : ReversibleMethodTransformer(Reflection::filterFields.javaMethod!!) {
    
    override fun transform() {
        methodNode.instructions = buildInsnList {
            aLoad(1)
            areturn()
        }
    }
    
}