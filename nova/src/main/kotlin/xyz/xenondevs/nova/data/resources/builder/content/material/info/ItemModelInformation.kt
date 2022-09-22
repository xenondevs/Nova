package xyz.xenondevs.nova.data.resources.builder.content.material.info

import org.bukkit.Material
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.util.contentEquals
import xyz.xenondevs.nova.util.enumMapOf

internal object VanillaMaterialTypes {
    
    private val MATERIAL_TYPES = enumMapOf(
        Material.FISHING_ROD to setOf(VanillaMaterialProperty.DAMAGEABLE),
        Material.WOODEN_SWORD to setOf(VanillaMaterialProperty.DAMAGEABLE, VanillaMaterialProperty.CREATIVE_NON_BLOCK_BREAKING),
        Material.APPLE to setOf(VanillaMaterialProperty.CONSUMABLE_NORMAL),
        Material.DRIED_KELP to setOf(VanillaMaterialProperty.CONSUMABLE_FAST),
        Material.GOLDEN_APPLE to setOf(VanillaMaterialProperty.CONSUMABLE_ALWAYS)
    )
    
    val DEFAULT_MATERIAL = Material.SHULKER_SHELL
    val MATERIALS = buildList { add(DEFAULT_MATERIAL); addAll(MATERIAL_TYPES.keys) }
    
    fun getMaterial(properties: Set<VanillaMaterialProperty>): Material {
        if (properties.isEmpty())
            return DEFAULT_MATERIAL
        
        return MATERIAL_TYPES.entries.firstOrNull { (_, materialProperties) -> materialProperties.contentEquals(properties) }?.key
            ?: throw IllegalArgumentException("No material type for property combination: $properties")
    }
    
}

internal class ItemModelInformation(
    override val id: String,
    override val models: List<String>,
    val material: Material? = null
) : ModelInformation {
    
    fun toBlockInfo() = BlockModelInformation(id, BlockModelType.DEFAULT, null, models, BlockDirection.values().toList(), 0)
    
}