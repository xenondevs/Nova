package xyz.xenondevs.nova.data.resources.model.data

import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.ItemWrapper
import org.bukkit.Material
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.resources.builder.content.material.info.VanillaMaterialTypes
import xyz.xenondevs.nova.data.resources.model.blockstate.BlockStateConfig
import xyz.xenondevs.nova.util.data.LazyArray
import xyz.xenondevs.nova.world.block.model.ArmorStandModelProvider
import xyz.xenondevs.nova.world.block.model.BlockModelProviderType
import xyz.xenondevs.nova.world.block.model.BlockStateBlockModelProvider

sealed interface BlockModelData {
    val id: String
    val modelProviderType: BlockModelProviderType<*>
}

class ArmorStandBlockModelData(
    id: String,
    val hitboxType: Material,
    dataArray: IntArray
) : ItemModelData(id, VanillaMaterialTypes.DEFAULT_MATERIAL, dataArray), BlockModelData {
    
    override val modelProviderType = ArmorStandModelProvider
    
    private val blockProviders: LazyArray<ItemProvider> by lazy {
        LazyArray(dataArray.size) { subId ->
            ItemWrapper(createClientsideItemBuilder(null, null, subId).get())
        }
    }
    
    operator fun get(index: Int): ItemProvider = blockProviders[index]
    
}

class BlockStateBlockModelData(
    override val id: String,
    val data: Map<BlockFace, List<BlockStateConfig>>
) : BlockModelData {
    
    override val modelProviderType = BlockStateBlockModelProvider
    
    operator fun get(face: BlockFace, index: Int): BlockStateConfig = data[face]!![index]
    
    operator fun get(index: Int): BlockStateConfig = data.values.first()[index]
    
}
