package xyz.xenondevs.nova.api.item

import xyz.xenondevs.nova.api.data.NamespacedId

interface NovaItemRegistry {
    
    fun get(id: String): NovaItem
    
    fun get(id: NamespacedId): NovaItem
    
    fun getOrNull(id: String): NovaItem?
    
    fun getOrNull(id: NamespacedId): NovaItem?
    
    fun getNonNamespaced(name: String): List<NovaItem>
    
}