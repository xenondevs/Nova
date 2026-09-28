package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundEntityEventPacket

class ClientboundEntityEventPacketEvent(
    player: Player,
    packet: ClientboundEntityEventPacket
) : PlayerPacketEvent<ClientboundEntityEventPacket>(player, packet) {
    
    var entityId: Int = packet.entityId
        set(value) {
            field = value
            changed = true
        }
    
    var eventId: Byte = packet.eventId
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundEntityEventPacket =
        ClientboundEntityEventPacket(entityId, eventId)
    
}
