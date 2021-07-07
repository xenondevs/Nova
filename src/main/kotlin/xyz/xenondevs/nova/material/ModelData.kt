package xyz.xenondevs.nova.material

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.item.NovaItemBuilder

class ModelData(val material: Material, val dataArray: IntArray) {
    
    val data: Int
        get() = dataArray[0]
    
    fun getItem(localizedName: String, dataIndex: Int = 0): ItemStack =
        getItemBuilder(localizedName, dataIndex).build().apply { maxStackSize }
    
    fun getItem(dataIndex: Int = 0): ItemStack =
        getItem("", dataIndex)
    
    fun getItemBuilder(localizedName: String, dataIndex: Int = 0): NovaItemBuilder =
        NovaItemBuilder(material)
            .setLocalizedName(localizedName)
            .setCustomModelData(dataArray[dataIndex]) as NovaItemBuilder
    
    fun getItemBuilder(dataIndex: Int = 0): NovaItemBuilder =
        getItemBuilder("", dataIndex)
    
}