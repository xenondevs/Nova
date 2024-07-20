package xyz.xenondevs.nova.item.behavior

import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.world.food.FoodProperties
import org.bukkit.craftbukkit.potion.CraftPotionUtil
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.potion.PotionEffect
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.combinedProvider
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.commons.provider.immutable.mapNonNull
import xyz.xenondevs.commons.provider.immutable.orElse
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.util.data.getInputStacks
import xyz.xenondevs.nova.util.unwrap
import java.util.*

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
) : ItemBehavior {
    
    val nutrition by nutrition
    val saturation by saturation
    val canAlwaysEat by canAlwaysEat
    val consumeTime by consumeTime
    val remains by remains
    val possibleEffects by possibleEffects
    
    constructor(
        nutrition: Int,
        saturation: Float,
        canAlwaysEat: Boolean,
        consumeTime: Int,
        remains: ItemStack? = null,
        possibleEffects: Map<PotionEffect, Float> = emptyMap()
    ) : this(
        provider(nutrition),
        provider(saturation),
        provider(canAlwaysEat),
        provider(consumeTime),
        provider(remains),
        provider(possibleEffects)
    )
    
    override val baseDataComponents = combinedProvider(
        nutrition, saturation, canAlwaysEat, consumeTime, remains, possibleEffects
    ) { nutrition, saturation, canAlwaysEat, consumeTime, remains, possibleEffects ->
        DataComponentMap.builder()
            .set(DataComponents.FOOD, FoodProperties(
                nutrition,
                saturation,
                canAlwaysEat,
                consumeTime / 20f,
                Optional.ofNullable(remains?.unwrap()),
                possibleEffects.map { (potionEffect, probability) ->
                    FoodProperties.PossibleEffect(CraftPotionUtil.fromBukkit(potionEffect), probability)
                }
            )).build()
    }
    
    companion object : ItemBehaviorFactory<Consumable> {
        
        override fun create(item: NovaItem): Consumable {
            val cfg = item.config
            return Consumable(
                cfg.entry("nutrition"),
                cfg.entry("saturation"),
                cfg.optionalEntry<Boolean>("can_always_eat").orElse(false),
                cfg.entry("consume_time"),
                // RecipeChoice is not optimal here, but ItemStack currently has no serializer
                cfg.optionalEntry<RecipeChoice>("remains").mapNonNull { it.getInputStacks()[0] },
                cfg.node("effects").map {
                    it.childrenList().associate { e ->
                        val effect = e.get(PotionEffect::class.java)!!
                        val probability = e.node("probability").getFloat(1.0f)
                        effect to probability
                    }
                }
            )
        }
        
    }
    
}