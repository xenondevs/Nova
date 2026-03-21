package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaToolIconProvider
import xyz.xenondevs.nova.util.novaKey
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilterType
import xyz.xenondevs.nova.world.item.Equipment
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.TooltipStyle
import xyz.xenondevs.nova.world.item.recipe.RecipeType
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.ToolTier
import xyz.xenondevs.nova.world.player.ability.AbilityType
import xyz.xenondevs.nova.world.player.attachment.AttachmentType

/**
 * Contains all default Nova registries like [NovaRegistries.BLOCK] and [NovaRegistries.ITEM].
 */
object NovaRegistries {
    
    internal val RELOADABLE: Boolean by MAIN_CONFIG.entry("debug", "reloading", "registry")
    
    internal val registries = HashMap<Key, MutableNovaRegistry<*>>()
    internal var isFrozen: Boolean = false
        private set
    
    internal val INTERNAL_BLOCK = createRegistry<NovaBlock>(novaKey("block"), reloadable = false)
    internal val INTERNAL_ITEM = createRegistry<NovaItem>(novaKey("item"))
    internal val INTERNAL_EQUIPMENT = createRegistry<Equipment>(novaKey("equipment"))
    internal val INTERNAL_TOOL_TIER = createRegistry<ToolTier>(novaKey("tool_tier"))
    internal val INTERNAL_TOOL_CATEGORY = createRegistry<ToolCategory>(novaKey("tool_category"))
    internal val INTERNAL_NETWORK_TYPE = createRegistry<NetworkType<*>>(novaKey("network_type"), reloadable = false)
    internal val INTERNAL_ABILITY_TYPE = createRegistry<AbilityType<*>>(novaKey("ability_type"))
    internal val INTERNAL_ATTACHMENT_TYPE = createRegistry<AttachmentType<*>>(novaKey("attachment_type"))
    internal val INTERNAL_RECIPE_TYPE = createRegistry<RecipeType<*>>(novaKey("recipe_type"))
    internal val INTERNAL_GUI_TEXTURE = createRegistry<GuiTexture>(novaKey("gui_texture"))
    internal val INTERNAL_WAILA_INFO_PROVIDER = createRegistry<WailaInfoProvider<*, *>>(novaKey("waila_info_provider"))
    internal val INTERNAL_WAILA_TOOL_ICON_PROVIDER = createRegistry<WailaToolIconProvider>(novaKey("waila_tool_icon_provider"))
    internal val INTERNAL_ITEM_FILTER_TYPE = createRegistry<ItemFilterType<*>>(novaKey("item_filter_type"))
    internal val INTERNAL_TOOLTIP_STYLE = createRegistry<TooltipStyle>(novaKey("tooltip_style"))
    
    /**
     * Registry for all [NovaBlocks][NovaBlock].
     */
    @JvmField
    val BLOCK: NovaRegistry<NovaBlock> = INTERNAL_BLOCK.unmodifiableView
    
    /**
     * Registry for all [NovaItems][NovaItem].
     */
    @JvmField
    val ITEM: NovaRegistry<NovaItem> = INTERNAL_ITEM.unmodifiableView
    
    /**
     * Registry for all [Equipments][Equipment].
     */
    @JvmField
    val EQUIPMENT: NovaRegistry<Equipment> = INTERNAL_EQUIPMENT.unmodifiableView
    
    /**
     * Registry for all [ToolTiers][ToolTier].
     */
    @JvmField
    val TOOL_TIER: NovaRegistry<ToolTier> = INTERNAL_TOOL_TIER.unmodifiableView
    
    /**
     * Registry for all [ToolCategories][ToolCategory].
     */
    @JvmField
    val TOOL_CATEGORY: NovaRegistry<ToolCategory> = INTERNAL_TOOL_CATEGORY.unmodifiableView
    
    /**
     * Registry for all [NetworkTypes][NetworkType].
     */
    @JvmField
    val NETWORK_TYPE: NovaRegistry<NetworkType<*>> = INTERNAL_NETWORK_TYPE.unmodifiableView
    
    /**
     * Registry for all [AbilityTypes][AbilityType].
     */
    @JvmField
    val ABILITY_TYPE: NovaRegistry<AbilityType<*>> = INTERNAL_ABILITY_TYPE.unmodifiableView
    
    /**
     * Registry for all [AttachmentTypes][AttachmentType].
     */
    @JvmField
    val ATTACHMENT_TYPE: NovaRegistry<AttachmentType<*>> = INTERNAL_ATTACHMENT_TYPE.unmodifiableView
    
    /**
     * Registry for all [RecipeTypes][RecipeType].
     */
    @JvmField
    val RECIPE_TYPE: NovaRegistry<RecipeType<*>> = INTERNAL_RECIPE_TYPE.unmodifiableView
    
    /**
     * Registry for all [GuiTextures][GuiTexture].
     */
    @JvmField
    val GUI_TEXTURE: NovaRegistry<GuiTexture> = INTERNAL_GUI_TEXTURE.unmodifiableView
    
    /**
     * Registry for all [WailaInfoProviders][WailaInfoProvider].
     */
    @JvmField
    val WAILA_INFO_PROVIDER: NovaRegistry<WailaInfoProvider<*, *>> = INTERNAL_WAILA_INFO_PROVIDER.unmodifiableView
    
    /**
     * Registry for all [WailaToolIconProviders][WailaToolIconProvider].
     */
    @JvmField
    val WAILA_TOOL_ICON_PROVIDER: NovaRegistry<WailaToolIconProvider> = INTERNAL_WAILA_TOOL_ICON_PROVIDER.unmodifiableView
    
    /**
     * Registry for all [ItemFilterTypes][ItemFilterType].
     */
    @JvmField
    val ITEM_FILTER_TYPE: NovaRegistry<ItemFilterType<*>> = INTERNAL_ITEM_FILTER_TYPE.unmodifiableView
    
    /**
     * Registry for all [TooltipStyles][TooltipStyle].
     */
    @JvmField
    val TOOLTIP_STYLE: NovaRegistry<TooltipStyle> = INTERNAL_TOOLTIP_STYLE.unmodifiableView
    
    /**
     * Creates a new registry with the given [key] that is tracked by Nova.
     * Contrary to creating a [MutableNovaRegistry] directly by itself, registries tracked by
     * Nova can be reloaded with the built-in reload command and should be loaded via [RegistryLoader].
     */
    internal fun <T : NovaRegistryElement<T>> createRegistry(
        key: Key,
        reloadable: Boolean = RELOADABLE
    ): MutableNovaRegistry<T> {
        val registry = MutableNovaRegistry<T>(key, reloadable)
        if (registries.putIfAbsent(key, registry) != null)
            throw IllegalArgumentException("Registry key $key is already in use")
        return registry
    }
    
    /**
     * Freezes all Nova-tracked registries, binding their entries and takes and making them immutable.
     */
    internal fun freeze() {
        isFrozen = true
        registries.values.forEach(MutableNovaRegistry<*>::freeze)
    }
    
}