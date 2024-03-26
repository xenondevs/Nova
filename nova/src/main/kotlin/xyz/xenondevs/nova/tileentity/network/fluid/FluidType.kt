package xyz.xenondevs.nova.tileentity.network.fluid

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

enum class FluidType(val localizedName: String, private val _bucket: ItemStack) {
    
    WATER("block.minecraft.water", ItemStack(Material.WATER_BUCKET)),
    LAVA("block.minecraft.lava", ItemStack(Material.LAVA_BUCKET));
    
    val bucket: ItemStack
        get() = _bucket.clone()
    
}