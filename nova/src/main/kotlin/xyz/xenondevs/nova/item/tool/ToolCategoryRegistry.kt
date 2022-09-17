package xyz.xenondevs.nova.item.tool

import org.bukkit.Material
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.item.behavior.Tool
import xyz.xenondevs.nova.util.item.novaMaterial

object ToolCategoryRegistry {
    
    private val _categories = HashMap<NamespacedId, ToolCategory>()
    val categories: Collection<ToolCategory>
        get() = _categories.values
    
    internal fun register(
        name: String,
        multipliers: Map<Material, Double>
    ): ToolCategory = register(name, multipliers) { ResourcePath("minecraft", "item/" + (it?.id?.name ?: "wooden") + "_" + name) }
    
    internal fun register(
        name: String,
        multipliers: Map<Material, Double>,
        getIcon: (ToolLevel?) -> ResourcePath
    ): ToolCategory {
        val id = NamespacedId("minecraft", name)
        check(id !in _categories) { "A ToolCategory is already registered under that id." }
        
        val category = ToolCategory(id, { multipliers[it.type] ?: 0.0 }, getIcon)
        _categories[id] = category
        return category
    }
    
    fun register(addon: Addon, name: String, getIcon: (ToolLevel?) -> ResourcePath): ToolCategory {
        val id = NamespacedId(addon, name)
        check(id !in _categories) { "A ToolCategory is already registered under that id." }
        
        val category = ToolCategory(
            id,
            { it.novaMaterial?.novaItem?.getBehavior(Tool::class)?.options?.speedMultiplier ?: 0.0 },
            getIcon
        )
        
        _categories[id] = category
        return category
    }
    
    fun of(id: NamespacedId): ToolCategory? = _categories[id]
    
}