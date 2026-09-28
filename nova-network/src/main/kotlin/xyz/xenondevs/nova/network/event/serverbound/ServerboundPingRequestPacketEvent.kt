package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket
import xyz.xenondevs.nova.network.event.PacketEvent

class ServerboundPingRequestPacketEvent(
    packet: ServerboundPingRequestPacket
) : PacketEvent<ServerboundPingRequestPacket>(packet) {
    
    var time: Long = packet.time
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundPingRequestPacket =
        ServerboundPingRequestPacket(time)
    
}
