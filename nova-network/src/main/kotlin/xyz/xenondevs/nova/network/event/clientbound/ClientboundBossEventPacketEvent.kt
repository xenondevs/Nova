package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import java.util.*

class ClientboundBossEventPacketEvent(
    player: Player,
    packet: ClientboundBossEventPacket
) : PlayerPacketEvent<ClientboundBossEventPacket>(player, packet) {
    
    var id: UUID = packet.id
        set(value) {
            field = value
            changed = true
        }
    var operation: ClientboundBossEventPacket.Operation = packet.operation
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundBossEventPacket {
        return ClientboundBossEventPacket(id, operation)
    }
    
}