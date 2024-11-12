package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.item.component.UseRemainder
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect
import org.bukkit.craftbukkit.potion.CraftPotionUtil
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.potion.PotionEffect
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.commons.provider.mapNonNull
import xyz.xenondevs.commons.provider.orElse
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.config.node
import xyz.xenondevs.nova.config.optionalEntry
import xyz.xenondevs.nova.util.data.getInputStacks
import xyz.xenondevs.nova.util.getOrNull
import xyz.xenondevs.nova.util.item.isNotNullOrEmpty
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.NovaItem
import net.minecraft.world.item.component.Consumable as ConsumableComponent

/**
 * Allows items to be consumed.
 */
class Consumable(
    nutrition: Provider<Int>,
    saturation: Provider<Float>,
    canAlwaysEat: Provider<Boolean>,
    consumeTime: Provider<Int>,
    remains: Provider<ItemStack?>,
    possibleEffects: Provider<Map<PotionEffect, Float>>,
    animation: Provider<ItemUseAnimation>,
    sound: Provider<Holder<SoundEvent>>,
    particles: Provider<Boolean>
) : ItemBehavior {
    
    val nutrition by nutrition
    val saturation by saturation
    val canAlwaysEat by canAlwaysEat
    val consumeTime by consumeTime
    val remains by remains
    val possibleEffects by possibleEffects
    val animation by animation
    val sound by sound
    val particles by particles
    
    constructor(
        nutrition: Int,
        saturation: Float,
        canAlwaysEat: Boolean,
        consumeTime: Int,
        remains: ItemStack? = null,
        possibleEffects: Map<PotionEffect, Float> = emptyMap(),
        animation: ItemUseAnimation = ItemUseAnimation.EAT,
        sound: Holder<SoundEvent> = SoundEvents.GENERIC_EAT,
        particles: Boolean = true
    ) : this(
        provider(nutrition),
        provider(saturation),
        provider(canAlwaysEat),
        provider(consumeTime),
        provider(remains),
        provider(possibleEffects),
        provider(animation),
        provider(sound),
        provider(particles)
    )
    
    override val baseDataComponents = combinedProvider(
        nutrition, saturation, canAlwaysEat, consumeTime, remains, possibleEffects, animation, sound, particles,
    ) { nutrition, saturation, canAlwaysEat, consumeTime, remains, possibleEffects, animation, sound, particles ->
        DataComponentMap.builder().apply {
            set(
                DataComponents.FOOD,
                FoodProperties(
                    nutrition,
                    saturation,
                    canAlwaysEat,
                )
            )
            
            set(
                DataComponents.CONSUMABLE,
                ConsumableComponent(
                    consumeTime / 20f,
                    animation,
                    sound,
                    particles,
                    possibleEffects.map { (potionEffect, probability) ->
                        ApplyStatusEffectsConsumeEffect(CraftPotionUtil.fromBukkit(potionEffect), probability)
                    }
                )
            )
            
            if (remains.isNotNullOrEmpty())
                set(DataComponents.USE_REMAINDER, UseRemainder(remains.unwrap()))
        }.build()
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
    
    companion object : ItemBehaviorFactory<Consumable> {
        
        override fun create(item: NovaItem): Consumable {
            val cfg = item.config
            return Consumable(
                cfg.optionalEntry<Int>("nutrition").orElse(0),
                cfg.optionalEntry<Float>("saturation").orElse(0f),
                cfg.optionalEntry<Boolean>("can_always_eat").orElse(false),
                cfg.optionalEntry<Int>("consume_time").orElse(32),
                // RecipeChoice is not optimal here, but ItemStack currently has no serializer
                cfg.optionalEntry<RecipeChoice>("remains").mapNonNull { it.getInputStacks()[0] },
                cfg.node("effects").map {
                    it.childrenList().associate { e ->
                        val effect = e.get(PotionEffect::class.java)!!
                        val probability = e.node("probability").getFloat(1.0f)
                        effect to probability
                    }
                },
                cfg.optionalEntry<ItemUseAnimation>("animation").orElse(ItemUseAnimation.EAT),
                cfg.optionalEntry<String>("sound")
                    .mapNonNull { BuiltInRegistries.SOUND_EVENT.getOrNull(it) }
                    .orElse(SoundEvents.GENERIC_EAT),
                cfg.optionalEntry<Boolean>("particles").orElse(true)
            )
        }
        
    }
    
}