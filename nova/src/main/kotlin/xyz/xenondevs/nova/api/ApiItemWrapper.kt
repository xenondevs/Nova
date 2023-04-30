package xyz.xenondevs.nova.api

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.block.NovaBlock
import xyz.xenondevs.nova.api.data.NamespacedId
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.util.namespacedId
import xyz.xenondevs.nova.api.item.NovaItem as INovaItem

internal class ApiItemWrapper(private val item: NovaItem): INovaItem {
    
    override fun getId(): NamespacedId = item.id.namespacedId
    override fun getBlock(): NovaBlock? = item.block?.let(::ApiBlockWrapper)
    override fun getMaxStackSize(): Int = item.maxStackSize
    
    override fun getLocalizedName(locale: String): String {
        return LocaleManager.getTranslation(locale, item.localizedName)
    }
    
    override fun createItemStack(amount: Int): ItemStack {
        return item.createItemStack(amount)
    }
    
    override fun createClientsideItemStack(amount: Int): ItemStack {
        return item.createClientsideItemStack(amount)
    }
    
}