package xyz.xenondevs.nova.api

import net.minecraft.core.registries.BuiltInRegistries
import xyz.xenondevs.nova.api.block.NovaBlockRegistry
import xyz.xenondevs.nova.api.data.NamespacedId
import xyz.xenondevs.nova.util.getValue
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.api.block.NovaBlock as INovaBlock1

internal object ApiBlockRegistry : NovaBlockRegistry {
    
    override fun getOrNull(id: String): INovaBlock1? {
        return (BuiltInRegistries.BLOCK.getValue(id) as? NovaBlock)?.let(::ApiBlockWrapper)
    }
    
    override fun getOrNull(id: NamespacedId) = getOrNull(id.toString())
    override fun get(id: String) = getOrNull(id) ?: throw IllegalArgumentException("No block with id $id found!")
    override fun get(id: NamespacedId) = get(id.toString())
    
}