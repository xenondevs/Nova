package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable.consumable
import io.papermc.paper.datacomponent.item.FoodProperties.food
import io.papermc.paper.datacomponent.item.UseRemainder.useRemainder
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import net.kyori.adventure.key.Key
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.potion.PotionEffect
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.mapNonNull
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.nova.config.entryOrElse
import xyz.xenondevs.nova.config.node
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.util.data.getInputStacks
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider

/**
 * Creates a factory for [Consumable] behaviors using the given values, if not specified otherwise in the item's config.
 *
 * @param nutrition The nutrition that consuming the item provides.
 * Used when `nutrition` is not specified in the config.
 *
 * @param saturation The saturation that consuming the item provides.
 * Used when `saturation` is not specified in the config.
 *
 * @param canAlwaysEat Whether the item can always be eaten, even if the player is not hungry.
 * Used when `can_always_eat` is not specified in the config.
 *
 * @param consumeTime The time it takes to consume the item in ticks.
 * Used when `consume_time` is not specified in the config.
 *
 * @param remains The item that remains after consuming the item.
 * Used when `remains` is not specified in the config.
 *
 * @param possibleEffects The possible effects that consuming the item can apply.
 * Used when `effects` is not specified in the config.
 *
 * @param animation The animation that is played when consuming the item.
 * Used when `animation` is not specified in the config.
 *
 * @param sound The sound that is played when consuming the item. Defaults to `entity.generic.eat`
 * Used when `sound` is not specified in the config.
 *
 * @param particles Whether particles are spawned when consuming the item.
 * Used when `particles` is not specified in the config.
 */
@Suppress("FunctionName")
fun Consumable(
    nutrition: Int = 0,
    saturation: Float = 0f,
    canAlwaysEat: Boolean = false,
    consumeTime: Int = 32,
    remains: ItemStack? = null,
    possibleEffects: Map<PotionEffect, Float>? = null,
    animation: ItemUseAnimation = ItemUseAnimation.EAT,
    sound: Key = Key.key("minecraft", "entity.generic.eat"),
    particles: Boolean = true
) = ItemBehaviorFactory<Consumable> {
    val cfg = it.config
    
    Consumable(
        nutrition = cfg.entryOrElse(nutrition, "nutrition"),
        saturation = cfg.entryOrElse(saturation, "saturation"),
        canAlwaysEat = cfg.entryOrElse(canAlwaysEat, "can_always_eat"),
        consumeTime = cfg.entryOrElse(consumeTime, "consume_time"),
        particles = cfg.entryOrElse(particles, "particles"),
        animation = cfg.entryOrElse(animation, "animation"),
        sound = cfg.entryOrElse(sound, "sound"),
        
        // RecipeChoice is not optimal here, but ItemStack currently has no serializer
        remains = cfg.optionalEntry<RecipeChoice>("remains")
            .mapNonNull { it.getInputStacks()[0] }
            .orElse(remains),
        
        possibleEffects = cfg.node("effects").map {
            it.childrenList().associate { e ->
                val effect = e.get(PotionEffect::class.java)!!
                val probability = e.node("probability").getFloat(1.0f)
                effect to probability
            }
        }.map { it.takeUnlessEmpty() ?: possibleEffects ?: emptyMap() },
    )
}

/**
 * Allows items to be consumed.
 *
 * @param nutrition The nutrition that consuming the item provides.
 * @param saturation The saturation that consuming the item provides.
 * @param canAlwaysEat Whether the item can always be eaten, even if the player is not hungry.
 * @param consumeTime The time it takes to consume the item in ticks.
 * @param remains The item that remains after consuming the item.
 * @param possibleEffects The possible effects that consuming the item can apply.
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
    possibleEffects: Provider<Map<PotionEffect, Float>>,
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
     * The possible effects that consuming the item can apply.
     */
    val possibleEffects by possibleEffects
    
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
            consumeTime, animation, sound, particles, possibleEffects
        ) { consumeTime, animation, sound, particles, possibleEffects ->
            consumable()
                .consumeSeconds(consumeTime / 20f)
                .animation(animation)
                .sound(sound)
                .hasConsumeParticles(particles)
                .addEffects(possibleEffects.map { (potionEffect, probability) ->
                    ConsumeEffect.applyStatusEffects(listOf(potionEffect), probability)
                })
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
            "possibleEffects=$possibleEffects, " +
            "animation=$animation, " +
            "sound=$sound, " +
            "particles=$particles" +
            ")"
    }
    
}