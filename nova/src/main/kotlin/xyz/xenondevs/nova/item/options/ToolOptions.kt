package xyz.xenondevs.nova.item.options

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.commons.provider.immutable.orElse
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.data.config.ConfigAccess
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolTier
import xyz.xenondevs.nova.item.tool.VanillaToolCategories
import xyz.xenondevs.nova.registry.NovaRegistries.TOOL_CATEGORY
import xyz.xenondevs.nova.registry.NovaRegistries.TOOL_TIER
import xyz.xenondevs.nova.util.get

@HardcodedMaterialOptions
fun ToolOptions(
    tier: ToolTier,
    category: ToolCategory,
    breakSpeed: Double,
    attackDamage: Double?,
    attackSpeed: Double?,
    knockbackBonus: Int,
    canSweepAttack: Boolean = false,
    canBreakBlocksInCreative: Boolean = category != VanillaToolCategories.SWORD
): ToolOptions = HardcodedToolOptions(tier, category, breakSpeed, attackDamage, attackSpeed, knockbackBonus, canSweepAttack, canBreakBlocksInCreative)

sealed interface ToolOptions {
    
    val tierProvider: Provider<ToolTier>
    val categoryProvider: Provider<ToolCategory>
    val breakSpeedProvider: Provider<Double>
    val attackDamageProvider: Provider<Double?>
    val attackSpeedProvider: Provider<Double?>
    val knockbackBonusProvider: Provider<Int>
    val canSweepAttackProvider: Provider<Boolean>
    val canBreakBlocksInCreativeProvider: Provider<Boolean>
    
    val tier: ToolTier
        get() = tierProvider.value
    val category: ToolCategory
        get() = categoryProvider.value
    val breakSpeed: Double
        get() = breakSpeedProvider.value
    val attackDamage: Double?
        get() = attackDamageProvider.value
    val attackSpeed: Double?
        get() = attackSpeedProvider.value
    val knockbackBonus: Int
        get() = knockbackBonusProvider.value
    val canSweepAttack: Boolean
        get() = canSweepAttackProvider.value
    val canBreakBlocksInCreative: Boolean
        get() = canBreakBlocksInCreativeProvider.value
    
    companion object {
        
        fun configurable(item: NovaItem): ToolOptions =
            ConfigurableToolOptions(item)
        
        fun configurable(path: String): ToolOptions =
            ConfigurableToolOptions(path)
        
    }
    
}

private class HardcodedToolOptions(
    tier: ToolTier,
    category: ToolCategory,
    breakSpeed: Double,
    attackDamage: Double?,
    attackSpeed: Double?,
    knockbackBonus: Int,
    canSweepAttack: Boolean,
    canBreakBlocksInCreative: Boolean
) : ToolOptions {
    
    override val tierProvider = provider(tier)
    override val categoryProvider = provider(category)
    override val breakSpeedProvider = provider(breakSpeed)
    override val attackDamageProvider = provider(attackDamage)
    override val attackSpeedProvider = provider(attackSpeed)
    override val knockbackBonusProvider = provider(knockbackBonus)
    override val canSweepAttackProvider = provider(canSweepAttack)
    override val canBreakBlocksInCreativeProvider = provider(canBreakBlocksInCreative)
    
}

private class ConfigurableToolOptions : ConfigAccess, ToolOptions {
    
    override val tierProvider = getEntry<String>("tool_tier", "tool_level").map { TOOL_TIER[it]!! }
    override val categoryProvider = getEntry<String>("tool_category").map { TOOL_CATEGORY[it]!! }
    override val breakSpeedProvider = getEntry<Double>("break_speed")
    override val attackDamageProvider = getOptionalEntry<Double>("attack_damage")
    override val attackSpeedProvider = getOptionalEntry<Double>("attack_speed")
    override val knockbackBonusProvider = getOptionalEntry<Int>("knockback_bonus").orElse(0)
    override val canSweepAttackProvider = getOptionalEntry<Boolean>("can_sweep_attack").orElse(false)
    override val canBreakBlocksInCreativeProvider = getOptionalEntry<Boolean>("can_break_blocks_in_creative").orElse(true)
    
    constructor(path: String) : super(path)
    constructor(item: NovaItem) : super(item)
    
}