package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable.consumable
import io.papermc.paper.datacomponent.item.FoodProperties.food
import io.papermc.paper.datacomponent.item.UseRemainder.useRemainder
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect.*
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import io.papermc.paper.registry.keys.SoundEventKeys
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedClassSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flatMapNonNull
import xyz.xenondevs.commons.provider.mapNonNull
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.commons.provider.orElseBy
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.alias.PotionEffectTypeEntry
import xyz.xenondevs.nova.registry.alias.PotionEffectTypeEntrySet
import xyz.xenondevs.nova.registry.entry
import xyz.xenondevs.nova.serialization.kotlinx.KeySerializer
import xyz.xenondevs.nova.serialization.kotlinx.NamespacedPolymorphicSerializer
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.behavior.Consumable.ConsumeEffect.ApplyStatusEffects
import xyz.xenondevs.nova.world.item.behavior.Consumable.ConsumeEffect.ApplyStatusEffects.Effect
import xyz.xenondevs.nova.world.item.behavior.Consumable.ConsumeEffect.ClearAllStatusEffects
import xyz.xenondevs.nova.world.item.behavior.Consumable.ConsumeEffect.PlaySound
import xyz.xenondevs.nova.world.item.behavior.Consumable.ConsumeEffect.RemoveStatusEffects
import xyz.xenondevs.nova.world.item.behavior.Consumable.ConsumeEffect.TeleportRandomly
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect as PaperConsumeEffect
import org.bukkit.potion.PotionEffect as BukkitPotionEffect

/**
 * Creates a factory for [Consumable] behaviors using the given values, if not specified otherwise in the item's config.
 *
 * @param nutrition The nutrition that consuming the item provides.
 * Defaults to `0`.
 * Used when `nutrition` is not specified in the config.
 *
 * @param saturation The saturation that consuming the item provides.
 * Defaults to `0f`.
 * Used when `saturation` is not specified in the config.
 *
 * @param canAlwaysEat Whether the item can always be eaten, even if the player is not hungry.
 * Defaults to `false`.
 * Used when `can_always_eat` is not specified in the config.
 *
 * @param consumeTime The time it takes to consume the item in ticks.
 * Defaults to `32`.
 * Used when `consume_time` is not specified in the config.
 *
 * @param remains The item that remains after consuming the item.
 * Defaults to `null`.
 * Used when `remains` is not specified in the config.
 *
 * @param consumeEffects The possible effects that consuming the item can apply.
 * Defaults to `emptyList()`.
 * Used when `consume_effects` is not specified in the config.
 *
 * @param animation The animation that is played when consuming the item.
 * Defaults to `ItemUseAnimation.EAT`.
 * Used when `animation` is not specified in the config.
 *
 * @param sound The sound that is played when consuming the item.
 * Defaults to `entity.generic.eat`.
 * Used when `sound` is not specified in the config.
 *
 * @param particles Whether particles are spawned when consuming the item.
 * Defaults to `true`.
 * Used when `particles` is not specified in the config.
 */
@Suppress("FunctionName")
fun Consumable(
    nutrition: Int = 0,
    saturation: Float = 0f,
    canAlwaysEat: Boolean = false,
    consumeTime: Int = 32,
    remains: RegistryEntry.Paper<ItemType>? = null,
    consumeEffects: List<Consumable.ConsumeEffect> = emptyList(),
    animation: ItemUseAnimation = ItemUseAnimation.EAT,
    sound: Key = SoundEventKeys.ENTITY_GENERIC_EAT.key(),
    particles: Boolean = true
) = ItemBehaviorFactory { _, cfg ->
    val legacyPossibleEffects = combinedProvider(
        cfg.entry<List<BukkitPotionEffect>>(emptyList(), "effects"),
        cfg.entry<List<ProbabilitySurrogate>>(emptyList(), "effects")
    ) { effects, probabilities ->
        effects.zip(probabilities.map { it.probability })
            .takeUnlessEmpty()
            ?.map { [potionEffect, probability] ->
                ApplyStatusEffects(
                    [
                        Effect(
                            potionEffect.type.entry,
                            potionEffect.duration,
                            potionEffect.amplifier,
                            potionEffect.isAmbient,
                            potionEffect.hasParticles(),
                            potionEffect.hasIcon()
                        )
                    ],
                    probability
                )
            }
    }
    
    Consumable(
        nutrition = cfg.entry(nutrition, "nutrition"),
        saturation = cfg.entry(saturation, "saturation"),
        canAlwaysEat = cfg.entry(canAlwaysEat, "can_always_eat"),
        consumeTime = cfg.entry(consumeTime, "consume_time"),
        particles = cfg.entry(particles, "particles"),
        animation = cfg.entry(animation, "animation"),
        sound = cfg.entry(sound, "sound"),
        
        remains = cfg.optionalEntry<RegistryEntry.Paper<ItemType>>("remains")
            .orElse(remains)
            .flatMapNonNull { entry -> entry?.map { type -> type.createItemStack() } },
        
        consumeEffects = cfg.optionalEntry<List<Consumable.ConsumeEffect>>("consume_effects")
            .orElseBy(legacyPossibleEffects)
            .orElse(consumeEffects)
    )
}

