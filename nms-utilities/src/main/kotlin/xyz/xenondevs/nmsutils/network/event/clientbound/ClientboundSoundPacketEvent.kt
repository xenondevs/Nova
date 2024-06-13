package xyz.xenondevs.nmsutils.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSoundPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent

class ClientboundSoundPacketEvent(
    player: Player,
    packet: ClientboundSoundPacket
) : PlayerPacketEvent<ClientboundSoundPacket>(player, packet) {
    
    var sound = packet.sound
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
    
    override fun buildChangedPacket(): ClientboundSoundPacket {
        return ClientboundSoundPacket(sound, source, x, y, z, volume, pitch, seed)
    }
    
}