package xyz.xenondevs.nmsutils.network.event

import net.minecraft.network.protocol.Packet
import org.bukkit.entity.Player

abstract class PacketEvent<P : Packet<*>> internal constructor(packet: P) {
    
    var packet: P = packet
        get() {
            if (changed) {
                field = buildChangedPacket()
                changed = false
            }
            
            return field
        }
    
    var isCancelled = false
    
    protected var changed = false
    
    protected open fun buildChangedPacket(): P =
        throw NotImplementedError()
    
}

abstract class PlayerPacketEvent<P : Packet<*>> internal constructor(val player: Player, packet: P) : PacketEvent<P>(packet)