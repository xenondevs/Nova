package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.util.data.MutableLazy

class PlayerActionPacketEvent(
    player: Player,
    packet: ServerboundPlayerActionPacket
) : PlayerPacketEvent<ServerboundPlayerActionPacket>(player, packet) {
    
    companion object {
        @JvmStatic
        private val handlers = HandlerList()
        
        @JvmStatic
        fun getHandlerList() = handlers
    }
    
    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }
    
    private var changed = false
    
    override val packet: ServerboundPlayerActionPacket
        get() = if (changed) {
            ServerboundPlayerActionPacket(action, pos, direction)
        } else super.packet
    
    var action by MutableLazy<Action>({ changed = true }) { packet.action }
    var pos by MutableLazy<BlockPos>({ changed = true }) { packet.pos }
    var direction by MutableLazy<Direction>({ changed = true }) { packet.direction }
    
}