@Serializable
private data class ProbabilitySurrogate(val probability: Float = 1.0f)

/**
 * Allows items to be consumed.
 *
 * @param nutrition The nutrition that consuming the item provides.
 * @param saturation The saturation that consuming the item provides.
 * @param canAlwaysEat Whether the item can always be eaten, even if the player is not hungry.
 * @param consumeTime The time it takes to consume the item in ticks.
 * @param remains The item that remains after consuming the item.
 * @param consumeEffects The possible effects that consuming the item can apply.
 * @param animation The animation that is played when consuming the item.
 * @param sound The sound that is played when consuming the item.
 * @param particles Whether particles are spawned when consuming the item.
 */
class Consumable(
    nutrition: Provider<Int>,
    saturation: Provider<Float>,
    canAlwaysEat: Provider<Boolean>,
    consumeTime: Provider<Int>,
    remains: Provider<ItemStack?>,
    consumeEffects: Provider<List<ConsumeEffect>>,
    animation: Provider<ItemUseAnimation>,
    sound: Provider<Key>,
    particles: Provider<Boolean>
) : ItemBehavior {
    
    /**
     * The nutrition that consuming the item provides.
     */
    val nutrition by nutrition
    
    /**
     * The saturation that consuming the item provides.
     */
    val saturation by saturation
    
    /**
     * Whether the item can always be eaten, even if the player is not hungry.
     */
    val canAlwaysEat by canAlwaysEat
    
    /**
     * The time it takes to consume the item in ticks.
     */
    val consumeTime by consumeTime
    
    /**
     * The item that remains after consuming the item.
     */
    val remains by remains
    
    /**
     * The effects that consuming the item can apply.
     */
    val consumeEffects by consumeEffects
    
    /**
     * The animation that is played when consuming the item.
     */
    val animation by animation
    
    /**
     * The sound that is played when consuming the item.
     */
    val sound by sound
    
    /**
     * Whether particles are spawned when consuming the item.
     */
    val particles by particles
    
    override val baseDataComponents: Provider<DataComponentMap> = buildDataComponentMapProvider {
        this[DataComponentTypes.FOOD] = combinedProvider(
            nutrition, saturation, canAlwaysEat
        ) { nutrition, saturation, canAlwaysEat ->
            food()
                .nutrition(nutrition)
                .saturation(saturation)
                .canAlwaysEat(canAlwaysEat)
                .build()
        }
        
        this[DataComponentTypes.CONSUMABLE] = combinedProvider(
            consumeTime, animation, sound, particles, consumeEffects
        ) { consumeTime, animation, sound, particles, consumeEffects ->
            consumable()
                .consumeSeconds(consumeTime / 20f)
                .animation(animation)
                .sound(sound)
                .hasConsumeParticles(particles)
                .addEffects(consumeEffects.map(ConsumeEffect::toPaper))
                .build()
        }
        
        this[DataComponentTypes.USE_REMAINDER] = remains
            .map { it?.takeUnlessEmpty() }
            .mapNonNull { useRemainder(it) }
    }
    
    override fun toString(itemStack: ItemStack): String {
        return "Consumable(" +
            "nutrition=$nutrition, " +
            "saturation=$saturation, " +
            "canAlwaysEat=$canAlwaysEat, " +
            "consumeTime=$consumeTime, " +
            "remains=$remains, " +
            "consumeEffects=$consumeEffects, " +
            "animation=$animation, " +
            "sound=${sound.asString()}, " +
            "particles=$particles" +
            ")"
    }
    
    /**
     * An effect that is applied after an item is consumed.
     * Similar to [PaperConsumeEffect] with the exception that it is [Serializable] and can be created during the bootstrap phase.
     */
    @Serializable(ConsumeEffectSerializer::class)
    sealed interface ConsumeEffect {
        
        /**
         * Applies [effects] with the given [probability].
         */
        @Serializable
        @SerialName("minecraft:apply_effects")
        data class ApplyStatusEffects(
            val effects: List<Effect>,
            val probability: Float = 1f
        ) : ConsumeEffect {
            
            init {
                require(probability in 0f..1f) { "Probability must be in range 0.0..1.0" }
            }
            
            /**
             * A status effect instance used by [ApplyStatusEffects].
             */
            @Serializable
            data class Effect(
                val id: PotionEffectTypeEntry,
                val duration: Int = 0,
                val amplifier: Int = 0,
                val ambient: Boolean = false,
                @SerialName("show_particles")
                val showParticles: Boolean = true,
                @SerialName("show_icon")
                val showIcon: Boolean = showParticles,
                @SerialName("hidden_effect")
                val hiddenEffect: Effect? = null
            ) {
                
                init {
                    require(amplifier in 0..255) { "Amplifier must be in range 0..255" }
                }
                
            }
            
        }
        
        /**
         * Removes all active effects matching [effects].
         */
        @Serializable
        @SerialName("minecraft:remove_effects")
        data class RemoveStatusEffects(
            val effects: PotionEffectTypeEntrySet
        ) : ConsumeEffect
        
        /**
         * Removes all active status effects.
         */
        @Serializable
        @SerialName("minecraft:clear_all_effects")
        data object ClearAllStatusEffects : ConsumeEffect
        
        /**
         * Randomly teleports the consumer within [diameter].
         */
        @Serializable
        @SerialName("minecraft:teleport_randomly")
        data class TeleportRandomly(
            val diameter: Float = 16f,
            @SerialName("directional_particles")
            val directionalParticles: Boolean = true
        ) : ConsumeEffect {
            
            init {
                require(diameter > 0f) { "Diameter must be positive" }
            }
            
        }
        
        /**
         * Plays [sound] at the consumer's position.
         */
        @Serializable
        @SerialName("minecraft:play_sound")
        data class PlaySound(
            @Serializable(with = KeySerializer::class)
            val sound: Key
        ) : ConsumeEffect
        
    }
    
}

