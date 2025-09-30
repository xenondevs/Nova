package xyz.xenondevs.nova.world.block.tileentity.network.type.fluid

import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.keys.ItemTypeKeys
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType

enum class FluidType(val localizedName: String, private val _bucket: TypedKey<ItemType>) {
    
    WATER("block.minecraft.water", ItemTypeKeys.WATER_BUCKET),
    LAVA("block.minecraft.lava", ItemTypeKeys.LAVA_BUCKET);
    
    val bucket: ItemStack
        get() = Registry.ITEM.get(_bucket)?.createItemStack() ?: ItemStack.empty()
    
}