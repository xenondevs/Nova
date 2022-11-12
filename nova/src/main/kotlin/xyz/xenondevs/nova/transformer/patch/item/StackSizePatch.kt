@file:Suppress("unused")

package xyz.xenondevs.nova.transformer.patch.item

import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import net.minecraft.world.item.ItemStack as MojangStack

/**
 * Patches the [MojangStack.getMaxStackSize] method to account for Nova's custom max stack sizes.
 */
internal object StackSizePatch : MethodTransformer(ReflectionRegistry.ITEM_STACK_GET_MAX_STACK_SIZE_METHOD, computeFrames = true) {
    
    override fun transform() {
        methodNode.instructions = buildInsnList {
            aLoad(0)
            invokeStatic(ReflectionUtils.getMethodByName(StackSizePatch::class.java, false, "getMaxStackSize"))
            ireturn()
        }
    }
    
    @JvmStatic
    fun getMaxStackSize(item: MojangStack): Int {
        return item.novaMaterial?.maxStackSize ?: item.item.maxStackSize
    }
    
}