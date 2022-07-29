package xyz.xenondevs.nmsutils.network.event.clientbound

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.chat.ComponentSerializer
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.internal.MutableLazy
import xyz.xenondevs.nmsutils.internal.util.toComponent
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent

class ClientboundSystemChatPacketEvent(
    player: Player,
    packet: ClientboundSystemChatPacket
) : PlayerPacketEvent<ClientboundSystemChatPacket>(player, packet) {
    
    var overlay = packet.overlay
        set(value) {
            field = value
            changed = true
        }
    var message: Array<BaseComponent> by MutableLazy({ changed = true }) {
        ComponentSerializer.parse(packet.content)
    }
    
    override fun buildChangedPacket(): ClientboundSystemChatPacket {
        return ClientboundSystemChatPacket(message.toComponent(), overlay)
    }
    
}