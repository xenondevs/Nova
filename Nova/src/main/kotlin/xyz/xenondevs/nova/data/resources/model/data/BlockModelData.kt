package xyz.xenondevs.nova.data.resources.model.data

import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.ItemWrapper
import org.bukkit.Material
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.resources.model.config.BlockStateConfig
import xyz.xenondevs.nova.data.resources.model.config.BlockStateConfigType
import xyz.xenondevs.nova.util.data.LazyArray
import xyz.xenondevs.nova.world.block.model.ArmorStandModelProvider
import xyz.xenondevs.nova.world.block.model.BlockModelProviderType

sealed interface BlockModelData {
    val id: String
    val modelProviderType: BlockModelProviderType<*>
}

class ArmorStandBlockModelData(
    id: String,
    val hitboxType: Material,
    material: Material,
    dataArray: IntArray
) : ItemModelData(id, material, dataArray), BlockModelData {
    
    override val modelProviderType = ArmorStandModelProvider
    
    private val blockProviders: LazyArray<ItemProvider> by lazy {
        LazyArray(dataArray.size) { subId ->
            ItemWrapper(createClientsideItemBuilder(null, null, subId).get())
        }
    }
    
    operator fun get(index: Int): ItemProvider = blockProviders[index]
    
}

class SolidBlockModelData<T : BlockStateConfig>(
    val type: BlockStateConfigType<T>,
    override val id: String,
    val data: Map<BlockFace, List<T>>
) : BlockModelData {
    
    override val modelProviderType = type.modelProvider
    
    operator fun get(face: BlockFace, index: Int): T = data[face]!![index]
    
    operator fun get(index: Int): T = data.values.first()[index]
    
}
