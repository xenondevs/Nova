package xyz.xenondevs.nova.world.block.state.model

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import io.papermc.paper.math.BlockPosition
import io.papermc.paper.math.Position.block
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.event.world.WorldUnloadEvent
import xyz.xenondevs.commons.collections.mapToIntArray
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.packetentity.PacketBlockDisplay
import xyz.xenondevs.nova.packetentity.PacketItemDisplay
import xyz.xenondevs.nova.packetentity.isGlowing
import xyz.xenondevs.nova.util.levelChunk
import xyz.xenondevs.nova.util.nmsEntity
import xyz.xenondevs.nova.util.nmsPos
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.pos
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

private class DisplayEntities(
    val provider: DisplayEntityBlockModelProvider,
    val displayEntities: List<PacketItemDisplay>,
    val colliderEntities: List<PacketBlockDisplay>
) {
    
    fun despawn() {
        displayEntities.forEach(PacketItemDisplay::despawn)
        colliderEntities.forEach(PacketBlockDisplay::despawn)
    }
    
}

private class FallingBlockModel(
    private val displays: List<PacketItemDisplay>,
    private val passengers: AtomicReference<IntArray>
) {
    
    init {
        displays.forEach(PacketItemDisplay::spawn)
    }
    
    fun update(location: Location, viewers: Set<UUID>, realPassengers: IntArray) {
        passengers.set(realPassengers)
        displays.forEach { display ->
            display.location = location
            display.viewerWhitelist = viewers
        }
    }
    
    fun detach() {
        displays.forEach(PacketItemDisplay::despawn)
    }
    
}

internal class DisplayEntityModelProviderManager private constructor(private val world: World) {
    
