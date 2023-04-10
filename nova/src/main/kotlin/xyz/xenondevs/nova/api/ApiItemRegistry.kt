package xyz.xenondevs.nova.api

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.api.data.NamespacedId
import xyz.xenondevs.nova.api.item.NovaItem
import xyz.xenondevs.nova.api.item.NovaItemRegistry
import xyz.xenondevs.nova.registry.NovaRegistries

internal object ApiItemRegistry: NovaItemRegistry {
    
    override fun getOrNull(id: String): NovaItem? {
        val loc = ResourceLocation.of(id, ':')
        return NovaRegistries.ITEM[loc]?.let(::ApiItemWrapper)
    }
    
    override fun getOrNull(id: NamespacedId) = getOrNull(id.toString())
    override fun get(id: String) = getOrNull(id) ?: throw IllegalArgumentException("No block with id $id found!")
    override fun get(id: NamespacedId) = get(id.toString())
    override fun getNonNamespaced(name: String) = NovaRegistries.ITEM.getByName(name).map(::ApiItemWrapper)
    
}