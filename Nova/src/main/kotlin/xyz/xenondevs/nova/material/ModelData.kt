package xyz.xenondevs.nova.material

import de.studiocode.invui.item.builder.ItemBuilder
import de.studiocode.invui.util.ComponentUtils
import net.md_5.bungee.api.chat.TranslatableComponent
import net.md_5.bungee.chat.ComponentSerializer
import net.minecraft.nbt.CompoundTag
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.data.setLocalizedName
import xyz.xenondevs.nova.util.item.unhandledTags

class ModelData(val material: Material, val dataArray: IntArray, val id: String, val isBlock: Boolean) {
    
    val data: Int
        get() = dataArray[0]
    
    fun createItemStack(localizedName: String, subId: Int = 0): ItemStack =
        createItemBuilder(localizedName, subId).get().apply { maxStackSize }
    
    fun createItemStack(subId: Int = 0): ItemStack =
        createItemStack("", subId)
    
    fun createItemBuilder(localizedName: String, subId: Int = 0): ItemBuilder =
        ItemBuilder(PacketItems.SERVER_SIDE_MATERIAL)
            .addModifier { modifyNBT(it, localizedName, subId) }
    
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
            .addModifier { modifyNBT(it, localizedName, subId) }
    
    private fun modifyNBT(itemStack: ItemStack, localizedName: String, subId: Int): ItemStack {
        val novaCompound = CompoundTag()
        novaCompound.putString("id", id)
        novaCompound.putInt("subId", subId)
        novaCompound.putBoolean("isBlock", isBlock)
        novaCompound.putString("name", ComponentSerializer.toString(ComponentUtils.withoutPreFormatting(TranslatableComponent(localizedName))))
        
        val meta = itemStack.itemMeta!!
        meta.unhandledTags["nova"] = novaCompound
        itemStack.itemMeta = meta
        
        return itemStack
    }
    
}