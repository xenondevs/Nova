package xyz.xenondevs.nova.tileentity.network.type.item

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound

interface ItemFilter<S: ItemFilter<S>> {
    
    val type: ItemFilterType<S>
    
    fun allows(itemStack: ItemStack): Boolean
    
    fun toItemStack(): ItemStack
    
    @Suppress("UNCHECKED_CAST")
    fun toCompound(): Compound = type.serialize(this as S)
    
}

interface ItemFilterType<T: ItemFilter<T>> {
    
    fun serialize(filter: T): Compound
    
    fun deserialize(compound: Compound): T
    
    fun copy(filter: T): T

}