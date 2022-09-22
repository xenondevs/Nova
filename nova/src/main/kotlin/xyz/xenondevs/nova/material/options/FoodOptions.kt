package xyz.xenondevs.nova.material.options

import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty

/**
 * @property type The type of food
 * @property consumeTime The time it takes for the food to be consumed, in ticks.
 * @property nutrition The nutrition value this food provides.
 * @property saturationModifier The saturation modifier this food provides. The saturation is calculated like this:
 * ```
 * saturation = min(saturation + nutrition * saturationModifier * 2.0f, foodLevel)
 * ```
 * @property instantHealth The amount of health to be restored immediately.
 * @property effects A list of effects to apply to the player when this food is consumed.
 * @property custom A custom lambda that is run when the food is consumed.
 * @property
 */
data class FoodOptions(
    val type: FoodType,
    val consumeTime: Int,
    val nutrition: Int,
    val saturationModifier: Float,
    val instantHealth: Double = 0.0,
    val effects: List<PotionEffect>? = null,
    val custom: ((Player) -> Unit)? = null,
)

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