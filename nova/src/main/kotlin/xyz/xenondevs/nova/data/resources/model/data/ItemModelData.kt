package xyz.xenondevs.nova.data.resources.model.data

import net.kyori.adventure.text.Component
import net.minecraft.nbt.CompoundTag
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.builder.setLore
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.material.PacketItems
import xyz.xenondevs.nova.util.item.unhandledTags

open class ItemModelData(val id: NamespacedId, val material: Material, val dataArray: IntArray) {
    
    val data: Int
        get() = dataArray[0]
    
    fun createItemBuilder(modelId: Int = 0): ItemBuilder =
        ItemBuilder(PacketItems.SERVER_SIDE_MATERIAL)
            .addModifier { modifyNbt(it, modelId) }
    
    fun createClientsideItemBuilder(name: Component? = null, lore: List<Component>? = null, modelId: Int = 0): ItemBuilder =
        ItemBuilder(material)
            .setDisplayName(name ?: Component.empty())
            .setCustomModelData(dataArray[modelId])
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .apply { if (lore != null) setLore(lore) }
    
    private fun modifyNbt(itemStack: ItemStack, modelId: Int): ItemStack {
        val novaCompound = CompoundTag()
        novaCompound.putString("id", id.toString())
        novaCompound.putInt("subId", modelId)
        
        val meta = itemStack.itemMeta!!
        meta.unhandledTags["nova"] = novaCompound
        itemStack.itemMeta = meta
        
        return itemStack
    }
    
}