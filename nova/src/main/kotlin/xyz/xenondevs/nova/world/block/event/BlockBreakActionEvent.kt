package xyz.xenondevs.nova.world.block.event

import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class BlockBreakActionEvent(
    player: Player,
    val block: Block,
    val action: Action
) : PlayerEvent(player) {
    
    override fun getHandlers(): HandlerList {
        return handlerList
    }
    
    companion object {
        
        @JvmStatic
        private val handlerList = HandlerList()
        
        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlerList
        }
        
    }
    
    enum class Action {
        START,
        CANCEL,
        FINISH
    }
    
}