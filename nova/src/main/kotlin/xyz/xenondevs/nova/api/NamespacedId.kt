@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.api

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import net.minecraft.resources.ResourceLocation
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.util.data.asDataResult
import xyz.xenondevs.nova.api.data.NamespacedId as INamespacedId

internal val NamespacedKey.namespacedId: NamespacedId
    get() = NamespacedId(namespace, key)

@Deprecated("Use namespacedId property", ReplaceWith("namespacedId"))
internal fun NamespacedKey.toNamespacedId() = namespacedId

internal operator fun <T> MutableMap<NamespacedId, T>.set(namespace: String, key: String, value: T) {
    this[NamespacedId(namespace, key)] = value
}

internal operator fun <T> MutableMap<NamespacedId, T>.get(namespace: String, key: String): T? {
    return this[NamespacedId(namespace, key)]
}

internal fun NamespacedId.toResourceLocation(): ResourceLocation {
    return ResourceLocation.fromNamespaceAndPath(namespace, name)
}

@Suppress("DEPRECATION")
@Deprecated("Use ResourceLocation instead")
internal class NamespacedId(@JvmField val namespace: String, @JvmField val name: String) : INamespacedId {
    
    private val id = "$namespace:$name"
    
    val namespacedKey: NamespacedKey
        get() = NamespacedKey(namespace, name)
    val resourceLocation: ResourceLocation
        get() = ResourceLocation.fromNamespaceAndPath(namespace, name)
    
    constructor(plugin: Plugin, name: String) : this(plugin.name.lowercase(), name)
    constructor(addon: Addon, name: String) : this(addon.id, name)
    constructor(name: String) : this("nova", name)
    
    init {
        require(namespace.matches(PART_PATTERN)) { "Namespace \"$namespace\" does not match pattern $PART_PATTERN" }
        require(name.matches(PART_PATTERN)) { "Name \"$name\" does not match pattern $PART_PATTERN" }
    }
    
    @Deprecated("Use namespacedKey property", ReplaceWith("namespacedKey"))
    override fun toNamespacedKey() = namespacedKey
    
    fun toString(separator: String): String = namespace + separator + name
    
    override fun toString(): String = id
    override fun getNamespace(): String = namespace
    override fun getName(): String = name
    
    override fun equals(other: Any?): Boolean {
        return other is NamespacedId && other.id == id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    companion object {
        
        val CODEC: Codec<NamespacedId> = Codec.STRING.comapFlatMap(::ofSafe, NamespacedId::toString).stable()
        
        val PART_PATTERN = Regex("""^[a-z][a-z\d_-]*$""")
        val COMPLETE_PATTERN = Regex("""^[a-z][a-z\d_-]*:[a-z][a-z\d_-]*$""")
        
        fun of(id: String, fallbackNamespace: String? = null): NamespacedId {
            val namespace: String
            val name: String
            
            if (id.matches(COMPLETE_PATTERN)) {
                val parts = id.split(':')
                namespace = parts[0]
                name = parts[1]
            } else if (id.matches(PART_PATTERN) && fallbackNamespace != null) {
                namespace = fallbackNamespace
                name = id
            } else {
                throw IllegalArgumentException("Namespaced id \"$id\" does not match pattern $COMPLETE_PATTERN")
            }
            
            return NamespacedId(namespace, name)
        }
        
        private fun ofSafe(id: String): DataResult<NamespacedId> = runCatching { of(id) }.asDataResult()
        
    }
    
}