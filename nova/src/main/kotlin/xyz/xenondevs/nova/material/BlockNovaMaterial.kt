package xyz.xenondevs.nova.material

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.model.data.ArmorStandBlockModelData
import xyz.xenondevs.nova.data.resources.model.data.BlockModelData
import xyz.xenondevs.nova.data.resources.model.data.BlockStateBlockModelData
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.material.options.BlockOptions
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import java.util.concurrent.CompletableFuture

typealias PlaceCheckFun = ((Player, ItemStack, Location) -> CompletableFuture<Boolean>)
typealias MultiBlockLoader = (BlockPos) -> List<BlockPos>

open class BlockNovaMaterial internal constructor(
    id: NamespacedId,
    localizedName: String,
    novaItem: NovaItem,
    maxStackSize: Int,
    isHidden: Boolean,
    val novaBlock: NovaBlock<NovaBlockState>,
    options: BlockOptions,
    val properties: List<BlockPropertyType<*>>,
    val placeCheck: PlaceCheckFun?,
    val multiBlockLoader: MultiBlockLoader?
) : ItemNovaMaterial(id, localizedName, novaItem, maxStackSize, isHidden) {
    
    val block: BlockModelData by lazy { Resources.getModelData(id).block!! }
    
    val hardness = options.hardness
    val toolCategories = options.toolCategories
    val toolTier = options.toolTier
    val requiresToolForDrops = options.requiresToolForDrops
    val soundGroup = options.soundGroup
    val breakParticles = options.breakParticles
    val showBreakAnimation = options.showBreakAnimation
    
    internal val vanillaBlockMaterial: Material
        get() = when (val block = block) {
            is ArmorStandBlockModelData -> block.hitboxType
            is BlockStateBlockModelData -> block[0].type.material
        }
    
    internal open fun createBlockState(pos: BlockPos): NovaBlockState =
        NovaBlockState(pos, this)
    
    internal open fun createNewBlockState(ctx: BlockPlaceContext): NovaBlockState =
        NovaBlockState(this, ctx)
    
}