package xyz.xenondevs.nova.data.resources.builder.content.material.info

import org.bukkit.Material
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.util.contentEquals
import xyz.xenondevs.nova.util.enumMapOf

internal object VanillaMaterialTypes {
    
    private val MATERIAL_TYPES = enumMapOf(
        Material.WOODEN_PICKAXE to setOf(VanillaMaterialProperty.DAMAGEABLE),
        Material.WOODEN_SWORD to setOf(VanillaMaterialProperty.DAMAGEABLE, VanillaMaterialProperty.CREATIVE_NON_BLOCK_BREAKING),
        
        Material.APPLE to setOf(VanillaMaterialProperty.CONSUMABLE_NORMAL),
        Material.DRIED_KELP to setOf(VanillaMaterialProperty.CONSUMABLE_FAST),
        Material.GOLDEN_APPLE to setOf(VanillaMaterialProperty.CONSUMABLE_ALWAYS),
        
        Material.LEATHER_HELMET to setOf(VanillaMaterialProperty.DAMAGEABLE, VanillaMaterialProperty.HELMET),
        Material.LEATHER_CHESTPLATE to setOf(VanillaMaterialProperty.DAMAGEABLE, VanillaMaterialProperty.CHESTPLATE),
        Material.LEATHER_LEGGINGS to setOf(VanillaMaterialProperty.DAMAGEABLE, VanillaMaterialProperty.LEGGINGS),
        Material.LEATHER_BOOTS to setOf(VanillaMaterialProperty.DAMAGEABLE, VanillaMaterialProperty.BOOTS),
        
        Material.NETHERITE_INGOT to setOf(VanillaMaterialProperty.FIRE_RESISTANT),
        Material.NETHERITE_PICKAXE to setOf(VanillaMaterialProperty.FIRE_RESISTANT, VanillaMaterialProperty.DAMAGEABLE),
        Material.NETHERITE_SWORD to setOf(VanillaMaterialProperty.FIRE_RESISTANT, VanillaMaterialProperty.DAMAGEABLE, VanillaMaterialProperty.CREATIVE_NON_BLOCK_BREAKING),
    )
    
    val DEFAULT_MATERIAL = Material.SHULKER_SHELL
    val MATERIALS = buildList { add(DEFAULT_MATERIAL); addAll(MATERIAL_TYPES.keys) }
    
    fun getMaterial(properties: Set<VanillaMaterialProperty>): Material {
        if (properties.isEmpty())
            return DEFAULT_MATERIAL
        
        val material = MATERIAL_TYPES.entries.firstOrNull { (_, materialProperties) -> materialProperties.contentEquals(properties) }?.key // first, search for an exact match
            ?: MATERIAL_TYPES.entries.filter { (_, materialProperties) -> materialProperties.containsAll(properties) }.minByOrNull { it.value.size }?.key // then, search for a material that might bring more properties with it
        
        // if there is still no such material, remove the least important properties and search again
        if (material == null) {
            val lowestImportance = properties.minOf(VanillaMaterialProperty::importance)
            return getMaterial(properties.filterTo(HashSet()) { it.importance == lowestImportance })
        }
        
        return material
    }
    
}

internal class ItemModelInformation(
    override val id: NamespacedId,
    override val models: List<String>,
    val material: Material? = null
) : ModelInformation {
    
    fun toBlockInfo() = BlockModelInformation(id, BlockModelType.DEFAULT, null, models, BlockDirection.values().toList(), 0)
    
}