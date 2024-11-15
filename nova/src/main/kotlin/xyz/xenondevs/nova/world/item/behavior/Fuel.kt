package xyz.xenondevs.nova.world.item.behavior

import it.unimi.dsi.fastutil.objects.Object2IntSortedMap
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.entity.FuelValues
import org.bukkit.Material
import org.bukkit.craftbukkit.util.CraftMagicNumbers
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.config.entryOrElse
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import xyz.xenondevs.nova.util.item.novaItem
import java.lang.invoke.MethodHandles
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

private val FUEL_VALUES_VALUES = MethodHandles.privateLookupIn(FuelValues::class.java, MethodHandles.lookup())
    .findGetter(FuelValues::class.java, "values", Object2IntSortedMap::class.java)

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
        
        @Suppress("UNCHECKED_CAST")
        private val NMS_VANILLA_FUELS: Map<Item, Int> by lazy {
            FUEL_VALUES_VALUES.invoke(MINECRAFT_SERVER.fuelValues()) as Map<Item, Int>
        }
        private val VANILLA_FUELS: Map<Material, Int> by lazy {
            NMS_VANILLA_FUELS.mapKeysTo(enumMap()) { (item, _) -> CraftMagicNumbers.getMaterial(item) }
        }
        
        fun isFuel(material: Material): Boolean = material in VANILLA_FUELS
        fun getBurnTime(material: Material): Int? = VANILLA_FUELS[material]
        
        fun isFuel(itemStack: BukkitStack): Boolean {
            val novaItem = itemStack.novaItem
            if (novaItem != null) {
                return novaItem.hasBehavior(Fuel::class)
            }
            
            return itemStack.type in VANILLA_FUELS
        }
        
        fun getBurnTime(itemStack: BukkitStack): Int? {
            val novaItem = itemStack.novaItem
            if (novaItem != null)
                return novaItem.getBehaviorOrNull(Fuel::class)?.burnTime
            
            return getBurnTime(itemStack.type)
        }
        
        fun isFuel(itemStack: MojangStack): Boolean {
            val novaItem = itemStack.novaItem
            if (novaItem != null)
                return novaItem.hasBehavior<Fuel>()
            
            return itemStack.item in NMS_VANILLA_FUELS
        }
        
        fun getBurnTime(itemStack: MojangStack): Int? {
            val novaItem = itemStack.novaItem
            if (novaItem != null) {
                return novaItem.getBehaviorOrNull(Fuel::class)?.burnTime
            }
            
            return NMS_VANILLA_FUELS[itemStack.item]
        }
        
    }
    
}