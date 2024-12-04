package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.NovaItemBuilder
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorHolder

interface ItemRegistry : AddonGetter {
    
    fun item(name: String, item: NovaItemBuilder.() -> Unit): NovaItem =
        NovaItemBuilder(addon, name).apply(item).register()
    
    fun item(block: NovaBlock, name: String = block.id.value(), item: NovaItemBuilder.() -> Unit): NovaItem {
        require(block.id.namespace() == addon.id) { "The block must be from the same addon (${block.id})!" }
        return NovaItemBuilder.fromBlock(Key(addon, name), block).apply(item).register()
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
    
    fun registerItem(
        block: NovaBlock,
        name: String,
        vararg behaviors: ItemBehaviorHolder,
        localizedName: String? = null,
        isHidden: Boolean = false
    ): NovaItem = item(block, name) {
        behaviors(*behaviors)
        localizedName?.let(::localizedName)
        hidden(isHidden)
    }
    
}