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
import xyz.xenondevs.commons.provider.immutable.combinedProvider
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.commons.provider.immutable.orElse
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.ToolTier
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories
import xyz.xenondevs.nova.world.item.vanilla.VanillaMaterialProperty

private const val PLAYER_ATTACK_SPEED = 4.0
private const val PLAYER_ATTACK_DAMAGE = 1.0

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
    
    constructor(
        tier: ToolTier,
        categories: Set<ToolCategory>,
        breakSpeed: Double,
        attackDamage: Double?,
        attackSpeed: Double?,
        knockbackBonus: Int,
        canSweepAttack: Boolean = false,
        canBreakBlocksInCreative: Boolean = categories != setOf(VanillaToolCategories.SWORD)
    ) : this(
        provider(tier),
        provider(categories),
        provider(breakSpeed),
        provider(attackDamage),
        provider(attackSpeed),
        provider(knockbackBonus),
        provider(canSweepAttack),
        provider(canBreakBlocksInCreative)
    )
    
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
    
    companion object : ItemBehaviorFactory<Tool> {
        
        override fun create(item: NovaItem): Tool {
            val cfg = item.config
            return Tool(
                cfg.entry<ToolTier>(arrayOf("tool_tier"), arrayOf("tool_level")),
                cfg.entry<Set<ToolCategory>>("tool_category"),
                cfg.entry<Double>("break_speed"),
                cfg.optionalEntry<Double>("attack_damage"),
                cfg.optionalEntry<Double>("attack_speed"),
                cfg.optionalEntry<Int>("knockback_bonus").orElse(0),
                cfg.optionalEntry<Boolean>("can_sweep_attack").orElse(false),
                cfg.optionalEntry<Boolean>("can_break_blocks_in_creative").orElse(true)
            )
        }
        
    }
    
}