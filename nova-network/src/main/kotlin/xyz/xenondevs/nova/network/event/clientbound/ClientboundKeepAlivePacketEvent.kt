package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket
import xyz.xenondevs.nova.network.event.PacketEvent

class ClientboundKeepAlivePacketEvent(
    packet: ClientboundKeepAlivePacket
) : PacketEvent<ClientboundKeepAlivePacket>(packet) {
    
    var id: Long = packet.id
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundKeepAlivePacket =
        ClientboundKeepAlivePacket(id)
    
}
