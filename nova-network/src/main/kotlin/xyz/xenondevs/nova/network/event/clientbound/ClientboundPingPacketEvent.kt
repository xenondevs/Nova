package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.common.ClientboundPingPacket
import xyz.xenondevs.nova.network.event.PacketEvent

class ClientboundPingPacketEvent(
    packet: ClientboundPingPacket
) : PacketEvent<ClientboundPingPacket>(packet) {
    
    var id: Int = packet.id
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundPingPacket =
        ClientboundPingPacket(id)
    
}
