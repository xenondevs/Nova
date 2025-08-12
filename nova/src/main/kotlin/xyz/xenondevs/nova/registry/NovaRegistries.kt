package xyz.xenondevs.nova.registry

import net.minecraft.core.WritableRegistry
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaToolIconProvider
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemFilterType
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjection
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlock
import xyz.xenondevs.nova.world.item.Equipment
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.TooltipStyle
import xyz.xenondevs.nova.world.item.recipe.RecipeType
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.ToolTier
import xyz.xenondevs.nova.world.player.ability.AbilityType
import xyz.xenondevs.nova.world.player.attachment.AttachmentType

@OptIn(ExperimentalWorldGen::class)
object NovaRegistries {
    
    @JvmField
    val WRAPPER_BLOCK: WritableRegistry<WrapperBlock> = simpleRegistry("wrapper_block")
    
    @JvmField
    val BLOCK: WritableRegistry<NovaBlock> = simpleRegistry("block") // TODO: wrapper registry
    
    @JvmField
    val ITEM: FuzzyMappedRegistry<NovaItem> = fuzzyRegistry("item")
    
    @JvmField
    val EQUIPMENT: WritableRegistry<Equipment> = simpleRegistry("armor")
    
    @JvmField
    val TOOL_TIER: WritableRegistry<ToolTier> = simpleRegistry("tool_tier")
    
    @JvmField
    val TOOL_CATEGORY: WritableRegistry<ToolCategory> = simpleRegistry("tool_category")
    
    @JvmField
    val NETWORK_TYPE: WritableRegistry<NetworkType<*>> = simpleRegistry("network_type")
    
    @JvmField
    val ABILITY_TYPE: WritableRegistry<AbilityType<*>> = simpleRegistry("ability_type")
    
    @JvmField
    val ATTACHMENT_TYPE: WritableRegistry<AttachmentType<*>> = simpleRegistry("attachment_type")
    
    @JvmField
    val RECIPE_TYPE: WritableRegistry<RecipeType<*>> = simpleRegistry("recipe_type")
    
    @JvmField
    val GUI_TEXTURE: WritableRegistry<GuiTexture> = simpleRegistry("gui_texture")
    
    @JvmField
    val BIOME_INJECTION: WritableRegistry<BiomeInjection> = simpleRegistry("biome_injection")
    
    @JvmField
    val WAILA_INFO_PROVIDER: WritableRegistry<WailaInfoProvider<*>> = simpleRegistry("waila_info_provider")
    
    @JvmField
    val WAILA_TOOL_ICON_PROVIDER: WritableRegistry<WailaToolIconProvider> = simpleRegistry("waila_tool_icon_provider")
    
    @JvmField
    val ITEM_FILTER_TYPE: WritableRegistry<ItemFilterType<*>> = simpleRegistry("item_filter_type")
    
    @JvmField
    val TOOLTIP_STYLE: WritableRegistry<TooltipStyle> = simpleRegistry("tooltip_style")
    
    private fun <E : Any> simpleRegistry(name: String): WritableRegistry<E> {
        val resourceLocation = ResourceLocation.fromNamespaceAndPath("nova", name)
        return NovaRegistryAccess.addRegistry(resourceLocation)
    }
    
    private fun <E : Any, W : Any> wrappingRegistry(
        name: String,
        wrapperRegistry: WritableRegistry<W>,
        toWrapper: (E) -> W
    ): WritableRegistry<E> {
        val resourceLocation = ResourceLocation.fromNamespaceAndPath("nova", name)
        return NovaRegistryAccess.addRegistry(resourceLocation) { key, lifecycle ->
            WrappingRegistry(key, lifecycle, wrapperRegistry, toWrapper)
        }
    }
    
    private fun <E : Any> fuzzyRegistry(name: String): FuzzyMappedRegistry<E> {
        val resourceLocation = ResourceLocation.fromNamespaceAndPath("nova", name)
        return NovaRegistryAccess.addFuzzyRegistry(resourceLocation)
    }
    
}