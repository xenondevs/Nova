package xyz.xenondevs.nova.api.material

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.data.NamespacedId

@Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
interface NovaMaterialRegistry {
    
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    /**
     * Gets the [NovaMaterial] of this [id] or throws an exception if there isn't one.
     * The id has to be in the format namespace:name.
     */
    fun get(id: String): NovaMaterial
    
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    /**
     * Gets the [NovaMaterial] of this [id] or throws an exception if there isn't one.
     */
    fun get(id: NamespacedId): NovaMaterial
    
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    /**
     * Gets the [NovaMaterial] of this [item] or throws an exception if this [ItemStack] is not from Nova.
     */
    fun get(item: ItemStack): NovaMaterial
    
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    /**
     * Gets the [NovaMaterial] of this [id] or null if there isn't one.
     * The id has to be in the format namespace:name.
     */
    fun getOrNull(id: String): NovaMaterial?
    
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    /**
     * Gets the [NovaMaterial] of this [id] or null if there isn't one.
     */
    fun getOrNull(id: NamespacedId): NovaMaterial?
    
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    /**
     * Gets the [NovaMaterial] of this [item] or null if this [ItemStack] is not from Nova.
     */
    fun getOrNull(item: ItemStack): NovaMaterial?
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    /**
     * Gets a list of [NovaMaterials][NovaMaterial] registered under this name in all Nova namespaces.
     */
    fun getNonNamespaced(name: String): List<NovaMaterial>
    
}