package xyz.xenondevs.nova.network.event.impl

import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.network.event.PacketEvent

class ClientboundSetEntityDataPacketEvent(
    player: Player,
    packet: ClientboundSetEntityDataPacket
) : PacketEvent<ClientboundSetEntityDataPacket>(player, packet) {
    
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