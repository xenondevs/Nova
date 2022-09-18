package xyz.xenondevs.nova.item.tool

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable

object ToolLevelRegistry {
    
    private val _levels = HashMap<NamespacedId, ToolLevel>()
    val levels: Collection<ToolLevel>
        get() = _levels.values
    
    internal fun register(id: NamespacedId): ToolLevel {
        check(id !in _levels) { "A ToolLevel is already registered under that id." }
        
        val level = ToolLevel(id, configReloadable {
            val namespace = if (id.namespace == "minecraft") "nova" else id.namespace
            NovaConfig["$namespace:tool_levels"].getDouble(id.name)
        })
        _levels[id] = level
        return level
    }
    
    internal fun register(id: String): ToolLevel =
        register(NamespacedId.of(id, "minecraft"))
    
    fun register(addon: Addon, name: String): ToolLevel =
        register(NamespacedId(addon, name))
    
    fun of(id: NamespacedId): ToolLevel? = _levels[id]
    
}