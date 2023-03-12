package xyz.xenondevs.nova.registry

import net.minecraft.core.WritableRegistry
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.recipe.NovaRecipe
import xyz.xenondevs.nova.data.recipe.RecipeType
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolTier
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.player.ability.AbilityType
import xyz.xenondevs.nova.player.attachment.AttachmentType
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import xyz.xenondevs.nova.ui.waila.info.WailaInfoProvider
import xyz.xenondevs.nova.world.generation.ExperimentalLevelGen
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjection

object NovaRegistries {

    val BLOCK = registerSimple<BlockNovaMaterial>("block")
    
    val ITEM = registerFuzzy<ItemNovaMaterial>("item")
    
    val TOOL_TIER = registerSimple<ToolTier>("tool_tier")
    
    val TOOL_CATEGORY = registerSimple<ToolCategory>("tool_category")
    
    val UPGRADE_TYPE = registerSimple<UpgradeType<*>>("upgrade_type")
    
    val NETWORK_TYPE = registerSimple<NetworkType>("network_type")
    
    val ABILITY_TYPE = registerSimple<AbilityType<*>>("ability_type")
    
    val ATTACHMENT_TYPE = registerSimple<AttachmentType<*>>("attachment_type")
    
    val RECIPE_TYPE = registerSimple<RecipeType<*>>("recipe_type")
    
    val RECIPE = registerSimple<NovaRecipe>("recipe")
    
    @ExperimentalLevelGen
    val BIOME_INJECTION = registerSimple<BiomeInjection>("biome_injection")
    
    val WAILA_INFO_PROVIDER = registerSimple<WailaInfoProvider<*>>("waila_info_provider")
    
    
    private fun <E : Any> registerSimple(name: String): WritableRegistry<E> {
        val resourceLocation = ResourceLocation("nova", name)
        return NovaRegistryAccess.addRegistry(resourceLocation)
    }
    
    private fun <E: Any> registerFuzzy(name: String): FuzzyMappedRegistry<E> {
        val resourceLocation = ResourceLocation("nova", name)
        return NovaRegistryAccess.addFuzzyRegistry(resourceLocation)
    }
    
}