package xyz.xenondevs.nova.packetentity

import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.craftbukkit.CraftEquipmentSlot
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
import org.bukkit.plugin.java.JavaPlugin
import org.joml.Vector3d
import org.slf4j.Logger
import xyz.xenondevs.nova.network.event.PacketHandler
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.network.event.serverbound.ServerboundAttackPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundInteractPacketEvent
import xyz.xenondevs.nova.network.send
import xyz.xenondevs.nova.world.InteractionResult
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

private data class PlayerViewRegion(
    val sectionCenter: TrackingCell,
    val nearCenter: TrackingCell,
    val range: Int
) {
    
    fun center(visibility: PacketEntityVisibility): TrackingCell =
        if (visibility == PacketEntityVisibility.NEAR) nearCenter else sectionCenter
    
    fun maxRange(visibility: PacketEntityVisibility): Int =
        minOf(visibility.maxRange, range)
    
}

private fun Location.playerViewRegion(range: Int): PlayerViewRegion =
    PlayerViewRegion(
        trackingCell(PacketEntityVisibility.STANDARD),
        trackingCell(PacketEntityVisibility.NEAR),
        range
    )

private data class PlayerViewChange(val player: Player, val to: PlayerViewRegion?)

internal class PacketEntityManager(private val world: World) {
    
    private val sectionViewers = HashMap<TrackingCell, HashSet<Player>>()
    private val nearViewers = HashMap<TrackingCell, HashSet<Player>>()
    private val playerViewRegions = HashMap<Player, PlayerViewRegion>()
    private val trackingCellEntities = HashMap<PacketEntityVisibility, HashMap<TrackingCell, HashSet<PacketEntityImpl<*>>>>()
    private val entitiesById = ConcurrentHashMap<Int, PacketEntityNodeImpl<*>>()
    
    private val entityUpdateQueue = ConcurrentLinkedQueue<PacketEntityImpl<*>>()
    private val viewChangeQueue = ConcurrentLinkedQueue<PlayerViewChange>()
    
    @Volatile
    private var isRunning = true
    
    init {
        Thread(this::tick, "Nova PacketEntityManager - ${world.key.asString()}").start()
    }
    
    fun queueUpdate(entity: PacketEntityImpl<*>) {
        check(isRunning) { "PacketEntityManager for world ${world.key.asString()} was shut down" }
        entityUpdateQueue += entity
    }
    
    fun getEntity(entityId: Int): PacketEntityNodeImpl<*>? =
        entitiesById[entityId]
    
    fun shutdown() {
        isRunning = false
    }
    
