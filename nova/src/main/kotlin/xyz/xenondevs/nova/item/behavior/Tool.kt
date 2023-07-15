package xyz.xenondevs.nova.item.behavior

import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
import net.minecraft.world.entity.ai.attributes.Attributes
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.commons.provider.immutable.orElse
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.data.config.ConfigAccess
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolTier
import xyz.xenondevs.nova.item.tool.VanillaToolCategories
import xyz.xenondevs.nova.item.vanilla.AttributeModifier
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.get
import java.util.*

private const val PLAYER_ATTACK_SPEED = 4.0
private const val PLAYER_ATTACK_DAMAGE = 1.0

fun Tool(
    tier: ToolTier,
    category: ToolCategory,
    breakSpeed: Double,
    attackDamage: Double?,
    attackSpeed: Double?,
    knockbackBonus: Int,
    canSweepAttack: Boolean = false,
    canBreakBlocksInCreative: Boolean = category != VanillaToolCategories.SWORD
) = Tool.Default(
    provider(tier),
    provider(category),
    provider(breakSpeed),
    provider(attackDamage), 
    provider(attackSpeed),
    provider(knockbackBonus),
    provider(canSweepAttack),
    provider(canBreakBlocksInCreative)
)

/**
 * Allows items to be used as tools, by specifying break and attack properties.
 */
sealed interface Tool {
    
    /**
     * The [ToolTier] of this tool.
     */
    val tier: ToolTier
    
    /**
     * The [ToolCategory] of this tool.
     */
    val category: ToolCategory
    
    /**
     * The break speed of this tool.
     */
    val breakSpeed: Double
    
    /**
     * The attack damage of this tool.
     */
    val attackDamage: Double?
    
    /**
     * The attack speed of this tool.
     */
    val attackSpeed: Double?
    
    /**
     * The knockback bonus of this tool when attacking.
     */
    val knockbackBonus: Int
    
    /**
     * Whether this tool can perform a sweep attack.
     */
    val canSweepAttack: Boolean
    
    /**
     * Whether this tool can break blocks in creative mode.
     */
    val canBreakBlocksInCreative: Boolean
    
    class Default(
        tier: Provider<ToolTier>,
        category: Provider<ToolCategory>,
        breakSpeed: Provider<Double>,
        attackDamage: Provider<Double?>,
        attackSpeed: Provider<Double?>,
        knockbackBonus: Provider<Int>,
        canSweepAttack: Provider<Boolean>,
        canBreakBlocksInCreative: Provider<Boolean>
    ) : ItemBehavior, Tool {
        
        override val tier by tier
        override val category by category
        override val breakSpeed by breakSpeed
        override val attackDamage by attackDamage
        override val attackSpeed by attackSpeed
        override val knockbackBonus by knockbackBonus
        override val canSweepAttack by canSweepAttack
        override val canBreakBlocksInCreative by canBreakBlocksInCreative
        
        override fun getVanillaMaterialProperties(): List<VanillaMaterialProperty> {
            val properties = ArrayList<VanillaMaterialProperty>()
            properties += VanillaMaterialProperty.DAMAGEABLE
            if (!canBreakBlocksInCreative)
                properties += VanillaMaterialProperty.CREATIVE_NON_BLOCK_BREAKING
            return properties
        }
        
        override fun getAttributeModifiers(): List<AttributeModifier> {
            val modifiers = ArrayList<AttributeModifier>()
            
            val attackDamage = attackDamage
            if (attackDamage != null) {
                modifiers += AttributeModifier(
                    BASE_ATTACK_DAMAGE_UUID,
                    "Nova Attack Damage",
                    Attributes.ATTACK_DAMAGE,
                    Operation.ADDITION,
                    attackDamage - PLAYER_ATTACK_DAMAGE,
                    true,
                    EquipmentSlot.MAINHAND
                )
            }
            
            val attackSpeed = attackSpeed
            if (attackSpeed != null) {
                modifiers += AttributeModifier(
                    BASE_ATTACK_SPEED_UUID,
                    "Nova Attack Speed",
                    Attributes.ATTACK_SPEED,
                    Operation.ADDITION,
                    attackSpeed - PLAYER_ATTACK_SPEED,
                    true,
                    EquipmentSlot.MAINHAND
                )
            }
            
            return modifiers
        }
        
    }
    
    companion object : ItemBehaviorFactory<Default> {
        
        val BASE_ATTACK_DAMAGE_UUID: UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF")
        val BASE_ATTACK_SPEED_UUID: UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3")
        
        override fun create(item: NovaItem): Default {
            val cfg = ConfigAccess(item)
            return Default(
                cfg.getEntry<String>("tool_tier", "tool_level").map { NovaRegistries.TOOL_TIER[it]!! },
                cfg.getEntry<String>("tool_category").map { NovaRegistries.TOOL_CATEGORY[it]!! },
                cfg.getEntry<Double>("break_speed"),
                cfg.getOptionalEntry<Double>("attack_damage"),
                cfg.getOptionalEntry<Double>("attack_speed"),
                cfg.getOptionalEntry<Int>("knockback_bonus").orElse(0),
                cfg.getOptionalEntry<Boolean>("can_sweep_attack").orElse(false),
                cfg.getOptionalEntry<Boolean>("can_break_blocks_in_creative").orElse(true)
            )
        }
        
    }
    
}