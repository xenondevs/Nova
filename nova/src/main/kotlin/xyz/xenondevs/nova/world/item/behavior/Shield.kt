@file:Suppress("FunctionName")

package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.BlocksAttacks
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.damage.DamageType
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.nova.config.entryOrElse
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider

/**
 * Creates a factory for [Shield] behaviors using the given values, if not specified otherwise in the item's config.
 *
 * @param blockDelay The number of ticks that right-click must be held before successfully blocking attacks.
 * Defaults to `0`. Used when `block_delay` is not specified in the config.
 *
 * @param disableCooldownScale A multiplier that is applied to the number of ticks that the item will be on cooldown for when attacked by a disabling attack.
 * Defaults to `1.0`. Used when `disable_cooldown_scale` is not specified in the config.
 *
 * @param bypassedBy A damage type tag containing the damage types that bypass the shield. Can be `null` if no damage types bypass the shield.
 * Defaults to `null`. Used when `bypassed_by` is not specified in the config.
 *
 * @param blockSound The sound that is played when the shield successfully blocks an attack. Can be `null` if no sound should be played.
 * Defaults to `null`. Used when `block_sound` is not specified in the config.
 *
 * @param disableSound The sound that is played when the shield is disabled by a disabling attack. Can be `null` if no sound should be played.
 * Defaults to `null`. Used when `disable_sound` is not specified in the config.
 */
fun Shield(
    blockDelay: Int = 0,
    disableCooldownScale: Double = 1.0,
    bypassedBy: TagKey<DamageType>?,
    blockSound: Sound?,
    disableSound: Sound?
) = Shield(
    blockDelay,
    disableCooldownScale,
    bypassedBy,
    blockSound?.let(Registry.SOUND_EVENT::getKey),
    disableSound?.let(Registry.SOUND_EVENT::getKey)
)

/**
 * Creates a factory for [Shield] behaviors using the given values, if not specified otherwise in the item's config.
 * 
 * @param blockDelay The number of ticks that right-click must be held before successfully blocking attacks.
 * Defaults to `0`. Used when `block_delay` is not specified in the config.
 * 
 * @param disableCooldownScale A multiplier that is applied to the number of ticks that the item will be on cooldown for when attacked by a disabling attack.
 * Defaults to `1.0`. Used when `disable_cooldown_scale` is not specified in the config.
 * 
 * @param bypassedBy A damage type tag containing the damage types that bypass the shield. Can be `null` if no damage types bypass the shield.
 * Defaults to `null`. Used when `bypassed_by` is not specified in the config.
 * 
 * @param blockSound The sound that is played when the shield successfully blocks an attack. Can be `null` if no sound should be played.
 * Defaults to `null`. Used when `block_sound` is not specified in the config.
 * 
 * @param disableSound The sound that is played when the shield is disabled by a disabling attack. Can be `null` if no sound should be played.
 * Defaults to `null`. Used when `disable_sound` is not specified in the config.
 */
fun Shield(
    blockDelay: Int = 0,
    disableCooldownScale: Double = 1.0,
    bypassedBy: TagKey<DamageType>? = null,
    blockSound: Key? = null,
    disableSound: Key? = null
) = ItemBehaviorFactory<Shield> {
    val cfg = it.config
    Shield(
        cfg.entryOrElse(blockDelay, "block_delay"),
        cfg.entryOrElse(disableCooldownScale, "disable_cooldown_scale"),
        cfg.optionalEntry<TagKey<DamageType>>("bypassed_by").orElse(bypassedBy),
        cfg.optionalEntry<Key>("block_sound").orElse(blockSound),
        cfg.optionalEntry<Key>("disable_sound").orElse(disableSound)
    )
}

/**
 * @param blockDelay The number of ticks that right-click must be held before successfully blocking attacks.
 * @param disableCooldownScale A multiplier that is applied to the number of ticks that the item will be on cooldown for when attacked by a disabling attack.
 * @param bypassedBy A damage type tag containing the damage types that bypass the shield. Can be `null` if no damage types bypass the shield.
 * @param blockSound The sound that is played when the shield successfully blocks an attack. Can be `null` if no sound should be played.
 * @param disableSound The sound that is played when the shield is disabled by a disabling attack. Can be `null` if no sound should be played.
 */
class Shield(
    blockDelay: Provider<Int>,
    disableCooldownScale: Provider<Double>,
    // TODO: damage reductions, item damage (not yet in paper api)
    bypassedBy: Provider<TagKey<DamageType>?>,
    blockSound: Provider<Key?>,
    disableSound: Provider<Key?>
) : ItemBehavior {
    
    /**
     * The number of ticks that right-click must be held before successfully blocking attacks.
     */
    val blockDelay: Int by blockDelay
    
    /**
     * A multiplier that is applied to the number of ticks that the item will be on cooldown for when attacked by a disabling attack.
     */
    val disableCooldownScale: Double by disableCooldownScale
    
    /**
     * A damage type tag containing the damage types that bypass the shield.
     */
    val bypassedBy: TagKey<DamageType>? by bypassedBy
    
    /**
     * The key of the sound that is played when the shield successfully blocks an attack.
     */
    val blockSoundKey: Key? by blockSound
    
    /**
     * The sound that is played when the shield successfully blocks an attack.
     */
    val blockSound: Sound?
        get() = blockSoundKey?.let(Registry.SOUND_EVENT::get)
    
    /**
     * The key of the sound that is played when the shield is disabled by a disabling attack.
     */
    val disableSoundKey: Key? by disableSound
    
    /**
     * The sound that is played when the shield is disabled by a disabling attack.
     */
    val disableSound: Sound?
        get() = disableSoundKey?.let(Registry.SOUND_EVENT::get)
    
    override val baseDataComponents: Provider<DataComponentMap> = buildDataComponentMapProvider {
        this[DataComponentTypes.BLOCKS_ATTACKS] = combinedProvider(
            blockDelay, disableCooldownScale, bypassedBy, blockSound, disableSound
        ) { blockDelay, disableCooldownScale, bypassedBy, blockSound, disableSound ->
            BlocksAttacks.blocksAttacks()
                .blockDelaySeconds(blockDelay / 20f)
                .disableCooldownScale(disableCooldownScale.toFloat())
                .bypassedBy(bypassedBy)
                .blockSound(blockSound)
                .disableSound(disableSound)
                .build()
        }
    }
    
    override fun toString(itemStack: ItemStack): String {
        return "Shield(" +
            "blockDelay=$blockDelay, " +
            "disableCooldownScale=$disableCooldownScale, " +
            "bypassedBy=$bypassedBy, " +
            "blockSound=$blockSound, " +
            "disableSound=$disableSound" +
            ")"
    }
    
}