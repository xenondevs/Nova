package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.nova.config.entryOrElse
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.util.unwrap

/**
 * Creates a factory for [Damageable] behaviors using the given values, if not specified otherwise in the item's config.
 *
 * @param maxDurability The maximum durability of the item.
 * Used when `max_durability` is not specified in the item's config, or `null` to require the presence of a config entry.
 *
 * @param itemDamageOnAttackEntity The amount of damage the item receives when attacking an entity.
 * Used when `item_damage_on_attack_entity` is not specified in the item's config.
 *
 * @param itemDamageOnBreakBlock The amount of damage the item receives when breaking a block.
 * Used when `item_damage_on_break_block` is not specified in the item's config.
 *
 * @param repairIngredient The ingredient required to repair the item. Can be null for items that cannot be repaired.
 * Used when `repair_ingredient` is not specified in the item's config.
 */
@Suppress("FunctionName")
fun Damageable(
    maxDurability: Int? = null,
    itemDamageOnAttackEntity: Int = 0,
    itemDamageOnBreakBlock: Int = 0,
    repairIngredient: RecipeChoice? = null
) = ItemBehaviorFactory<Damageable> {
    val cfg = it.config
    Damageable(
        cfg.entryOrElse(maxDurability, arrayOf("max_durability"), arrayOf("durability")),
        cfg.entryOrElse(itemDamageOnAttackEntity, "item_damage_on_attack_entity"),
        cfg.entryOrElse(itemDamageOnBreakBlock, "item_damage_on_break_block"),
        cfg.optionalEntry<RecipeChoice>("repair_ingredient").orElse(repairIngredient)
    )
}

/**
 * Allows items to store and receive damage.
 */
class Damageable(
    maxDurability: Provider<Int>,
    itemDamageOnAttackEntity: Provider<Int>,
    itemDamageOnBreakBlock: Provider<Int>,
    repairIngredient: Provider<RecipeChoice?>
) : ItemBehavior {
    
    val maxDurability by maxDurability
    val itemDamageOnAttackEntity by itemDamageOnAttackEntity
    val itemDamageOnBreakBlock by itemDamageOnBreakBlock
    val repairIngredient by repairIngredient
    
    override val baseDataComponents = maxDurability.map {
        DataComponentMap.builder()
            .set(DataComponents.MAX_DAMAGE, it)
            .set(DataComponents.DAMAGE, 0)
            .build()
    }
    
    override fun toString(itemStack: ItemStack): String {
        return "Damageable(" +
            "damage=${itemStack.unwrap().get(DataComponents.DAMAGE) ?: 0}, " +
            "maxDurability=$maxDurability, " +
            "itemDamageOnAttackEntity=$itemDamageOnAttackEntity, " +
            "itemDamageOnBreakBlock=$itemDamageOnBreakBlock, " +
            "repairIngredient=$repairIngredient" +
            ")"
    }
    
}