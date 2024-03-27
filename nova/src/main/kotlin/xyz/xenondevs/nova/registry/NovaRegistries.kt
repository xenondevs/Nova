package xyz.xenondevs.nova.registry

import net.minecraft.core.WritableRegistry
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.recipe.RecipeType
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.armor.Armor
import xyz.xenondevs.nova.item.enchantment.Enchantment
import xyz.xenondevs.nova.item.enchantment.EnchantmentCategory
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolTier
import xyz.xenondevs.nova.player.ability.AbilityType
import xyz.xenondevs.nova.player.attachment.AttachmentType
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaToolIconProvider
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjection

object NovaRegistries {
    
    @JvmField
    val BLOCK = registerSimple<NovaBlock>("block")
    
    @JvmField
    val ITEM = registerFuzzy<NovaItem>("item")
    
    @JvmField
    val ARMOR = registerSimple<Armor>("armor")
    
    @JvmField
    val TOOL_TIER = registerSimple<ToolTier>("tool_tier")
    
    @JvmField
    val TOOL_CATEGORY = registerSimple<ToolCategory>("tool_category")
    
    @JvmField
    val ENCHANTMENT_CATEGORY = registerSimple<EnchantmentCategory>("enchantment_category")
    
    @JvmField
    val ENCHANTMENT = registerSimple<Enchantment>("enchantment")
    
    @JvmField
    val NETWORK_TYPE = registerSimple<NetworkType>("network_type")
    
    @JvmField
    val ABILITY_TYPE = registerSimple<AbilityType<*>>("ability_type")
    
    @JvmField
    val ATTACHMENT_TYPE = registerSimple<AttachmentType<*>>("attachment_type")
    
    @JvmField
    val RECIPE_TYPE = registerSimple<RecipeType<*>>("recipe_type")
    
    @JvmField
    @ExperimentalWorldGen
    val BIOME_INJECTION = registerSimple<BiomeInjection>("biome_injection")
    
    @JvmField
    val WAILA_INFO_PROVIDER = registerSimple<WailaInfoProvider<*>>("waila_info_provider")
    
    @JvmField
    val WAILA_TOOL_ICON_PROVIDER = registerSimple<WailaToolIconProvider>("waila_tool_icon_provider")
    
    private fun <E : Any> registerSimple( name: String): WritableRegistry<E> {
        val resourceLocation = ResourceLocation("nova", name)
        return NovaRegistryAccess.addRegistry(resourceLocation)
    }
    
    private fun <E : Any> registerFuzzy(name: String): FuzzyMappedRegistry<E> {
        val resourceLocation = ResourceLocation("nova", name)
        return NovaRegistryAccess.addFuzzyRegistry(resourceLocation)
    }
    
}