package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import java.util.UUID

class ClientboundAddEntityPacketEvent(
    player: Player,
    packet: ClientboundAddEntityPacket
) : PlayerPacketEvent<ClientboundAddEntityPacket>(player, packet) {
    
    var id: Int = packet.id
        set(value) {
            field = value
            changed = true
        }
    
    var uuid: UUID = packet.uuid
        set(value) {
            field = value
            changed = true
        }
    
    var x: Double = packet.x
        set(value) {
            field = value
            changed = true
        }
    
    var y: Double = packet.y
        set(value) {
            field = value
            changed = true
        }
    
    var z: Double = packet.z
        set(value) {
            field = value
            changed = true
        }
    
    var xRot: Float = packet.xRot
        set(value) {
            field = value
            changed = true
        }
    
    var yRot: Float = packet.yRot
        set(value) {
            field = value
            changed = true
        }
    
    var type: EntityType<*> = packet.type
        set(value) {
            field = value
            changed = true
        }
    
    var data: Int = packet.data
        set(value) {
            field = value
            changed = true
        }
    
    var movement: Vec3 = packet.movement
        set(value) {
            field = value
            changed = true
        }
    
    var yHeadRot: Float = packet.yHeadRot
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundAddEntityPacket =
        ClientboundAddEntityPacket(id, uuid, x, y, z, xRot, yRot, type, data, movement, yHeadRot.toDouble())
    
}
