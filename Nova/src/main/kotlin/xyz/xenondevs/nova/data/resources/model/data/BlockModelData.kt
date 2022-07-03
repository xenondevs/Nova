package xyz.xenondevs.nova.data.resources.model.data

import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.ItemWrapper
import org.bukkit.Material
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
    type: BlockStateConfigType<T>,
    override val id: String,
    val dataArray: Array<T>
) : BlockModelData {
    
    override val modelProviderType = type.modelProvider
    
    operator fun get(index: Int): T = dataArray[index]
    
}
