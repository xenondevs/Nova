package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundSetBorderCenterPacket

class ClientboundSetBorderCenterPacketEvent(
    player: Player,
    packet: ClientboundSetBorderCenterPacket
) : PlayerPacketEvent<ClientboundSetBorderCenterPacket>(player, packet) {
    
    var newCenterX = packet.newCenterX
        set(value) {
            field = value
            changed = true
        }
    
    var newCenterZ = packet.newCenterZ
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundSetBorderCenterPacket =
        ClientboundSetBorderCenterPacket(newCenterX, newCenterZ)
}
