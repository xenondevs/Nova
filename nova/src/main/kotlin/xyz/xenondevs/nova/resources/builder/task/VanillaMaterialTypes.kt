package xyz.xenondevs.nova.resources.builder.task

import org.bukkit.Material
import xyz.xenondevs.commons.collections.contentEquals
import xyz.xenondevs.commons.collections.enumMapOf
import xyz.xenondevs.nova.world.item.vanilla.VanillaMaterialProperty

internal object VanillaMaterialTypes {
    
    private val MATERIAL_TYPES = enumMapOf(
        Material.LEATHER_HELMET to setOf(VanillaMaterialProperty.DYEABLE),
        Material.BUNDLE to setOf(VanillaMaterialProperty.BUNDLE)
    )
    
    val DEFAULT_MATERIAL = Material.SHULKER_SHELL
    val MATERIALS = buildList { add(DEFAULT_MATERIAL); addAll(MATERIAL_TYPES.keys) }
    
    fun getMaterial(properties: Set<VanillaMaterialProperty>): Material {
        if (properties.isEmpty())
            return DEFAULT_MATERIAL
        
        val material = MATERIAL_TYPES.entries.firstOrNull { (_, materialProperties) -> materialProperties.contentEquals(properties) }?.key // first, search for an exact match
            ?: MATERIAL_TYPES.entries.filter { (_, materialProperties) -> materialProperties.containsAll(properties) }.minByOrNull { it.value.size }?.key // then, search for a material that might bring more properties with it
        
        // conflicting vanilla material properties
        if (material == null)
            throw IllegalArgumentException("No vanilla material available for this combination of properties: $properties")
        
        return material
    }
    
}