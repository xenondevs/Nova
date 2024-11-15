package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.Material
import xyz.xenondevs.commons.collections.isNotNullOrEmpty
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.nova.config.entryOrElse
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.ToolTier

/**
 * Creates a factory for [Breakable] behaviors using the given values, if not specified otherwise in the block's config.
 *
 * @param hardness The hardness of the block.
 * Used when `hardness` is not specified in the config, or `null` to require the presence of a config entry.
 * 
 * @param toolCategories The [ToolCategories][ToolCategory] required to break the block.
 * Used when `tool_categories` is not specified in the config.
 * 
 * @param toolTier The [ToolTier] required to break the block. Can be null to not require a specific tool tier.
 * Used when `tool_tier` is not specified in the config.
 * 
 * @param requiresToolForDrops Whether the block requires a tool to drop its item.
 * Used when `requires_tool_for_drops` is not specified in the config, or `null` to require the presence of a config entry.
 * 
 * @param breakParticles The type break particles to spawn in case the block is backed by barriers. Can be null.
 * Used when `break_particles` is not specified in the config.
 * 
 * @param showBreakAnimation Whether the break animation should be shown.
 * Used when `show_break_animation` is not specified in the config.
 */
@Suppress("FunctionName")
fun Breakable(
    hardness: Double? = null,
    toolCategories: Set<ToolCategory> = emptySet(),
    toolTier: ToolTier? = null,
    requiresToolForDrops: Boolean? = null,
    breakParticles: Material? = null,
    showBreakAnimation: Boolean = true
) = BlockBehaviorFactory<Breakable> {
    require(toolTier == null || toolCategories.isNotNullOrEmpty()) { "Tool categories cannot be empty if a tool tier is specified!" }
    
    val cfg = it.config
    Breakable.Default(
        cfg.entryOrElse(hardness, "hardness"),
        cfg.entryOrElse(toolCategories, "tool_categories"),
        cfg.optionalEntry<ToolTier>("tool_tier").orElse(toolTier),
        cfg.entryOrElse(requiresToolForDrops, "requires_tool_for_drops"),
        cfg.optionalEntry<Material>("break_particles").orElse(breakParticles),
        cfg.optionalEntry<Boolean>("show_break_animation").orElse(showBreakAnimation)
    )
}

/**
 * Defines values used for block breaking. Makes blocks breakable.
 */
interface Breakable : BlockBehavior {
    
    val hardness: Double
    val toolCategories: Set<ToolCategory>
    val toolTier: ToolTier?
    val requiresToolForDrops: Boolean
    val breakParticles: Material?
    val showBreakAnimation: Boolean
    
    class Default(
        hardness: Provider<Double>,
        toolCategories: Provider<Set<ToolCategory>>,
        toolTier: Provider<ToolTier?>,
        requiresToolForDrops: Provider<Boolean>,
        breakParticles: Provider<Material?>,
        showBreakAnimation: Provider<Boolean>
    ) : Breakable {
        
        override val hardness by hardness
        override val toolCategories by toolCategories
        override val toolTier by toolTier
        override val requiresToolForDrops by requiresToolForDrops
        override val breakParticles by breakParticles
        override val showBreakAnimation by showBreakAnimation
        
    }
    
}