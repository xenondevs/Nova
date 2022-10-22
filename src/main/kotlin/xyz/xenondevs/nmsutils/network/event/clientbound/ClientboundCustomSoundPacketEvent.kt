package xyz.xenondevs.nmsutils.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket
import net.minecraft.world.phys.Vec3
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent

class ClientboundCustomSoundPacketEvent(
    player: Player,
    packet: ClientboundCustomSoundPacket
) : PlayerPacketEvent<ClientboundCustomSoundPacket>(player, packet) {
    
    var name = packet.name
        set(value) {
            field = value
            changed = true
        }
    var source = packet.source
        set(value) {
            field = value
            changed = true
        }
    var x = packet.x
        set(value) {
            field = value
            changed = true
        }
    var y = packet.y
        set(value) {
            field = value
            changed = true
        }
    var z = packet.z
        set(value) {
            field = value
            changed = true
        }
    var volume = packet.volume
        set(value) {
            field = value
            changed = true
        }
    var pitch = packet.pitch
        set(value) {
            field = value
            changed = true
        }
    var seed = packet.seed
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundCustomSoundPacket {
        return ClientboundCustomSoundPacket(name, source, Vec3(x, y, z), volume, pitch, seed)
    }
    
}