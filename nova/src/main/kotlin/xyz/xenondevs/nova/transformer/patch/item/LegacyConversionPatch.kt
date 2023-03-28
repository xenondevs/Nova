package xyz.xenondevs.nova.transformer.patch.item

import org.objectweb.asm.Opcodes
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.item.legacy.ItemStackLegacyConversion
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ITEM_STACK_LOAD_METHOD

internal object LegacyConversionPatch : MethodTransformer(ITEM_STACK_LOAD_METHOD, computeFrames = true) {
    
    override fun transform() {
        methodNode.instructions.replaceFirst(0, 0, buildInsnList {
            aLoad(0)
            invokeStatic(ItemStackLegacyConversion::convert)
            _return()
        }) { it.opcode == Opcodes.RETURN }
    }
    
}