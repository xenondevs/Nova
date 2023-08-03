package xyz.xenondevs.nova.world.block

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.item.options.BlockOptions
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext

typealias TileEntityConstructor = ((NovaTileEntityState) -> TileEntity)

@Suppress("UNCHECKED_CAST")
class NovaTileEntityBlock internal constructor(
    id: ResourceLocation,
    name: Component,
    style: Style,
    logic: BlockLogic<NovaTileEntityState>,
    options: BlockOptions,
    internal val tileEntityConstructor: TileEntityConstructor,
    properties: List<BlockPropertyType<*>>,
    placeCheck: PlaceCheckFun?,
    multiBlockLoader: MultiBlockLoader?,
    configId: String
) : NovaBlock(
    id,
    name,
    style,
    logic as BlockLogic<NovaBlockState>, // fixme: users could cast to BlockNovaMaterial and then call methods on the NovaBlock with a BlockState that is not a NovaTileEntityState
    options,
    properties,
    placeCheck,
    multiBlockLoader,
    configId
) {
    
    override fun createBlockState(pos: BlockPos): NovaTileEntityState =
        NovaTileEntityState(pos, this)
    
    override fun createNewBlockState(ctx: BlockPlaceContext): NovaTileEntityState =
        NovaTileEntityState(this, ctx)
    
}