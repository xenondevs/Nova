package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundEntityTagQueryPacketEvent(
    player: Player,
    packet: ServerboundEntityTagQueryPacket
) : PlayerPacketEvent<ServerboundEntityTagQueryPacket>(player, packet) {
    
    var transactionId = packet.transactionId
        set(value) {
            field = value
            changed = true
        }
    
    var entityId = packet.entityId
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundEntityTagQueryPacket(transactionId, entityId)
}
