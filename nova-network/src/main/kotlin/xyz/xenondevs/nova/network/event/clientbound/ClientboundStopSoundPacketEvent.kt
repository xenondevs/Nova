package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundStopSoundPacket
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundSource
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundStopSoundPacketEvent(
    player: Player,
    packet: ClientboundStopSoundPacket
) : PlayerPacketEvent<ClientboundStopSoundPacket>(player, packet) {
    
    var name: Identifier? = packet.name
        set(value) {
            field = value
            changed = true
        }
    
    var source: SoundSource? = packet.source
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ClientboundStopSoundPacket(name, source)
}
