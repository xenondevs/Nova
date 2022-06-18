package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.util.data.MutableLazy

class SoundPacketEvent(
    player: Player,
    packet: ClientboundSoundPacket
) : PlayerPacketEvent<ClientboundSoundPacket>(player, packet) {
    
    companion object {
        @JvmStatic
        private val handlers = HandlerList()
        
        @JvmStatic
        fun getHandlerList() = handlers
        
    }
    
    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }
    
    private var changed = false
    
    override val packet: ClientboundSoundPacket
        get() = if (changed) {
            ClientboundSoundPacket(sound, source, x, y, z, volume, pitch, seed)
        } else super.packet
    
    var sound by MutableLazy<SoundEvent>({ changed = true }) { packet.sound }
    var source by MutableLazy<SoundSource>({ changed = true }) { packet.source }
    var x by MutableLazy<Double>({ changed = true }) { packet.x }
    var y by MutableLazy<Double>({ changed = true }) { packet.y }
    var z by MutableLazy<Double>({ changed = true }) { packet.z }
    var volume by MutableLazy<Float>({ changed = true }) { packet.volume }
    var pitch by MutableLazy<Float>({ changed = true }) { packet.pitch }
    var seed by MutableLazy<Long>({changed = true}) { packet.seed }
    
}