@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package xyz.xenondevs.nova.patch.impl

import jdk.internal.reflect.Reflection
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.nova.patch.ReversibleMethodTransformer

internal object FieldFilterPatch : ReversibleMethodTransformer(Reflection::filterFields) {
    
    override fun transform() {
        methodNode.instructions = buildInsnList {
            aLoad(1)
            areturn()
        }
    }
    
}