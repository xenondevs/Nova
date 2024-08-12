package xyz.xenondevs.nova.serialization.configurate

import net.kyori.adventure.text.minimessage.MiniMessage
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.nova.util.data.get
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.world.item.CategorizedItem
import xyz.xenondevs.nova.world.item.ItemCategory
import java.lang.reflect.Type

internal object ItemCategorySerializer : TypeSerializer<ItemCategory> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): ItemCategory {
        val iconId = node.node("icon").string ?: throw NoSuchElementException("Missing property 'icon'")
        val name = node.node("name").string ?: throw NoSuchElementException("Missing property 'name'")
        val items = node.node("items").get<List<String>>() ?: throw NoSuchElementException("Missing property 'items'")
        
        val iconItemStack = ItemUtils.getItemStack(iconId)
        val icon = iconItemStack.novaItem?.model?.createClientsideItemBuilder() ?: ItemBuilder(iconItemStack)
        icon.setDisplayName(MiniMessage.miniMessage().deserialize(name))
        icon.setLore(emptyList())
        
        return ItemCategory(icon, items.map { CategorizedItem(it) })
    }
    
    override fun serialize(type: Type, obj: ItemCategory?, node: ConfigurationNode) {
        throw UnsupportedOperationException()
    }
    
}