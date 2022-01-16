package xyz.xenondevs.nova.network.event.impl

import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.network.event.PacketEvent

class ServerboundPlaceRecipePacketEvent(
    player: Player,
    packet: ServerboundPlaceRecipePacket
) : PacketEvent<ServerboundPlaceRecipePacket>(player, packet) {
    
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