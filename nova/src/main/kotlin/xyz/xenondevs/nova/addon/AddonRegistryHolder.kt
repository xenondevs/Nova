package xyz.xenondevs.nova.addon

import xyz.xenondevs.nova.addon.registry.AbilityTypeRegistry
import xyz.xenondevs.nova.addon.registry.AttachmentTypeRegistry
import xyz.xenondevs.nova.addon.registry.BlockRegistry
import xyz.xenondevs.nova.addon.registry.ItemRegistry
import xyz.xenondevs.nova.addon.registry.NetworkTypeRegistry
import xyz.xenondevs.nova.addon.registry.RecipeTypeRegistry
import xyz.xenondevs.nova.addon.registry.ToolCategoryRegistry
import xyz.xenondevs.nova.addon.registry.ToolTierRegistry
import xyz.xenondevs.nova.addon.registry.WailaInfoProviderRegistry

class AddonRegistryHolder internal constructor(
    override val addon: Addon
) : AbilityTypeRegistry, AttachmentTypeRegistry, BlockRegistry, ItemRegistry, NetworkTypeRegistry, RecipeTypeRegistry,
    ToolCategoryRegistry,  ToolTierRegistry, WailaInfoProviderRegistry