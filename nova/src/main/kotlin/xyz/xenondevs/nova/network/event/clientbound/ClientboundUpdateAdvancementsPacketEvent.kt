package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementProgress
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket
import net.minecraft.resources.Identifier
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundUpdateAdvancementsPacketEvent(
    player: Player,
    packet: ClientboundUpdateAdvancementsPacket
) : PlayerPacketEvent<ClientboundUpdateAdvancementsPacket>(player, packet) {
    
    var added: List<AdvancementHolder> = packet.added
        set(value) {
            field = value
            changed = true
        }
    
    var removed: Set<Identifier> = packet.removed
        set(value) {
            field = value
            changed = true
        }
    
    var progress: Map<Identifier, AdvancementProgress> = packet.progress
        set(value) {
            field = value
            changed = true
        }
    
    var shouldReset: Boolean = packet.shouldReset()
        set(value) {
            field = value
            changed = true
        }
    
    var shouldShowAdvancements: Boolean = packet.shouldShowAdvancements()
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket() =
        ClientboundUpdateAdvancementsPacket(shouldReset, added, removed, progress, shouldShowAdvancements)
    
}