package xyz.xenondevs.nova.tileentity.network.fluid

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

enum class FluidType(private val _bucket: ItemStack?) {
    
    NONE(null),
    WATER(ItemStack(Material.WATER_BUCKET)),
    LAVA(ItemStack(Material.LAVA_BUCKET));
    
    val bucket: ItemStack?
        get() = _bucket?.clone()
    
}