@file:Suppress("UNUSED_PARAMETER")

package xyz.xenondevs.nova.patch.impl.item

import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.block.entity.FuelValues
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.world.item.behavior.Fuel

internal object FuelPatches : MultiTransformer(AbstractFurnaceBlockEntity::class) {
    
    override fun transform() {
        VirtualClassPath[FuelValues::isFuel].delegateStatic(::isFuel)
        VirtualClassPath[FuelValues::burnDuration].delegateStatic(::getBurnDuration)
    }
    
    @JvmStatic
    fun isFuel(fuelValues: FuelValues, itemStack: ItemStack): Boolean {
        return Fuel.isFuel(itemStack)
    }
    
    @JvmStatic
    fun getBurnDuration(fuelValues: FuelValues, itemStack: ItemStack): Int {
        if (itemStack.isEmpty)
            return 0
        
        return Fuel.getBurnTime(itemStack) ?: 0
    }
    
}