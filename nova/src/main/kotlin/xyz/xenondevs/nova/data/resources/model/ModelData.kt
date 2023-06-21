package xyz.xenondevs.nova.data.resources.model

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import xyz.xenondevs.nova.data.resources.model.data.BlockModelData
import xyz.xenondevs.nova.data.resources.model.data.ItemModelData

data class ModelData(
    val item: Map<Material, ItemModelData>? = null,
    val block: BlockModelData? = null,
    val armor: ResourceLocation? = null
)