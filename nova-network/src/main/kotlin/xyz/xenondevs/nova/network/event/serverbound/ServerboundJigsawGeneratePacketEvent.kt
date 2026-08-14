package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundJigsawGeneratePacketEvent(
    player: Player,
    packet: ServerboundJigsawGeneratePacket
) : PlayerPacketEvent<ServerboundJigsawGeneratePacket>(player, packet) {
    
    var pos: BlockPos = packet.pos
        set(value) {
            field = value
            changed = true
        }
    
    var levels = packet.levels()
        set(value) {
            field = value
            changed = true
        }
    
    var keepJigsaws = packet.keepJigsaws()
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundJigsawGeneratePacket(pos, levels, keepJigsaws)
}
