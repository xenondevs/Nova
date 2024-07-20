package xyz.xenondevs.nova.api

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.api.block.NovaBlock
import xyz.xenondevs.nova.api.block.NovaBlockRegistry
import xyz.xenondevs.nova.api.data.NamespacedId
import xyz.xenondevs.nova.registry.NovaRegistries

internal object ApiBlockRegistry: NovaBlockRegistry {
    
    override fun getOrNull(id: String): NovaBlock? {
        val loc = ResourceLocation.parse(id)
        return NovaRegistries.BLOCK[loc]?.let(::ApiBlockWrapper)
    }
    
    override fun getOrNull(id: NamespacedId) = getOrNull(id.toString())
    override fun get(id: String) = getOrNull(id) ?: throw IllegalArgumentException("No block with id $id found!")
    override fun get(id: NamespacedId) = get(id.toString())
    
}