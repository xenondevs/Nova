package xyz.xenondevs.nova.api.block

import xyz.xenondevs.nova.api.data.NamespacedId

interface NovaBlockRegistry {
    
    fun get(id: String): NovaBlock
    
    fun get(id: NamespacedId): NovaBlock
    
    fun getOrNull(id: String): NovaBlock?
    
    fun getOrNull(id: NamespacedId): NovaBlock?
    
}