@file:Suppress("unused")

package xyz.xenondevs.nova.transformer.patch.item

import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.item.novaItem
import net.minecraft.world.item.ItemStack as MojangStack

/**
 * Patches the [MojangStack.getMaxStackSize] method to account for Nova's custom max stack sizes.
 */
internal object StackSizePatch : MethodTransformer(MojangStack::getMaxStackSize) {
    
    override fun transform() {
        methodNode.instructions = buildInsnList {
            aLoad(0)
            invokeStatic(::getMaxStackSize)
            ireturn()
        }
    }
    
    @JvmStatic
    fun getMaxStackSize(item: MojangStack): Int {
        return item.novaItem?.maxStackSize ?: item.item.maxStackSize
    }
    
}