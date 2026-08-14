package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.entity.JigsawBlockEntity
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundSetJigsawBlockPacketEvent(
    player: Player,
    packet: ServerboundSetJigsawBlockPacket
) : PlayerPacketEvent<ServerboundSetJigsawBlockPacket>(player, packet) {
    
    var pos: BlockPos = packet.pos
        set(value) {
            field = value
            changed = true
        }
    
    var name: Identifier = packet.name
        set(value) {
            field = value
            changed = true
        }
    
    var target: Identifier = packet.target
        set(value) {
            field = value
            changed = true
        }
    
    var pool: Identifier = packet.pool
        set(value) {
            field = value
            changed = true
        }
    
    var finalState: String = packet.finalState
        set(value) {
            field = value
            changed = true
        }
    
    var joint: JigsawBlockEntity.JointType = packet.joint
        set(value) {
            field = value
            changed = true
        }
    
    var selectionPriority = packet.selectionPriority
        set(value) {
            field = value
            changed = true
        }
    
    var placementPriority = packet.placementPriority
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundSetJigsawBlockPacket =
        ServerboundSetJigsawBlockPacket(
            pos,
            name,
            target,
            pool,
            finalState,
            joint,
            selectionPriority,
            placementPriority
        )
}
