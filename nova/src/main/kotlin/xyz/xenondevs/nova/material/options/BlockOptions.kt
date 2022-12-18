package xyz.xenondevs.nova.material.options

import org.bukkit.Material
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolTier
import xyz.xenondevs.nova.world.block.sound.SoundGroup

/**
 * Creates [BlockOptions] for a block that has a preferred tool with multiple tool categories.
 *
 * @param hardness The hardness of the block.
 * @param toolCategories The preferred tool categories. Cannot be empty.
 * @param toolTier The preferred tool tier.
 * @param requiresToolForDrops Whether the block needs to be broken with a tool of one of the given
 * [toolCategories] and with the given [toolTier] or higher a tier of higher level for item drops.
 * @param soundGroup The [SoundGroup] of the block.
 * @param breakParticles The [Material] of the particles that should be spawned when the block is broken.
 * This is only relevant for armor stand based blocks.
 * @param showBreakAnimation Whether a break animation should be shown when breaking the block.
 */
fun BlockOptions(
    hardness: Double,
    toolCategories: List<ToolCategory>,
    toolTier: ToolTier,
    requiresToolForDrops: Boolean,
    soundGroup: SoundGroup? = null,
    breakParticles: Material? = null,
    showBreakAnimation: Boolean = true
): BlockOptions {
    require(toolCategories.isNotEmpty()) { "Tool categories cannot be empty if a tool tier is specified! " }
    return BlockOptions(
        hardness,
        toolCategories,
        toolTier,
        requiresToolForDrops,
        soundGroup,
        breakParticles,
        showBreakAnimation
    )
}

class BlockOptions private constructor(
    val hardness: Double,
    val toolCategories: List<ToolCategory>,
    val toolTier: ToolTier?,
    val requiresToolForDrops: Boolean,
    val soundGroup: SoundGroup?,
    val breakParticles: Material?,
    val showBreakAnimation: Boolean
) {
    
    /**
     * Creates [BlockOptions] for a block that has a preferred tool with only one tool category.
     *
     * @param hardness The hardness of the block.
     * @param toolCategory The preferred tool category.
     * @param toolTier The preferred tool tier.
     * @param requiresToolForDrops Whether the block needs to be broken with a tool of the given
     * [toolCategory] and with the given [toolTier] or higher a tier of higher level for item drops.
     * @param soundGroup The [SoundGroup] of the block.
     * @param breakParticles The [Material] of the particles that should be spawned when the block is broken.
     * This is only relevant for armor stand based blocks.
     * @param showBreakAnimation Whether a break animation should be shown when breaking the block.
     */
    constructor(
        hardness: Double,
        toolCategory: ToolCategory,
        toolTier: ToolTier,
        requiresToolForDrops: Boolean,
        soundGroup: SoundGroup? = null,
        breakParticles: Material? = null,
        showBreakAnimation: Boolean = true
    ) : this(
        hardness,
        listOf(toolCategory),
        toolTier,
        requiresToolForDrops,
        soundGroup,
        breakParticles,
        showBreakAnimation
    )
    
    /**
     * Creates [BlockOptions] for a block that has no preferred tool.
     *
     * @param hardness The hardness of the block.
     * @param soundGroup The [SoundGroup] of the block.
     * @param breakParticles The [Material] of the particles that should be spawned when the block is broken.
     * This is only relevant for armor stand based blocks.
     * @param showBreakAnimation Whether a break animation should be shown when breaking the block.
     */
    constructor(
        hardness: Double,
        soundGroup: SoundGroup? = null,
        breakParticles: Material? = null,
        showBreakAnimation: Boolean = true
    ) : this(
        hardness,
        emptyList(),
        null,
        false,
        soundGroup,
        breakParticles,
        showBreakAnimation
    )
    
}