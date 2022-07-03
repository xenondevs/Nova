package xyz.xenondevs.nova.data.resources.model.data

import de.studiocode.invui.item.builder.ItemBuilder
import net.md_5.bungee.api.chat.BaseComponent
import net.minecraft.nbt.CompoundTag
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.PacketItems
import xyz.xenondevs.nova.util.data.withoutPreFormatting
import xyz.xenondevs.nova.util.item.unhandledTags

open class ItemModelData(val id: String, val material: Material, val dataArray: IntArray) {
    
    val data: Int
        get() = dataArray[0]
    
    fun createItemBuilder(subId: Int = 0): ItemBuilder =
        ItemBuilder(PacketItems.SERVER_SIDE_MATERIAL)
            .addModifier { modifyNBT(it, subId) }
    
    fun createClientsideItemBuilder(name: Array<BaseComponent>? = null, lore: List<Array<BaseComponent>>? = null, subId: Int = 0): ItemBuilder =
        ItemBuilder(material)
            .setDisplayName(*name ?: emptyArray())
            .setCustomModelData(dataArray[subId])
            .apply { lore?.forEach { addLoreLines(it.withoutPreFormatting()) } }
    
    private fun modifyNBT(itemStack: ItemStack, subId: Int): ItemStack {
        val novaCompound = CompoundTag()
        novaCompound.putString("id", id)
        novaCompound.putInt("subId", subId)
        
        val meta = itemStack.itemMeta!!
        meta.unhandledTags["nova"] = novaCompound
        itemStack.itemMeta = meta
        
        return itemStack
    }
    
}