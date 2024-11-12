package xyz.xenondevs.nova.api

import net.minecraft.resources.ResourceLocation
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.data.NamespacedId
import xyz.xenondevs.nova.api.item.NovaItem
import xyz.xenondevs.nova.api.item.NovaItemRegistry
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.item.novaItem

internal object ApiItemRegistry : NovaItemRegistry {
    
    override fun getOrNull(id: String): NovaItem? {
        val loc = ResourceLocation.parse(id)
        return NovaRegistries.ITEM.getValue(loc)?.let(::ApiItemWrapper)
    }
    
    override fun getOrNull(id: NamespacedId): NovaItem? =
        getOrNull(id.toString())
    
    override fun getOrNull(itemStack: ItemStack): NovaItem? =
        itemStack.novaItem?.let(::ApiItemWrapper)
    
    override fun get(id: String): NovaItem =
        getOrNull(id) ?: throw IllegalArgumentException("No block with id $id found")
    
    override fun get(id: NamespacedId): NovaItem =
        get(id.toString())
    
    override fun get(itemStack: ItemStack): NovaItem =
        itemStack.novaItem?.let(::ApiItemWrapper) ?: throw IllegalArgumentException("ItemStack is not a Nova item")
    
    override fun getNonNamespaced(name: String): List<NovaItem> =
        NovaRegistries.ITEM.getByName(name).map(::ApiItemWrapper)
    
}