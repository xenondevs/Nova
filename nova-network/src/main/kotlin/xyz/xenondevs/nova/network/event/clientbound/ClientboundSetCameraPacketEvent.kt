package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetCameraPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundSetCameraPacket

class ClientboundSetCameraPacketEvent(
    player: Player,
    packet: ClientboundSetCameraPacket
) : PlayerPacketEvent<ClientboundSetCameraPacket>(player, packet) {
    
    var cameraId = packet.cameraId
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundSetCameraPacket =
        ClientboundSetCameraPacket(cameraId)
}
