package xyz.xenondevs.nova.network.event

import net.minecraft.network.protocol.Packet
import org.bukkit.entity.Player

/**
 * Base class for packet events.
 *
 * @param P the type of the packet
 */
abstract class PacketEvent<P : Packet<*>> internal constructor(packet: P) {
    
    /**
     * The packet that is being sent or received.
     */
    var packet: P = packet
        get() {
            if (changed) {
                field = buildChangedPacket()
                changed = false
            }
            
            return field
        }
    
    /**
     * Whether the event is canceled.
     * Packets that are canceled will not be sent or processed by the server.
     */
    var isCancelled = false
    
    protected var changed = false
    
    protected open fun buildChangedPacket(): P =
        throw NotImplementedError()
    
}

/**
 * Base class for packet events that are related to a player.
 * 
 * @param P
 */
abstract class PlayerPacketEvent<P : Packet<*>> internal constructor(val player: Player, packet: P) : PacketEvent<P>(packet)