package xyz.xenondevs.nova.data

import net.minecraft.resources.ResourceLocation
import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.api.data.NamespacedId as INamespacedId

fun NamespacedKey.toNamespacedId(): NamespacedId {
    return NamespacedId(namespace, key)
}

internal fun NamespacedId.toResourceLocation(): ResourceLocation {
    return ResourceLocation(namespace, name)
}

@Suppress("DEPRECATION")
class NamespacedId(override val namespace: String, override val name: String) : INamespacedId {
    
    private val id = "$namespace:$name"
    
    constructor(addon: Addon, name: String) : this(addon.description.id, name)
    
    constructor(name: String) : this("nova", name)
    
    init {
        require(namespace.matches(PART_PATTERN)) { "Namespace \"$namespace\" does not match pattern $PART_PATTERN" }
        require(name.matches(PART_PATTERN)) { "Name \"$name\" does not match pattern $PART_PATTERN" }
    }
    
    override fun toNamespacedKey(): NamespacedKey {
        return NamespacedKey(namespace, name)
    }
    
    fun toString(separator: String): String {
        return namespace + separator + name
    }
    
    override fun toString(): String {
        return id
    }
    
    override fun equals(other: Any?): Boolean {
        return other is NamespacedId && other.id == id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    companion object {
        
        val PART_PATTERN = Regex("""^[a-z][a-z\d_]*$""")
        val COMPLETE_PATTERN = Regex("""^[a-z][a-z\d_]*:[a-z][a-z\d_]*$""")
        
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
        
    }
    
}