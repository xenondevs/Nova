package xyz.xenondevs.nova.world.block.behavior

import xyz.xenondevs.nova.world.*

import org.bukkit.block.Block
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.block.NovaBlockState
import xyz.xenondevs.nova.world.block.novaTileEntity
import xyz.xenondevs.nova.world.block.tileentity.TileEntity

/**
 * Delegates [BlockBehavior.useItemOn] to [TileEntity.useItemOn] and
 * [BlockBehavior.use] to [TileEntity.use]
 */
object TileEntityInteractive : BlockBehavior {
    
    override fun useItemOn(block: Block, state: NovaBlockState, ctx: Context<BlockInteract>): InteractionResult {
        return block.novaTileEntity?.useItemOn(ctx) ?: InteractionResult.Pass
    }
    
    override fun use(block: Block, state: NovaBlockState, ctx: Context<BlockInteract>): InteractionResult {
        return block.novaTileEntity?.use(ctx) ?: InteractionResult.Pass
    }
    
}
