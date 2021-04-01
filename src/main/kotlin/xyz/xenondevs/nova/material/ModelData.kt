package xyz.xenondevs.nova.material

import de.studiocode.invui.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class ModelData(val material: Material, val dataArray: IntArray) {
    
    val data: Int
        get() = dataArray[0]
    
    fun getItem(name: String, dataIndex: Int = 0): ItemStack =
        getItemBuilder(name, dataIndex).build()
    
    fun getItem(dataIndex: Int = 0): ItemStack =
        getItem("", dataIndex)
    
    fun getItemBuilder(name: String, dataIndex: Int = 0): ItemBuilder =
        ItemBuilder(material)
            .setDisplayName("§r$name : ${dataArray[dataIndex]}")
            .setCustomModelData(dataArray[dataIndex])
    
    fun getItemBuilder(dataIndex: Int = 0): ItemBuilder =
        getItemBuilder("", dataIndex)
    
}