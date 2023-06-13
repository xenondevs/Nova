package xyz.xenondevs.nova.registry

import net.minecraft.core.WritableRegistry
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.recipe.RecipeType
import xyz.xenondevs.nova.data.resources.ModelData
import xyz.xenondevs.nova.data.resources.builder.content.armor.info.ArmorTexture
import xyz.xenondevs.nova.data.resources.builder.content.font.FontChar
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.enchantment.Enchantment
import xyz.xenondevs.nova.item.enchantment.EnchantmentCategory
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolTier
import xyz.xenondevs.nova.player.ability.AbilityType
import xyz.xenondevs.nova.player.attachment.AttachmentType
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaToolIconProvider
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjection

object NovaRegistries {
    
    @JvmField
    val BLOCK = registerSimple<NovaBlock>(NovaRegistryAccess.DEFAULT, "block")
    
    @JvmField
    val ITEM = registerFuzzy<NovaItem>(NovaRegistryAccess.DEFAULT, "item")
    
    @JvmField
    val TOOL_TIER = registerSimple<ToolTier>(NovaRegistryAccess.DEFAULT, "tool_tier")
    
    @JvmField
    val TOOL_CATEGORY = registerSimple<ToolCategory>(NovaRegistryAccess.DEFAULT, "tool_category")
    
    @JvmField
    val ENCHANTMENT_CATEGORY = registerSimple<EnchantmentCategory>(NovaRegistryAccess.DEFAULT, "enchantment_category")
    
    @JvmField
    val ENCHANTMENT = registerSimple<Enchantment>(NovaRegistryAccess.DEFAULT, "enchantment")
    
    @JvmField
    val UPGRADE_TYPE = registerSimple<UpgradeType<*>>(NovaRegistryAccess.DEFAULT, "upgrade_type")
    
    @JvmField
    val NETWORK_TYPE = registerSimple<NetworkType>(NovaRegistryAccess.DEFAULT, "network_type")
    
    @JvmField
    val ABILITY_TYPE = registerSimple<AbilityType<*>>(NovaRegistryAccess.DEFAULT, "ability_type")
    
    @JvmField
    val ATTACHMENT_TYPE = registerSimple<AttachmentType<*>>(NovaRegistryAccess.DEFAULT, "attachment_type")
    
    @JvmField
    val RECIPE_TYPE = registerSimple<RecipeType<*>>(NovaRegistryAccess.DEFAULT, "recipe_type")
    
    @JvmField
    @ExperimentalWorldGen
    val BIOME_INJECTION = registerSimple<BiomeInjection>(NovaRegistryAccess.DEFAULT, "biome_injection")
    
    @JvmField
    val WAILA_INFO_PROVIDER = registerSimple<WailaInfoProvider<*>>(NovaRegistryAccess.DEFAULT, "waila_info_provider")
    
    @JvmField
    val WAILA_TOOL_ICON_PROVIDER = registerSimple<WailaToolIconProvider>(NovaRegistryAccess.DEFAULT, "waila_tool_icon_provider")
    
    @JvmField
    val MODEL_DATA_LOOKUP = registerSimple<ModelData>(NovaRegistryAccess.LOOKUPS, "model_data_lookup")
    
    @JvmField
    val ARMOR_DATA_LOOKUP = registerSimple<ArmorTexture>(NovaRegistryAccess.LOOKUPS, "armor_data_lookup")
    
    @JvmField
    val GUI_DATA_LOOKUP = registerSimple<FontChar>(NovaRegistryAccess.LOOKUPS, "gui_data_lookup")
    
    @JvmField
    val WAILA_DATA_LOOKUP = registerSimple<FontChar>(NovaRegistryAccess.LOOKUPS, "waila_data_lookup")
    
    @JvmField
    val TEXTURE_ICON_LOOKUP = registerSimple<FontChar>(NovaRegistryAccess.LOOKUPS, "texture_icon_lookup")
    
    @JvmField
    val LANGUAGE_LOOKUP = registerSimple<Map<String, String>>(NovaRegistryAccess.LOOKUPS, "language_lookup") // TODO: nested registries?
    
    private fun <E : Any> registerSimple(registryAccess: NovaRegistryAccess, name: String): WritableRegistry<E> {
        val resourceLocation = ResourceLocation("nova", name)
        return registryAccess.addRegistry(resourceLocation)
    }
    
    private fun <E : Any> registerFuzzy(registryAccess: NovaRegistryAccess, name: String): FuzzyMappedRegistry<E> {
        val resourceLocation = ResourceLocation("nova", name)
        return registryAccess.addFuzzyRegistry(resourceLocation)
    }
    
}