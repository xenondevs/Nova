package xyz.xenondevs.nova.network.event

import net.minecraft.network.protocol.Packet
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event

abstract class PacketEvent<P : Packet<*>>(val player: Player, open val packet: P) : Event(true), Cancellable {
    
    private var _cancelled = false
    
    override fun isCancelled(): Boolean {
        return _cancelled
    }
    
    override fun setCancelled(cancelled: Boolean) {
        _cancelled = cancelled
    }
    
}