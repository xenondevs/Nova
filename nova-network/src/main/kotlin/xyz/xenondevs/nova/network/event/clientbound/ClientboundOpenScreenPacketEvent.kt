package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundOpenScreenPacketEvent(
    player: Player,
    packet: ClientboundOpenScreenPacket
) : PlayerPacketEvent<ClientboundOpenScreenPacket>(player, packet) {
    
    var containerId = packet.containerId
        set(value) {
            field = value
            changed = true
        }
    
    var type = packet.type
        set(value) {
            field = value
            changed = true
        }
    
    var title = packet.title
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundOpenScreenPacket {
        return ClientboundOpenScreenPacket(containerId, type, title)
    }
    
}