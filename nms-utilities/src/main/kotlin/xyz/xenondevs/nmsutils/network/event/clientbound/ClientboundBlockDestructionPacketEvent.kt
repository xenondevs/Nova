package xyz.xenondevs.nmsutils.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent

class ClientboundBlockDestructionPacketEvent(
    player: Player,
    packet: ClientboundBlockDestructionPacket
) : PlayerPacketEvent<ClientboundBlockDestructionPacket>(player, packet) {
    
    var entityId = packet.id
        set(value) {
            field = value
            changed = true
        }
    var pos = packet.pos
        set(value) {
            field = value
            changed = true
        }
    var progress = packet.progress
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundBlockDestructionPacket {
        return ClientboundBlockDestructionPacket(entityId, pos, progress)
    }
    
}