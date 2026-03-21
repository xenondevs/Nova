package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.Material
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.emptyRegistryEntrySet
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.ToolTier

/**
 * Creates a factory for [Breakable] behaviors using the given values, if not specified otherwise in the block's config.
 *
 * @param hardness The hardness of the block. A higher value means the block takes longer to break.
 * Defaults to `1.0`
 * Used when `hardness` is not specified in the config.
 *
 * @param toolCategories The [ToolCategories][ToolCategory] required to break the block.
 * Used when `tool_categories` is not specified in the config.
 *
 * @param toolTier The [ToolTier] required to break the block. Can be null to not require a specific tool tier.
 * Used when `tool_tier` is not specified in the config.
 *
 * @param requiresToolForDrops Whether the block requires a tool to drop its item.
 * Defaults to `true`.
 * Used when `requires_tool_for_drops` is not specified in the config.
 *
 * @param breakParticles The type of break particles to spawn in case the block is entity-backed or model-less with no vanilla particles.
 * Can be null to not spawn break particles in such cases.
 * Used when `break_particles` is not specified in the config.
 *
 * @param showBreakAnimation Whether the break animation should be shown.
 * Used when `show_break_animation` is not specified in the config.
 */
@Suppress("FunctionName")
fun Breakable(
    hardness: Double = 1.0,
    toolCategories: RegistryEntrySet.Nova<ToolCategory> = emptyRegistryEntrySet(NovaRegistries.TOOL_CATEGORY),
    toolTier: RegistryEntry.Nova<ToolTier>? = null,
    requiresToolForDrops: Boolean = true,
    breakParticles: Material? = null,
    showBreakAnimation: Boolean = true
) = BlockBehaviorFactory {
    val cfg = it.config
    Breakable(
        cfg.entry(hardness, "hardness"),
        cfg.entry<RegistryEntrySet.Nova<ToolCategory>>(toolCategories, "tool_categories").flatten(),
        cfg.optionalEntry<RegistryEntry.Nova<ToolTier>>("tool_tier").orElse(toolTier).flatten(),
        cfg.entry(requiresToolForDrops, "requires_tool_for_drops"),
        cfg.optionalEntry<Material>("break_particles").orElse(breakParticles),
        cfg.optionalEntry<Boolean>("show_break_animation").orElse(showBreakAnimation)
    )
}

/**
 * Defines values used for block breaking. Makes blocks breakable.
 */
class Breakable(
    hardness: Provider<Double>,
    toolCategories: Provider<Set<ToolCategory>>,
    toolTier: Provider<ToolTier?>,
    requiresToolForDrops: Provider<Boolean>,
    breakParticles: Provider<Material?>,
    showBreakAnimation: Provider<Boolean>
) : BlockBehavior {
    
    /**
     * The hardness of the block. Higher values mean the block takes longer to break.
     */
    val hardness: Double by hardness
    
    /**
     * The [ToolCategories][ToolCategory] required to break the block.
     * If empty, no specific tool is required.
     */
    val toolCategories: Set<ToolCategory> by toolCategories
    
    /**
     * The [ToolTier] required to break the block.
     * Can be null to not require a specific tool tier.
     */
    val toolTier: ToolTier? by toolTier
    
    /**
     * Whether the block requires a tool to drop its item.
     */
    val requiresToolForDrops: Boolean by requiresToolForDrops
    
    /**
     * The type of break particles to spawn in case the block is entity-backed or model-less with no vanilla particles.
     */
    val breakParticles: Material? by breakParticles
    
    /**
     * Whether the break animation should be shown.
     */
    val showBreakAnimation: Boolean by showBreakAnimation
    
}