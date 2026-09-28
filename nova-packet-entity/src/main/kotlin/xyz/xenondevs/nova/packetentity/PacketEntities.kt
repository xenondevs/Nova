package xyz.xenondevs.nova.packetentity

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

/**
 * The minimum render distance for packet entities, in 16x16x16-block chunk sections.
 */
const val MIN_PACKET_ENTITY_RENDER_DISTANCE = 0

/**
 * The maximum render distance for packet entities, in 16x16x16-block chunk sections.
 */
const val MAX_PACKET_ENTITY_RENDER_DISTANCE = 16

/**
 * The default render distance for packet entities, in 16x16x16-block chunk sections.
 */
const val DEFAULT_PACKET_ENTITY_RENDER_DISTANCE = 8

/**
 * The key for the render distance in the [player's][Player] [persistent data container][Player.getPersistentDataContainer].
 */
private val RENDER_DISTANCE_KEY = NamespacedKey("nova", "entity_render_distance")

/**
 * The render distance for packet entities for this player, in 16x16x16-block chunk sections.
 * Stored under `nova:entity_render_distance` in the [player's][Player] [persistent data container][Player.getPersistentDataContainer].
 * 
 * Automatically coerced between [MIN_PACKET_ENTITY_RENDER_DISTANCE] and [MAX_PACKET_ENTITY_RENDER_DISTANCE].
 * Defaults to [DEFAULT_PACKET_ENTITY_RENDER_DISTANCE].
 */
var Player.packetEntityRenderDistance: Int
    get() = (persistentDataContainer.get(RENDER_DISTANCE_KEY, PersistentDataType.INTEGER) ?: DEFAULT_PACKET_ENTITY_RENDER_DISTANCE)
        .coerceIn(MIN_PACKET_ENTITY_RENDER_DISTANCE..MAX_PACKET_ENTITY_RENDER_DISTANCE)
    set(value) {
        val oldDistance = packetEntityRenderDistance
        val newDistance = value.coerceIn(MIN_PACKET_ENTITY_RENDER_DISTANCE..MAX_PACKET_ENTITY_RENDER_DISTANCE)
        if (oldDistance == newDistance)
            return
        
        persistentDataContainer.set(RENDER_DISTANCE_KEY, PersistentDataType.INTEGER, newDistance)
        PacketEntityManager.queueRenderDistanceChange(this, newDistance)
    }

/**
 * Initializes the packet entity manager, registering event listeners under [plugin].
 * Must be called [on enable][JavaPlugin.onEnable] for packet entities to work.
 * Does not need to be called by Nova addons as this is already done by Nova itself.
 */
fun initPacketEntityManager(plugin: JavaPlugin) {
    PacketEntityManager.init(plugin)
}

/**
 * Refreshes all packet entities by despawning and respawning them.
 */
fun Player.refreshPacketEntities() {
    PacketEntityManager.queueResendAll(this)
}