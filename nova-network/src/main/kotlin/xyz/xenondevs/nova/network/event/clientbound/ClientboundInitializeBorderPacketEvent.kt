package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.network.packet.ClientboundInitializeBorderPacket

class ClientboundInitializeBorderPacketEvent(
    player: Player,
    packet: ClientboundInitializeBorderPacket
) : PlayerPacketEvent<ClientboundInitializeBorderPacket>(player, packet) {
    
    var newCenterX = packet.newCenterX
        set(value) {
            field = value
            changed = true
        }
    
    var newCenterZ = packet.newCenterZ
        set(value) {
            field = value
            changed = true
        }
    
    var oldSize = packet.oldSize
        set(value) {
            field = value
            changed = true
        }
    
    var newSize = packet.newSize
        set(value) {
            field = value
            changed = true
        }
    
    var lerpTime = packet.lerpTime
        set(value) {
            field = value
            changed = true
        }
    
    var newAbsoluteMaxSize = packet.newAbsoluteMaxSize
        set(value) {
            field = value
            changed = true
        }
    
    var warningBlocks = packet.warningBlocks
        set(value) {
            field = value
            changed = true
        }
    
    var warningTime = packet.warningTime
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundInitializeBorderPacket =
        ClientboundInitializeBorderPacket(
            newCenterX,
            newCenterZ,
            oldSize,
            newSize,
            lerpTime,
            newAbsoluteMaxSize,
            warningBlocks,
            warningTime
        )
    
}
