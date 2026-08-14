package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket
import xyz.xenondevs.nova.network.event.PacketEvent

class ClientboundLoginCompressionPacketEvent(
    packet: ClientboundLoginCompressionPacket
) : PacketEvent<ClientboundLoginCompressionPacket>(packet) {
    
    var compressionThreshold: Int = packet.compressionThreshold
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundLoginCompressionPacket =
        ClientboundLoginCompressionPacket(compressionThreshold)
    
}
