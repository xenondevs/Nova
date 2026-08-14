package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.StructureBlockEntity
import net.minecraft.world.level.block.state.properties.StructureMode
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ServerboundSetStructureBlockPacketEvent(
    player: Player,
    packet: ServerboundSetStructureBlockPacket
) : PlayerPacketEvent<ServerboundSetStructureBlockPacket>(player, packet) {
    
    var pos: BlockPos = packet.pos
        set(value) {
            field = value
            changed = true
        }
    
    var updateType: StructureBlockEntity.UpdateType = packet.updateType
        set(value) {
            field = value
            changed = true
        }
    
    var mode: StructureMode = packet.mode
        set(value) {
            field = value
            changed = true
        }
    
    var name: String = packet.name
        set(value) {
            field = value
            changed = true
        }
    
    var offset: BlockPos = packet.offset
        set(value) {
            field = value
            changed = true
        }
    
    var size: Vec3i = packet.size
        set(value) {
            field = value
            changed = true
        }
    
    var mirror: Mirror = packet.mirror
        set(value) {
            field = value
            changed = true
        }
    
    var rotation: Rotation = packet.rotation
        set(value) {
            field = value
            changed = true
        }
    
    var data: String = packet.data
        set(value) {
            field = value
            changed = true
        }
    
    var ignoreEntities = packet.isIgnoreEntities
        set(value) {
            field = value
            changed = true
        }
    
    var strict = packet.isStrict
        set(value) {
            field = value
            changed = true
        }
    
    var showAir = packet.isShowAir
        set(value) {
            field = value
            changed = true
        }
    
    var showBoundingBox = packet.isShowBoundingBox
        set(value) {
            field = value
            changed = true
        }
    
    var integrity = packet.integrity
        set(value) {
            field = value
            changed = true
        }
    
    var seed = packet.seed
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ServerboundSetStructureBlockPacket =
        ServerboundSetStructureBlockPacket(
            pos,
            updateType,
            mode,
            name,
            offset,
            size,
            mirror,
            rotation,
            data,
            ignoreEntities,
            strict,
            showAir,
            showBoundingBox,
            integrity,
            seed
        )
}
