@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.api

import net.kyori.adventure.text.Component
import xyz.xenondevs.nova.api.item.NovaItem
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.util.component.adventure.toPlainText
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.api.block.NovaBlock as INovaBlock
import xyz.xenondevs.nova.api.data.NamespacedId as INamespacedId

internal class ApiBlockWrapper(val block: NovaBlock) : INovaBlock {
    
    override fun getId(): INamespacedId = NamespacedId(block.id.namespace, block.id.path)
    override fun getItem(): NovaItem? = block.item?.let(::ApiItemWrapper)
    override fun getName(): Component = block.name
    override fun getPlaintextName(locale: String): String = block.name.toPlainText(locale)
    
}