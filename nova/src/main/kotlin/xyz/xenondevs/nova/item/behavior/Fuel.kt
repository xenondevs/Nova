package xyz.xenondevs.nova.item.behavior

import net.minecraft.world.item.Item
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.data.config.entry
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.util.item.novaItem
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

fun Fuel(burnTime: Int) = Fuel.Default(provider(burnTime))

/**
 * Allows items to be used as fuel in furnaces.
 */
interface Fuel {
    
    /**
     * The burn time of this fuel, in ticks.
     */
    val burnTime: Int
    
    class Default(
        burnTime: Provider<Int>
    ): ItemBehavior, Fuel {
        override val burnTime by burnTime
    }
    
    companion object : ItemBehaviorFactory<Default> {
        
        private val NMS_VANILLA_FUELS: Map<Item, Int> = AbstractFurnaceBlockEntity.getFuel()
        private val VANILLA_FUELS: Map<Material, Int> = NMS_VANILLA_FUELS
            .mapKeysTo(enumMap()) { (item, _) -> CraftMagicNumbers.getMaterial(item) }
        
        override fun create(item: NovaItem): Default {
            return Default(item.config.entry<Int>("burn_time"))
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