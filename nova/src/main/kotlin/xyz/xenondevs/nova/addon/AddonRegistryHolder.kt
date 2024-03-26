package xyz.xenondevs.nova.addon

import xyz.xenondevs.nova.addon.registry.AbilityTypeRegistry
import xyz.xenondevs.nova.addon.registry.AttachmentTypeRegistry
import xyz.xenondevs.nova.addon.registry.BlockRegistry
import xyz.xenondevs.nova.addon.registry.EnchantmentCategoryRegistry
import xyz.xenondevs.nova.addon.registry.EnchantmentRegistry
import xyz.xenondevs.nova.addon.registry.ItemRegistry
import xyz.xenondevs.nova.addon.registry.MinecraftUtilTypeRegistry
import xyz.xenondevs.nova.addon.registry.NetworkTypeRegistry
import xyz.xenondevs.nova.addon.registry.RecipeTypeRegistry
import xyz.xenondevs.nova.addon.registry.ToolCategoryRegistry
import xyz.xenondevs.nova.addon.registry.ToolTierRegistry
import xyz.xenondevs.nova.addon.registry.WailaInfoProviderRegistry
import xyz.xenondevs.nova.addon.registry.worldgen.BiomeRegistry
import xyz.xenondevs.nova.addon.registry.worldgen.CarverRegistry
import xyz.xenondevs.nova.addon.registry.worldgen.DimensionRegistry
import xyz.xenondevs.nova.addon.registry.worldgen.FeatureRegistry
import xyz.xenondevs.nova.addon.registry.worldgen.NoiseRegistry
import xyz.xenondevs.nova.addon.registry.worldgen.StructureRegistry

class AddonRegistryHolder internal constructor(
    override val addon: Addon
) : AbilityTypeRegistry, AttachmentTypeRegistry, BlockRegistry, EnchantmentCategoryRegistry, EnchantmentRegistry,
    ItemRegistry, NetworkTypeRegistry, RecipeTypeRegistry, ToolCategoryRegistry, ToolTierRegistry,
    WailaInfoProviderRegistry, MinecraftUtilTypeRegistry, BiomeRegistry, CarverRegistry, DimensionRegistry,
    FeatureRegistry, NoiseRegistry, StructureRegistry
