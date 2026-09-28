package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CookingFuel
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider
import org.bukkit.inventory.ItemStack as BukkitStack

/**
 * Creates a factory for [Fuel] behaviors using the given values, if not specified otherwise in the item's config.
 *
 * @param burnTime The burn time of the fuel, in ticks.
 * Defaults to `20` (1 second).
 * Used when `burn_time` is not specified in the item's config.
 */
@Suppress("FunctionName")
fun Fuel(
    burnTime: Int = 20
) = ItemBehaviorFactory { _, cfg ->
    Fuel(cfg.entry(burnTime, "burn_time"))
}

/**
 * Allows items to be used as fuel in furnaces.
 *
 * @param burnTime The burn time of this fuel, in ticks.
 */
class Fuel(burnTime: Provider<Int>) : ItemBehavior {
    
    /**
     * The burn time of this fuel, in ticks.
     */
    val burnTime: Int by burnTime
    
    override val baseDataComponents: Provider<DataComponentMap> = buildDataComponentMapProvider {
        this[DataComponents.COOKING_FUEL] = CookingFuel(
            ResolvableInt.Constant(this@Fuel.burnTime),
            ResolvableFloat.fromKey(ContextFloatProviders.COOKING_DEFAULT_SPEED_MULTIPLIER)
        )
    }
    
    override fun toString(itemStack: BukkitStack): String {
        return "Fuel(burnTime=$burnTime)"
    }
    
}