    private fun tick() {
        while (isRunning && !Bukkit.isStopping()) {
            try {
                generateSequence { entityUpdateQueue.poll() }.forEach(::handleEntityUpdate)
                generateSequence { viewChangeQueue.poll() }.forEach { (player, to) ->
                    handlePlayerViewChange(player, to)
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
        val pos = entity.actualLocation.trackingCell(entity.visibility)
        val removePacket = entity.graphRemovePacket
        viewersByCell(entity.visibility)[pos]?.forEach { viewer ->
            val region = playerViewRegions[viewer] ?: return@forEach
            if (isVisible(entity, viewer, region, pos, entity.actualViewerWhitelist, entity.actualViewerBlacklist)) {
                viewer.send(removePacket)
                entity.runDespawnHandlers(viewer)
            }
        }
        entity.graphEntities.forEach { entitiesById -= it.id }
        removeTrackingCellEntity(pos, entity)
    }
    
    private fun handleSpawn(entity: PacketEntityImpl<*>) {
        val pos = entity.actualLocation.trackingCell(entity.visibility)
        val spawnPacket = entity.spawnBundlePacket
        entitiesByCell(entity.visibility).getOrPut(pos, ::HashSet) += entity
        entity.graphEntities.forEach { entitiesById[it.id] = it }
        viewersByCell(entity.visibility)[pos]?.forEach { viewer ->
            val region = playerViewRegions[viewer] ?: return@forEach
            if (isVisible(entity, viewer, region, pos, entity.actualViewerWhitelist, entity.actualViewerBlacklist)) {
                viewer.send(spawnPacket)
                entity.runSpawnHandlers(viewer)
            }
        }
    }
    
    private fun handleFlush(entity: PacketEntityImpl<*>) {
        val fromPos = entity.actualLocation.trackingCell(entity.visibility)
        val fromViewerWhitelist = entity.actualViewerWhitelist
        val fromViewerBlacklist = entity.actualViewerBlacklist
        val packets = entity.flush()
        val toPos = entity.actualLocation.trackingCell(entity.visibility)
        val removePacket = entity.graphRemovePacket
        var spawnPacket: ClientboundBundlePacket? = null
        
        if (toPos != fromPos)
            moveTrackingCellEntity(entity, fromPos, toPos)
        
        val candidates = HashSet<Player>()
        val viewersByCell = viewersByCell(entity.visibility)
        viewersByCell[fromPos]?.let(candidates::addAll)
        viewersByCell[toPos]?.let(candidates::addAll)
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
    
    private fun handlePlayerViewChange(player: Player, to: PlayerViewRegion?) {
        val from = playerViewRegions[player]
        if (from == to)
            return
        
        updateViewerIndex(
            player,
            sectionViewers,
            from?.sectionCenter,
            from?.range ?: 0,
            to?.sectionCenter,
            to?.range ?: 0
        )
        updateViewerIndex(
            player,
            nearViewers,
            from?.nearCenter,
            from?.maxRange(PacketEntityVisibility.NEAR) ?: 0,
            to?.nearCenter,
            to?.maxRange(PacketEntityVisibility.NEAR) ?: 0
        )
        
        for (visibility in PacketEntityVisibility.entries) {
            forEachVisibleTrackingCellDifference(from, to, visibility) { cell ->
                entitiesAt(visibility, cell)?.forEach { entity ->
                    if (isAllowedViewer(entity.actualViewerWhitelist, entity.actualViewerBlacklist, player)) {
                        player.send(entity.graphRemovePacket)
                        entity.runDespawnHandlers(player)
                    }
                }
            }
            forEachVisibleTrackingCellDifference(to, from, visibility) { cell ->
                entitiesAt(visibility, cell)?.forEach { entity ->
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
    
    private fun updateViewerIndex(
        player: Player,
        viewersByCell: HashMap<TrackingCell, HashSet<Player>>,
        fromCenter: TrackingCell?,
        fromRange: Int,
        toCenter: TrackingCell?,
        toRange: Int
    ) {
        when {
            fromCenter == null && toCenter != null ->
                forEachTrackingCellInRange(toCenter, toRange) {
                    viewersByCell.getOrPut(it, ::HashSet) += player
                }
            
            fromCenter != null && toCenter == null ->
                forEachTrackingCellInRange(fromCenter, fromRange) {
                    removeTrackingCellViewer(viewersByCell, it, player)
                }
            
            fromCenter != null && toCenter != null -> {
                forEachTrackingCellInRangeDifference(toCenter, toRange, fromCenter, fromRange) {
                    viewersByCell.getOrPut(it, ::HashSet) += player
                }
                forEachTrackingCellInRangeDifference(fromCenter, fromRange, toCenter, toRange) {
                    removeTrackingCellViewer(viewersByCell, it, player)
                }
            }
            
            else -> throw AssertionError()
        }
    }
    
    private fun forEachVisibleTrackingCellDifference(
        region: PlayerViewRegion?,
        excludedRegion: PlayerViewRegion?,
        visibility: PacketEntityVisibility,
        run: (TrackingCell) -> Unit
    ) {
        if (region == null)
            return
        
        val center = region.center(visibility)
        val minRange = visibility.minRange
        val maxRange = region.maxRange(visibility)
        if (excludedRegion == null) {
            forEachTrackingCellInDonut(center, minRange, maxRange, run)
        } else {
            val excludedMinRange = visibility.minRange
            val excludedMaxRange = excludedRegion.maxRange(visibility)
            forEachTrackingCellInDonutDifference(
                center, minRange, maxRange,
                excludedRegion.center(visibility), excludedMinRange, excludedMaxRange,
                run
            )
        }
    }
    
    private fun moveTrackingCellEntity(entity: PacketEntityImpl<*>, from: TrackingCell, to: TrackingCell) {
        removeTrackingCellEntity(from, entity)
        entitiesByCell(entity.visibility).getOrPut(to, ::HashSet) += entity
    }
    
    private fun isVisible(
        entity: PacketEntityImpl<*>,
        player: Player,
        region: PlayerViewRegion,
        entityPos: TrackingCell,
        viewerWhitelist: Set<UUID>?,
        viewerBlacklist: Set<UUID>
    ): Boolean {
        if (!isAllowedViewer(viewerWhitelist, viewerBlacklist, player))
            return false
        return entityPos.isVisibleFrom(
            region.center(entity.visibility),
            entity.visibility,
            region.range
        )
    }
    
    private fun removeTrackingCellViewer(
        viewersByCell: HashMap<TrackingCell, HashSet<Player>>,
        cell: TrackingCell,
        player: Player
    ) {
        val viewers = viewersByCell[cell]
            ?: return
        viewers.remove(player)
        if (viewers.isEmpty())
            viewersByCell -= cell
    }
    
    private fun removeTrackingCellEntity(cell: TrackingCell, entity: PacketEntityImpl<*>) {
        val entitiesByCell = entitiesByCell(entity.visibility)
        val entities = entitiesByCell[cell]
            ?: return
        entities.remove(entity)
        if (entities.isEmpty())
            entitiesByCell -= cell
    }
    
    private fun entitiesByCell(
        visibility: PacketEntityVisibility
    ): HashMap<TrackingCell, HashSet<PacketEntityImpl<*>>> =
        trackingCellEntities.getOrPut(visibility, ::HashMap)
    
    private fun entitiesAt(
        visibility: PacketEntityVisibility,
        cell: TrackingCell
    ): Set<PacketEntityImpl<*>>? =
        trackingCellEntities[visibility]?.get(cell)
    
    private fun viewersByCell(
        visibility: PacketEntityVisibility
    ): HashMap<TrackingCell, HashSet<Player>> =
        if (visibility == PacketEntityVisibility.NEAR) nearViewers else sectionViewers
    
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
                val region = player.location.playerViewRegion(player.packetEntityRenderDistance)
                get(player.world).viewChangeQueue += PlayerViewChange(player, region)
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
            get(player.world).viewChangeQueue += PlayerViewChange(player, player.location.playerViewRegion(to))
        }
        
        fun queueResendAll(player: Player) {
            val manager = get(player.world)
            val region = player.location.playerViewRegion(player.packetEntityRenderDistance)
            manager.viewChangeQueue += PlayerViewChange(player, null)
            manager.viewChangeQueue += PlayerViewChange(player, region)
        }
        
        fun queueMainThreadTask(run: () -> Unit) {
            mainThreadQueue += run
        }
        
        private fun getEntity(player: Player, entityId: Int): PacketEntityNodeImpl<*>? =
            managers[player.world]?.getEntity(entityId)
        
        @JvmStatic
        fun handleAttack(player: Player, entityId: Int): Boolean {
            val entity = getEntity(player, entityId)
                ?: return false
            entity.runAttackHandlers(player)
            return true
        }
        
        @JvmStatic
        fun handleInteract(
            player: Player,
            entityId: Int,
            interactionHand: InteractionHand,
            location: Vec3
        ): InteractionResult? {
            val entity = getEntity(player, entityId)
                ?: return null
            
            val hand = CraftEquipmentSlot.getHand(interactionHand)
            val result = entity.runInteractHandlers(
                player,
                hand,
                Vector3d(location.x, location.y, location.z)
            )
            if (result is InteractionResult.Success)
                result.performActions(player, hand)
            return result
        }
        
        @PacketHandler
        private fun handleInteract(event: ServerboundInteractPacketEvent) {
            val manager = managers[event.player.world] ?: return
            val entity = manager.getEntity(event.entityId) ?: return
            entity.runInteractAsyncHandlers(event)
        }
        
        @PacketHandler
        private fun handleAttack(event: ServerboundAttackPacketEvent) {
            val manager = managers[event.player.world] ?: return
            val entity = manager.getEntity(event.entityId) ?: return
            entity.runAttackAsyncHandlers(event)
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
            
            val range = player.packetEntityRenderDistance
            val fromRegion = from.playerViewRegion(range)
            val toRegion = to.playerViewRegion(range)
            if (fromWorld == toWorld && fromRegion == toRegion)
                return
            
            if (fromWorld != toWorld) {
                get(fromWorld).viewChangeQueue += PlayerViewChange(player, null)
                get(toWorld).viewChangeQueue += PlayerViewChange(player, toRegion)
            } else {
                get(fromWorld).viewChangeQueue += PlayerViewChange(player, toRegion)
            }
        }
        
        @EventHandler(priority = EventPriority.MONITOR)
        private fun handleJoin(event: PlayerJoinEvent) {
            val player = event.player
            val region = player.location.playerViewRegion(player.packetEntityRenderDistance)
            get(player.world).viewChangeQueue += PlayerViewChange(player, region)
        }
        
        @EventHandler(priority = EventPriority.MONITOR)
        private fun handleQuit(event: PlayerQuitEvent) {
            val player = event.player
            get(player.world).viewChangeQueue += PlayerViewChange(player, null)
        }
        
        @EventHandler(priority = EventPriority.MONITOR)
        private fun handleWorldUnload(event: WorldUnloadEvent) {
            managers.remove(event.world)?.shutdown()
        }
        
    }
    
}