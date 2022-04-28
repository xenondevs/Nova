package xyz.xenondevs.nova.material

import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.builder.MaterialType
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.behavior.Consumable

/**
 * @property consumeTime The time it takes for the food to be consumed, in ticks.
 * @property nutrition The nutrition value this food provides.
 * @property saturationModifier The saturation modifier this food provides. The saturation is calculated like this:
 * ```
 * saturation = min(saturation + nutrition * saturationModifier * 2.0f, foodLevel)
 * ```
 * @property instantHealth The amount of health to be restored immediately.
 * @property effects A list of effects to apply to the player when this food is consumed.
 * @property custom A custom lambda that is run when the food is consumed.
 * @property alwaysConsumable If the food can always be consumed, even if the player is not hungry.
 * For this to work properly clientside, [MaterialType.ALWAYS_CONSUMABLE] is required.
 * @property fast If the food eating sounds should start playing immediately, as this food is consumed fast.
 * For this to work properly clientside, [MaterialType.FAST_CONSUMABLE] is required.
 * @property
 */
data class FoodOptions(
    val consumeTime: Int,
    val nutrition: Int,
    val saturationModifier: Float,
    val instantHealth: Double = 0.0,
    val effects: List<PotionEffect>? = null,
    val custom: ((Player) -> Unit)? = null,
    val alwaysConsumable: Boolean = false,
    val fast: Boolean = false
)

class FoodNovaMaterial(
    id: NamespacedId,
    localizedName: String,
    val options: FoodOptions
) : ItemNovaMaterial(id, localizedName, NovaItem(Consumable(options)))