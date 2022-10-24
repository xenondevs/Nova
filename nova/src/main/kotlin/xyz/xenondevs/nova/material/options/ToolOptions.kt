package xyz.xenondevs.nova.material.options

import xyz.xenondevs.nova.data.config.ConfigAccess
import xyz.xenondevs.nova.data.provider.map
import xyz.xenondevs.nova.data.provider.orElse
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolCategoryRegistry
import xyz.xenondevs.nova.item.tool.ToolLevel
import xyz.xenondevs.nova.item.tool.ToolLevelRegistry
import xyz.xenondevs.nova.material.ItemNovaMaterial

@HardcodedMaterialOptions
fun ToolOptions(
    level: ToolLevel,
    category: ToolCategory,
    breakSpeed: Double,
    attackDamage: Double? = null,
    attackSpeed: Double? = null,
    canSweepAttack: Boolean = false,
    canBreakBlocksInCreative: Boolean = category != ToolCategory.SWORD
): ToolOptions = HardcodedToolOptions(level, category, breakSpeed, attackDamage, attackSpeed, canSweepAttack, canBreakBlocksInCreative)

sealed interface ToolOptions {
    
    val level: ToolLevel
    val category: ToolCategory
    val breakSpeed: Double
    val attackDamage: Double?
    val attackSpeed: Double?
    val canSweepAttack: Boolean
    val canBreakBlocksInCreative: Boolean
    
    companion object : MaterialOptionsType<ToolOptions> {
        
        override fun configurable(material: ItemNovaMaterial): ToolOptions =
            ConfigurableToolOptions(material)
        
        override fun configurable(path: String): ToolOptions =
            ConfigurableToolOptions(path)
        
    }
    
}

private class HardcodedToolOptions(
    override val level: ToolLevel,
    override val category: ToolCategory,
    override val breakSpeed: Double,
    override val attackDamage: Double?,
    override val attackSpeed: Double?,
    override val canSweepAttack: Boolean,
    override val canBreakBlocksInCreative: Boolean
) : ToolOptions

private class ConfigurableToolOptions : ConfigAccess, ToolOptions {
    
    override val level by getEntry<String>("tool_level").map { ToolLevelRegistry.of(it)!! }
    override val category by getEntry<String>("tool_category").map { ToolCategoryRegistry.of(it)!! }
    override val breakSpeed by getEntry<Double>("break_speed")
    override val attackDamage by getOptionalEntry<Double>("attack_damage")
    override val attackSpeed by getOptionalEntry<Double>("attack_speed")
    override val canSweepAttack by getOptionalEntry<Boolean>("can_sweep_attack").orElse(false)
    override val canBreakBlocksInCreative by getOptionalEntry<Boolean>("can_break_blocks_in_creative").orElse(true)
    
    constructor(path: String) : super(path)
    constructor(material: ItemNovaMaterial) : super(material)
    
}