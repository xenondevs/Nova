package xyz.xenondevs.nova.world.block

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions.BlockBreak
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions.BlockPlace
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.data.resources.layout.block.BlockModelLayout
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorHolder
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty
import xyz.xenondevs.nova.world.format.WorldDataManager

typealias TileEntityConstructor = ((BlockPos, NovaBlockState, Compound) -> TileEntity)

class NovaTileEntityBlock internal constructor(
    id: ResourceLocation,
    name: Component,
    style: Style,
    behaviors: List<BlockBehaviorHolder>,
    internal val tileEntityConstructor: TileEntityConstructor,
    val syncTickrate: Int,
    val asyncTickrate: Double,
    properties: List<ScopedBlockStateProperty<*>>,
    configId: String,
    requestedLayout: BlockModelLayout
) : NovaBlock(id, name, style, behaviors, properties, configId, requestedLayout) {
    
    override fun handlePlace(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockPlace>) {
        val tileEntityBlock = state.block as NovaTileEntityBlock
        val data = ctx[DefaultContextParamTypes.TILE_ENTITY_DATA_NOVA] ?: Compound()
        val tileEntity = tileEntityBlock.tileEntityConstructor(pos, state, data)
        WorldDataManager.setTileEntity(pos, tileEntity)
        tileEntity.handlePlace(ctx)
        
        // call super method after creating the tile-entity, so that behaviors can access it
        super.handlePlace(pos, state, ctx)
    }
    
    override fun handleBreak(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>) {
        // call super method before removing the tile-entity, so that behaviors can still access it
        super.handleBreak(pos, state, ctx)
        
        WorldDataManager.setTileEntity(pos, null)?.handleBreak(ctx)
    }
    
}