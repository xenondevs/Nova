package xyz.xenondevs.nova.packetentity

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.joml.Vector3dc
import xyz.xenondevs.commons.provider.dsl.DslProperty
import xyz.xenondevs.nova.network.event.serverbound.ServerboundAttackPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundInteractPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPickItemFromEntityPacketEvent
import xyz.xenondevs.nova.world.InteractionResult
import java.util.*

/**
 * DSL scope available inside [PacketEntityDsl.onAttack] handlers.
 *
 * ```kotlin
 * packetInteraction {
 *     onAttack {
 *         player.sendMessage("Attacked")
 *     }
 * }
 * ```
 */
@PacketEntityDslMarker
sealed interface AttackDsl {
    
    /** The player that attacked the packet entity. */
    val player: Player
    
}

/**
 * DSL scope available inside [PacketEntityDsl.onInteract] handlers.
 */
@PacketEntityDslMarker
sealed interface InteractDsl {
    
    /** The player that interacted with the packet entity. */
    val player: Player
    
    /** The hand used to interact with the packet entity. */
    val hand: EquipmentSlot
    
    /**
     * The interaction position sent by the client, relative to the interacted entity's position,
     */
    val interactLocation: Vector3dc
    
}

/**
 * DSL scope available inside [PacketEntityDsl.onSpawn] handlers.
 */
@PacketEntityDslMarker
sealed interface SpawnDsl {
    
    /** The player that can now see the packet entity. */
    val viewer: Player
    
}

/**
 * DSL scope available inside [PacketEntityDsl.onDespawn] handlers.
 */
@PacketEntityDslMarker
sealed interface DespawnDsl {
    
    /** The player that can no longer see the packet entity. */
    val viewer: Player
    
}

/**
 * DSL scope for configuring a [PacketEntity].
 *
 * ```kotlin
 * val entity = packetItemDisplay {
 *     location by player.location
 *     viewerBlacklist by setOf(player.uniqueId)
 *
 *     metadata {
 *         itemStack by item
 *     }
 *
 *     onSpawn {
 *         viewer.sendMessage("Display spawned")
 *     }
 * }
 * ```
 */
@PacketEntityDslMarker
sealed interface PacketEntityDsl<M : EntityMetadataDsl> {
    
    /**
     * The visibility of this entity.
     *
     * Defaults to [PacketEntityVisibility.STANDARD].
     */
    var visibility: PacketEntityVisibility
    
    /**
     * Whether location changes emit movement packets.
     *
     * Defaults to `true`. Set to `false` when the client receives movement from another source,
     * such as a packet entity mounted as a passenger of a real entity.
     */
    var sendMovementPackets: Boolean
    
    /**
     * The entity location.
     *
     * This property must be set before spawning the entity. It can be bound to a provider or set to
     * a static location:
     * ```kotlin
     * location by player.location
     * ```
     */
    val location: DslProperty<Location>
    
    /**
     * The entity's equipment slots.
     *
     * ```kotlin
     * equipment[EquipmentSlot.HEAD] by helmet
     * ```
     */
    val equipment: EquipmentDslProperty
    
    /**
     * The entity's attributes.
     * 
     * ```kotlin
     * attributes[Attribute.SCALE] by 0.5
     * attributes[Attribute.SCALE] by null
     * ```
     */
    val attributes: AttributesDslProperty
    
    /**
     * Optional whitelist of players that may see this entity.
     *
     * `null` means all players may see it. A non-null set restricts visibility to the listed UUIDs.
     */
    val viewerWhitelist: DslProperty<Set<UUID>?>
    
    /**
     * Blacklist of players that must not see this entity.
     *
     * Defaults to an empty set. The blacklist takes precedence over [viewerWhitelist].
     */
    val viewerBlacklist: DslProperty<Set<UUID>>
    
    /**
     * Configures equipment using named equipment slots.
     */
    fun equipment(equipment: EquipmentDsl.() -> Unit)
    
