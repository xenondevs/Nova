package xyz.xenondevs.nova.world.item.behavior

import org.bukkit.Material
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.config.entryOrElse
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.unwrap
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

/**
 * Creates a factory for [Fuel] behaviors using the given values, if not specified otherwise in the item's config.
 *
 * @param burnTime The burn time of the fuel, in ticks.
 * Used when `burn_time` is not specified in the item's config, or `null` to require the presence of a config entry.
 */
@Suppress("FunctionName")
fun Fuel(
    burnTime: Int? = null
) = ItemBehaviorFactory<Fuel> {
    Fuel(it.config.entryOrElse(burnTime, "burn_time"))
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
    
    /**
     * Allows items to be used as fuel in furnaces.
     *
     * @param burnTime The burn time of this fuel, in ticks.
     */
    constructor(burnTime: Int) : this(provider(burnTime))
    
    override fun toString(itemStack: BukkitStack): String {
        return "Fuel(burnTime=$burnTime)"
    }
    
    companion object {
        
        /**
         * Checks if the given [Material] is a fuel item.
         */
        fun isFuel(material: Material): Boolean =
            isFuel(BukkitStack.of(material))
        
        /**
         * Gets the burn time of the given [Material] in ticks,
         * or 0 if the material is not a fuel item.
         */
        fun getBurnTime(material: Material): Int =
            getBurnTime(BukkitStack.of(material))
        
        /**
         * Checks if the given [BukkitStack] is a fuel item,
         * regardless of whether it is a Nova item or not.
         */
        fun isFuel(itemStack: BukkitStack): Boolean =
            isFuel(itemStack.unwrap())
        
        /**
         * Gets the burn time of the given [BukkitStack] in ticks,
         * regardless of whether it is a Nova item or not, 
         * or 0 if the item is not a fuel item.
         */
        fun getBurnTime(itemStack: BukkitStack): Int = 
            getBurnTime(itemStack.unwrap())
        
        /**
         * Checks if the given [MojangStack] is a fuel item,
         * regardless of whether it is a Nova item or not.
         */
        fun isFuel(itemStack: MojangStack): Boolean = 
            MINECRAFT_SERVER.fuelValues().isFuel(itemStack)
        
        /**
         * Gets the burn time of the given [MojangStack] in ticks,
         * regardless of whether it is a Nova item or not,
         * or 0 if the item is not a fuel item.
         */
        fun getBurnTime(itemStack: MojangStack): Int = 
            MINECRAFT_SERVER.fuelValues().burnDuration(itemStack)
        
    }
    
}