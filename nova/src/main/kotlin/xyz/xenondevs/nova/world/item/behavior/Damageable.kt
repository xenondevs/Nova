@file:Suppress("FunctionName")

package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Weapon.weapon
import io.papermc.paper.registry.keys.SoundEventKeys
import net.kyori.adventure.key.Key
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mapNonNull
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider

/**
 * Creates a factory for [Damageable] behaviors using the given values, if not specified otherwise in the item's config.
 *
 * @param maxDurability The maximum durability of the item.
 * Defaults to `1`.
 * Used when `max_durability` is not specified in the item's config.
 *
 * @param itemDamageOnAttackEntity The amount of damage the item receives when attacking an entity.
 * Defaults to `1`.
 * Used when `item_damage_on_attack_entity` is not specified in the item's config.
 *
 * @param itemDamageOnBreakBlock The amount of damage the item receives when breaking a block.
 * Defaults to `1`.
 * Used when `item_damage_on_break_block` is not specified in the item's config.
 *
 * @param repairIngredient The ingredient required to repair the item. Can be null for items that cannot be repaired.
 * Defaults to `null`.
 * Used when `repair_ingredient` is not specified in the item's config.
 *
 * @param breakSound The break sound that is played when the item breaks.
 * Defaults to `minecraft:entity.item.break`.
 * Used when `break_sound` is not specified in the item's config.
 */
fun Damageable(
    maxDurability: Int = 1,
    itemDamageOnAttackEntity: Int = 1,
    itemDamageOnBreakBlock: Int = 1,
    repairIngredient: RecipeChoice? = null,
    breakSound: Key = SoundEventKeys.ENTITY_ITEM_BREAK
) = ItemBehaviorFactory { _, cfg ->
    Damageable(
        cfg.entry(maxDurability, listOf("max_durability"), listOf("durability")),
        cfg.entry(itemDamageOnAttackEntity, "item_damage_on_attack_entity"),
        cfg.entry(itemDamageOnBreakBlock, "item_damage_on_break_block"),
        cfg.optionalEntry<RecipeChoice>("repair_ingredient").orElse(repairIngredient),
        cfg.entry(breakSound, "break_sound")
    )
}

/**
 * Allows items to store and receive damage.
 * 
 * @param maxDurability The maximum durability of the item.
 * @param itemDamageOnAttackEntity The amount of damage the item receives when attacking an entity.
 * @param itemDamageOnBreakBlock The amount of damage the item receives when breaking a block.
 * @param repairIngredient The ingredient required to repair the item in an anvil. Can be null for items that cannot be repaired.
 * @param breakSound The sound that is played when the item breaks.
 */
class Damageable(
    maxDurability: Provider<Int>,
    itemDamageOnAttackEntity: Provider<Int>,
    itemDamageOnBreakBlock: Provider<Int>,
    repairIngredient: Provider<RecipeChoice?>,
    breakSound: Provider<Key>
) : ItemBehavior {
    
    /**
     * The maximum durability of the item.
     */
    val maxDurability: Int by maxDurability
    
    /**
     * The amount of damage the item receives when attacking an entity.
     */
    val itemDamageOnAttackEntity: Int by itemDamageOnAttackEntity
    
    /**
     * The amount of damage the item receives when breaking a block.
     */
    val itemDamageOnBreakBlock: Int by itemDamageOnBreakBlock
    
    /**
     * The ingredient required to repair the item in an anvil.
     */
    val repairIngredient: RecipeChoice? by repairIngredient
    
    /**
     * The key of the sound that is played when the item breaks.
     */
    val breakSoundKey: Key by breakSound
    
    /**
     * The sound that is played when the item breaks.
     */
    val breakSound: Sound?
        get() = Registry.SOUND_EVENT.get(breakSoundKey)
    
    override val baseDataComponents: Provider<DataComponentMap> = buildDataComponentMapProvider {
        this[DataComponentTypes.MAX_DAMAGE] = maxDurability
        this[DataComponentTypes.DAMAGE] = 0
        this[DataComponentTypes.BREAK_SOUND] = breakSound
        this[DataComponentTypes.WEAPON] = itemDamageOnAttackEntity
            .map { it.takeUnless { it < 0 } }
            .mapNonNull {
                weapon()
                    .itemDamagePerAttack(it)
                    .disableBlockingForSeconds(0f) // use the lowest value for merging with weapon component of Tool
                    .build()
            }
    }
    
    override fun toString(itemStack: ItemStack): String {
        return "Damageable(" +
            "damage=${itemStack.getData(DataComponentTypes.DAMAGE) ?: 0}, " +
            "maxDurability=$maxDurability, " +
            "itemDamageOnAttackEntity=$itemDamageOnAttackEntity, " +
            "itemDamageOnBreakBlock=$itemDamageOnBreakBlock, " +
            "repairIngredient=$repairIngredient" +
            "breakSound=$breakSoundKey" +
            ")"
    }
    
}