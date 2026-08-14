package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

class ClientboundOpenSignEditorPacketEvent(
    player: Player,
    packet: ClientboundOpenSignEditorPacket
) : PlayerPacketEvent<ClientboundOpenSignEditorPacket>(player, packet) {
    
    var pos: BlockPos = packet.pos
        set(value) {
            field = value
            changed = true
        }
    
    var isFrontText = packet.isFrontText
        set(value) {
            field = value
            changed = true
        }
    
    override fun buildChangedPacket(): ClientboundOpenSignEditorPacket =
        ClientboundOpenSignEditorPacket(pos, isFrontText)
    
}
