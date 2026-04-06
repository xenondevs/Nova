package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundAttackPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundAttackPacketEvent(
    player: Player,
    packet: ServerboundAttackPacket
) : PlayerPacketEvent<ServerboundAttackPacket>(player, packet) {
    
    var entityId: Int = packet.entityId()
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundAttackPacket {
        return ServerboundAttackPacket(entityId)
    }
    
}