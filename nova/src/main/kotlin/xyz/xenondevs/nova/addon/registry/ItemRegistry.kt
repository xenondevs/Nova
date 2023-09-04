package xyz.xenondevs.nova.addon.registry

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.NovaItemBuilder
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.block.NovaBlock

interface ItemRegistry : AddonGetter {
    
    fun item(name: String): NovaItemBuilder =
        NovaItemBuilder(addon, name)
    
    fun item(block: NovaBlock): NovaItemBuilder {
        require(block.id.namespace == addon.description.id) { "The block must be from the same addon (${block.id})!" }
        return NovaItemBuilder.fromBlock(block)
    }
    
    fun registerItem(
        name: String,
        vararg behaviors: ItemBehaviorHolder,
        localizedName: String = "item.${addon.description.id}.$name",
        isHidden: Boolean = false
    ): NovaItem {
        val item = NovaItem(
            ResourceLocation(addon, name),
            Component.translatable(localizedName),
            Style.empty(),
            behaviors.asList(),
            isHidden = isHidden
        )
        NovaRegistries.ITEM[item.id] = item
        return item
    }
    
    fun registerItem(
        block: NovaBlock,
        vararg behaviors: ItemBehaviorHolder,
        localizedName: String? = null,
        isHidden: Boolean = false
    ): NovaItem {
        require(block.id.namespace == addon.description.id) { "The block must be from the same addon (${block.id})!" }
        val item = NovaItem(
            block.id,
            localizedName?.let(Component::translatable) ?: block.name,
            Style.empty(),
            behaviors.asList(),
            isHidden = isHidden,
            block = block
        )
        block.item = item
        NovaRegistries.ITEM[item.id] = item
        return item
    }
    
    fun registerUnnamedItem(
        name: String,
        isHidden: Boolean = false
    ): NovaItem {
        val item = NovaItem(
            ResourceLocation(addon, name),
            Component.empty(),
            Style.empty(),
            emptyList(),
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
            Component.empty(),
            Style.empty(),
            emptyList(),
            isHidden = true
        )
        NovaRegistries.ITEM[item.id] = item
        return item
    }

}