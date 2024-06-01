package xyz.xenondevs.nova.tileentity.network.type.item

import xyz.xenondevs.cbf.Compound
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

interface ItemFilter<S: ItemFilter<S>> {
    
    val type: ItemFilterType<S>
    
    fun allows(itemStack: MojangStack): Boolean
    
    fun toItemStack(): BukkitStack
    
    @Suppress("UNCHECKED_CAST")
    fun toCompound(): Compound = type.serialize(this as S)
    
}

interface ItemFilterType<T: ItemFilter<T>> {
    
    fun serialize(filter: T): Compound
    
    fun deserialize(compound: Compound): T
    
    fun copy(filter: T): T

}