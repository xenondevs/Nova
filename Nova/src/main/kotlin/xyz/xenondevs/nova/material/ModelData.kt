package xyz.xenondevs.nova.material

import de.studiocode.invui.item.builder.ItemBuilder
import net.minecraft.nbt.CompoundTag
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.data.setLocalizedName
import xyz.xenondevs.nova.util.unhandledTags

class ModelData(val material: Material, val dataArray: IntArray, val id: String, val isBlock: Boolean) {
    
    val data: Int
        get() = dataArray[0]
    
    fun createItemStack(localizedName: String, dataIndex: Int = 0): ItemStack =
        createItemBuilder(localizedName, dataIndex).get().apply { maxStackSize }
    
    fun createItemStack(dataIndex: Int = 0): ItemStack =
        createItemStack("", dataIndex)
    
    fun createItemBuilder(localizedName: String, subId: Int = 0): ItemBuilder =
        ItemBuilder(PacketItems.SERVER_SIDE_MATERIAL)
            .setLocalizedName(localizedName)
            .addModifier {
                val novaCompound = CompoundTag()
                novaCompound.putString("id", id)
                novaCompound.putInt("subId", subId)
                novaCompound.putBoolean("isBlock", isBlock)
                
                val meta = it.itemMeta!!
                meta.unhandledTags["nova"] = novaCompound
                it.itemMeta = meta
                return@addModifier it
            }
    
    fun createItemBuilder(dataIndex: Int = 0): ItemBuilder =
        createItemBuilder("", dataIndex)
    
}