package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundSetCommandMinecartPacketEvent(
    player: Player,
    packet: ServerboundSetCommandMinecartPacket
) : PlayerPacketEvent<ServerboundSetCommandMinecartPacket>(player, packet) {
    
    var entityId = packet.entity
        set(value) {
            field = value
            changed = true
        }
    
    var command: String = packet.command
        set(value) {
            field = value
            changed = true
        }
    
    var trackOutput = packet.isTrackOutput
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundSetCommandMinecartPacket(entityId, command, trackOutput)
}
