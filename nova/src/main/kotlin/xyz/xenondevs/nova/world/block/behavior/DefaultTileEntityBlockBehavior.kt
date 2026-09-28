package xyz.xenondevs.nova.world.block.behavior

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.block.Block
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.world.block.DefaultBlockTags
import xyz.xenondevs.nova.world.block.NovaBlockState
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.limits.TileEntityLimits
import xyz.xenondevs.nova.world.block.limits.TileEntityTracker

internal object DefaultTileEntityBlockBehavior : BlockBehavior {
    
    override val tags = provider(setOf(DefaultBlockTags.NOVA_TILE_ENTITY))
    
    override suspend fun canPlace(block: Block, data: NovaBlockState, ctx: Context<BlockPlace>): Boolean {
        if (ctx[BlockPlace.BYPASS_TILE_ENTITY_LIMITS])
            return true
        
        val result = TileEntityLimits.canPlace(ctx)
        if (!result.allowed) {
            ctx[BlockPlace.SOURCE_PLAYER]?.sendMessage(Component.translatable(result.message, NamedTextColor.RED))
            return false
        }
        
        return true
    }
    
    override fun handlePlace(block: Block, state: NovaBlockState, ctx: Context<BlockPlace>) {
        TileEntityTracker.handlePlace(block.blockType, ctx)
    }
    
    override fun handleBreak(block: Block, state: NovaBlockState, ctx: Context<BlockBreak>) {
        val tileEntity = ctx[BlockBreak.TILE_ENTITY_NOVA] ?: return
        TileEntityTracker.handleBreak(tileEntity, ctx)
    }
    
}
