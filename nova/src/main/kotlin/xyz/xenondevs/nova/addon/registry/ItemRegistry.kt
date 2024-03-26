package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.NovaItemBuilder
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.world.block.NovaBlock

interface ItemRegistry : AddonGetter {
    
    fun item(name: String, init: NovaItemBuilder.() -> Unit): NovaItem =
        NovaItemBuilder(addon, name).apply(init).register()
    
    fun item(block: NovaBlock, init: NovaItemBuilder.() -> Unit): NovaItem {
        require(block.id.namespace == addon.description.id) { "The block must be from the same addon (${block.id})!" }
        return NovaItemBuilder.fromBlock(block).apply(init).register()
    }
    
    fun registerItem(
        name: String,
        vararg behaviors: ItemBehaviorHolder,
        localizedName: String? = null,
        isHidden: Boolean = false
    ): NovaItem = item(name) {
        behaviors(*behaviors)
        localizedName?.let(::localizedName)
        hidden(isHidden)
    }
    
    fun registerItem(
        block: NovaBlock,
        vararg behaviors: ItemBehaviorHolder,
        localizedName: String? = null,
        isHidden: Boolean = false
    ): NovaItem = item(block) {
        behaviors(*behaviors)
        localizedName?.let(::localizedName)
        hidden(isHidden)
    }
    
}