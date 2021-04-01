package xyz.xenondevs.nova.material

import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.impl.CoalGenerator
import xyz.xenondevs.nova.util.toIntArray
import java.util.*

private fun itemOf(vararg data: Int) = ModelData(STRUCTURE_VOID, *data)

enum class NovaMaterial(
    val itemName: String,
    val item: ModelData,
    val block: ModelData?, // should only be different from item if it actually needs to be a different material because of minecraft's restrictions
    val hitbox: Material?,
    val tileEntityConstructor: ((NovaMaterial, ArmorStand) -> TileEntity)?
) {
    
    COAL_GENERATOR("Coal Generator", itemOf(1), itemOf(1), COBBLESTONE, ::CoalGenerator),
    PROGRESS_ARROW("", itemOf(*(10_000..10_016).toIntArray())),
    ENERGY_PROGRESS("", itemOf(*(10_100..10_116).toIntArray())),
    ENERGY_BAR("", itemOf(*(10_200..10_216).toIntArray()));
    
    val isBlock = block != null && hitbox != null && tileEntityConstructor != null
    
    constructor(itemName: String, item: ModelData) : this(itemName, item, null, null, null)
    
    fun createItemStack() = item.getItem(itemName)
    
    fun createItemBuilder() = item.getItemBuilder(itemName)
    
    companion object {
        
        fun toNovaMaterial(itemStack: ItemStack): NovaMaterial? =
            values().find {
                it.item.material == itemStack.type
                    && it.item.dataArray.contains(itemStack.itemMeta?.customModelData ?: -1)
            }
        
    }
    
}