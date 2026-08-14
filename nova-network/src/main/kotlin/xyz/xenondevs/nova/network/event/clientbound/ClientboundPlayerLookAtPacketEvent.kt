package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundPlayerLookAtPacket

class ClientboundPlayerLookAtPacketEvent(
    player: Player,
    packet: ClientboundPlayerLookAtPacket
) : PlayerPacketEvent<ClientboundPlayerLookAtPacket>(player, packet) {
    
    var fromAnchor: EntityAnchorArgument.Anchor = packet.fromAnchor
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
    
    var atEntity = packet.atEntity
        set(value) {
            field = value
            changed = true
        }
    
    var entityId = packet.entity
        set(value) {
            field = value
            changed = true
        }
    
    var toAnchor: EntityAnchorArgument.Anchor? = packet.toAnchor
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundPlayerLookAtPacket =
        ClientboundPlayerLookAtPacket(fromAnchor, x, y, z, entityId.takeIf { atEntity }, toAnchor)
}
