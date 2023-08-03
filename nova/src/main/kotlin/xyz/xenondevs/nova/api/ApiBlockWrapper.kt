package xyz.xenondevs.nova.api

import net.kyori.adventure.text.Component
import xyz.xenondevs.nova.api.data.NamespacedId
import xyz.xenondevs.nova.api.item.NovaItem
import xyz.xenondevs.nova.util.component.adventure.toPlainText
import xyz.xenondevs.nova.util.namespacedId
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.api.block.NovaBlock as INovaBlock

internal class ApiBlockWrapper(val block: NovaBlock): INovaBlock {
    
    override fun getId(): NamespacedId = block.id.namespacedId
    override fun getItem(): NovaItem? = block.item?.let(::ApiItemWrapper)
    override fun getName(): Component  = block.name
    override fun getPlaintextName(locale: String): String = block.name.toPlainText(locale)
    
}