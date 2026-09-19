@file:Suppress("FunctionName")

package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers.itemAttributes
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
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.registry.entries.ItemTypeTags
import xyz.xenondevs.nova.util.toNamespacedKey
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider

private const val PLAYER_ATTACK_SPEED = 4.0
private const val PLAYER_ATTACK_DAMAGE = 1.0

/**
 * Creates a factory for [Weapon] behaviors using the given values, if not specified otherwise in the item's config.
 *
 * @param attackDamage The player's attack damage while holding this weapon.
 * Defaults to `1.0`.
 * Used when `attack_damage` is not specified in the item's config.
 *
 * @param attackSpeed The player's attack speed while holding this weapon.
 * Defaults to `1.0`.
 * Used when `attack_speed` is not specified in the item's config.
 *
 * @param knockbackBonus The additional attack knockback applied by this weapon.
 * Defaults to `0.0`.
 * Used when `knockback_bonus` is not specified in the item's config.
 *
 * @param canSweepAttack Whether this weapon can perform sweep attacks.
 * Defaults to `false`.
 * Used when `can_sweep_attack` is not specified in the item's config.
 *
 * @param disableBlocking The number of ticks for which this weapon disables blocking after an
 * attack.
 * Defaults to `0`.
 * Used when `disable_blocking` is not specified in the item's config.
 *
 * @param itemDamageOnAttackEntity The durability removed whenever an entity is attacked.
 * Used when neither `item_damage_on_attack_entity` nor `item_damage_per_attack` is specified in the item's config.
 */
fun Weapon(
    attackDamage: Double = 1.0,
    attackSpeed: Double = 1.0,
    knockbackBonus: Double = 0.0,
    canSweepAttack: Boolean = false,
    disableBlocking: Int = 0,
    itemDamageOnAttackEntity: Int = 1
) = ItemBehaviorFactory { _, cfg ->
    Weapon(
        cfg.entry(attackDamage, "attack_damage"),
        cfg.entry(attackSpeed, "attack_speed"),
        cfg.entry(knockbackBonus, "knockback_bonus"),
        cfg.entry(canSweepAttack, "can_sweep_attack"),
        cfg.entry(disableBlocking, "disable_blocking"),
        cfg.entry(itemDamageOnAttackEntity, listOf("item_damage_on_attack_entity"), listOf("item_damage_per_attack"))
    )
}

/**
 * Allows items to be used as weapons.
 *
 * @param attackDamage The player's attack damage while holding this weapon.
 * @param attackSpeed The player's attack speed while holding this weapon.
 * @param knockbackBonus The additional attack knockback applied by this weapon.
 * @param canSweepAttack Whether this weapon can perform sweep attacks.
 * @param disableBlocking The number of ticks for which this weapon disables blocking after an
 * attack.
 * @param itemDamageOnAttackEntity The durability removed whenever an entity is attacked.
 */
class Weapon(
    attackDamage: Provider<Double>,
    attackSpeed: Provider<Double>,
    knockbackBonus: Provider<Double>,
    canSweepAttack: Provider<Boolean>,
    disableBlocking: Provider<Int>,
    itemDamageOnAttackEntity: Provider<Int>
) : ItemBehavior {
    
    /**
     * The player's attack damage while holding this weapon.
     */
    val attackDamage: Double by attackDamage
    
    /**
     * The player's attack speed while holding this weapon.
     */
    val attackSpeed: Double by attackSpeed
    
    /**
     * The additional attack knockback applied by this weapon.
     */
    val knockbackBonus: Double by knockbackBonus
    
    /**
     * Whether this weapon can perform sweep attacks.
     */
    val canSweepAttack: Boolean by canSweepAttack
    
    /**
     * The number of ticks for which this weapon disables blocking after an attack.
     */
    val disableBlocking: Int by disableBlocking
    
    /**
     * The durability removed whenever an entity is attacked.
     */
    val itemDamageOnAttackEntity: Int by itemDamageOnAttackEntity
    
    override val tags = canSweepAttack.map { if (it) setOf(ItemTypeTags.SWORDS) else emptySet() }
    
    override val baseDataComponents: Provider<DataComponentMap> = buildDataComponentMapProvider {
        this[DataComponentTypes.WEAPON] = combinedProvider(
            itemDamageOnAttackEntity, disableBlocking
        ) { itemDamageOnAttackEntity, disableBlocking ->
            weapon()
                .itemDamagePerAttack(itemDamageOnAttackEntity)
                .disableBlockingForSeconds(disableBlocking / 20f)
                .build()
        }
        this[DataComponentTypes.ATTRIBUTE_MODIFIERS] = combinedProvider(
            attackDamage, attackSpeed, knockbackBonus
        ) { attackDamage, attackSpeed, knockbackBonus ->
            itemAttributes()
                .addModifier(
                    Attribute.ATTACK_DAMAGE,
                    AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_ID.toNamespacedKey(),
                        attackDamage - PLAYER_ATTACK_DAMAGE,
                        Operation.ADD_NUMBER
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .addModifier(
                    Attribute.ATTACK_SPEED,
                    AttributeModifier(
                        Item.BASE_ATTACK_SPEED_ID.toNamespacedKey(),
                        attackSpeed - PLAYER_ATTACK_SPEED,
                        Operation.ADD_NUMBER
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .addModifier(
                    Attribute.ATTACK_KNOCKBACK,
                    AttributeModifier(
                        NamespacedKey("nova", "knockback_bonus"),
                        knockbackBonus,
                        Operation.ADD_NUMBER
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
                .build()
        }
    }
    
    override fun toString(itemStack: ItemStack): String {
        return "Weapon(" +
            "attackDamage=$attackDamage, " +
            "attackSpeed=$attackSpeed, " +
            "knockbackBonus=$knockbackBonus, " +
            "canSweepAttack=$canSweepAttack, " +
            "disableBlocking=$disableBlocking, " +
            "itemDamageOnAttackEntity=$itemDamageOnAttackEntity" +
            ")"
    }
    
}
