package xyz.xenondevs.nova.api

import net.minecraft.core.registries.BuiltInRegistries
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.data.NamespacedId
import xyz.xenondevs.nova.api.item.NovaItemRegistry
import xyz.xenondevs.nova.util.getValue
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.novaItem
import xyz.xenondevs.nova.api.item.NovaItem as INovaItem

internal object ApiItemRegistry : NovaItemRegistry {
    
    override fun getOrNull(id: String): INovaItem? {
        return (BuiltInRegistries.ITEM.getValue(id) as? NovaItem)?.let(::ApiItemWrapper)
    }
    
    override fun getOrNull(id: NamespacedId): INovaItem? =
        getOrNull(id.toString())
    
    override fun getOrNull(itemStack: ItemStack): INovaItem? =
        itemStack.novaItem?.let(::ApiItemWrapper)
    
    override fun get(id: String): INovaItem =
        getOrNull(id) ?: throw IllegalArgumentException("No block with id $id found")
    
    override fun get(id: NamespacedId): INovaItem =
        get(id.toString())
    
    override fun get(itemStack: ItemStack): INovaItem =
        itemStack.novaItem?.let(::ApiItemWrapper) ?: throw IllegalArgumentException("ItemStack is not a Nova item")
    
    override fun getNonNamespaced(name: String): List<INovaItem> = BuiltInRegistries.ITEM.entrySet()
        .filter { [key, item] -> item is NovaItem && key.identifier().path == name }
        .map { [_, item] -> ApiItemWrapper(item as NovaItem) }
    
}