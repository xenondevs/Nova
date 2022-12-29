package xyz.xenondevs.nova.api.material

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.data.NamespacedId

interface NovaMaterial {
    
    /**
     * The [NamespacedId] of this [NovaMaterial].
     */
    val id: NamespacedId
    
    /**
     * The maximum stack size for items of this [NovaMaterial].
     */
    val maxStackSize: Int
    
    /**
     * Gets the localized name for this [NovaMaterial].
     * The [locale] should be in the same format as the language file names in
     * resource packs (e.g. en_us).
     */
    fun getLocalizedName(locale: String): String
    
    /**
     * Creates an [ItemStack] for this [NovaMaterial] with the given [amount].
     */
    fun createItemStack(amount: Int): ItemStack
    
    /**
     * Creates an [ItemStack] for this [NovaMaterial] with an amount of 1.
     */
    fun createItemStack(): ItemStack = createItemStack(1)
    
    /**
     * Creates the client-side [ItemStack] for this [NovaMaterial] with the given [amount].
     */
    fun createClientsideItemStack(amount: Int): ItemStack
    
    /**
     * Creates the client-side [ItemStack] for this [NovaMaterial] with an amount of 1.
     */
    fun createClientsideItemStack(): ItemStack = createClientsideItemStack(1)
    
}