package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers.itemAttributes
import io.papermc.paper.datacomponent.item.Tool.tool
import io.papermc.paper.datacomponent.item.Weapon.weapon
import net.minecraft.world.item.Item
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.attribute.AttributeModifier.Operation
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.registryEntrySetOf
import xyz.xenondevs.nova.util.toNamespacedKey
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider
import xyz.xenondevs.nova.world.item.tool.ToolCategory
import xyz.xenondevs.nova.world.item.tool.ToolTier
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers

private const val PLAYER_ATTACK_SPEED = 4.0
private const val PLAYER_ATTACK_DAMAGE = 1.0

/**
 * Creates a factory for [Tool] behaviors using the given values, if not specified otherwise in the config.
 *
 * @param tier The [ToolTier] of the tool.
 * Defaults to `VanillaToolTiers.WOOD`.
 * Used when `tool_tier` is not specified in the config.
 *
 * @param categories The [ToolCategory] of the tool.
 * Defaults to `VanillaToolCategories.PICKAXE`.
 * Used when `tool_category` is not specified in the config.
 *
 * @param breakSpeed The break speed of the tool.
 * Defaults to `1.0`.
 * Used when `break_speed` is not specified in the config.
 *
 * @param attackDamage The attack damage of the tool.
 * Defaults to `1.0`.
 * Used when `attack_damage` is not specified in the config.
 *
 * @param attackSpeed The attack speed of the tool.
 * Defaults to `1.0`.
 * Used when `attack_speed` is not specified in the config.
 *
 * @param knockbackBonus The knockback bonus of the tool when attacking.
 * Defaults to `0`.
 * Used when `knockback_bonus` is not specified in the config.
 *
 * @param canSweepAttack Whether the tool can perform a sweep attack.
 * Defaults to `false`.
 * Used when `can_sweep_attack` is not specified in the config.
 *
 * @param canBreakBlocksInCreative Whether the tool can break blocks in creative mode.
 * Defaults to `true`.
 * Used when `can_break_blocks_in_creative` is not specified in the config.
 *
 * @param disableBlocking The number of ticks to disable an attacked shield's blocking status for when attacking with this tool.
 * Defaults to `0`.
 * Used when `disable_blocking` is not specified in the config.
 */
@Suppress("FunctionName")
fun Tool(
    tier: RegistryEntry.Nova<ToolTier> = VanillaToolTiers.WOOD,
    categories: RegistryEntrySet.Nova<ToolCategory> = registryEntrySetOf(VanillaToolCategories.PICKAXE),
    breakSpeed: Double = 1.0,
    attackDamage: Double = 1.0,
    attackSpeed: Double = 1.0,
    knockbackBonus: Int = 0,
    canSweepAttack: Boolean = false,
    canBreakBlocksInCreative: Boolean = true,
    disableBlocking: Int = 0
) = ItemBehaviorFactory { _, cfg ->
    Tool(
        cfg.entry<RegistryEntry.Nova<ToolTier>>(tier, listOf("tool_tier"), listOf("tool_level")).flatten(),
        cfg.entry<RegistryEntrySet.Nova<ToolCategory>>(categories, "tool_category").flatten(),
        cfg.entry(breakSpeed, "break_speed"),
        cfg.entry(attackDamage, "attack_damage"),
        cfg.entry(attackSpeed, "attack_speed"),
        cfg.entry(knockbackBonus, "knockback_bonus"),
        cfg.entry(canSweepAttack, "can_sweep_attack"),
        cfg.entry(canBreakBlocksInCreative, "can_break_blocks_in_creative"),
        cfg.entry(disableBlocking, "disable_blocking")
    )
}

