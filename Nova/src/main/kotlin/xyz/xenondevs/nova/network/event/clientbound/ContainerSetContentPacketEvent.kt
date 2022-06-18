package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.util.NonNullList
import xyz.xenondevs.nova.util.data.MutableLazy
import net.minecraft.world.item.ItemStack as MojangStack

class ContainerSetContentPacketEvent(
    player: Player,
    packet: ClientboundContainerSetContentPacket
) : PlayerPacketEvent<ClientboundContainerSetContentPacket>(player, packet) {
    
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
    
    var items: List<MojangStack> by MutableLazy({ changed = true }) {
        packet.items
    }
    
    var carriedItem: MojangStack by MutableLazy({ changed = true }) {
        packet.carriedItem
    }
    
    override val packet: ClientboundContainerSetContentPacket
        get() {
            val original = super.packet
            return if (changed)
                ClientboundContainerSetContentPacket(
                    original.containerId,
                    original.stateId,
                    NonNullList(items, MojangStack.EMPTY),
                    carriedItem
                )
            else original
        }
    
}