package xyz.xenondevs.nova.data.resources.builder.task.material.info

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material

internal class ItemModelInformation(
    override val id: ResourceLocation,
    override val models: List<String>,
    val material: Material? = null
) : ModelInformation {
    
    fun toBlockInfo() = BlockModelInformation(id, BlockModelType.DEFAULT, null, models, BlockDirection.values().toList(), 0)
    
}