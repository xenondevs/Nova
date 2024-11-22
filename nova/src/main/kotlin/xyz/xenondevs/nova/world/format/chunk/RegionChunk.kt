package xyz.xenondevs.nova.world.format.chunk

import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import net.minecraft.world.level.GameRules
import org.bukkit.World
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.commons.collections.mapToIntArray
import xyz.xenondevs.commons.collections.removeIf
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.util.AsyncExecutor
import xyz.xenondevs.nova.util.ceilDiv
import xyz.xenondevs.nova.util.concurrent.checkServerThread
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.DefaultBlocks
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.block.tileentity.vanilla.VanillaTileEntity
import xyz.xenondevs.nova.world.format.BlockStateIdResolver
import xyz.xenondevs.nova.world.format.chunk.RegionizedChunk.Companion.packBlockPos
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.random.Random

internal class RegionChunk(
    val pos: ChunkPos,
    private val sections: Array<RegionChunkSection<NovaBlockState>> = Array(getSectionCount(pos.world!!)) { RegionChunkSection(BlockStateIdResolver) },
    private val vanillaTileEntityData: MutableMap<BlockPos, Compound> = HashMap(),
    private val tileEntityData: MutableMap<BlockPos, Compound> = HashMap(),
) : RegionizedChunk {
    
    private val lock = ReentrantLock()
    
    @Volatile
    var isEnabled = false
        private set
    @Volatile
    var hasBeenEnabled = false
        private set
    
    private var tickingAllowed = false
    private var isTicking = false
    
    private val world = pos.world!!
    private val level = world.serverLevel
    private val minHeight = world.minHeight
    private val maxHeight = world.maxHeight
    private val minSection = minHeight shr 4
    private val maxSection = maxHeight shr 4
    
    private val vanillaTileEntities: MutableMap<BlockPos, VanillaTileEntity> = HashMap()
    private val tileEntities: MutableMap<BlockPos, TileEntity> = ConcurrentHashMap() // concurrent to allow modifications during ticking
    
    private var randomTickBlockCounts = sections.mapToIntArray { it.countNonEmpty { state -> state.ticksRandomly } }
    private var randomTickBlockCount = randomTickBlockCounts.sum()
    private var tickingTileEntityCount = 0
    private var tick = 0
    private var coroutineSupervisor: Job? = null
    private var tickTask: BukkitTask? = null
    
    init {
        initVanillaTileEntities()
        initNovaTileEntities()
    }
    
    /**
     * Initializes the [VanillaTileEntities][VanillaTileEntity] in this chunk.
     */
    private fun initVanillaTileEntities() {
        for ((pos, data) in vanillaTileEntityData) {
            try {
                // temporary legacy conversion measure: nullable type to then be read from vanilla block state in enable()
                val type: VanillaTileEntity.Type? = data["type"]
                if (type != null) {
                    vanillaTileEntities[pos] = type.create(pos, data)
                }
            } catch (t: Throwable) {
                LOGGER.error("Failed to initialize vanilla tile entity pos=$pos, data=$data", t)
            }
        }
    }
    
    /**
     * Initializes the [TileEntities][TileEntity] in this chunk.
     */
    private fun initNovaTileEntities() {
        for ((pos, data) in tileEntityData) {
            val blockState = getBlockState(pos)
            if (blockState == null) {
                LOGGER.error("Failed to initialize tile entity at $pos because there is no block state")
                return
            }
            
            val block = blockState.block as? NovaTileEntityBlock
            if (blockState.block == DefaultBlocks.UNKNOWN) {
                // nova:unknown is the only non-tile-entity block that is allowed to have tile-entity data
                continue
            } else if (block == null) {
                LOGGER.error("Failed to initialize tile entity at $pos because ${blockState.block} is not a tile entity type")
                return
            }
            
            try {
                tileEntities[pos] = block.tileEntityConstructor(pos, blockState, data)
                if (block.tickrate > 0)
                    tickingTileEntityCount++
            } catch (t: Throwable) {
                LOGGER.error("Failed to initialize tile entity pos=$pos, blockState=$blockState, data=$data", t)
            }
        }
    }
    
    /**
     * Gets the [NovaBlockState] at the given [pos].
     */
    fun getBlockState(pos: BlockPos): NovaBlockState? = lock.withLock {
        return getSection(pos.y)?.get(pos.x and 0xF, pos.y and 0xF, pos.z and 0xF)
    }
    
    /**
     * Sets the [BlockState][state] at the given [pos] and returns the previous [NovaBlockState].
     */
    fun setBlockState(pos: BlockPos, state: NovaBlockState?): NovaBlockState? = lock.withLock {
        val sectionIdx = getSectionIndex(pos.y)
        val section = sections.getOrNull(sectionIdx)
            ?: return null
        val previous = section.set(pos.x and 0xF, pos.y and 0xF, pos.z and 0xF, state)
        
        if (previous != null && previous.ticksRandomly) {
            randomTickBlockCounts[sectionIdx]--
            randomTickBlockCount--
        }
        
        if (state != null) {
            if (state.ticksRandomly) {
                randomTickBlockCounts[sectionIdx]++
                randomTickBlockCount++
            }
            tileEntities[pos]?.blockState = state
        }
        
        reconsiderTicking()
        
        return previous
    }
    
    /**
     * Iterates over all non-empty block states in this chunk and calls the specified [action]
     * for each of them.
     */
    fun forEachNonEmpty(action: (pos: BlockPos, blockState: NovaBlockState) -> Unit): Unit = lock.withLock {
        for ((idx, section) in sections.withIndex()) {
            if (section.isEmpty())
                continue
            val bottomY = (idx shl 4) + minHeight
            
            section.container.forEachNonEmpty { x, y, z, blockState ->
                action(pos.blockPos(x, bottomY + y, z), blockState)
            }
        }
    }
    
    /**
     * Gets the [VanillaTileEntity] at the given [pos].
     */
    fun getVanillaTileEntity(pos: BlockPos): VanillaTileEntity? = lock.withLock {
        return vanillaTileEntities[pos]
    }
    
    /**
     * Gets a snapshot of all [VanillaTileEntities][VanillaTileEntity] in this chunk.
     */
    fun getVanillaTileEntities(): List<VanillaTileEntity> = lock.withLock {
        return ArrayList(vanillaTileEntities.values)
    }
    
    // temporary legacy-conversion related function
    fun setVanillaTileEntityData(pos: BlockPos, data: Compound): Unit = lock.withLock {
        vanillaTileEntityData[pos] = data
    }
    
    /**
     * Sets the [VanillaTileEntity][vte] at the given [pos].
     */
    fun setVanillaTileEntity(pos: BlockPos, vte: VanillaTileEntity?): VanillaTileEntity? = lock.withLock {
        val previous: VanillaTileEntity?
        if (vte == null) {
            previous = vanillaTileEntities.remove(pos)
            vanillaTileEntityData.remove(pos)
        } else {
            previous = vanillaTileEntities.put(pos, vte)
            vanillaTileEntityData[pos] = vte.data
            
            if (isEnabled) {
                try {
                    vte.handleEnable()
                } catch (t: Throwable) {
                    LOGGER.error("Failed to enable vanilla tile-entity $vte", t)
                }
            }
        }
        
        try {
            previous?.handleDisable()
        } catch (t: Throwable) {
            LOGGER.error("Failed to disable vanilla tile-entity $previous", t)
        }
        
        return previous
    }
    
    /**
     * Gets the [TileEntity] at the given [pos].
     */
    fun getTileEntity(pos: BlockPos): TileEntity? = lock.withLock {
        return tileEntities[pos]
    }
    
    /**
     * Gets a snapshot of all [TileEntities][TileEntity] in this chunk.
     */
    fun getTileEntities(): List<TileEntity> = lock.withLock {
        return tileEntities.values.toList()
    }
    
    /**
     * Sets the [tileEntity] at the given [pos].
     */
    fun setTileEntity(pos: BlockPos, tileEntity: TileEntity?): TileEntity? = lock.withLock {
        val previous: TileEntity?
        if (tileEntity == null) {
            previous = tileEntities.remove(pos)
            tileEntityData.remove(pos)
        } else {
            previous = tileEntities.put(pos, tileEntity)
            tileEntityData[pos] = tileEntity.data
            
            if (isEnabled) {
                tileEntity.isEnabled = true
                try {
                    tileEntity.handleEnable()
                } catch (t: Throwable) {
                    LOGGER.error("Failed to enable tile-entity $tileEntity", t)
                }
            }
            
            if (tileEntity.block.tickrate > 0) {
                tickingTileEntityCount++
                
                if (isTicking) {
                    try {
                        tileEntity.coroutineSupervisor = SupervisorJob(coroutineSupervisor)
                        tileEntity.handleEnableTicking()
                    } catch (t: Throwable) {
                        LOGGER.error("Failed to enable ticking for $tileEntity", t)
                    }
                }
            }
        }
        
        if (previous != null) {
            if (previous.block.tickrate > 0) {
                tickingTileEntityCount--
                
                if (isTicking) {
                    try {
                        previous.handleDisableTicking()
                    } catch(t: Throwable) {
                        LOGGER.error("Failed to disable ticking for $tileEntity", t)
                    }
                }
            }
            
            try {
                previous.handleDisable()
            } catch (t: Throwable) {
                LOGGER.error("Failed to disable tile-entity $previous", t)
            }
            previous.isEnabled = false
        }
        
        reconsiderTicking()
        
        return previous
    }
    
    /**
     * Gets the [RegionChunkSection] at the given [y] coordinate, or null if out of bounds.
     */
    private fun getSection(y: Int): RegionChunkSection<NovaBlockState>? =
        sections.getOrNull(getSectionIndex(y))
    
    /**
     * Gets the index of the [RegionChunkSection] at the given [y] coordinate.
     */
    private fun getSectionIndex(y: Int): Int {
        return (y - minHeight) shr 4
    }
    
    /**
     * Enables this RegionChunk.
     *
     * This loads all models and calls [TileEntity.handleEnable], [VanillaTileEntity.handleEnable].
     *
     * May only be called from the server thread.
     */
    fun enable() {
        checkServerThread()
        lock.withLock {
            if (isEnabled)
                return
            
            // temporary legacy conversion measure: init vanilla tile entities based on block type
            for ((pos, vteData) in vanillaTileEntityData) {
                if (pos in vanillaTileEntities)
                    continue
                
                val vteType = VanillaTileEntity.Type.of(pos.block.type)
                if (vteType != null) {
                    vanillaTileEntities[pos] = vteType.create(pos, vteData)
                } else {
                    vanillaTileEntityData -= pos
                }
            }
            
            // remove vanilla tile entities that have de-synced from the block type
            vanillaTileEntities.removeIf { (pos, vte) -> 
                val invalid = pos.block.type !in vte.type.materials
                if (invalid) vanillaTileEntityData.remove(pos)
                invalid
            }
            
            // load models
            for ((pos, tileEntity) in tileEntities)
                tileEntity.blockState.modelProvider.load(pos)
            
            // It is assumed that (vanilla-) tile-entities will not update this RegionChunk's tile-entity map
            // during handleEnable, as that would cause the new tile-entity to not be enabled properly.
            for (tileEntity in tileEntities.values) {
                tileEntity.isEnabled = true
                try {
                    tileEntity.handleEnable()
                } catch (t: Throwable) {
                    LOGGER.error("Failed to enable tile-entity $tileEntity", t)
                }
            }
            for (vte in vanillaTileEntities.values) {
                try {
                    vte.handleEnable()
                } catch (t: Throwable) {
                    LOGGER.error("Failed to enable vanilla tile-entity $vte", t)
                }
            }
            
            hasBeenEnabled = true
            isEnabled = true
        }
    }
    
    /**
     * Disables this [RegionChunk].
     *
     * This unloads all models and calls [TileEntity.handleDisable], [VanillaTileEntity.handleDisable].
     *
     * May only be called from the server thread.
     */
    fun disable() {
        checkServerThread()
        lock.withLock {
            if (!isEnabled)
                return
            
            if (isTicking)
                stopTicking()
            
            for ((pos, tileEntity) in tileEntities) {
                tileEntity.isEnabled = false
                tileEntity.blockState.modelProvider.unload(pos)
                try {
                    tileEntity.handleDisable()
                } catch (t: Throwable) {
                    LOGGER.error("Failed to disable tile-entity $tileEntity", t)
                }
            }
            
            for (vte in vanillaTileEntities.values) {
                try {
                    vte.handleDisable()
                } catch (t: Throwable) {
                    LOGGER.error("Failed to disable vanilla tile-entity $vte", t)
                }
            }
            
            isEnabled = false
        }
    }
    
    /**
     * Reconsiders the ticking status based on [tickingAllowed], [isTicking], [randomTickBlockCount], [tickingTileEntityCount]
     * and starts or stops ticking as required.
     */
    private fun reconsiderTicking(): Unit = lock.withLock {
        if (isTicking) {
            if (!tickingAllowed || (randomTickBlockCount == 0 && tickingTileEntityCount == 0))
                stopTicking()
        } else {
            if (tickingAllowed && (randomTickBlockCount > 0 || tickingTileEntityCount > 0))
                startTicking()
        }
    }
    
    /**
     * Permits this [RegionChunk] to start ticking.
     */
    fun allowTicking(): Unit = lock.withLock {
        tickingAllowed = true
        reconsiderTicking()
    }
    
    /**
     * Prevents this [RegionChunk] from ticking.
     */
    fun disallowTicking(): Unit = lock.withLock {
        tickingAllowed = false
        reconsiderTicking()
    }
    
    /**
     * Starts ticking this [RegionChunk].
     */
    private fun startTicking(): Unit = lock.withLock {
        if (isTicking)
            return
        
        tickTask = runTaskTimer(0, 1, ::tick)
        
        val chunkSupervisor = SupervisorJob(AsyncExecutor.SUPERVISOR)
        coroutineSupervisor = chunkSupervisor
        for ((_, tileEntity) in tileEntities) {
            if (tileEntity.block.tickrate <= 0)
                continue
            
            try {
                tileEntity.coroutineSupervisor = SupervisorJob(chunkSupervisor)
                tileEntity.handleEnableTicking()
            } catch (t: Throwable) {
                LOGGER.error("Failed to enable ticking for $tileEntity", t)
            }
        }
        
        isTicking = true
    }
    
    /**
     * Stops ticking this [RegionChunk].
     */
    private fun stopTicking(): Unit = lock.withLock {
        if (!isTicking)
            return
        
        tickTask?.cancel()
        tickTask = null
        
        coroutineSupervisor?.cancel("Ticking disabled")
        
        for ((_, tileEntity) in tileEntities) {
            if (tileEntity.block.tickrate <= 0)
                continue
            
            try {
                tileEntity.handleDisableTicking()
            } catch (t: Throwable) {
                LOGGER.error("Failed to disable ticking for $tileEntity", t)
            }
        }
        
        isTicking = false
    }
    
    /**
     * Suspends until all tile-entity coroutines have completed.
     */
    suspend fun awaitShutdown() {
        coroutineSupervisor?.join()
    }
    
    private fun tick(): Unit = lock.withLock {
        tick++
        
        // tile-entity ticks
        for (tileEntity in tileEntities.values) {
            val tickRate = tileEntity.block.tickrate
            if (tickRate == 0)
                continue
            
            val interval = 20 - tickRate
            if (interval == 0 || tick % interval == 0) {
                try {
                    if (tileEntity.isEnabled) // this should prevent tile-entities that were removed during ticking from being ticked
                        tileEntity.handleTick()
                } catch (t: Throwable) {
                    LOGGER.error("An exception occurred while ticking tile entity $tileEntity", t)
                }
            }
        }
        
        // random ticks
        if (randomTickBlockCount == 0)
            return
        val randomTickSpeed = level.gameRules.getInt(GameRules.RULE_RANDOMTICKING)
        if (randomTickSpeed > 0) {
            for ((sectionIdx, section) in sections.withIndex()) {
                if (randomTickBlockCounts[sectionIdx] > 0) {
                    repeat(randomTickSpeed) {
                        val rand = Random.nextInt()
                        val x = rand shr 8 and 0xF
                        val y = rand shr 4 and 0xF
                        val z = rand and 0xF
                        val blockState = section[x, y, z]
                        if (blockState != null && blockState.ticksRandomly) {
                            val pos = BlockPos(world, (pos.x shl 4) + x, (sectionIdx shl 4) + minHeight + y, (pos.z shl 4) + z)
                            try {
                                blockState.block.handleRandomTick(pos, blockState)
                            } catch (t: Throwable) {
                                LOGGER.error("An exception occurred while ticking block $blockState at $pos", t)
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Writes this chunk to the given [writer].
     */
    override fun write(writer: ByteWriter): Boolean = lock.withLock {
        if (vanillaTileEntityData.isEmpty() && tileEntityData.isEmpty() && sections.all { it.isEmpty() })
            return false
        
        // sections
        writer.writeInt(minSection)
        writer.writeInt(maxSection)
        val sectionBitmask = BitSet(sections.size)
        val sectionsBuffer = ByteArrayOutputStream()
        val sectionsWriter = ByteWriter.fromStream(sectionsBuffer)
        for ((sectionIdx, section) in sections.withIndex()) {
            sectionBitmask.set(sectionIdx, section.write(sectionsWriter))
        }
        writer.writeBytes(Arrays.copyOf(sectionBitmask.toByteArray(), sections.size.ceilDiv(8)))
        writer.writeBytes(sectionsBuffer.toByteArray())
        
        // tile-entities
        vanillaTileEntities.values.forEach(VanillaTileEntity::saveData)
        tileEntities.values.forEach(TileEntity::saveData)
        writer.writeVarInt(vanillaTileEntityData.size)
        for ((pos, data) in vanillaTileEntityData) {
            writer.writeInt(packBlockPos(pos))
            writer.writeBytes(CBF.write(data))
        }
        writer.writeVarInt(tileEntityData.size)
        for ((pos, data) in tileEntityData) {
            writer.writeInt(packBlockPos(pos))
            writer.writeBytes(CBF.write(data))
        }
        
        return true
    }
    
    companion object : RegionizedChunkReader<RegionChunk>() {
        
        override fun read(pos: ChunkPos, reader: ByteReader): RegionChunk {
            // read sections
            val minFileSection = reader.readInt()
            val maxFileSection = reader.readInt()
            val fileSectionCount = maxFileSection - minFileSection
            val sectionsBitmask = BitSet.valueOf(reader.readBytes(fileSectionCount.ceilDiv(8)))
            val fileSections = Array(fileSectionCount) { sectionIdx ->
                if (sectionsBitmask.get(sectionIdx))
                    RegionChunkSection.read(BlockStateIdResolver, reader)
                else RegionChunkSection(BlockStateIdResolver)
            }
            
            // fix section array if section count changed
            val minWorldSection = pos.world!!.minHeight shr 4
            val maxWorldSection = pos.world!!.maxHeight shr 4
            val worldSectionCount = maxWorldSection - minWorldSection
            val sections: Array<RegionChunkSection<NovaBlockState>>
            if (fileSectionCount != worldSectionCount) {
                val d = minWorldSection - minFileSection
                sections = Array(worldSectionCount) { sectionIdx ->
                    fileSections.getOrNull(sectionIdx + d)
                        ?: RegionChunkSection(BlockStateIdResolver)
                }
            } else {
                sections = fileSections
            }
            
            // read tile-entities
            val vanillaTileEntityData = readPosCompoundMap(pos, reader)
            val tileEntityData = readPosCompoundMap(pos, reader)
            
            return RegionChunk(pos, sections, vanillaTileEntityData, tileEntityData)
        }
        
        private fun readPosCompoundMap(chunkPos: ChunkPos, reader: ByteReader): HashMap<BlockPos, Compound> {
            val size = reader.readVarInt()
            val map = HashMap<BlockPos, Compound>(size)
            repeat(size) {
                val pos = unpackBlockPos(chunkPos, reader.readInt())
                val data = CBF.read<Compound>(reader)!!
                map[pos] = data
            }
            return map
        }
        
        override fun createEmpty(pos: ChunkPos): RegionChunk {
            return RegionChunk(pos)
        }
        
        private fun getSectionCount(world: World): Int =
            (world.maxHeight - world.minHeight) shr 4
        
    }
    
}