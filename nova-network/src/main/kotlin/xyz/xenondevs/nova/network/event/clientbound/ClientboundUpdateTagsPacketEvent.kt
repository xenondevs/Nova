package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket
import xyz.xenondevs.nova.network.event.PacketEvent

class ClientboundUpdateTagsPacketEvent(
    packet: ClientboundUpdateTagsPacket
) : PacketEvent<ClientboundUpdateTagsPacket>(packet) {
    
    var tags = packet.tags
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ClientboundUpdateTagsPacket(tags)
    
}