package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.common.ServerboundPongPacket
import xyz.xenondevs.nova.network.event.PacketEvent

class ServerboundPongPacketEvent(
    packet: ServerboundPongPacket
) : PacketEvent<ServerboundPongPacket>(packet) {
    
    var id: Int = packet.id
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundPongPacket =
        ServerboundPongPacket(id)
    
}
