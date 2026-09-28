package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
import net.minecraft.world.scores.DisplaySlot
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundSetDisplayObjectivePacket

class ClientboundSetDisplayObjectivePacketEvent(
    player: Player,
    packet: ClientboundSetDisplayObjectivePacket
) : PlayerPacketEvent<ClientboundSetDisplayObjectivePacket>(player, packet) {
    
    var slot: DisplaySlot = packet.slot
        set(value) {
            field = value
            changed = true
        }
    
    var objectiveName: String? = packet.objectiveName
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundSetDisplayObjectivePacket =
        ClientboundSetDisplayObjectivePacket(slot, objectiveName)
}
