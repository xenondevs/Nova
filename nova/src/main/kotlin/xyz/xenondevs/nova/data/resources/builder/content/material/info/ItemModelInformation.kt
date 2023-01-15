package xyz.xenondevs.nova.data.resources.builder.content.material.info

import org.bukkit.Material
import xyz.xenondevs.nova.data.NamespacedId

internal class ItemModelInformation(
    override val id: NamespacedId,
    override val models: List<String>,
    val material: Material? = null
) : ModelInformation {
    
    fun toBlockInfo() = BlockModelInformation(id, BlockModelType.DEFAULT, null, models, BlockDirection.values().toList(), 0)
    
}