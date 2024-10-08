package xyz.xenondevs.nova.world.block.behavior

import org.bukkit.Material
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.orElse
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.ToolTier
import xyz.xenondevs.nova.world.block.NovaBlock

fun Breakable(
    hardness: Double,
    toolCategories: Set<ToolCategory>,
    toolTier: ToolTier?,
    requiresToolForDrops: Boolean,
    breakParticles: Material? = null,
    showBreakAnimation: Boolean = true
): Breakable.Default {
    require(toolCategories.isNotEmpty()) { "Tool categories cannot be empty if a tool tier is specified!" }
    return Breakable.Default(
        hardness,
        toolCategories,
        toolTier,
        requiresToolForDrops,
        breakParticles,
        showBreakAnimation
    )
}

fun Breakable(
    hardness: Double,
    toolCategory: ToolCategory,
    toolTier: ToolTier,
    requiresToolForDrops: Boolean,
    breakParticles: Material? = null,
    showBreakAnimation: Boolean = true
) = Breakable.Default(
    hardness,
    setOf(toolCategory),
    toolTier,
    requiresToolForDrops,
    breakParticles,
    showBreakAnimation
)

fun Breakable(
    hardness: Double,
    breakParticles: Material? = null,
    showBreakAnimation: Boolean = true
) = Breakable.Default(
    hardness,
    emptySet(),
    null,
    false,
    breakParticles,
    showBreakAnimation
)

interface Breakable {
    
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
    ) : BlockBehavior, Breakable {
        
        override val hardness by hardness
        override val toolCategories by toolCategories
        override val toolTier by toolTier
        override val requiresToolForDrops by requiresToolForDrops
        override val breakParticles by breakParticles
        override val showBreakAnimation by showBreakAnimation
        
        constructor(
            hardness: Double,
            toolCategories: Set<ToolCategory>,
            toolTier: ToolTier?,
            requiresToolForDrops: Boolean,
            breakParticles: Material?,
            showBreakAnimation: Boolean
        ) : this(
            provider(hardness),
            provider(toolCategories),
            provider(toolTier),
            provider(requiresToolForDrops),
            provider(breakParticles),
            provider(showBreakAnimation)
        )
        
    }
    
    companion object : BlockBehaviorFactory<Default> {
        
        override fun create(block: NovaBlock): Default {
            val cfg = block.config
            return Default(
                cfg.entry<Double>("hardness"),
                cfg.entry<Set<ToolCategory>>("toolCategories"),
                cfg.optionalEntry<ToolTier>("toolTier"),
                cfg.entry<Boolean>("requiresToolForDrops"),
                cfg.optionalEntry<Material>("breakParticles"),
                cfg.optionalEntry<Boolean>("showBreakAnimation").orElse(true)
            )
        }
        
    }
    
}