package xyz.xenondevs.nmsutils.network.event.clientbound

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nmsutils.network.event.PlayerPacketEvent

class ClientboundLevelEventPacketEvent(
    player: Player,
    packet: ClientboundLevelEventPacket
) : PlayerPacketEvent<ClientboundLevelEventPacket>(player, packet) {
    
    var type: Int = packet.type
        set(value) {
            field = value
            changed = true
        }
    var pos: BlockPos = packet.pos
        set(value) {
            field = value
            changed = true
        }
    var data: Int = packet.data
        set(value) {
            field = value
            changed = true
        }
    var isGlobalEvent: Boolean = packet.isGlobalEvent
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundLevelEventPacket {
        return ClientboundLevelEventPacket(type, pos, data, isGlobalEvent)
    }
    
}