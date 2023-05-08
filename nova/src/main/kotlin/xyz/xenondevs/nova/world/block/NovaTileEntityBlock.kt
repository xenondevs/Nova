package xyz.xenondevs.nova.world.block

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.item.options.BlockOptions
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.world.BlockLocation
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext

typealias TileEntityConstructor = ((NovaTileEntityState) -> TileEntity)

@Suppress("UNCHECKED_CAST")
class NovaTileEntityBlock internal constructor(
    id: ResourceLocation,
    localizedName: String,
    logic: BlockLogic<NovaTileEntityState>,
    options: BlockOptions,
    internal val tileEntityConstructor: TileEntityConstructor,
    properties: List<BlockPropertyType<*>>,
    placeCheck: PlaceCheckFun?,
    multiBlockLoader: MultiBlockLoader?
) : NovaBlock(
    id,
    localizedName,
    logic as BlockLogic<NovaBlockState>, // fixme: users could cast to BlockNovaMaterial and then call methods on the NovaBlock with a BlockState that is not a NovaTileEntityState
    options,
    properties,
    placeCheck,
    multiBlockLoader
) {
    
    override fun createBlockState(pos: BlockLocation): NovaTileEntityState =
        NovaTileEntityState(pos, this)
    
    override fun createNewBlockState(ctx: BlockPlaceContext): NovaTileEntityState =
        NovaTileEntityState(this, ctx)
    
}