package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.Item
import net.minecraft.world.item.component.ItemAttributeModifiers
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.nova.config.entryOrElse
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.ToolTier
import xyz.xenondevs.nova.world.item.vanilla.VanillaMaterialProperty

private const val PLAYER_ATTACK_SPEED = 4.0
private const val PLAYER_ATTACK_DAMAGE = 1.0

/**
 * Creates a factory for [Tool] behaviors using the given values, if not specified otherwise in the config.
 * 
 * @param tier The [ToolTier] of the tool.
 * Used when `tool_tier` is not specified in the config, or null to require the presence of a config entry.
 * 
 * @param categories The [ToolCategory] of the tool.
 * Used when `tool_category` is not specified in the config, or null to require the presence of a config entry.
 * 
 * @param breakSpeed The break speed of the tool.
 * Used when `break_speed` is not specified in the config, or null to require the presence of a config entry.
 * 
 * @param attackDamage The attack damage of the tool.
 * Used when `attack_damage` is not specified in the config, or null to require the presence of a config entry.
 * 
 * @param attackSpeed The attack speed of the tool.
 * Used when `attack_speed` is not specified in the config, or null to require the presence of a config entry.
 * 
 * @param knockbackBonus The knockback bonus of the tool when attacking.
 * Used when `knockback_bonus` is not specified in the config.
 * 
 * @param canSweepAttack Whether the tool can perform a sweep attack.
 * Used when `can_sweep_attack` is not specified in the config.
 * 
 * @param canBreakBlocksInCreative Whether the tool can break blocks in creative mode.
 * Used when `can_break_blocks_in_creative` is not specified in the config.
 */
@Suppress("FunctionName")
fun Tool(
    tier: ToolTier? = null,
    categories: Set<ToolCategory>? = null,
    breakSpeed: Double? = null,
    attackDamage: Double? = null,
    attackSpeed: Double? = null,
    knockbackBonus: Int = 0,
    canSweepAttack: Boolean = false,
    canBreakBlocksInCreative: Boolean = true
) = ItemBehaviorFactory<Tool> {
    val cfg = it.config
    Tool(
        cfg.entryOrElse(tier, arrayOf("tool_tier"), arrayOf("tool_level")),
        cfg.entryOrElse(categories, "tool_category"),
        cfg.entryOrElse(breakSpeed, "break_speed"),
        cfg.entryOrElse(attackDamage, "attack_damage"),
        cfg.entryOrElse(attackSpeed, "attack_speed"),
        cfg.entryOrElse(knockbackBonus, "knockback_bonus"),
        cfg.entryOrElse(canSweepAttack, "can_sweep_attack"),
        cfg.entryOrElse(canBreakBlocksInCreative, "can_break_blocks_in_creative")
    )
}

/**
 * Allows items to be used as tools, by specifying break and attack properties.
 */
class Tool(
    tier: Provider<ToolTier>,
    categories: Provider<Set<ToolCategory>>,
    breakSpeed: Provider<Double>,
    attackDamage: Provider<Double?>,
    attackSpeed: Provider<Double?>,
    knockbackBonus: Provider<Int>,
    canSweepAttack: Provider<Boolean>,
    canBreakBlocksInCreative: Provider<Boolean>
) : ItemBehavior {
    
    /**
     * The [ToolTier] of this tool.
     */
    val tier by tier
    
    /**
     * The [ToolCategory] of this tool.
     */
    val categories by categories
    
    /**
     * The break speed of this tool.
     */
    val breakSpeed by breakSpeed
    
    /**
     * The attack damage of this tool.
     */
    val attackDamage by attackDamage
    
    /**
     * The attack speed of this tool.
     */
    val attackSpeed by attackSpeed
    
    /**
     * The knockback bonus of this tool when attacking.
     */
    val knockbackBonus by knockbackBonus
    
    /**
     * Whether this tool can perform a sweep attack.
     */
    val canSweepAttack by canSweepAttack
    
    /**
     * Whether this tool can break blocks in creative mode.
     */
    val canBreakBlocksInCreative by canBreakBlocksInCreative
    
    override val vanillaMaterialProperties = canBreakBlocksInCreative.map { canBreakBlocksInCreative ->
        buildList {
            if (!canBreakBlocksInCreative)
                add(VanillaMaterialProperty.CREATIVE_NON_BLOCK_BREAKING)
        }
    }
    
    override val baseDataComponents = combinedProvider(
        attackDamage, attackSpeed, knockbackBonus
    ) { attackDamage, attackSpeed, knockbackBonus ->
        val modifiers = ItemAttributeModifiers.builder()
        if (attackDamage != null) {
            modifiers.add(
                Attributes.ATTACK_DAMAGE,
                AttributeModifier(
                    Item.BASE_ATTACK_DAMAGE_ID,
                    attackDamage - PLAYER_ATTACK_DAMAGE,
                    Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
        }
        
        if (attackSpeed != null) {
            modifiers.add(
                Attributes.ATTACK_SPEED,
                AttributeModifier(
                    Item.BASE_ATTACK_SPEED_ID,
                    attackSpeed - PLAYER_ATTACK_SPEED,
                    Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
        }
        
        if (knockbackBonus != 0) {
            modifiers.add(
                Attributes.ATTACK_KNOCKBACK,
                AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("nova", "knockback_bonus"),
                    knockbackBonus.toDouble(),
                    Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
        }
        
        return@combinedProvider DataComponentMap.builder()
            .set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers.build())
            .build()
    }
    
    override fun toString(itemStack: ItemStack): String {
        return "Tool(" +
            "tier=$tier, " +
            "categories=$categories, " +
            "breakSpeed=$breakSpeed, " +
            "attackDamage=$attackDamage, " +
            "attackSpeed=$attackSpeed, " +
            "knockbackBonus=$knockbackBonus, " +
            "canSweepAttack=$canSweepAttack, " +
            "canBreakBlocksInCreative=$canBreakBlocksInCreative" +
            ")"
    }
    
}