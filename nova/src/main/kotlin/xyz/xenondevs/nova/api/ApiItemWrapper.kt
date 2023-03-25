package xyz.xenondevs.nova.api

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.block.NovaBlock
import xyz.xenondevs.nova.api.data.NamespacedId
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.material.NovaItem
import xyz.xenondevs.nova.util.namespacedId
import xyz.xenondevs.nova.api.item.NovaItem as INovaItem

internal class ApiItemWrapper(private val item: NovaItem): INovaItem {
    /**
     * The [NamespacedId] of this [NovaItem].
     */
    override val id = item.id.namespacedId
    
    /**
     * The [NovaBlock] of this [NovaItem] or null if there is none.
     */
    override val block: NovaBlock? = item.block?.let(::ApiBlockWrapper)
    
    /**
     * The maximum stack size for items of this [NovaItem].
     */
    override val maxStackSize = item.maxStackSize
    
    /**
     * Gets the localized name for this [NovaItem].
     * The [locale] should be in the same format as the language file names in
     * resource packs (e.g. en_us).
     */
    override fun getLocalizedName(locale: String): String {
        return LocaleManager.getTranslation(locale, item.localizedName)
    }
    
    /**
     * Creates an [ItemStack] for this [NovaItem] with the given [amount].
     */
    override fun createItemStack(amount: Int): ItemStack {
        return item.createItemStack(amount)
    }
    
    /**
     * Creates the client-side [ItemStack] for this [NovaItem] with the given [amount].
     */
    override fun createClientsideItemStack(amount: Int): ItemStack {
        return item.createClientsideItemStack(amount)
    }
}