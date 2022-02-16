package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class PlaceRecipePacketEvent(
    player: Player,
    packet: ServerboundPlaceRecipePacket
) : PlayerPacketEvent<ServerboundPlaceRecipePacket>(player, packet) {
    
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