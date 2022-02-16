package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.util.data.MutableLazy

class ContainerSetSlotPacketEvent(
    player: Player,
    packet: ClientboundContainerSetSlotPacket
) : PlayerPacketEvent<ClientboundContainerSetSlotPacket>(player, packet) {
    
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
    
    var item by MutableLazy({ changed = true }) {
        super.packet.item
    }
    
    override val packet: ClientboundContainerSetSlotPacket
        get() {
            val original = super.packet
            return if (changed)
                ClientboundContainerSetSlotPacket(
                    original.containerId,
                    original.stateId,
                    original.slot,
                    item
                )
            else original
        }
}