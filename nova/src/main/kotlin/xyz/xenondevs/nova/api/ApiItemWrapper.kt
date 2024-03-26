package xyz.xenondevs.nova.api

import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.block.NovaBlock
import xyz.xenondevs.nova.api.data.NamespacedId
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.util.component.adventure.toPlainText
import xyz.xenondevs.nova.util.namespacedId
import xyz.xenondevs.nova.api.item.NovaItem as INovaItem

internal class ApiItemWrapper(private val item: NovaItem): INovaItem {
    
    override fun getId(): NamespacedId = item.id.namespacedId
    override fun getBlock(): NovaBlock? = item.block?.let(::ApiBlockWrapper)
    override fun getMaxStackSize(): Int = item.maxStackSize
    
    override fun getName(): Component = item.name
    override fun getPlaintextName(locale: String): String = item.name.toPlainText(locale)
    
    override fun createItemStack(amount: Int): ItemStack {
        return item.createItemStack(amount)
    }
    
    override fun createClientsideItemStack(amount: Int): ItemStack {
        return item.model.createClientsideItemStack(false).apply { setAmount(amount) }
    }
    
}