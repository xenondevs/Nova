package xyz.xenondevs.nova.world.block.tileentity.network.type.item

import kotlinx.serialization.Serializable
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.serialization.kotlinx.ItemFilterTypeSerializer
import xyz.xenondevs.nova.world.item.behavior.ItemFilterContainer

/**
 * Allows or prevents items from being transferred in item networks.
 */
interface ItemFilter<S : ItemFilter<S>> {
    
    /**
     * The [ItemFilterType] of this [ItemFilter].
     */
    val type: ItemFilterType<S>
    
    /**
     * Returns whether [itemStack] is allowed by this filter.
     */
    fun allows(itemStack: ItemStack): Boolean
    
    /**
     * Converts this filter to an [ItemStack],
     * which is a nova item with the [ItemFilterContainer] behavior.
     */
    fun toItemStack(): ItemStack
    
    /**
     * Serializes this filter to a [Compound].
     */
    @Suppress("UNCHECKED_CAST")
    fun toCompound(): Compound = type.serializer.serialize(this as S)
    
}

/**
 * Serializes [ItemFilters][ItemFilter] to and from [Compounds][Compound].
 */
interface ItemFilterSerializer<T : ItemFilter<T>> {
    
    /**
     * Serializes [filter] to a [Compound].
     */
    fun serialize(filter: T): Compound
    
    /**
     * Deserializes an [ItemFilter] from [compound].
     */
    fun deserialize(compound: Compound): T
    
    /**
     * Returns a deep copy of [filter].
     */
    fun copy(filter: T): T
    
}

/**
 * Represents a type of [ItemFilter].
 */
@Serializable(with = ItemFilterTypeSerializer::class)
class ItemFilterType<T : ItemFilter<T>> internal constructor(
    override val entry: RegistryEntry.Nova<ItemFilterType<T>>,
    /**
     * The serializer used to (de)serialize filters of this type from and to [Compounds][Compound].
     */
    val serializer: ItemFilterSerializer<T>
) : NovaRegistryElement<ItemFilterType<T>>