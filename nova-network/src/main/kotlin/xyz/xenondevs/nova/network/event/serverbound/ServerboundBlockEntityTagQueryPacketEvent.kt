package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundBlockEntityTagQueryPacketEvent(
    player: Player,
    packet: ServerboundBlockEntityTagQueryPacket
) : PlayerPacketEvent<ServerboundBlockEntityTagQueryPacket>(player, packet) {
    
    var transactionId = packet.transactionId
        set(value) {
            field = value
            changed = true
        }
    
    var pos: BlockPos = packet.pos
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundBlockEntityTagQueryPacket(transactionId, pos)
}
