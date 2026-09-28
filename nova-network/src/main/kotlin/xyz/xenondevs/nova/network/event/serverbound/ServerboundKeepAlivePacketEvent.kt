package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket
import xyz.xenondevs.nova.network.event.PacketEvent

class ServerboundKeepAlivePacketEvent(
    packet: ServerboundKeepAlivePacket
) : PacketEvent<ServerboundKeepAlivePacket>(packet) {
    
    var id: Long = packet.id
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundKeepAlivePacket =
        ServerboundKeepAlivePacket(id)
    
}
