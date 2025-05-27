package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.NovaItemBuilder
import xyz.xenondevs.nova.world.item.behavior.ItemBehaviorHolder

@Deprecated(REGISTRIES_DEPRECATION)
interface ItemRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun item(name: String, item: NovaItemBuilder.() -> Unit): NovaItem =
        addon.item(name, item)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun item(block: NovaBlock, name: String = block.id.value(), item: NovaItemBuilder.() -> Unit): NovaItem =
        addon.item(block, name, item)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun registerItem(
        name: String,
        vararg behaviors: ItemBehaviorHolder,
        localizedName: String? = null,
        isHidden: Boolean = false
    ): NovaItem = addon.registerItem(name, *behaviors, localizedName = localizedName, isHidden = isHidden)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun registerItem(
        block: NovaBlock,
        vararg behaviors: ItemBehaviorHolder,
        localizedName: String? = null,
        isHidden: Boolean = false
    ): NovaItem = addon.registerItem(block, *behaviors, localizedName = localizedName, isHidden = isHidden)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun registerItem(
        block: NovaBlock,
        name: String,
        vararg behaviors: ItemBehaviorHolder,
        localizedName: String? = null,
        isHidden: Boolean = false
    ): NovaItem = addon.registerItem(block, name, *behaviors, localizedName = localizedName, isHidden = isHidden)
    
}