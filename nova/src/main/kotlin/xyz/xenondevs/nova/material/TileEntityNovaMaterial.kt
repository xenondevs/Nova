package xyz.xenondevs.nova.material

import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.material.options.BlockOptions
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext

typealias TileEntityConstructor = ((NovaTileEntityState) -> TileEntity)

@Suppress("UNCHECKED_CAST")
class TileEntityNovaMaterial internal constructor(
    id: NamespacedId,
    localizedName: String,
    novaItem: NovaItem,
    maxStackSize: Int,
    isHidden: Boolean,
    novaBlock: NovaBlock<NovaTileEntityState>,
    options: BlockOptions,
    internal val tileEntityConstructor: TileEntityConstructor,
    properties: List<BlockPropertyType<*>>,
    placeCheck: PlaceCheckFun?,
    multiBlockLoader: MultiBlockLoader?
) : BlockNovaMaterial(
    id,
    localizedName,
    novaItem,
    maxStackSize,
    isHidden,
    novaBlock as NovaBlock<NovaBlockState>, // fixme: users could cast to BlockNovaMaterial and then call methods on the NovaBlock with a BlockState that is not a NovaTileEntityState
    options,
    properties,
    placeCheck,
    multiBlockLoader
) {
    
    override fun createBlockState(pos: BlockPos): NovaTileEntityState =
        NovaTileEntityState(pos, this)
    
    override fun createNewBlockState(ctx: BlockPlaceContext): NovaTileEntityState =
        NovaTileEntityState(this, ctx)
    
}