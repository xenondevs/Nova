package xyz.xenondevs.nova.network.event.impl

import net.md_5.bungee.api.chat.BaseComponent
import net.minecraft.network.protocol.game.ClientboundChatPacket
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.network.event.PacketEvent
import xyz.xenondevs.nova.util.data.MutableLazy
import xyz.xenondevs.nova.util.data.toBaseComponentArray

class ClientboundChatPacketEvent(
    player: Player,
    packet: ClientboundChatPacket
) : PacketEvent<ClientboundChatPacket>(player, packet) {
    
    companion object {
        @JvmStatic
        private val handlers = HandlerList()
        
        @JvmStatic
        fun getHandlerList() = handlers
    }
    
    private var changed = false
    
    override val packet: ClientboundChatPacket
        get() {
            return if (changed)
                ClientboundChatPacket(null, chatType, sender).also { it.components = message }
            else super.packet
        }
    
    var message: Array<BaseComponent>? by MutableLazy({ changed = true }) {
        packet.components ?: packet.message?.toBaseComponentArray()
    }
    
    var sender = packet.sender
        set(value) {
            field = value
            changed = true
        }
    
    var chatType = packet.type
        set(value) {
            field = value
            changed = true
        }
    
    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }
    
}