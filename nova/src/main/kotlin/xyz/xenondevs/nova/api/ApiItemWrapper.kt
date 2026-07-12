@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.api

import net.kyori.adventure.text.Component
import org.bukkit.craftbukkit.block.CraftBlockType
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.component.adventure.toPlainText
import xyz.xenondevs.nova.util.item.clientsideCopy
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.name
import xyz.xenondevs.nova.api.block.NovaBlock as INovaBlock
import xyz.xenondevs.nova.api.data.NamespacedId as INamespacedId
import xyz.xenondevs.nova.api.item.NovaItem as INovaItem

internal class ApiItemWrapper(private val item: NovaItem) : INovaItem {
    
    override fun getId(): INamespacedId = NamespacedId(item.key.namespace(), item.key.value())
    override fun getBlock(): INovaBlock? = ((item.block as? CraftBlockType<*>)?.handle as? NovaBlock)?.let(::ApiBlockWrapper)
    override fun getMaxStackSize(): Int = item.entry.get().maxStackSize
    
    override fun getName(): Component = item.entry.get().name
    override fun getPlaintextName(locale: String): String = item.entry.get().name.toPlainText(locale)
    
    override fun createItemStack(amount: Int): ItemStack = item.entry.get().createItemStack(amount)
    override fun createClientsideItemStack(amount: Int): ItemStack = item.entry.get().createItemStack(amount).clientsideCopy()
    
}