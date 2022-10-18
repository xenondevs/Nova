package xyz.xenondevs.nova.item.tool

import org.bukkit.Material
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.ResourcePath

object ToolCategoryRegistry {
    
    private val _categories = HashMap<NamespacedId, ToolCategory>()
    val categories: Collection<ToolCategory>
        get() = _categories.values
    
    internal fun registerVanilla(
        name: String,
        canDoSweepAttack: Boolean, canBreakBlocksInCreative: Boolean,
        itemDamageOnAttackEntity: Int, itemDamageOnBreakBlock: Int,
        multipliers: Map<Material, Double>,
        getIcon: ((ToolLevel?) -> ResourcePath)? = null
    ): VanillaToolCategory {
        val id = NamespacedId("minecraft", name)
        check(id !in _categories) { "A ToolCategory is already registered under that id." }
        
        val category = VanillaToolCategory(
            id,
            canDoSweepAttack, canBreakBlocksInCreative,
            itemDamageOnAttackEntity, itemDamageOnBreakBlock,
            multipliers,
            getIcon ?: {
                if (it != null)
                    ResourcePath(it.id.namespace, "item/${it.id.name}_$name")
                else ResourcePath("minecraft", "item/wooden_$name")
            }
        )
        
        _categories[id] = category
        return category
    }
    
    fun register(
        addon: Addon, name: String,
        getIcon: (ToolLevel?) -> ResourcePath
    ): ToolCategory {
        val id = NamespacedId(addon, name)
        check(id !in _categories) { "A ToolCategory is already registered under that id." }
        
        val category = ToolCategory(id, getIcon)
        
        _categories[id] = category
        return category
    }
    
    fun of(id: NamespacedId): ToolCategory? = _categories[id]
    
    fun of(name: String): ToolCategory? = _categories[NamespacedId.of(name, "minecraft")]
    
}