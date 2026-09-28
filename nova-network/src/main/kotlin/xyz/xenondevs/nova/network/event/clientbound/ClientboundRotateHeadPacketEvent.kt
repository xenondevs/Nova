package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket
import net.minecraft.util.Mth
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundRotateHeadPacket

class ClientboundRotateHeadPacketEvent(
    player: Player,
    packet: ClientboundRotateHeadPacket
) : PlayerPacketEvent<ClientboundRotateHeadPacket>(player, packet) {
    
    var entityId = packet.entityId
        set(value) {
            field = value
            changed = true
        }
    
    var yHeadRot = packet.yHeadRot
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundRotateHeadPacket =
        ClientboundRotateHeadPacket(entityId, Mth.packDegrees(yHeadRot))
}
