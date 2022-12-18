package xyz.xenondevs.nova.item.tool

import org.bukkit.Material
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.util.associateWithNotNullTo
import xyz.xenondevs.nova.util.enumMapOf
import java.util.function.Predicate

object ToolCategoryRegistry {
    
    private val _categories = HashMap<NamespacedId, ToolCategory>()
    val categories: Collection<ToolCategory>
        get() = _categories.values
    
    @JvmName("registerVanilla1")
    internal fun registerVanilla(
        name: String,
        canDoSweepAttack: Boolean, canBreakBlocksInCreative: Boolean,
        itemDamageOnAttackEntity: Int, itemDamageOnBreakBlock: Int,
        genericMultipliers: Map<Material, Double>,
        getIcon: ((ToolTier?) -> ResourcePath)? = null
    ): VanillaToolCategory {
        return registerVanilla(
            name,
            canDoSweepAttack, canBreakBlocksInCreative,
            itemDamageOnAttackEntity, itemDamageOnBreakBlock,
            genericMultipliers,
            emptyMap<Material, Map<Material, Double>>(),
            getIcon
        )
    }
    
    @JvmName("registerVanilla1")
    internal fun registerVanilla(
        name: String,
        canDoSweepAttack: Boolean, canBreakBlocksInCreative: Boolean,
        itemDamageOnAttackEntity: Int, itemDamageOnBreakBlock: Int,
        genericMultipliers: Map<Material, Double>,
        specialMultipliers: Map<Material, Map<Predicate<Material>, Double>>,
        getIcon: ((ToolTier?) -> ResourcePath)? = null
    ): VanillaToolCategory {
        val flatSpecialMultipliers = specialMultipliers.mapValuesTo(enumMapOf()) { (_, map) ->
            Material.values().associateWithNotNullTo(enumMapOf()) { material ->
                map.entries.firstOrNull { it.key.test(material) }?.value
            }
        }
        
        return registerVanilla(
            name,
            canDoSweepAttack, canBreakBlocksInCreative,
            itemDamageOnAttackEntity, itemDamageOnBreakBlock,
            genericMultipliers,
            flatSpecialMultipliers,
            getIcon
        )
    }
    
    internal fun registerVanilla(
        name: String,
        canDoSweepAttack: Boolean, canBreakBlocksInCreative: Boolean,
        itemDamageOnAttackEntity: Int, itemDamageOnBreakBlock: Int,
        genericMultipliers: Map<Material, Double>,
        specialMultipliers: Map<Material, Map<Material, Double>>,
        getIcon: ((ToolTier?) -> ResourcePath)? = null
    ): VanillaToolCategory {
        val id = NamespacedId("minecraft", name)
        check(id !in _categories) { "A ToolCategory is already registered under that id." }
        
        val category = VanillaToolCategory(
            id,
            canDoSweepAttack, canBreakBlocksInCreative,
            itemDamageOnAttackEntity, itemDamageOnBreakBlock,
            genericMultipliers,
            specialMultipliers,
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
        getIcon: (ToolTier?) -> ResourcePath
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