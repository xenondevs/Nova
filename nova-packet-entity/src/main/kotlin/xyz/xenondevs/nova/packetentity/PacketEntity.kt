package xyz.xenondevs.nova.packetentity

import org.bukkit.Location
import org.bukkit.entity.Player
import xyz.xenondevs.nova.network.event.serverbound.ServerboundAttackPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundInteractPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPickItemFromEntityPacketEvent
import xyz.xenondevs.nova.world.InteractionResult
import java.util.*

/**
 * A [PacketEntity] or a passenger of a [PacketEntity].
 *
 * @param M the metadata API type for this entity.
 */
sealed interface PacketEntityNode<M : EntityMetadata> {
    
    /**
     * The entity id used in packets for this entity.
     */
    val id: Int
    
    /**
     * The UUID used in packets for this entity.
     */
    val uuid: UUID
    
    /**
     * The mutable metadata of this entity.
     */
    val metadata: M
    
    /**
     * The mutable equipment of this node.
     */
    val equipment: PacketEntityEquipment
    
    /**
     * The direct passengers mounted on this entity.
     */
    val passengers: List<PacketEntityNode<*>>
    
}

/**
 * A packet-only entity that is spawned, updated, and removed by sending packets to players.
 *
 * ```kotlin
 * val display = packetItemDisplay {
 *     location by player.location
 *     metadata {
 *         itemStack by item
 *     }
 * }
 *
 * display.spawn()
 * ```
 *
 * @param M the metadata API type for this entity.
 */
sealed interface PacketEntity<M : EntityMetadata> : PacketEntityNode<M> {
    
    /**
     * The visibility of this entity.
     */
    val visibility: PacketEntityVisibility
    
    /**
     * Handlers that are called on the main thread whenever this entity becomes visible to a player.
     */
    val spawnHandlers: MutableList<(Player) -> Unit>
    
    /**
     * Handlers that are called on the main thread whenever this entity stops being visible to a player.
     */
    val despawnHandlers: MutableList<(Player) -> Unit>
    
    /**
     * Handlers that are called on the main thread when a player attacks this entity (left click).
     */
    val attackHandlers: MutableList<AttackDsl.() -> Unit>
    
    /**
     * Handlers that are called on the player's Netty event-loop thread when they attack this entity (left click).
     */
    val attackAsyncHandlers: MutableList<(ServerboundAttackPacketEvent) -> Unit>
    
    /**
     * Handlers that are called on the player's Netty event-loop thread when they pick this entity (middle click).
     */
    val pickAsyncHandlers: MutableList<(ServerboundPickItemFromEntityPacketEvent) -> Unit>
    
    /**
     * Handlers that are called on the main thread when a player interacts with this entity (right click).
     */
    val interactHandlers: MutableList<InteractDsl.() -> InteractionResult>
    
    /**
     * Handlers that are called on the player's Netty event-loop thread when they interact with this entity (right click).
     */
    val interactAsyncHandlers: MutableList<(ServerboundInteractPacketEvent) -> Unit>
    
    /**
     * The entity location.
     *
     * Assigning a new location after [spawn] queues a packet update. Packet entities cannot move
     * between worlds; changing the world of this location after spawning is invalid.
     */
    var location: Location
    
    /**
     * The optional whitelist of players that may see this entity.
     *
     * A value of `null` means all players may see the entity, subject to [viewerBlacklist] and
     * render distance. A non-null set restricts visibility to the listed player UUIDs.
     */
    var viewerWhitelist: Set<UUID>?
    
    /**
     * The set of players that must not see this entity.
     *
     * The blacklist is applied after [viewerWhitelist], so a UUID in both sets is hidden.
     */
    var viewerBlacklist: Set<UUID>
    
    /**
     * Spawns the entity, making it visible for players.
     */
    fun spawn()
    
    /**
     * Despawns the entity, making it invisible to players.
     */
    fun despawn()
    
    /**
     * Updates [location] by applying [modifyLocation] to a clone of the current location.
     *
     * ```kotlin
     * entity.teleport {
     *     add(0.0, 1.0, 0.0)
     * }
     * ```
     */
    fun teleport(modifyLocation: Location.() -> Unit)
    
}

/**
 * The visibility of a packet entity.
 */
enum class PacketEntityVisibility(
    internal val range: IntRange,
    internal val cellShift: Int
) {
    
    /** Visible in all 16x16x16-block chunk sections within the player's packet-entity render distance. */
    STANDARD(0..MAX_PACKET_ENTITY_RENDER_DISTANCE, 4),
    
    /** Visible in the same and adjacent 2x2x2-block cells. */
    NEAR(0..1, 1)
    
}
