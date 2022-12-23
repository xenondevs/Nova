package xyz.xenondevs.nova.item.tool

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable

object ToolTierRegistry {
    
    private val _tiers = HashMap<NamespacedId, ToolTier>()
    val tiers: Collection<ToolTier>
        get() = _tiers.values
    
    internal fun register(id: NamespacedId): ToolTier {
        check(id !in _tiers) { "A ToolLevel is already registered under that id." }
        
        val level = ToolTier(id, configReloadable {
            val namespace = if (id.namespace == "minecraft") "nova" else id.namespace
            NovaConfig["$namespace:tool_levels"].getDouble(id.name)
        })
        _tiers[id] = level
        return level
    }
    
    internal fun register(id: String): ToolTier =
        register(NamespacedId.of(id, "minecraft"))
    
    fun register(addon: Addon, name: String): ToolTier =
        register(NamespacedId(addon, name))
    
    fun of(id: NamespacedId): ToolTier? = _tiers[id]
    
    fun of(name: String): ToolTier? = _tiers[NamespacedId.of(name, "minecraft")]
    
}