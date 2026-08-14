package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundSetBorderLerpSizePacket

class ClientboundSetBorderLerpSizePacketEvent(
    player: Player,
    packet: ClientboundSetBorderLerpSizePacket
) : PlayerPacketEvent<ClientboundSetBorderLerpSizePacket>(player, packet) {
    
    var oldSize = packet.oldSize
        set(value) {
            field = value
            changed = true
        }
    
    var newSize = packet.newSize
        set(value) {
            field = value
            changed = true
        }
    
    var lerpTime = packet.lerpTime
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundSetBorderLerpSizePacket =
        ClientboundSetBorderLerpSizePacket(oldSize, newSize, lerpTime)
}
