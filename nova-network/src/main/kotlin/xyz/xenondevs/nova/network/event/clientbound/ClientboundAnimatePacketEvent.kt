package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundAnimatePacket

class ClientboundAnimatePacketEvent(
    player: Player,
    packet: ClientboundAnimatePacket
) : PlayerPacketEvent<ClientboundAnimatePacket>(player, packet) {
    
    var id: Int = packet.id
        set(value) {
            field = value
            changed = true
        }
    
    var action: Int = packet.action
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundAnimatePacket =
        ClientboundAnimatePacket(id, action)
    
}
