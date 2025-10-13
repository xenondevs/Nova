package xyz.xenondevs.nova.world.block

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelLayout
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorHolder
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.ScopedBlockStateProperty
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.format.WorldDataManager

typealias TileEntityConstructor = ((BlockPos, NovaBlockState, Compound) -> TileEntity)

class NovaTileEntityBlock internal constructor(
    id: Key,
    name: Component,
    style: Style,
    behaviors: List<BlockBehaviorHolder>,
    internal val tileEntityConstructor: TileEntityConstructor,
    val tickrate: Int,
    properties: List<ScopedBlockStateProperty<*>>,
    configId: String,
    requestedLayout: BlockModelLayout
) : NovaBlock(id, name, style, behaviors, properties, configId, requestedLayout) {
    
    override fun handlePlace(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockPlace>) {
        val tileEntityBlock = state.block as NovaTileEntityBlock
        val data = ctx[BlockPlace.TILE_ENTITY_DATA_NOVA] ?: Compound()
        
        // write owner into data so that it is accessible during tile-entity construction
        val owner = ctx[BlockPlace.SOURCE_PLAYER] ?: ctx[BlockPlace.SOURCE_TILE_ENTITY]?.owner
        if (owner != null) data["ownerUuid"] = owner.uniqueId
        
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