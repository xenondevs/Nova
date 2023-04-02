package xyz.xenondevs.nova.api.material

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.data.NamespacedId

@Suppress("DEPRECATION")
@Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
interface NovaMaterialRegistry {
    
    /**
     * Gets the [NovaMaterial] of this [id] or throws an exception if there isn't one.
     * The id has to be in the format namespace:name.
     */
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    fun get(id: String): NovaMaterial
    
    /**
     * Gets the [NovaMaterial] of this [id] or throws an exception if there isn't one.
     */
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    fun get(id: NamespacedId): NovaMaterial
    
    
    /**
     * Gets the [NovaMaterial] of this [item] or throws an exception if this [ItemStack] is not from Nova.
     */
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    fun get(item: ItemStack): NovaMaterial
    
    /**
     * Gets the [NovaMaterial] of this [id] or null if there isn't one.
     * The id has to be in the format namespace:name.
     */
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    fun getOrNull(id: String): NovaMaterial?
    
    /**
     * Gets the [NovaMaterial] of this [id] or null if there isn't one.
     */
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    fun getOrNull(id: NamespacedId): NovaMaterial?
    
    
    /**
     * Gets the [NovaMaterial] of this [item] or null if this [ItemStack] is not from Nova.
     */
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    fun getOrNull(item: ItemStack): NovaMaterial?
    
    /**
     * Gets a list of [NovaMaterials][NovaMaterial] registered under this name in all Nova namespaces.
     */
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    fun getNonNamespaced(name: String): List<NovaMaterial>
    
}