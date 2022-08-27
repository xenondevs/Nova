package xyz.xenondevs.nova.api.data

import org.bukkit.NamespacedKey

interface NamespacedId {
    
    /**
     * The namespace of this [NamespacedId]
     */
    val namespace: String
    
    /**
     * The name of this [NamespacedId]
     */
    val name: String
    
    /**
     * Creates a [NamespacedKey] with the [namespace] and [name] of this [NamespacedId]
     */
    fun toNamespacedKey(): NamespacedKey
    
}