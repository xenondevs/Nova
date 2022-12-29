package xyz.xenondevs.nova.material.options

import org.bukkit.potion.PotionEffect
import xyz.xenondevs.nova.data.config.ConfigAccess
import xyz.xenondevs.nova.data.provider.Provider
import xyz.xenondevs.nova.data.provider.map
import xyz.xenondevs.nova.data.provider.orElse
import xyz.xenondevs.nova.data.provider.provider
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.options.FoodOptions.FoodType

/**
 * @param type The type of food
 * @param consumeTime The time it takes for the food to be consumed, in ticks.
 * @param nutrition The nutrition value this food provides.
 * @param saturationModifier The saturation modifier this food provides. The saturation is calculated like this:
 * ```
 * saturation = min(saturation + nutrition * saturationModifier * 2.0f, foodLevel)
 * ```
 * @param instantHealth The amount of health to be restored immediately.
 * @param effects A list of effects to apply to the player when this food is consumed.
 */
@HardcodedMaterialOptions
fun FoodOptions(
    type: FoodType,
    consumeTime: Int,
    nutrition: Int,
    saturationModifier: Float,
    instantHealth: Double = 0.0,
    effects: List<PotionEffect>? = null
): FoodOptions = HardcodedFoodOptions(type, consumeTime, nutrition, saturationModifier, instantHealth, effects)

sealed interface FoodOptions {
    
    val typeProvider: Provider<FoodType>
    val consumeTimeProvider: Provider<Int>
    val nutritionProvider: Provider<Int>
    val saturationModifierProvider: Provider<Float>
    val instantHealthProvider: Provider<Double>
    val effectsProvider: Provider<List<PotionEffect>?>
    
    val type: FoodType
        get() = typeProvider.value
    val consumeTime: Int
        get() = consumeTimeProvider.value
    val nutrition: Int
        get() = nutritionProvider.value
    val saturationModifier: Float
        get() = saturationModifierProvider.value
    val instantHealth: Double
        get() = instantHealthProvider.value
    val effects: List<PotionEffect>?
        get() = effectsProvider.value
    
    enum class FoodType(internal val vanillaMaterialProperty: VanillaMaterialProperty) {
        
        /**
         * Behaves like normal food.
         *
         * Has a small delay before the eating animation starts.
         *
         * Can only be eaten when hungry.
         */
        NORMAL(VanillaMaterialProperty.CONSUMABLE_NORMAL),
        
        /**
         * The eating animation starts immediately.
         *
         * Can only be eaten when hungry.
         */
        FAST(VanillaMaterialProperty.CONSUMABLE_FAST),
        
        /**
         * The food can always be eaten, no hunger is required.
         *
         * Has a small delay before the eating animation starts.
         */
        ALWAYS_EATABLE(VanillaMaterialProperty.CONSUMABLE_ALWAYS)
    }
    
    companion object : MaterialOptionsType<FoodOptions> {
        
        override fun configurable(material: ItemNovaMaterial): FoodOptions =
            ConfigurableFoodOptions(material)
        
        override fun configurable(path: String): FoodOptions =
            ConfigurableFoodOptions(path)
        
    }
    
}

private class HardcodedFoodOptions(
     type: FoodType,
     consumeTime: Int,
     nutrition: Int,
     saturationModifier: Float,
     instantHealth: Double,
     effects: List<PotionEffect>?
) : FoodOptions {
    override val typeProvider = provider(type)
    override val consumeTimeProvider = provider(consumeTime)
    override val nutritionProvider = provider(nutrition)
    override val saturationModifierProvider = provider(saturationModifier)
    override val instantHealthProvider = provider(instantHealth)
    override val effectsProvider = provider(effects)
}

private class ConfigurableFoodOptions : ConfigAccess, FoodOptions {
    
    override val typeProvider = getOptionalEntry<String>("food_type")
        .map { FoodType.valueOf(it.uppercase()) }
        .orElse(FoodType.NORMAL)
    override val consumeTimeProvider = getEntry<Int>("consume_time")
    override val nutritionProvider = getEntry<Int>("nutrition")
    override val saturationModifierProvider = getEntry<Float>("saturation_modifier")
    override val instantHealthProvider = getOptionalEntry<Double>("instant_health").orElse(0.0)
    override val effectsProvider = getOptionalEntry<List<PotionEffect>>("effects")
    
    constructor(path: String) : super(path)
    constructor(material: ItemNovaMaterial) : super(material)
    
}