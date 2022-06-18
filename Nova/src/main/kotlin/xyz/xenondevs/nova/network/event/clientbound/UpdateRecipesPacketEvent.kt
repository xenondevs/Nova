package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class UpdateRecipesPacketEvent(
    player: Player,
    packet: ClientboundUpdateRecipesPacket
) : PlayerPacketEvent<ClientboundUpdateRecipesPacket>(player, packet) {
    
    companion object {
        @JvmStatic
        private val handlers = HandlerList()
        
        @JvmStatic
        fun getHandlerList() = handlers
        
    }
    
    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }
    
}