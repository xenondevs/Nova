package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.login.ClientboundHelloPacket
import xyz.xenondevs.nova.network.event.PacketEvent
import java.security.PublicKey

class ClientboundHelloPacketEvent(
    packet: ClientboundHelloPacket
) : PacketEvent<ClientboundHelloPacket>(packet) {
    
    var serverId: String = packet.serverId
        set(value) {
            field = value
            changed = true
        }
    
    var publicKey: PublicKey = packet.publicKey
        set(value) {
            field = value
            changed = true
        }
    
    var challenge: ByteArray = packet.challenge
        set(value) {
            field = value
            changed = true
        }
    
    var shouldAuthenticate: Boolean = packet.shouldAuthenticate()
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundHelloPacket =
        ClientboundHelloPacket(serverId, publicKey.encoded, challenge, shouldAuthenticate)
    
}
