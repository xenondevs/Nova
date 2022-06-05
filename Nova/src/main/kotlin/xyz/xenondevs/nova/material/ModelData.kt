package xyz.xenondevs.nova.material

import de.studiocode.invui.item.builder.ItemBuilder
import net.md_5.bungee.api.chat.BaseComponent
import net.minecraft.nbt.CompoundTag
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.item.unhandledTags

class ModelData(val material: Material, val dataArray: IntArray, val id: String) {
    
    val data: Int
        get() = dataArray[0]
    
    fun createItemBuilder(subId: Int = 0): ItemBuilder =
        ItemBuilder(PacketItems.SERVER_SIDE_MATERIAL)
            .addModifier { modifyNBT(it, subId, false) }
    
    fun createClientsideItemBuilder(name: Array<BaseComponent>? = null, lore: List<Array<BaseComponent>>? = null, subId: Int = 0): ItemBuilder =
        ItemBuilder(material)
            .setDisplayName(*name ?: emptyArray())
            .setLore(lore)
            .setCustomModelData(dataArray[subId])
            .addModifier { modifyNBT(it, subId, true) }
    
    private fun modifyNBT(itemStack: ItemStack, subId: Int, clientside: Boolean): ItemStack {
        val novaCompound = CompoundTag()
        novaCompound.putString("id", id)
        novaCompound.putInt("subId", subId)
        if (clientside) novaCompound.putBoolean("clientside", true)
        
        val meta = itemStack.itemMeta!!
        meta.unhandledTags["nova"] = novaCompound
        itemStack.itemMeta = meta
        
        return itemStack
    }
    
}