    /**
     * Configures entity metadata.
     */
    fun metadata(metadata: M.() -> Unit)
    
    /**
     * Adds passenger packet entities to this entity.
     *
     * Passengers are tied to this packet entity and are spawned and despawned with it.
     */
    fun passengers(passengers: PacketEntityPassengersDsl.() -> Unit)
    
    /**
     * Registers a handler called when this entity becomes visible to a player.
     */
    fun onSpawn(handler: SpawnDsl.() -> Unit)
    
    /**
     * Registers a handler called when this entity stops being visible to a player.
     */
    fun onDespawn(handler: DespawnDsl.() -> Unit)
    
    /**
     * Registers a handler called on the main server thread when a player attacks this entity (left click).
     */
    fun onAttack(handler: AttackDsl.() -> Unit)
    
    /**
     * Registers a handler called on the player's Netty event-loop thread when they attack this entity (left click).
     */
    fun onAttackAsync(handler: (ServerboundAttackPacketEvent) -> Unit)
    
    /**
     * Registers a handler called on the player's Netty event-loop thread when they pick this entity (middle click).
     */
    fun onPickAsync(handler: (ServerboundPickItemFromEntityPacketEvent) -> Unit)
    
    /**
     * Registers a handler called on the main server thread when a player interacts with this entity (right click).
     */
    fun onInteract(handler: InteractDsl.() -> InteractionResult)
    
    /**
     * Registers a handler called on the player's Netty event-loop thread when they interact with this entity (right click).
     */
    fun onInteractAsync(handler: (ServerboundInteractPacketEvent) -> Unit)
    
}

/**
 * DSL scope for configuring a packet entity that is used as a passenger.
 *
 * Passenger entities do not define their own location, viewer whitelist/blacklist, or
 * spawn/despawn handlers. Instead, they are tied to the parent entity and are spawned and
 * despawned with it.
 */
@PacketEntityDslMarker
sealed interface PassengerPacketEntityDsl<M : EntityMetadataDsl> {
    
    /**
     * The entity's attributes.
     *
     * ```kotlin
     * attributes[Attribute.SCALE] by 0.5
     * attributes[Attribute.SCALE] by null
     * ```
     */
    val attributes: AttributesDslProperty
    
    /**
     * The entity's equipment slots.
     *
     * ```kotlin
     * equipment[EquipmentSlot.HEAD] by helmet
     * ```
     */
    val equipment: EquipmentDslProperty
    
    /**
     * Configures equipment using named equipment slots.
     */
    fun equipment(equipment: EquipmentDsl.() -> Unit)
    
    /**
     * Configures entity metadata.
     */
    fun metadata(metadata: M.() -> Unit)
    
    /**
     * Adds nested passenger packet entities to this passenger.
     */
    fun passengers(passengers: PacketEntityPassengersDsl.() -> Unit)
    
    /**
     * Registers a handler called on the main server thread when a player attacks this passenger (left click).
     */
    fun onAttack(handler: AttackDsl.() -> Unit)
    
    /**
     * Registers a handler called on the player's Netty event-loop thread when they attack this passenger (left click).
     */
    fun onAttackAsync(handler: (ServerboundAttackPacketEvent) -> Unit)
    
    /**
     * Registers a handler called on the player's Netty event-loop thread when they pick this passenger (middle click).
     */
    fun onPickAsync(handler: (ServerboundPickItemFromEntityPacketEvent) -> Unit)
    
    /**
     * Registers a handler called on the main server thread when a player interacts with this passenger (right click).
     */
    fun onInteract(handler: InteractDsl.() -> InteractionResult)
    
    /**
     * Registers a handler called on the player's Netty event-loop thread when they interact with this
     * passenger (right click).
     */
    fun onInteractAsync(handler: (ServerboundInteractPacketEvent) -> Unit)
    
}

@DslMarker
internal annotation class PacketEntityDslMarker