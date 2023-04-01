package xyz.xenondevs.nova.util.item

import net.minecraft.world.item.Item
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.item.behavior.Fuel
import net.minecraft.world.item.ItemStack as MojangStack

val Material.burnTime: Int?
    get() = FuelUtils.getBurnTime(this)

val ItemStack.isFuel: Boolean
    get() = FuelUtils.isFuel(this)

val ItemStack.burnTime: Int?
    get() = FuelUtils.getBurnTime(this)

object FuelUtils {
    
    private val NMS_VANILLA_FUELS: Map<Item, Int> = AbstractFurnaceBlockEntity.getFuel()
    private val VANILLA_FUELS: Map<Material, Int> = NMS_VANILLA_FUELS
        .mapKeysTo(enumMap()) { (item, _) -> CraftMagicNumbers.getMaterial(item) }
    
    fun isFuel(material: Material): Boolean = material in VANILLA_FUELS
    fun getBurnTime(material: Material): Int? = VANILLA_FUELS[material]
    
    fun isFuel(itemStack: ItemStack): Boolean {
        val novaMaterial = itemStack.novaItem
        if (novaMaterial != null) {
            return novaMaterial.itemLogic.hasBehavior(Fuel::class)
        }
        
        return itemStack.type in VANILLA_FUELS
    }
    
    fun getBurnTime(itemStack: ItemStack): Int? {
        val novaMaterial = itemStack.novaItem
        if (novaMaterial != null) {
            return novaMaterial.itemLogic.getBehavior(Fuel::class)?.options?.burnTime
        }
        
        return getBurnTime(itemStack.type)
    }
    
    fun isFuel(itemStack: MojangStack): Boolean {
        val novaMaterial = itemStack.novaItem
        if (novaMaterial != null) {
            return novaMaterial.itemLogic.hasBehavior(Fuel::class)
        }
        
        
        return itemStack.item in NMS_VANILLA_FUELS
    }
    
    fun getBurnTime(itemStack: MojangStack): Int? {
        val novaMaterial = itemStack.novaItem
        if (novaMaterial != null) {
            return novaMaterial.itemLogic.getBehavior(Fuel::class)?.options?.burnTime
        }
        
        return NMS_VANILLA_FUELS[itemStack.item]
    }
    
}