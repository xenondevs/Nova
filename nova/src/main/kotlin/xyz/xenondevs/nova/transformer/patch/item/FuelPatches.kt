@file:Suppress("unused", "UnusedReceiverParameter")

package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.item.FuelUtils
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ABSTRACT_FURNACE_BLOCK_ENTITY_GET_BURN_DURATION_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ABSTRACT_FURNACE_BLOCK_ENTITY_IS_FUEL_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionUtils

internal object FuelPatches : MultiTransformer(setOf(AbstractFurnaceBlockEntity::class), computeFrames = true) {
    
    override fun transform() {
        ABSTRACT_FURNACE_BLOCK_ENTITY_IS_FUEL_METHOD.replaceWith(
            ReflectionUtils.getMethodByName(FuelPatches::class, false, "isFuel")
        )
        
        ABSTRACT_FURNACE_BLOCK_ENTITY_GET_BURN_DURATION_METHOD.replaceWith(
            ReflectionUtils.getMethodByName(FuelPatches::class, false, "getBurnDuration")
        )
    }
    
    @JvmStatic
    fun isFuel(itemStack: ItemStack): Boolean {
        return FuelUtils.isFuel(itemStack)
    }
    
    @JvmStatic
    fun AbstractFurnaceBlockEntity.getBurnDuration(itemStack: ItemStack): Int {
        if (itemStack.isEmpty)
            return 0
        
        return FuelUtils.getBurnTime(itemStack) ?: 0
    }
    
}