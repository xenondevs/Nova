package xyz.xenondevs.nova.world.block.behavior

import kotlinx.serialization.json.JsonObject
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.block.DefaultBlocks
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.model.BlockModelProvider
import xyz.xenondevs.nova.world.format.BlockStateIdResolver
import xyz.xenondevs.nova.world.format.WorldDataManager

internal class UnknownNovaBlockState(serializedBlockState: JsonObject) : NovaBlockState(DefaultBlocks.UNKNOWN, IntArray(0), emptyMap()) {
    
    val serializedBlockState = serializedBlockState.toString()
    
    override val modelProvider: BlockModelProvider
        get() = block.defaultBlockState.modelProvider
    
}

internal object UnknownBlockBehavior : BlockBehavior {
    
    override fun use(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockInteract>): InteractionResult {
        val player = ctx[BlockInteract.SOURCE_PLAYER]
        if (player != null && player.hasPermission("nova.command.debug") && state is UnknownNovaBlockState) {
            player.sendMessage(
                Component.translatable()
                    .key("block.nova.unknown.message")
                    .color(NamedTextColor.GRAY)
                    .arguments(
                        Component.text(BlockStateIdResolver.toId(state)).color(NamedTextColor.AQUA),
                        Component.text(state.serializedBlockState).color(NamedTextColor.AQUA)
                    ).build()
            )
            return InteractionResult.Success()
        }
        return InteractionResult.Pass
    }
    
    override fun handleBreak(pos: BlockPos, state: NovaBlockState, ctx: Context<BlockBreak>) {
        // remove tile-entity data in the case that the previous block was a tile-entity
        WorldDataManager.setTileEntity(pos, null)
    }
    
}