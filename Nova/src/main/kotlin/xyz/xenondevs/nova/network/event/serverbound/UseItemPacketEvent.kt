package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.util.data.MutableLazy

class UseItemPacketEvent(
    player: Player,
    packet: ServerboundUseItemPacket
) : PlayerPacketEvent<ServerboundUseItemPacket>(player, packet) {
    
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
    
    override val packet: ServerboundUseItemPacket
        get() = if (changed) {
            ServerboundUseItemPacket(hand, sequence)
        } else super.packet
    
    var hand by MutableLazy<InteractionHand>({ changed = true }) { packet.hand }
    var sequence by MutableLazy<Int>({changed = true}) {packet.sequence}
    
}