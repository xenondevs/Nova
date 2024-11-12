package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.registry.worldgen.BiomeRegistry
import xyz.xenondevs.nova.addon.registry.worldgen.CarverRegistry
import xyz.xenondevs.nova.addon.registry.worldgen.DimensionRegistry
import xyz.xenondevs.nova.addon.registry.worldgen.FeatureRegistry
import xyz.xenondevs.nova.addon.registry.worldgen.NoiseRegistry
import xyz.xenondevs.nova.addon.registry.worldgen.StructureRegistry

class AddonRegistryHolder internal constructor(
    override val addon: Addon
) : AbilityTypeRegistry, AttachmentTypeRegistry, BlockRegistry, EnchantmentRegistry,
    ItemRegistry, NetworkTypeRegistry, RecipeTypeRegistry, ToolCategoryRegistry, ToolTierRegistry,
    WailaInfoProviderRegistry, WailaToolIconProviderRegistry, MinecraftUtilTypeRegistry, BiomeRegistry,
    CarverRegistry, DimensionRegistry, FeatureRegistry, NoiseRegistry, StructureRegistry, EquipmentRegistry,
    GuiTextureRegistry, ItemFilterTypeRegistry