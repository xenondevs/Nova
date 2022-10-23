package xyz.xenondevs.nova.material

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.model.data.BlockModelData
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolLevel
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import xyz.xenondevs.nova.world.block.sound.SoundGroup
import java.util.concurrent.CompletableFuture

typealias PlaceCheckFun = ((Player, ItemStack, Location) -> CompletableFuture<Boolean>)
typealias MultiBlockLoader = (BlockPos) -> List<BlockPos>

open class BlockNovaMaterial internal constructor(
    id: NamespacedId,
    localizedName: String,
    novaItem: NovaItem,
    maxStackSize: Int,
    val novaBlock: NovaBlock<NovaBlockState>,
    options: BlockOptions,
    val properties: List<BlockPropertyType<*>>,
    val placeCheck: PlaceCheckFun?,
    val multiBlockLoader: MultiBlockLoader?
) : ItemNovaMaterial(id, localizedName, novaItem, maxStackSize) {
    
    val block: BlockModelData by lazy { Resources.getModelData(id).second!! }
    
    val hardness = options.hardness
    val toolCategories = options.toolCategories
    val toolLevel = options.toolLevel
    val requiresToolForDrops = options.requiresToolForDrops
    val soundGroup = options.soundGroup
    val breakParticles = options.breakParticles
    val showBreakAnimation = options.showBreakAnimation
    
    internal open fun createBlockState(pos: BlockPos): NovaBlockState =
        NovaBlockState(pos, this)
    
    internal open fun createNewBlockState(ctx: BlockPlaceContext): NovaBlockState =
        NovaBlockState(this, ctx)
    
}

data class BlockOptions(
    val hardness: Double,
    val toolCategories: List<ToolCategory>,
    val toolLevel: ToolLevel?,
    val requiresToolForDrops: Boolean,
    val soundGroup: SoundGroup? = null,
    val breakParticles: Material? = null,
    val showBreakAnimation: Boolean = true
) {
    
    constructor(
        hardness: Double,
        toolCategory: ToolCategory?,
        toolLevel: ToolLevel?,
        requiresToolForDrops: Boolean,
        soundGroup: SoundGroup? = null,
        breakParticles: Material? = null,
        showBreakAnimation: Boolean = true
    ) : this(
        hardness,
        toolCategory?.let(::listOf) ?: emptyList(),
        toolLevel,
        requiresToolForDrops,
        soundGroup,
        breakParticles,
        showBreakAnimation
    )
    
}