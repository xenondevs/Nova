package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundPickItemFromBlockPacketEvent(
    player: Player,
    packet: ServerboundPickItemFromBlockPacket
) : PlayerPacketEvent<ServerboundPickItemFromBlockPacket>(player, packet) {
    
    var pos: BlockPos = packet.pos
        set(value) {
            field = value
            changed = true
        }
    
    var includeData: Boolean = packet.includeData
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundPickItemFromBlockPacket {
        return ServerboundPickItemFromBlockPacket(pos, includeData)
    }
    
}