package xyz.xenondevs.nova.network.event.clientbound

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.chat.ComponentSerializer
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.util.data.MutableLazy
import xyz.xenondevs.nova.util.data.toComponent

class SystemChatPacketEvent(
    player: Player,
    packet: ClientboundSystemChatPacket
) : PlayerPacketEvent<ClientboundSystemChatPacket>(player, packet) {
    
    companion object {
        @JvmStatic
        private val handlers = HandlerList()
        
        @JvmStatic
        fun getHandlerList() = handlers
    }
    
    private var changed = false
    
    override val packet: ClientboundSystemChatPacket
        get() {
            return if (changed)
                ClientboundSystemChatPacket(message.toComponent(), overlay)
            else super.packet
        }
    
    var message: Array<BaseComponent> by MutableLazy({ changed = true }) {
        ComponentSerializer.parse(packet.content)
    }
    
    var overlay by MutableLazy({changed = true}) { packet.overlay }
    
    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }
    
}