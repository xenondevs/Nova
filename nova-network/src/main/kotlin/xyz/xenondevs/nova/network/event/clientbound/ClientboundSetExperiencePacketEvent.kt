package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundSetExperiencePacketEvent(
    player: Player,
    packet: ClientboundSetExperiencePacket
) : PlayerPacketEvent<ClientboundSetExperiencePacket>(player, packet) {
    
    var experienceProgress = packet.experienceProgress
        set(value) {
            field = value
            changed = true
        }
    
    var totalExperience = packet.totalExperience
        set(value) {
            field = value
            changed = true
        }
    
    var experienceLevel = packet.experienceLevel
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() = ClientboundSetExperiencePacket(experienceProgress, totalExperience, experienceLevel)
}
