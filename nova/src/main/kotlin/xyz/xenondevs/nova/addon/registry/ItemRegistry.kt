package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.item.ItemLogic
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.material.NovaBlock
import xyz.xenondevs.nova.material.NovaItem
import xyz.xenondevs.nova.material.builder.NovaItemBuilder
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set

interface ItemRegistry : AddonGetter {
    
    fun item(name: String): NovaItemBuilder =
        NovaItemBuilder(addon, name)
    
    fun item(block: NovaBlock): NovaItemBuilder {
        require(block.id.namespace == addon.description.id) { "The block must be from the same addon (${block.id})!" }
        return NovaItemBuilder.fromBlock(block)
    }
    
    fun registerItem(
        name: String,
        vararg behaviors: ItemBehaviorHolder<*>,
        localizedName: String = "item.${addon.description.id}.$name",
        isHidden: Boolean = false
    ): NovaItem {
        val item = NovaItem(
            ResourceLocation(addon, name),
            localizedName,
            ItemLogic(*behaviors),
            isHidden = isHidden
        )
        NovaRegistries.ITEM[item.id] = item
        return item
    }
    
    fun registerItem(
        block: NovaBlock,
        vararg behaviors: ItemBehaviorHolder<*>,
        localizedName: String = block.localizedName,
        isHidden: Boolean = false
    ): NovaItem {
        require(block.id.namespace == addon.description.id) { "The block must be from the same addon (${block.id})!" }
        val item = NovaItem(
            block.id,
            localizedName,
            ItemLogic(*behaviors),
            isHidden = isHidden
        )
        NovaRegistries.ITEM[item.id] = item
        return item
    }
    
    fun registerUnnamedItem(
        name: String,
        isHidden: Boolean = false
    ): NovaItem {
        val item = NovaItem(
            ResourceLocation(addon, name),
            "",
            ItemLogic(),
            isHidden = isHidden
        )
        NovaRegistries.ITEM[item.id] = item
        return item
    }
    
    fun registerUnnamedHiddenItem(
        name: String
    ): NovaItem {
        val item = NovaItem(
            ResourceLocation(addon, name),
            "",
            ItemLogic(),
            isHidden = true
        )
        NovaRegistries.ITEM[item.id] = item
        return item
    }

}