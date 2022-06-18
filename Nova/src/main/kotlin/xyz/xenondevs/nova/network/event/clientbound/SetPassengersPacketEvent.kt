package xyz.xenondevs.nova.network.event.clientbound

import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.network.event.PlayerPacketEvent
import xyz.xenondevs.nova.util.ClientboundSetPassengersPacket
import xyz.xenondevs.nova.util.data.MutableLazy

class SetPassengersPacketEvent(
    player: Player,
    packet: ClientboundSetPassengersPacket
) : PlayerPacketEvent<ClientboundSetPassengersPacket>(player, packet) {
    
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
    
    override val packet: ClientboundSetPassengersPacket
        get() {
            val original = super.packet
            return if (changed)
                ClientboundSetPassengersPacket(vehicle, passengers)
            else original
        }
    
    var vehicle by MutableLazy({ changed = true }, packet::getVehicle)
    var passengers by MutableLazy({ changed = true }, packet::getPassengers)
    
}