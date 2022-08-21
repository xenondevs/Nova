package xyz.xenondevs.nova.data.resources

private val NAMESPACED_ENTRY = Regex("""^([a-z0-9._-]+):([a-z0-9/._-]+)$""")
private val NON_NAMESPACED_ENTRY = Regex("""^([a-z0-9/._-]+)$""")

internal data class ResourcePath(val namespace: String, val path: String) {
    
    private val id = "$namespace:$path"
    
    override fun equals(other: Any?): Boolean {
        return other is ResourcePath && other.id == id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return id
    }
    
    companion object {
        
        fun of(id: String, fallbackNamespace: String = "minecraft"): ResourcePath {
            return if (NON_NAMESPACED_ENTRY.matches(id)) {
                ResourcePath(fallbackNamespace, id)
            } else {
                val match = NAMESPACED_ENTRY.matchEntire(id)
                    ?: throw IllegalArgumentException("Invalid resource id: $id")
                
                ResourcePath(match.groupValues[1], match.groupValues[2])
            }
        }
        
    }
    
}