package xyz.xenondevs.nova.material

import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.impl.Cable
import xyz.xenondevs.nova.tileentity.impl.FurnaceGenerator
import xyz.xenondevs.nova.tileentity.impl.MechanicalPress
import xyz.xenondevs.nova.tileentity.impl.PowerCell
import xyz.xenondevs.nova.util.toIntArray

private fun itemOf(data: IntArray) = ModelData(STRUCTURE_VOID, data)

private fun itemOf(data: Int) = ModelData(STRUCTURE_VOID, intArrayOf(data))

enum class NovaMaterial(
    val itemName: String,
    val item: ModelData,
    val block: ModelData?, // should only be different from item if it actually needs to be a different material because of minecraft's restrictions
    val hitbox: Material?,
    val tileEntityConstructor: ((NovaMaterial, ArmorStand) -> TileEntity)?
) {
    
    // 1 - 1000: Blocks
    FURNACE_GENERATOR("Furnace Generator", itemOf(1), itemOf(1), COBBLESTONE, ::FurnaceGenerator),
    POWER_CELL("Power Cell", itemOf(2), itemOf(2), IRON_BLOCK, ::PowerCell),
    MECHANICAL_PRESS("Mechanical Press", itemOf(3), itemOf(3), IRON_BLOCK, ::MechanicalPress),
    
    // 1000 - 2000: Crafting Items
    IRON_PLATE("Iron Plate", itemOf(1000)),
    GOLD_PLATE("Gold Plate", itemOf(1001)),
    DIAMOND_PLATE("Diamond Plate", itemOf(1002)),
    NETHERITE_PLATE("Netherite Plate", itemOf(1003)),
    EMERALD_PLATE("Emerald Plate", itemOf(1004)),
    REDSTONE_PLATE("Redstone Plate", itemOf(1005)),
    LAPIS_PLATE("Lapis Plate", itemOf(1006)),
    IRON_GEAR("Iron Gear", itemOf(1010)),
    GOLD_GEAR("Gold Gear", itemOf(1011)),
    DIAMOND_GEAR("Diamond Gear", itemOf(1012)),
    NETHERITE_GEAR("Netherite Gear", itemOf(1013)),
    EMERALD_GEAR("Emerald Gear", itemOf(1014)),
    REDSTONE_GEAR("Redstone Gear", itemOf(1015)),
    LAPIS_GEAR("Lapis Gear", itemOf(1016)),
    
    // 2000 - 3000: Upgrades and similar
    
    // 5000 - 10.000 MultiModel Blocks
    CABLE("Cable", itemOf(5000), itemOf(intArrayOf(0) + (5000..5003).toIntArray()), null, ::Cable),
    
    // 9.000 - 10.000 UI Elements
    SIDE_CONFIG_BUTTON("", itemOf(9000)),
    GRAY_BUTTON("", itemOf(9001)),
    ORANGE_BUTTON("", itemOf(9002)),
    BLUE_BUTTON("", itemOf(9003)),
    GREEN_BUTTON("", itemOf(9004)),
    PLATE_ON_BUTTON("", itemOf(9005)),
    PLATE_OFF_BUTTON("", itemOf(9006)),
    GEAR_ON_BUTTON("", itemOf(9007)),
    GEAR_OFF_BUTTON("", itemOf(9008)),
    
    // 10.000 - ? Multi-Texture UI Elements
    PROGRESS_ARROW("", itemOf((10_000..10_016).toIntArray())),
    ENERGY_PROGRESS("", itemOf((10_100..10_116).toIntArray())),
    RED_BAR("", itemOf((10_200..10_216).toIntArray())),
    GREEN_BAR("", itemOf((10_300..10_316).toIntArray())),
    BLUE_BAR("", itemOf((10_400..10_416).toIntArray())),
    PRESS_PROGRESS("", itemOf((10_500..10_508).toIntArray()));
    
    
    val isBlock = block != null && tileEntityConstructor != null
    
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