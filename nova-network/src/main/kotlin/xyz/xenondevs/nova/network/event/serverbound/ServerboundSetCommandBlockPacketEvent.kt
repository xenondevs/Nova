package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket
import net.minecraft.world.level.block.entity.CommandBlockEntity
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundSetCommandBlockPacketEvent(
    player: Player,
    packet: ServerboundSetCommandBlockPacket
) : PlayerPacketEvent<ServerboundSetCommandBlockPacket>(player, packet) {
    
    var pos: BlockPos = packet.pos
        set(value) {
            field = value
            changed = true
        }
    
    var command: String = packet.command
        set(value) {
            field = value
            changed = true
        }
    
    var mode: CommandBlockEntity.Mode = packet.mode
        set(value) {
            field = value
            changed = true
        }
    
    var trackOutput = packet.isTrackOutput
        set(value) {
            field = value
            changed = true
        }
    
    var conditional = packet.isConditional
        set(value) {
            field = value
            changed = true
        }
    
    var automatic = packet.isAutomatic
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ServerboundSetCommandBlockPacket(pos, command, mode, trackOutput, conditional, automatic)
}
