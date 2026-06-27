package xyz.xenondevs.nova.packetentity

import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.*

/**
 * A packet-only entity that is spawned, updated, and removed by sending packets to players.
 *
 * Packet entities do not exist as Bukkit entities in the world. They are tracked by the
 * packet-entity manager and become visible to players based on their [location], [lod],
 * [viewerWhitelist], and [viewerBlacklist].
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
sealed interface PacketEntity<M : EntityMetadata> {
    
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
     *
     * Changes made through this object are flushed to visible players automatically after the
     * entity has been [spawned][spawn].
     */
    val metadata: M
    
    /**
     * The mutable equipment of this entity.
     *
     * Changes made through this object are flushed to visible players automatically after the
     * entity has been [spawned][spawn].
     */
    val equipment: PacketEntityEquipment
    
    /**
     * The level-of-detail range used to decide which players can see this entity.
     */
    val lod: PacketEntityLod
    
    /**
     * Handlers that are called on the main thread whenever this entity becomes visible to a player.
     */
    val spawnHandlers: MutableList<(Player) -> Unit>
    
    /**
     * Handlers that are called on the main thread whenever this entity stops being visible to a player.
     */
    val despawnHandlers: MutableList<(Player) -> Unit>
    
    /**
     * Handlers that are called on the main thread when a player interacts with this entity.
     */
    val interactHandlers: MutableList<InteractDsl.() -> Unit>
    
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
     * Starts tracking this entity and sends spawn packets to players that can currently see it.
     */
    fun spawn()
    
    /**
     * Stops tracking this entity and sends remove packets to players that can currently see it.
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
 * The level-of-detail (LOD) range used by packet entities. 
 * The LOD determines the range in which a packet entity is visible for players.
 * It can be used to show different packet entities at different ranges or hide certain packet entities earlier.
 * 
 * For example, invisible shulker entities (used for colliders) can be despawned once the player is not in an adjacent chunk and can't possibly touch
 * them anymore. For such a case, a [NEAR] LOD works well.
 * 
 * By default, packet entities use the [ALL] LOD.
 */
enum class PacketEntityLod(internal val range: IntRange) {
    
    /** Visible in all chunk sections that are in render distance. */
    ALL(0..MAX_PACKET_ENTITY_RENDER_DISTANCE),
    
    /** Visible in the same and adjacent chunk sections. */
    NEAR(0..1),
    
    /** Visible in all chunk sections that are in render distance outside [NEAR]. */
    FAR(2..MAX_PACKET_ENTITY_RENDER_DISTANCE)
    
}
