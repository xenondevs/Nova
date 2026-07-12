package xyz.xenondevs.nova.world.block.tileentity.network.type.fluid

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.entries.ItemTypeEntries

/**
 * A type of fluid.
 */
enum class FluidType(
    /**
     * The translation key of the fluid name.
     */
    val localizedName: String,
    /**
     * The bucket item type of the fluid.
     */
    val bucketType: RegistryEntry.Paper<ItemType>
) {
    
    WATER("block.minecraft.water", ItemTypeEntries.WATER_BUCKET),
    LAVA("block.minecraft.lava", ItemTypeEntries.LAVA_BUCKET);
    
    private val _bucket: ItemStack
        by bucketType.map { it.createItemStack() }
    
    /**
     * An item stack representing a bucket of this fluid.
     */
    val bucket: ItemStack
        get() = _bucket.clone()
    
}