package xyz.xenondevs.nova.api.item

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.block.NovaBlock
import xyz.xenondevs.nova.api.data.NamespacedId

interface NovaItem {
    
    /**
     * The [NamespacedId] of this [NovaItem].
     */
    val id: NamespacedId
    
    /**
     * The [NovaBlock] of this [NovaItem] or null if there is none.
     */
    val block: NovaBlock?
    
    /**
     * The maximum stack size for items of this [NovaItem].
     */
    val maxStackSize: Int
    
    /**
     * Gets the localized name for this [NovaItem].
     * The [locale] should be in the same format as the language file names in
     * resource packs (e.g. en_us).
     */
    fun getLocalizedName(locale: String): String
    
    /**
     * Creates an [ItemStack] for this [NovaItem] with the given [amount].
     */
    fun createItemStack(amount: Int): ItemStack

    /**
     * Creates an [ItemStack] for this [NovaItem] with an amount of 1.
     */
    fun createItemStack(): ItemStack = createItemStack(1)

    /**
     * Creates the client-side [ItemStack] for this [NovaItem] with the given [amount].
     */
    fun createClientsideItemStack(amount: Int): ItemStack

    /**
     * Creates the client-side [ItemStack] for this [NovaItem] with an amount of 1.
     */
    fun createClientsideItemStack(): ItemStack = createClientsideItemStack(1)

}