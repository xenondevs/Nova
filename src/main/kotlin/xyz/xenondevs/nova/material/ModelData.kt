package xyz.xenondevs.nova.material

import de.studiocode.invui.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.data.setLocalizedName

class ModelData(val material: Material, val dataArray: IntArray) {
    
    val data: Int
        get() = dataArray[0]
    
    fun createItemStack(localizedName: String, dataIndex: Int = 0): ItemStack =
        createItemBuilder(localizedName, dataIndex).get().apply { maxStackSize }
    
    fun createItemStack(dataIndex: Int = 0): ItemStack =
        createItemStack("", dataIndex)
    
    fun createItemBuilder(localizedName: String, dataIndex: Int = 0): ItemBuilder =
        ItemBuilder(material)
            .setLocalizedName(localizedName)
            .setCustomModelData(dataArray[dataIndex]) as ItemBuilder
    
    fun createItemBuilder(dataIndex: Int = 0): ItemBuilder =
        createItemBuilder("", dataIndex)
    
}