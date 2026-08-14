package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundPlayerAbilitiesPacket

class ClientboundPlayerAbilitiesPacketEvent(
    player: Player,
    packet: ClientboundPlayerAbilitiesPacket
) : PlayerPacketEvent<ClientboundPlayerAbilitiesPacket>(player, packet) {
    
    var invulnerable = packet.isInvulnerable
        set(value) {
            field = value
            changed = true
        }
    
    var flying = packet.isFlying
        set(value) {
            field = value
            changed = true
        }
    
    var canFly = packet.canFly()
        set(value) {
            field = value
            changed = true
        }
    
    var instabuild = packet.canInstabuild()
        set(value) {
            field = value
            changed = true
        }
    
    var flyingSpeed = packet.flyingSpeed
        set(value) {
            field = value
            changed = true
        }
    
    var walkingSpeed = packet.walkingSpeed
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundPlayerAbilitiesPacket =
        ClientboundPlayerAbilitiesPacket(invulnerable, flying, canFly, instabuild, flyingSpeed, walkingSpeed)
    
}
