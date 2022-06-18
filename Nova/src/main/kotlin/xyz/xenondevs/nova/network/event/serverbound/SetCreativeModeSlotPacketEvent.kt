package xyz.xenondevs.nova.network.event.serverbound

import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.util.data.MutableLazy
import net.minecraft.world.item.ItemStack as MojangStack

class SetCreativeModeSlotPacketEvent(
    player: Player,
    packet: ServerboundSetCreativeModeSlotPacket
) : PlayerPacketEvent<ServerboundSetCreativeModeSlotPacket>(player, packet) {
    
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
    
    var item: MojangStack by MutableLazy({ changed = true }) {
        packet.item
    }
    
    override val packet: ServerboundSetCreativeModeSlotPacket
        get() {
            val original = super.packet
            return if (changed)
                ServerboundSetCreativeModeSlotPacket(
                    original.slotNum,
                    item
                )
            else original
        }
}