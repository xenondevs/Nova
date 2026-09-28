package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundSetEntityLinkPacket

class ClientboundSetEntityLinkPacketEvent(
    player: Player,
    packet: ClientboundSetEntityLinkPacket
) : PlayerPacketEvent<ClientboundSetEntityLinkPacket>(player, packet) {
    
    var sourceId = packet.sourceId
        set(value) {
            field = value
            changed = true
        }
    
    var destId = packet.destId
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundSetEntityLinkPacket =
        ClientboundSetEntityLinkPacket(sourceId, destId)
}