private fun Consumable.ConsumeEffect.toPaper(): PaperConsumeEffect = when (this) {
    is ApplyStatusEffects -> applyStatusEffects(effects.map(Effect::toPaper), probability)
    is RemoveStatusEffects -> removeEffects(effects.toRegistryKeySet())
    is TeleportRandomly -> teleportRandomlyEffect(diameter, directionalParticles)
    is PlaySound -> playSoundConsumeEffect(sound)
    ClearAllStatusEffects -> clearAllStatusEffects()
}

private fun Effect.toPaper(): BukkitPotionEffect =
    BukkitPotionEffect(id.get(), duration, amplifier, ambient, showParticles, showIcon)

@OptIn(InternalSerializationApi::class)
internal object ConsumeEffectSerializer : KSerializer<Consumable.ConsumeEffect> by NamespacedPolymorphicSerializer(
    SealedClassSerializer(
        "xyz.xenondevs.nova.world.item.behavior.Consumable.ConsumeEffect",
        Consumable.ConsumeEffect::class,
        arrayOf(
            ApplyStatusEffects::class,
            RemoveStatusEffects::class,
            ClearAllStatusEffects::class,
            TeleportRandomly::class,
            PlaySound::class
        ),
        arrayOf(
            ApplyStatusEffects.serializer(),
            RemoveStatusEffects.serializer(),
            ClearAllStatusEffects.serializer(),
            TeleportRandomly.serializer(),
            PlaySound.serializer()
        )
    ),
    "minecraft"
)