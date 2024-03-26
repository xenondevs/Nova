package xyz.xenondevs.nova.data.serialization.configurate

import net.kyori.adventure.text.minimessage.MiniMessage
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.serialize.TypeSerializer
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.nova.item.CategorizedItem
import xyz.xenondevs.nova.item.ItemCategory
import xyz.xenondevs.nova.util.item.ItemUtils
import java.lang.reflect.Type

internal object ItemCategorySerializer : TypeSerializer<ItemCategory> {
    
    override fun deserialize(type: Type, node: ConfigurationNode): ItemCategory {
        val icon = node.node("icon").string ?: throw NoSuchElementException("Missing property 'icon'")
        val name = node.node("name").string ?: throw NoSuchElementException("Missing property 'name'")
        val items = node.node("items").get<List<String>>() ?: throw NoSuchElementException("Missing property 'items'")
        return ItemCategory(
            ItemUtils.getItemBuilder(icon, true).setDisplayName(MiniMessage.miniMessage().deserialize(name)),
            items.map { CategorizedItem(it) }
        )
    }
    
    override fun serialize(type: Type, obj: ItemCategory?, node: ConfigurationNode) {
        throw UnsupportedOperationException()
    }
    
}