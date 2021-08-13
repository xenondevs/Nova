package xyz.xenondevs.nova.material

import de.studiocode.invui.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.data.setLocalizedName

class ModelData(val material: Material, val dataArray: IntArray) {
    
    val data: Int
        get() = dataArray[0]
    
    fun getItem(localizedName: String, dataIndex: Int = 0): ItemStack =
        getItemBuilder(localizedName, dataIndex).get().apply { maxStackSize }
    
    fun getItem(dataIndex: Int = 0): ItemStack =
        getItem("", dataIndex)
    
    fun getItemBuilder(localizedName: String, dataIndex: Int = 0): ItemBuilder =
        ItemBuilder(material)
            .setLocalizedName(localizedName)
            .setCustomModelData(dataArray[dataIndex]) as ItemBuilder
    
    fun getItemBuilder(dataIndex: Int = 0): ItemBuilder =
        getItemBuilder("", dataIndex)
    
}