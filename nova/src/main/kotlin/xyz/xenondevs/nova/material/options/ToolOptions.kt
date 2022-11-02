package xyz.xenondevs.nova.material.options

import xyz.xenondevs.nova.data.config.ConfigAccess
import xyz.xenondevs.nova.data.provider.Provider
import xyz.xenondevs.nova.data.provider.map
import xyz.xenondevs.nova.data.provider.orElse
import xyz.xenondevs.nova.data.provider.provider
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
    
     val levelProvider: Provider<ToolLevel>
     val categoryProvider: Provider<ToolCategory>
     val breakSpeedProvider: Provider<Double>
     val attackDamageProvider: Provider<Double?>
     val attackSpeedProvider: Provider<Double?>
     val canSweepAttackProvider: Provider<Boolean>
     val canBreakBlocksInCreativeProvider: Provider<Boolean>
    
    val level: ToolLevel
        get() = levelProvider.value
    val category: ToolCategory
        get() = categoryProvider.value
    val breakSpeed: Double
        get() = breakSpeedProvider.value
    val attackDamage: Double?
        get() = attackDamageProvider.value
    val attackSpeed: Double?
        get() = attackSpeedProvider.value
    val canSweepAttack: Boolean
        get() = canSweepAttackProvider.value
    val canBreakBlocksInCreative: Boolean
        get() = canBreakBlocksInCreativeProvider.value
    
    companion object : MaterialOptionsType<ToolOptions> {
        
        override fun configurable(material: ItemNovaMaterial): ToolOptions =
            ConfigurableToolOptions(material)
        
        override fun configurable(path: String): ToolOptions =
            ConfigurableToolOptions(path)
        
    }
    
}

private class HardcodedToolOptions(
     level: ToolLevel,
     category: ToolCategory,
     breakSpeed: Double,
     attackDamage: Double?,
     attackSpeed: Double?,
     canSweepAttack: Boolean,
     canBreakBlocksInCreative: Boolean
) : ToolOptions {
    
    override val levelProvider = provider(level)
    override val categoryProvider = provider(category)
    override val breakSpeedProvider = provider(breakSpeed)
    override val attackDamageProvider = provider(attackDamage)
    override val attackSpeedProvider = provider(attackSpeed)
    override val canSweepAttackProvider = provider(canSweepAttack)
    override val canBreakBlocksInCreativeProvider = provider(canBreakBlocksInCreative)
    
}

private class ConfigurableToolOptions : ConfigAccess, ToolOptions {
    
    override val levelProvider = getEntry<String>("tool_level").map { ToolLevelRegistry.of(it)!! }
    override val categoryProvider = getEntry<String>("tool_category").map { ToolCategoryRegistry.of(it)!! }
    override val breakSpeedProvider = getEntry<Double>("break_speed")
    override val attackDamageProvider = getOptionalEntry<Double>("attack_damage")
    override val attackSpeedProvider = getOptionalEntry<Double>("attack_speed")
    override val canSweepAttackProvider = getOptionalEntry<Boolean>("can_sweep_attack").orElse(false)
    override val canBreakBlocksInCreativeProvider = getOptionalEntry<Boolean>("can_break_blocks_in_creative").orElse(true)
    
    constructor(path: String) : super(path)
    constructor(material: ItemNovaMaterial) : super(material)
    
}