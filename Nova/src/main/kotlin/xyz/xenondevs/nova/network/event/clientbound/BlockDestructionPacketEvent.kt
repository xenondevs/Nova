package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.util.data.MutableLazy

class BlockDestructionPacketEvent(
    player: Player,
    packet: ClientboundBlockDestructionPacket
) : PlayerPacketEvent<ClientboundBlockDestructionPacket>(player, packet) {
    
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
    
    override val packet: ClientboundBlockDestructionPacket
        get() = if (changed) {
            ClientboundBlockDestructionPacket(entityId, pos, progress)
        } else super.packet
    
    var entityId by MutableLazy<Int>({ changed = true }) { packet.id }
    var pos by MutableLazy<BlockPos>({ changed = true }) { packet.pos }
    var progress by MutableLazy<Int>({ changed = true }) { packet.progress }
    
}