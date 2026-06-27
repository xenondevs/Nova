package xyz.xenondevs.nova.packetentity

import net.minecraft.network.protocol.game.ClientboundBundlePacket
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.world.WorldUnloadEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.joml.Vector3d
import org.slf4j.Logger
import xyz.xenondevs.nova.network.event.PacketHandler
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.network.event.serverbound.ServerboundInteractPacketEvent
import xyz.xenondevs.nova.network.send
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * The minimum render distance for packet entities, in chunk sections (16x16x16 blocks).
 */
const val MIN_PACKET_ENTITY_RENDER_DISTANCE = 0

/**
 * The maximum render distance for packet entities, in chunk sections (16x16x16 blocks).
 */
const val MAX_PACKET_ENTITY_RENDER_DISTANCE = 16

/**
 * The default render distance for packet entities, in chunk sections (16x16x16 blocks).
 */
const val DEFAULT_PACKET_ENTITY_RENDER_DISTANCE = 8

/**
 * The key for the render distance in the [player's][Player] [persistent data container][Player.getPersistentDataContainer].
 */
private val RENDER_DISTANCE_KEY = NamespacedKey("nova", "entity_render_distance")

/**
 * The render distance for packet entities for this player, in chunk sections (16x16x16 blocks).
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

private data class PlayerViewRegion(val center: ChunkSec, val range: Int)

private data class PlayerChunkChange(val player: Player, val to: PlayerViewRegion?)

private data class PlayerInteraction(val player: Player, val entityId: Int, val location: Vector3d)

internal class PacketEntityManager(private val world: World) {
    
    private val chunkSecViewers = HashMap<ChunkSec, HashSet<Player>>()
    private val playerViewRegions = HashMap<Player, PlayerViewRegion>()
    private val chunkSecEntities = HashMap<PacketEntityLod, HashMap<ChunkSec, HashSet<PacketEntityImpl<*>>>>()
    private val entitiesById = HashMap<Int, PacketEntityNode<*>>()
    
    private val entityUpdateQueue = ConcurrentLinkedQueue<PacketEntityImpl<*>>()
    private val ccQueue = ConcurrentLinkedQueue<PlayerChunkChange>()
    private val interactionQueue = ConcurrentLinkedQueue<PlayerInteraction>()
    
    @Volatile
    private var isRunning = true
    
    init {
        Thread(this::tick, "Nova PacketEntityManager - ${world.key.asString()}").start()
    }
    
    fun queueUpdate(entity: PacketEntityImpl<*>) {
        check(isRunning) { "PacketEntityManager for world ${world.key.asString()} was shut down" }
        entityUpdateQueue += entity
    }
    
    fun queueInteraction(player: Player, entityId: Int, location: Vector3d) {
        check(isRunning) { "PacketEntityManager for world ${world.key.asString()} was shut down" }
        interactionQueue += PlayerInteraction(player, entityId, location)
    }
    
    fun shutdown() {
        isRunning = false
    }
    
    private fun tick() {
        while (isRunning && !Bukkit.isStopping()) {
            try {
                generateSequence { entityUpdateQueue.poll() }.forEach(::handleEntityUpdate)
                generateSequence { ccQueue.poll() }.forEach { (player, to) ->
                    handlePlayerChunkChange(player, to)
                }
                generateSequence { interactionQueue.poll() }.forEach { (player, entityId, location) ->
                    handleInteract(player, entityId, location)
                }
            } catch (t: Throwable) {
                logger.error("Exception in PacketEntityManager tick", t)
            }
            Thread.sleep(50)
        }
    }
    
    private fun handleEntityUpdate(entity: PacketEntityImpl<*>) {
        entity.markUpdateDequeued()
        
        val shouldBeSpawned = entity.shouldBeSpawned
        when {
            shouldBeSpawned && !entity.isSpawned -> {
                entity.observe(entity)
                entity.applyChangesWithoutPackets()
                handleSpawn(entity)
                entity.isSpawned = true
            }
            
            !shouldBeSpawned && entity.isSpawned -> {
                handleDespawn(entity)
                entity.isSpawned = false
                entity.unobserve()
                entity.applyChangesWithoutPackets()
            }
            
            shouldBeSpawned -> handleFlush(entity)
            else -> entity.applyChangesWithoutPackets()
        }
    }
    
    private fun handleDespawn(entity: PacketEntityImpl<*>) {
        val pos = entity.actualLocation.chunkSec
        val removePacket = entity.graphRemovePacket
        chunkSecViewers[pos]?.forEach { viewer ->
            val region = playerViewRegions[viewer] ?: return@forEach
            if (isVisible(entity, viewer, region, pos, entity.actualViewerWhitelist, entity.actualViewerBlacklist)) {
                viewer.send(removePacket)
                entity.runDespawnHandlers(viewer)
            }
        }
        entity.graphEntities.forEach { entitiesById -= it.id }
        removeChunkSecEntity(pos, entity)
    }
    
    private fun handleSpawn(entity: PacketEntityImpl<*>) {
        val pos = entity.actualLocation.chunkSec
        val spawnPacket = entity.spawnBundlePacket
        entitiesByChunk(entity.lod).getOrPut(pos, ::HashSet) += entity
        entity.graphEntities.forEach { entitiesById[it.id] = it }
        chunkSecViewers[pos]?.forEach { viewer ->
            val region = playerViewRegions[viewer] ?: return@forEach
            if (isVisible(entity, viewer, region, pos, entity.actualViewerWhitelist, entity.actualViewerBlacklist)) {
                viewer.send(spawnPacket)
                entity.runSpawnHandlers(viewer)
            }
        }
    }
    
    private fun handleFlush(entity: PacketEntityImpl<*>) {
        val fromPos = entity.actualLocation.chunkSec
        val fromViewerWhitelist = entity.actualViewerWhitelist
        val fromViewerBlacklist = entity.actualViewerBlacklist
        val packets = entity.flush()
        val toPos = entity.actualLocation.chunkSec
        val removePacket = entity.graphRemovePacket
        var spawnPacket: ClientboundBundlePacket? = null
        
        if (toPos != fromPos)
            moveChunkSecEntity(entity, fromPos, toPos)
        
        val candidates = HashSet<Player>()
        chunkSecViewers[fromPos]?.let(candidates::addAll)
        chunkSecViewers[toPos]?.let(candidates::addAll)
        for (viewer in candidates) {
            val region = playerViewRegions[viewer] ?: continue
            val wasVisible = isVisible(entity, viewer, region, fromPos, fromViewerWhitelist, fromViewerBlacklist)
            val isVisible = isVisible(entity, viewer, region, toPos, entity.actualViewerWhitelist, entity.actualViewerBlacklist)
            when {
                wasVisible && !isVisible -> {
                    viewer.send(removePacket)
                    entity.runDespawnHandlers(viewer)
                }
                
                !wasVisible && isVisible -> {
                    viewer.send(spawnPacket ?: entity.spawnBundlePacket.also { spawnPacket = it })
                    entity.runSpawnHandlers(viewer)
                }
                
                wasVisible && packets.isNotEmpty() -> viewer.send(packets)
            }
        }
    }
    
    private fun handlePlayerChunkChange(player: Player, to: PlayerViewRegion?) {
        val from = playerViewRegions[player]
        if (from == to)
            return
        
        updateBroadViewerIndex(player, from, to)
        
        for (lod in PacketEntityLod.entries) {
            forEachVisibleChunkSecDifference(from, to, lod) { chunk ->
                entitiesAt(lod, chunk)?.forEach { entity ->
                    if (isAllowedViewer(entity.actualViewerWhitelist, entity.actualViewerBlacklist, player)) {
                        player.send(entity.graphRemovePacket)
                        entity.runDespawnHandlers(player)
                    }
                }
            }
            forEachVisibleChunkSecDifference(to, from, lod) { chunk ->
                entitiesAt(lod, chunk)?.forEach { entity ->
                    if (isAllowedViewer(entity.actualViewerWhitelist, entity.actualViewerBlacklist, player)) {
                        player.send(entity.spawnBundlePacket)
                        entity.runSpawnHandlers(player)
                    }
                }
            }
        }
        
        if (to == null)
            playerViewRegions -= player
        else
            playerViewRegions[player] = to
    }
    
    private fun updateBroadViewerIndex(player: Player, from: PlayerViewRegion?, to: PlayerViewRegion?) {
        when {
            from == null && to != null ->
                forEachChunkSecInRange(to.center, to.range) { addChunkSecViewer(it, player) }
            
            from != null && to == null ->
                forEachChunkSecInRange(from.center, from.range) { removeChunkSecViewer(it, player) }
            
            from != null && to != null -> {
                forEachChunkSecInRangeDifference(to.center, to.range, from.center, from.range) {
                    addChunkSecViewer(it, player)
                }
                forEachChunkSecInRangeDifference(from.center, from.range, to.center, to.range) {
                    removeChunkSecViewer(it, player)
                }
            }
            
            else -> throw AssertionError()
        }
    }
    
    private fun forEachVisibleChunkSecDifference(
        region: PlayerViewRegion?,
        excludedRegion: PlayerViewRegion?,
        lod: PacketEntityLod,
        run: (ChunkSec) -> Unit
    ) {
        if (region == null)
            return
        
        val minRange = lod.minRange
        val maxRange = minOf(lod.maxRange, region.range)
        if (excludedRegion == null) {
            forEachChunkSecInDonut(region.center, minRange, maxRange, run)
        } else {
            val excludedMinRange = lod.minRange
            val excludedMaxRange = minOf(lod.maxRange, excludedRegion.range)
            forEachChunkSecInDonutDifference(
                region.center, minRange, maxRange,
                excludedRegion.center, excludedMinRange, excludedMaxRange,
                run
            )
        }
    }
    
    private fun moveChunkSecEntity(entity: PacketEntityImpl<*>, from: ChunkSec, to: ChunkSec) {
        removeChunkSecEntity(from, entity)
        entitiesByChunk(entity.lod).getOrPut(to, ::HashSet) += entity
    }
    
    private fun handleInteract(player: Player, entityId: Int, interactLocation: Vector3d) {
        val entity = entitiesById[entityId]
            ?: return
        
        entity.runInteractHandlers(player, interactLocation)
    }
    
    private fun isVisible(
        entity: PacketEntityImpl<*>,
        player: Player,
        region: PlayerViewRegion,
        entityPos: ChunkSec,
        viewerWhitelist: Set<UUID>?,
        viewerBlacklist: Set<UUID>
    ): Boolean {
        if (!isAllowedViewer(viewerWhitelist, viewerBlacklist, player))
            return false
        return entityPos.isVisibleFrom(region.center, entity.lod, region.range)
    }
    
    private fun addChunkSecViewer(chunk: ChunkSec, player: Player) {
        chunkSecViewers.getOrPut(chunk, ::HashSet) += player
    }
    
    private fun removeChunkSecViewer(chunk: ChunkSec, player: Player) {
        val viewers = chunkSecViewers[chunk]
            ?: return
        viewers.remove(player)
        if (chunkSecViewers[chunk]?.isEmpty() == true)
            chunkSecViewers -= chunk
    }
    
    private fun removeChunkSecEntity(chunk: ChunkSec, entity: PacketEntityImpl<*>) {
        val entitiesByChunk = entitiesByChunk(entity.lod)
        val entities = entitiesByChunk[chunk]
            ?: return
        entities.remove(entity)
        if (entities.isEmpty())
            entitiesByChunk -= chunk
    }
    
    private fun entitiesByChunk(lod: PacketEntityLod): HashMap<ChunkSec, HashSet<PacketEntityImpl<*>>> =
        chunkSecEntities.getOrPut(lod, ::HashMap)
    
    private fun entitiesAt(lod: PacketEntityLod, chunk: ChunkSec): Set<PacketEntityImpl<*>>? =
        chunkSecEntities[lod]?.get(chunk)
    
    private fun isAllowedViewer(viewerWhitelist: Set<UUID>?, viewerBlacklist: Set<UUID>, player: Player): Boolean {
        val uuid = player.uniqueId
        return uuid !in viewerBlacklist && (viewerWhitelist == null || uuid in viewerWhitelist)
    }
    
    companion object : Listener, PacketListener {
        
        lateinit var logger: Logger private set
        private lateinit var plugin: JavaPlugin
        private val managers = ConcurrentHashMap<World, PacketEntityManager>()
        private val mainThreadQueue = ConcurrentLinkedQueue<() -> Unit>()
        
        fun init(plugin: JavaPlugin) {
            this.plugin = plugin
            logger = plugin.componentLogger
            
            Bukkit.getOnlinePlayers().forEach { player ->
                val region = PlayerViewRegion(player.location.chunkSec, player.packetEntityRenderDistance)
                get(player.world).ccQueue += PlayerChunkChange(player, region)
            }
            
            Bukkit.getPluginManager().registerEvents(this, plugin)
            registerPacketListener()
            
            Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
                generateSequence { mainThreadQueue.poll() }
                    .forEach {
                        try {
                            it()
                        } catch (t: Throwable) {
                            logger.error("Exception in PacketEntityManager main thread task", t)
                        }
                    }
            }, 1L, 1L)
        }
        
        operator fun get(world: World): PacketEntityManager =
            managers.computeIfAbsent(world, ::PacketEntityManager)
        
        fun queueRenderDistanceChange(player: Player, to: Int) {
            val center = player.location.chunkSec
            get(player.world).ccQueue += PlayerChunkChange(player, PlayerViewRegion(center, to))
        }
        
        fun queueMainThreadTask(run: () -> Unit) {
            mainThreadQueue += run
        }
        
        @PacketHandler
        private fun handleInteract(event: ServerboundInteractPacketEvent) {
            val location = event.location
            managers[event.player.world]?.queueInteraction(
                event.player,
                event.entityId,
                Vector3d(location.x, location.y, location.z)
            )
        }
        
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        private fun handleMove(event: PlayerMoveEvent) {
            handleMove(event.player, event.from, event.to)
        }
        
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        private fun handleTeleport(event: PlayerTeleportEvent) {
            handleMove(event.player, event.from, event.to)
        }
        
        @EventHandler(priority = EventPriority.MONITOR)
        private fun handleSpawn(event: PlayerRespawnEvent) {
            handleMove(event.player, event.player.location, event.respawnLocation)
        }
        
        private fun handleMove(player: Player, from: Location, to: Location) {
            val fromWorld = from.world
            val toWorld = to.world
            
            require(fromWorld != null)
            require(toWorld != null)
            
            val fromChunk = from.chunkSec
            val toChunk = to.chunkSec
            if (fromChunk == toChunk)
                return
            
            val range = player.packetEntityRenderDistance
            if (fromWorld != toWorld) {
                get(fromWorld).ccQueue += PlayerChunkChange(player, null)
                get(toWorld).ccQueue += PlayerChunkChange(player, PlayerViewRegion(toChunk, range))
            } else {
                get(fromWorld).ccQueue += PlayerChunkChange(player, PlayerViewRegion(toChunk, range))
            }
        }
        
        @EventHandler(priority = EventPriority.MONITOR)
        private fun handleJoin(event: PlayerJoinEvent) {
            val player = event.player
            val region = PlayerViewRegion(player.location.chunkSec, player.packetEntityRenderDistance)
            get(player.world).ccQueue += PlayerChunkChange(player, region)
        }
        
        @EventHandler(priority = EventPriority.MONITOR)
        private fun handleQuit(event: PlayerQuitEvent) {
            val player = event.player
            get(player.world).ccQueue += PlayerChunkChange(player, null)
        }
        
        @EventHandler(priority = EventPriority.MONITOR)
        private fun handleWorldUnload(event: WorldUnloadEvent) {
            managers.remove(event.world)?.shutdown()
        }
        
    }
    
}

/**
 * Initializes the packet entity manager, registering event listeners under [plugin].
 * Must be called [on enable][JavaPlugin.onEnable] for packet entities to work.
 * Does not need to be called by Nova addons as this is already done by Nova itself.
 */
fun initPacketEntityManager(plugin: JavaPlugin) {
    PacketEntityManager.init(plugin)
}
