package xyz.xenondevs.nova.material

import org.bukkit.Material
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.item.NovaItem

open class BlockNovaMaterial internal constructor(
    id: String,
    localizedName: String,
    novaItem: NovaItem? = null,
    val hitboxType: Material,
    val properties: List<BlockPropertyType<*>>
) : ItemNovaMaterial(id, localizedName, novaItem) {
    
    val block: ModelData by lazy { Resources.getModelData(id).second!! }
    
}