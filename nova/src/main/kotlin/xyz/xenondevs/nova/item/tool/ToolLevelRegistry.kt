package xyz.xenondevs.nova.item.tool

import xyz.xenondevs.nova.data.NamespacedId

object ToolLevelRegistry {
    
    private val _levels = HashMap<NamespacedId, ToolLevel>()
    val level: Collection<ToolLevel>
        get() = _levels.values
    
    internal fun register(
        name: String
    ): ToolLevel {
        val id = NamespacedId("minecraft", name)
        check(id !in _levels) { "A ToolLevel is already registered under that id." }
        
        val level = ToolLevel(id)
        _levels[id] = level
        return level
    }
    
    fun of(id: NamespacedId): ToolLevel? = _levels[id]
    
}