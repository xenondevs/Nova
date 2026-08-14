package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.login.ServerboundKeyPacket
import xyz.xenondevs.nova.network.event.PacketEvent
import xyz.xenondevs.nova.network.packet.ServerboundKeyPacket

class ServerboundKeyPacketEvent(
    packet: ServerboundKeyPacket
) : PacketEvent<ServerboundKeyPacket>(packet) {
    
    var keyBytes: ByteArray = packet.keybytes
        set(value) {
            field = value
            changed = true
        }
    
    var encryptedChallenge: ByteArray = packet.encryptedChallenge
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundKeyPacket =
        ServerboundKeyPacket(keyBytes, encryptedChallenge)
    
}
