package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundAcceptTeleportationPacketEvent(
    player: Player,
    packet: ServerboundAcceptTeleportationPacket
) : PlayerPacketEvent<ServerboundAcceptTeleportationPacket>(player, packet) {
    
    var id = packet.id
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundAcceptTeleportationPacket(id)
}