/**
 * Allows items to be used as tools, by specifying break and attack properties.
 *
 * @param tier The [ToolTier] of the tool.
 * @param categories The [ToolCategory ToolCategories] of the tool.
 * @param breakSpeed The break speed of the tool.
 * @param attackDamage The attack damage of the tool.
 * @param attackSpeed The attack speed of the tool.
 * @param knockbackBonus The knockback bonus of the tool when attacking.
 * @param canSweepAttack Whether the tool can perform a sweep attack.
 * @param canBreakBlocksInCreative Whether the tool can break blocks in creative mode.
 * @param disableBlocking The amount of ticks to disable an attacked shield's blocking status for when attacking with this tool.
 */
class Tool(
    tier: Provider<ToolTier>,
    categories: Provider<Set<ToolCategory>>,
    breakSpeed: Provider<Double>,
    attackDamage: Provider<Double?>,
    attackSpeed: Provider<Double?>,
    knockbackBonus: Provider<Int>,
    canSweepAttack: Provider<Boolean>,
    canBreakBlocksInCreative: Provider<Boolean>,
    disableBlocking: Provider<Int>
) : ItemBehavior {
    
    /**
     * The [ToolTier] of this tool.
     */
    val tier: ToolTier by tier
    
    /**
     * The [ToolCategory ToolCategories] of this tool.
     */
    val categories: Set<ToolCategory> by categories
    
    /**
     * The break speed of this tool.
     */
    val breakSpeed: Double by breakSpeed
    
    /**
     * The attack damage of this tool.
     */
    val attackDamage: Double? by attackDamage
    
    /**
     * The attack speed of this tool.
     */
    val attackSpeed: Double? by attackSpeed
    
    /**
     * The knockback bonus of this tool when attacking.
     */
    val knockbackBonus: Int by knockbackBonus
    
    /**
     * Whether this tool can perform a sweep attack.
     */
    val canSweepAttack: Boolean by canSweepAttack
    
    /**
     * Whether this tool can break blocks in creative mode.
     */
    val canBreakBlocksInCreative: Boolean by canBreakBlocksInCreative
    
    /**
     * The amount of ticks to disable blocking for when using this tool.
     */
    val disableBlocking: Int by disableBlocking
    
    override val baseDataComponents: Provider<DataComponentMap> = buildDataComponentMapProvider {
        // The actual breaking logic is server-side and disregards this component. Only canBreakBlocksInCreative is relevant.
        this[DataComponentTypes.TOOL] = canBreakBlocksInCreative.map { tool().canDestroyBlocksInCreative(it).build() }
        this[DataComponentTypes.WEAPON] = disableBlocking.map {
            weapon()
                .itemDamagePerAttack(0) // // use the lowest value for merging with weapon component of Damageable
                .disableBlockingForSeconds(it / 20f)
                .build()
        }
        this[DataComponentTypes.ATTRIBUTE_MODIFIERS] = combinedProvider(
            attackDamage, attackSpeed, knockbackBonus
        ) { attackDamage, attackSpeed, knockbackBonus ->
            val modifiers = itemAttributes()
            
            if (attackDamage != null) {
                modifiers.addModifier(
                    Attribute.ATTACK_DAMAGE,
                    AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_ID.toNamespacedKey(),
                        attackDamage - PLAYER_ATTACK_DAMAGE,
                        Operation.ADD_NUMBER
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
            }
            
            if (attackSpeed != null) {
                modifiers.addModifier(
                    Attribute.ATTACK_SPEED,
                    AttributeModifier(
                        Item.BASE_ATTACK_SPEED_ID.toNamespacedKey(),
                        attackSpeed - PLAYER_ATTACK_SPEED,
                        Operation.ADD_NUMBER
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
            }
            
            if (knockbackBonus != 0) {
                modifiers.addModifier(
                    Attribute.ATTACK_KNOCKBACK,
                    AttributeModifier(
                        NamespacedKey("nova", "knockback_bonus"),
                        knockbackBonus.toDouble(),
                        Operation.ADD_NUMBER
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
            }
            
            return@combinedProvider modifiers.build()
        }
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
            "canBreakBlocksInCreative=$canBreakBlocksInCreative, " +
            "disableBlocking=$disableBlocking" +
            ")"
    }
    
}