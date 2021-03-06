package xyz.xenondevs.nova.material

import de.studiocode.invui.item.ItemProvider
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.util.SoundEffect
import xyz.xenondevs.nova.util.data.LazyArray
import xyz.xenondevs.nova.util.item.ToolCategory
import xyz.xenondevs.nova.util.item.ToolLevel
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import xyz.xenondevs.nova.world.block.model.BlockModelProviderType
import java.util.concurrent.CompletableFuture

typealias PlaceCheckFun = ((Player, ItemStack, Location) -> CompletableFuture<Boolean>)
typealias MultiBlockLoader = (BlockPos) -> List<BlockPos>

open class BlockNovaMaterial internal constructor(
    id: NamespacedId,
    localizedName: String,
    novaItem: NovaItem?,
    val novaBlock: NovaBlock<NovaBlockState>,
    options: BlockOptions,
    val modelProvider: BlockModelProviderType<*>,
    val properties: List<BlockPropertyType<*>>,
    val placeCheck: PlaceCheckFun?,
    val multiBlockLoader: MultiBlockLoader?
) : ItemNovaMaterial(id, localizedName, novaItem) {
    
    val block: ModelData by lazy { Resources.getModelData(id).second!! }
    
    val hardness = options.hardness
    val toolCategory = options.toolCategory
    val toolLevel = options.toolLevel
    val requiresToolForDrops = options.requiresToolForDrops
    val hitboxType = options.hitboxType
    val placeSound = options.placeSound
    val breakSound = options.breakSound
    val breakParticles = options.breakParticles
    val showBreakAnimation = options.showBreakAnimation
    
    val blockProviders: LazyArray<ItemProvider> by lazy { LazyArray(block.dataArray.size) { block.createClientsideItemBuilder(null, null, it) } }
    val blockProvider: ItemProvider by lazy { blockProviders[0] }
    
    internal open fun createBlockState(pos: BlockPos): NovaBlockState =
        NovaBlockState(pos, this)
    
    internal open fun createNewBlockState(ctx: BlockPlaceContext): NovaBlockState =
        NovaBlockState(this, ctx)
    
}

data class BlockOptions(
    val hardness: Double,
    val toolCategory: ToolCategory?,
    val toolLevel: ToolLevel?,
    val requiresToolForDrops: Boolean,
    val hitboxType: Material,
    val placeSound: SoundEffect? = null,
    val breakSound: SoundEffect? = null,
    val breakParticles: Material? = null,
    val showBreakAnimation: Boolean = true
)