package xyz.xenondevs.nova.network

import net.minecraft.network.PacketListener
import net.minecraft.server.network.ServerGamePacketListenerImpl
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.PlayerPacketEvent

@JvmField
internal val ACTIVE_PACKET_LISTENER = ScopedValue.newInstance<PacketListener>()

/**
 * The [Player] that sent the packet that is currently being handled, or `null` if
 * no packet is being handled at the moment or if there is no player associated with it.
 * 
 * Note that this is for game code triggered by the actual packet handling process and not
 * for packet events.
 * For packet events, use [PlayerPacketEvent.player].
 */
val currentPacketSourcePlayer: Player?
    get() {
        if (!ACTIVE_PACKET_LISTENER.isBound)
            return null
        return (ACTIVE_PACKET_LISTENER.get() as? ServerGamePacketListenerImpl)?.player?.bukkitEntity
    }