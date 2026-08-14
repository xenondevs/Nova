package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundSignUpdatePacketEvent(
    player: Player,
    packet: ServerboundSignUpdatePacket
) : PlayerPacketEvent<ServerboundSignUpdatePacket>(player, packet) {
    
    var pos: BlockPos = packet.pos
        set(value) {
            field = value
            changed = true
        }
    
    var frontText = packet.isFrontText
        set(value) {
            field = value
            changed = true
        }
    
    var lines: Array<String> = packet.lines
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundSignUpdatePacket {
        require(lines.size == 4)
        return ServerboundSignUpdatePacket(pos, frontText, lines[0], lines[1], lines[2], lines[3])
    }
}
