package xyz.xenondevs.nova.serialization.configurate

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.nova.util.data.get
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.clientsideCopy
import xyz.xenondevs.nova.world.item.CategorizedItem
import xyz.xenondevs.nova.world.item.ItemCategory
import java.lang.reflect.Type

internal object ItemCategorySerializer : TypeSerializer<ItemCategory> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): ItemCategory {
        val iconId = node.node("icon").string ?: throw NoSuchElementException("Missing property 'icon'")
        val name = node.node("name").string ?: throw NoSuchElementException("Missing property 'name'")
        val items = node.node("items").get<List<String>>() ?: throw NoSuchElementException("Missing property 'items'")
        
        val icon = ItemBuilder(ItemUtils.getItemStack(iconId))
        icon.setName(name)
        icon.clearLore()
        icon.setLore(emptyList())
        
        return ItemCategory(ItemWrapper(icon.get().clientsideCopy()), items.map { CategorizedItem(it) })
    }
    
    override fun serialize(type: Type, obj: ItemCategory?, node: ConfigurationNode) {
        throw UnsupportedOperationException()
    }
    
}