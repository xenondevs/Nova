package xyz.xenondevs.nova.world.block.behavior

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.block.Block
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.world.InteractionResult
import xyz.xenondevs.nova.world.block.NovaBlockState

internal object UnknownBlockBehavior : BlockBehavior {
    
    override fun use(block: Block, state: NovaBlockState, ctx: Context<BlockInteract>): InteractionResult {
        val player = ctx[BlockInteract.SOURCE_PLAYER]
        if (player != null && player.hasPermission("nova.command.debug")) {
            player.sendMessage(
                Component.translatable()
                    .key("block.nova.unknown.message")
                    .color(NamedTextColor.GRAY)
                    .arguments(Component.text(state.asString).color(NamedTextColor.AQUA))
                    .build()
            )
            return InteractionResult.Success()
        }
        return InteractionResult.Pass
    }
    
}
