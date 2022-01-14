package xyz.xenondevs.nova.api.material

import org.bukkit.inventory.ItemStack

interface NovaMaterialRegistry {
    
    /**
     * Gets the [NovaMaterial] of this [id] or throws an exception if there isn't one.
     * Both ``nova:item`` and just ``item`` are valid here.
     */
    fun get(id: String): NovaMaterial
    
    /**
     * Gets the [NovaMaterial] of this [modelData] or throws an exception if there isn't one.
     */
    fun get(modelData: Int): NovaMaterial
    
    /**
     * Gets the [NovaMaterial] of this [item] or throws an exception if this [ItemStack] is not from Nova.
     */
    fun get(item: ItemStack): NovaMaterial
    
    /**
     * Gets the [NovaMaterial] of this [id] or null if there isn't one.
     * Both ``nova:item`` and just ``item`` are valid here.
     */
    fun getOrNull(id: String): NovaMaterial?
    
    /**
     * Gets the [NovaMaterial] of this [modelData] or null if there isn't one.
     */
    fun getOrNull(modelData: Int): NovaMaterial?
    
    /**
     * Gets the [NovaMaterial] of this [item] or null if this [ItemStack] is not from Nova.
     */
    fun getOrNull(item: ItemStack): NovaMaterial?
    
}