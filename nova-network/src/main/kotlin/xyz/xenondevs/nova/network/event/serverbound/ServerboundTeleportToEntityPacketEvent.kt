package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import java.util.UUID

class ServerboundTeleportToEntityPacketEvent(
    player: Player,
    packet: ServerboundTeleportToEntityPacket
) : PlayerPacketEvent<ServerboundTeleportToEntityPacket>(player, packet) {
    
    var uuid: UUID = packet.uuid
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundTeleportToEntityPacket(uuid)
}
