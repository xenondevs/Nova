package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.numbers.NumberFormat
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundSetObjectivePacket
import java.util.Optional

class ClientboundSetObjectivePacketEvent(
    player: Player,
    packet: ClientboundSetObjectivePacket
) : PlayerPacketEvent<ClientboundSetObjectivePacket>(player, packet) {
    
    var objectiveName: String = packet.objectiveName
        set(value) {
            field = value
            changed = true
        }
    
    var displayName: Component = packet.displayName
        set(value) {
            field = value
            changed = true
        }
    
    var renderType: ObjectiveCriteria.RenderType = packet.renderType
        set(value) {
            field = value
            changed = true
        }
    
    var numberFormat: Optional<NumberFormat> = packet.numberFormat
        set(value) {
            field = value
            changed = true
        }
    
    var method = packet.method
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundSetObjectivePacket =
        ClientboundSetObjectivePacket(objectiveName, displayName, renderType, numberFormat, method)
}