    private val worldId = world.uid
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "Nova DisplayEntityManager - ${world.key.asString()}")
    }
    private val entitiesByChunk = HashMap<ChunkPos, HashMap<BlockPosition, DisplayEntities>>()
    private val displayEntities = ConcurrentHashMap<BlockPosition, List<PacketItemDisplay>>()
    private val fallingBlocks = Int2ObjectOpenHashMap<FallingBlockModel>()
    
    @Volatile
    private var closed = false
    
    fun submit(task: () -> Unit) {
        if (closed)
            return
        executor.execute {
            try {
                task()
            } catch (t: Throwable) {
                LOGGER.error("Exception in display entity manager for ${world.key.asString()}", t)
            }
        }
    }
    
    fun load(pos: BlockPosition, provider: DisplayEntityBlockModelProvider): Unit = submit {
        val chunkPos = ChunkPos(worldId, pos.blockX() shr 4, pos.blockZ() shr 4)
        val blocks = entitiesByChunk.getOrPut(chunkPos, ::HashMap)
        val previous = blocks[pos]
        val displays = provider.createDisplayEntities(world, pos, previous?.displayEntities.orEmpty())
        val colliders = if (previous?.provider?.info?.extraColliders == provider.info.extraColliders) {
            previous.colliderEntities
        } else {
            previous?.colliderEntities?.forEach(PacketBlockDisplay::despawn)
            provider.createColliderEntities(world, pos)
        }
        val entities = DisplayEntities(provider, displays, colliders)
        blocks[pos] = entities
        displayEntities[pos] = entities.displayEntities.toList()
    }
    
    fun getDisplayEntities(pos: BlockPosition): List<PacketItemDisplay>? = displayEntities[pos]
    
    fun remove(pos: BlockPosition): Unit = submit {
        val chunkPos = ChunkPos(worldId, pos.blockX() shr 4, pos.blockZ() shr 4)
        val blocks = entitiesByChunk[chunkPos] ?: return@submit
        blocks.remove(pos)?.despawn()
        displayEntities.remove(pos)
        if (blocks.isEmpty())
            entitiesByChunk.remove(chunkPos)
    }
    
    fun unloadChunk(chunkPos: ChunkPos): Unit = submit {
        entitiesByChunk.remove(chunkPos)?.forEach { [pos, entities] ->
            entities.despawn()
            displayEntities.remove(pos)
        }
    }
    
    fun setColliderOutlines(enabled: Boolean): Unit = submit {
        entitiesByChunk.values.asSequence()
            .flatMap { it.values }
            .flatMap { it.colliderEntities }
            .flatMap { it.passengers }
            .forEach { it.metadata.isGlowing = enabled }
    }
    
    fun loadFalling(
        id: Int,
        location: Location,
        height: Float,
        viewers: Set<UUID>,
        passengers: IntArray,
        provider: DisplayEntityBlockModelProvider
    ): Unit = submit {
        if (!fallingBlocks.containsKey(id)) {
            val passengerIds = AtomicReference(passengers)
            val displays = provider.createFallingDisplays(id, location, height, viewers, passengerIds)
            fallingBlocks[id] = FallingBlockModel(displays, passengerIds)
        }
    }
    
    fun updateFalling(id: Int, location: Location, viewers: Set<UUID>, passengers: IntArray): Unit = submit {
        fallingBlocks[id]?.update(location, viewers, passengers)
    }
    
    fun unloadFalling(id: Int): Unit = submit {
        fallingBlocks.remove(id)?.detach()
    }
    
    fun shutdown() {
        closed = true
        try {
            executor.submit {
                entitiesByChunk.values.flatMap { it.values }.forEach(DisplayEntities::despawn)
                fallingBlocks.values.forEach(FallingBlockModel::detach)
                entitiesByChunk.clear()
                displayEntities.clear()
                fallingBlocks.clear()
            }.get()
        } finally {
            executor.shutdown()
        }
    }
    
    
    @InternalInit(stage = InternalInitStage.POST_WORLD)
    companion object : Listener {
        
        private val managers = ConcurrentHashMap<World, DisplayEntityModelProviderManager>()
        private val fallingBlocks = HashMap<FallingBlock, DisplayEntityModelProviderManager>()
        
        @Volatile
        var colliderOutlinesEnabled = false
            set(value) {
                field = value
                managers.values.forEach { it.setColliderOutlines(value) }
            }
        
        @InitFun
        private fun init() {
            registerEvents()
            Bukkit.getWorlds().forEach { world ->
                world.loadedChunks.forEach(::loadChunk)
                world.entities.asSequence().filterIsInstance<FallingBlock>().forEach(::loadFallingBlock)
            }
            runTaskTimer(0, 1) {
                val iterator = fallingBlocks.iterator()
                while (iterator.hasNext()) {
                    val [entity, manager] = iterator.next()
                    if (entity.isValid) {
                        manager.updateFalling(
                            entity.entityId,
                            entity.location.add(0.0, entity.height, 0.0),
                            entity.trackedBy.mapTo(HashSet(), Player::getUniqueId),
                            entity.passengers.mapToIntArray(Entity::getEntityId)
                        )
                    } else {
                        manager.unloadFalling(entity.entityId)
                        iterator.remove()
                    }
                }
            }
        }
        
        operator fun get(world: World): DisplayEntityModelProviderManager =
            managers.computeIfAbsent(world, ::DisplayEntityModelProviderManager)
        
        fun load(block: Block, provider: DisplayEntityBlockModelProvider) {
            get(block.world).load(block(block.x, block.y, block.z), provider)
        }
        
        fun remove(block: Block) {
            get(block.world).remove(block(block.x, block.y, block.z))
        }
        
        fun getDisplayEntities(block: Block): List<PacketItemDisplay>? =
            managers[block.world]?.getDisplayEntities(block(block.x, block.y, block.z))
        
        @EventHandler
        private fun handleChunkLoad(event: ChunkLoadEvent) {
            loadChunk(event.chunk)
        }
        
        @EventHandler
        private fun handleChunkUnload(event: ChunkUnloadEvent) {
            get(event.world).unloadChunk(event.chunk.pos)
        }
        
        @EventHandler(priority = EventPriority.LOWEST)
        private fun handleWorldUnload(event: WorldUnloadEvent) {
            managers.remove(event.world)?.shutdown()
            fallingBlocks.entries.removeIf { it.key.world == event.world }
        }
        
        @EventHandler
        private fun handleEntityAdd(event: EntityAddToWorldEvent) {
            (event.entity as? FallingBlock)?.let(::loadFallingBlock)
        }
        
        @EventHandler
        private fun handleEntityRemove(event: EntityRemoveFromWorldEvent) {
            val entity = event.entity as? FallingBlock ?: return
            fallingBlocks.remove(entity)?.unloadFalling(entity.entityId)
        }
        
        private fun loadChunk(chunk: Chunk) {
            val levelChunk = chunk.levelChunk
            val providerMaps = HashMap<NovaBlock, Map<BlockState, BlockModelProvider>>()
            
            fun getProvider(state: BlockState): DisplayEntityBlockModelProvider? {
                val block = state.block as? NovaBlock ?: return null
                val providers = providerMaps.getOrPut(block) { block.modelProviders.get() }
                return providers[state] as? DisplayEntityBlockModelProvider
            }
            
            val blockX = chunk.x shl 4
            val blockZ = chunk.z shl 4
            for (sectionIndex in levelChunk.sections.indices) {
                val chunkSection = levelChunk.sections[sectionIndex]
                if (!chunkSection.maybeHas { getProvider(it) != null })
                    continue
                
                val blockY = levelChunk.getSectionYFromSectionIndex(sectionIndex) shl 4
                for (y in 0..<16) for (z in 0..<16) for (x in 0..<16) {
                    val provider = getProvider(chunkSection.getBlockState(x, y, z)) ?: continue
                    provider.load(chunk.world.getBlockAt(blockX + x, blockY + y, blockZ + z))
                }
            }
        }
        
        private fun loadFallingBlock(entity: FallingBlock) {
            if (entity in fallingBlocks)
                return
            val state = entity.nmsEntity.blockState
            val provider = (state.block as? NovaBlock)?.modelProviders?.get()?.get(state) as? DisplayEntityBlockModelProvider
                ?: return
            val manager = get(entity.world)
            fallingBlocks[entity] = manager
            manager.loadFalling(
                entity.entityId,
                entity.location.add(0.0, entity.height, 0.0),
                entity.height.toFloat(),
                entity.trackedBy.mapTo(HashSet(), Player::getUniqueId),
                entity.passengers.mapToIntArray(Entity::getEntityId),
                provider
            )
        }
        
    }
    
}
