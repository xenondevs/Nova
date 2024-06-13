package xyz.xenondevs.nmsutils.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.network.ClientboundSoundEntityPacket
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent

class ClientboundSoundEntityPacketEvent(
    player: Player,
    packet: ClientboundSoundEntityPacket
) : PlayerPacketEvent<ClientboundSoundEntityPacket>(player, packet) {
    
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
    var entityId = packet.id
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
    
    override fun buildChangedPacket(): ClientboundSoundEntityPacket {
        return ClientboundSoundEntityPacket(sound, source, entityId, volume, pitch, seed)
    }
    
}