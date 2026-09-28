@file:Suppress("FunctionName")

package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Repairable
import io.papermc.paper.registry.keys.SoundEventKeys
import net.kyori.adventure.key.Key
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mapNonNull
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider

/**
 * Creates a factory for [Damageable] behaviors using the given values, if not specified otherwise in the item's config.
 *
 * @param maxDurability The maximum durability of the item.
 * Defaults to `1`.
 * Used when `max_durability` is not specified in the item's config.
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
    repairIngredient: RegistryEntrySet.Paper<ItemType>? = null,
    breakSound: Key = SoundEventKeys.ENTITY_ITEM_BREAK.key()
) = ItemBehaviorFactory { _, cfg ->
    Damageable(
        cfg.entry(maxDurability, listOf("max_durability"), listOf("durability")),
        cfg.optionalEntry<RegistryEntrySet.Paper<ItemType>>("repair_ingredient").orElse(repairIngredient),
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
    repairIngredient: Provider<RegistryEntrySet.Paper<ItemType>?>,
    breakSound: Provider<Key>
) : ItemBehavior {
    
    /**
     * The maximum durability of the item.
     */
    val maxDurability: Int by maxDurability
    
    /**
     * The ingredient required to repair the item in an anvil.
     */
    val repairIngredient: RegistryEntrySet.Paper<ItemType>? by repairIngredient
    
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
        this[DataComponentTypes.REPAIRABLE] = repairIngredient.mapNonNull { Repairable.repairable(it.toRegistryKeySet()) }
    }
    
    override fun toString(itemStack: ItemStack): String {
        return "Damageable(" +
            "damage=${itemStack.getData(DataComponentTypes.DAMAGE) ?: 0}, " +
            "maxDurability=$maxDurability, " +
            "repairIngredient=$repairIngredient" +
            "breakSound=${breakSoundKey.asString()}" +
            ")"
    }
    
}