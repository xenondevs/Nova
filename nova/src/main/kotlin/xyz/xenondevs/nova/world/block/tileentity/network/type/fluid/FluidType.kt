package xyz.xenondevs.nova.world.block.tileentity.network.type.fluid

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.entries.ItemTypeEntries
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.asEither
import xyz.xenondevs.nova.world.item.mapToItemStack

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
    val bucketType: RegistryEntry.Either<NovaItem, ItemType>
) {
    
    WATER("block.minecraft.water", ItemTypeEntries.WATER_BUCKET.asEither()),
    LAVA("block.minecraft.lava", ItemTypeEntries.LAVA_BUCKET.asEither());
    
    private val _bucket: ItemStack
        by bucketType.mapToItemStack()
    
    /**
     * An item stack representing a bucket of this fluid.
     */
    val bucket: ItemStack
        get() = _bucket.clone()
    
}