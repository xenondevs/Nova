package xyz.xenondevs.nova.material.options

import org.bukkit.potion.PotionEffect
import xyz.xenondevs.nova.data.config.ConfigAccess
import xyz.xenondevs.nova.data.provider.map
import xyz.xenondevs.nova.data.provider.orElse
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
    
    /**
     * The type of food
     */
    val type: FoodType
    
    /**
     * The time it takes for the food to be consumed, in ticks.
     */
    val consumeTime: Int
    
    /**
     * The nutrition value this food provides.
     */
    val nutrition: Int
    
    /**
     * The saturation modifier this food provides. The saturation is calculated like this:
     * ```
     * saturation = min(saturation + nutrition * saturationModifier * 2.0f, foodLevel)
     * ```
     */
    val saturationModifier: Float
    
    /**
     * The amount of health to be restored immediately.
     */
    val instantHealth: Double
    
    /**
     * A list of effects to apply to the player when this food is consumed.
     */
    val effects: List<PotionEffect>?
    
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
    override val type: FoodType,
    override val consumeTime: Int,
    override val nutrition: Int,
    override val saturationModifier: Float,
    override val instantHealth: Double,
    override val effects: List<PotionEffect>?
) : FoodOptions

private class ConfigurableFoodOptions : ConfigAccess, FoodOptions {
    
    override val type by getOptionalEntry<String>("food_type")
        .map { FoodType.valueOf(it.uppercase()) }
        .orElse(FoodType.NORMAL)
    override val consumeTime by getEntry<Int>("consume_time")
    override val nutrition by getEntry<Int>("nutrition")
    override val saturationModifier by getEntry<Float>("saturation_modifier")
    override val instantHealth by getEntry<Double>("instant_health")
    override val effects by getOptionalEntry<List<PotionEffect>>("effects")
    
    constructor(path: String) : super(path)
    constructor(material: ItemNovaMaterial) : super(material)
    
}