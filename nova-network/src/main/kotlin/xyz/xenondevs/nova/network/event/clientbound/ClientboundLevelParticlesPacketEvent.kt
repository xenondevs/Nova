package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.core.particles.ParticleOptions
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundLevelParticlesPacketEvent(
    player: Player,
    packet: ClientboundLevelParticlesPacket
) : PlayerPacketEvent<ClientboundLevelParticlesPacket>(player, packet) {
    
    var particle: ParticleOptions = packet.particle
        set(value) {
            field = value
            changed = true
        }
    
    var overrideLimiter = packet.isOverrideLimiter
        set(value) {
            field = value
            changed = true
        }
    
    var alwaysShow = packet.alwaysShow()
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
    
    var xDist = packet.xDist
        set(value) {
            field = value
            changed = true
        }
    
    var yDist = packet.yDist
        set(value) {
            field = value
            changed = true
        }
    
    var zDist = packet.zDist
        set(value) {
            field = value
            changed = true
        }
    
    var maxSpeed = packet.maxSpeed
        set(value) {
            field = value
            changed = true
        }
    
    var count = packet.count
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundLevelParticlesPacket =
        ClientboundLevelParticlesPacket(particle, overrideLimiter, alwaysShow, x, y, z, xDist, yDist, zDist, maxSpeed, count)
    
}
