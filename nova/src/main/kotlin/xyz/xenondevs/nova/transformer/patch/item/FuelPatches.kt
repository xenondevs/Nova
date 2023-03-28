@file:Suppress("UNUSED_PARAMETER")

package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.item.FuelUtils
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ABSTRACT_FURNACE_BLOCK_ENTITY_GET_BURN_DURATION_METHOD

internal object FuelPatches : MultiTransformer(AbstractFurnaceBlockEntity::class) {
    
    override fun transform() {
        AbstractFurnaceBlockEntity::isFuel.replaceWith(::isFuel)
        ABSTRACT_FURNACE_BLOCK_ENTITY_GET_BURN_DURATION_METHOD.replaceWith(::getBurnDuration)
    }
    
    @JvmStatic
    fun isFuel(itemStack: ItemStack): Boolean {
        return FuelUtils.isFuel(itemStack)
    }
    
    @JvmStatic
    fun getBurnDuration(furnace: AbstractFurnaceBlockEntity, itemStack: ItemStack): Int {
        if (itemStack.isEmpty)
            return 0
        
        return FuelUtils.getBurnTime(itemStack) ?: 0
    }
    
}