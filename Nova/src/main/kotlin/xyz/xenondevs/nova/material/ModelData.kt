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
    
    fun createItemStack(localizedName: String, subId: Int = 0): ItemStack =
        createItemBuilder(localizedName, subId).get().apply { maxStackSize }
    
    fun createItemStack(subId: Int = 0): ItemStack =
        createItemStack("", subId)
    
    fun createItemBuilder(localizedName: String, subId: Int = 0): ItemBuilder =
        ItemBuilder(PacketItems.SERVER_SIDE_MATERIAL)
            .setLocalizedName(localizedName)
            .addModifier { modifyNBT(it, subId) }
    
    fun createItemBuilder(subId: Int = 0): ItemBuilder =
        createItemBuilder("", subId)
    
    fun createClientsideItemStack(localizedName: String, subId: Int = 0): ItemStack =
        createItemBuilder(localizedName, subId).get()
    
    fun createClientsideItemStack(subId: Int = 0): ItemStack =
        createClientsideItemBuilder("", subId).get()
    
    fun createClientsideItemBuilder(localizedName: String, subId: Int = 0): ItemBuilder =
        ItemBuilder(material)
            .setLocalizedName(localizedName)
            .setCustomModelData(dataArray[subId])
            .addModifier { modifyNBT(it, subId) }
    
    fun createClientsideItemBuilder(subId: Int = 0): ItemBuilder =
        createClientsideItemBuilder("", subId)
    
    private fun modifyNBT(itemStack: ItemStack, subId: Int): ItemStack {
        val novaCompound = CompoundTag()
        novaCompound.putString("id", id)
        novaCompound.putInt("subId", subId)
        novaCompound.putBoolean("isBlock", isBlock)
        
        val meta = itemStack.itemMeta!!
        meta.unhandledTags["nova"] = novaCompound
        itemStack.itemMeta = meta
        
        return itemStack
    }
    
}