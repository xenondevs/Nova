package xyz.xenondevs.nova.world.format.chunk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.world.level.GameRules
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntity
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
import xyz.xenondevs.nova.world.format.BlockStateIdResolver
import xyz.xenondevs.nova.world.format.chunk.RegionizedChunk.Companion.packBlockPos
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level
import kotlin.concurrent.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToLong
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
    
    private var shouldTick = false
    private var isTicking = false
    
    private val world = pos.world!!
    private val level = world.serverLevel
    private val minHeight = world.minHeight
    private val maxHeight = world.maxHeight
    private val sectionCount = getSectionCount(world)
    
    private val vanillaTileEntities: MutableMap<BlockPos, VanillaTileEntity> = HashMap()
    private val tileEntities: MutableMap<BlockPos, TileEntity> = ConcurrentHashMap() // concurrent to allow modifications during ticking
    
    private var sectionsEmpty = sections.all { it.isEmpty() }
    private var tick = 0
    private var asyncTickerSupervisor: Job? = null
    private var tileEntityAsyncTickers: HashMap<TileEntity, Job>? = null
    private var syncTicker: BukkitTask? = null
    
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
                val type: VanillaTileEntity.Type = data["type"]!!
                vanillaTileEntities[pos] = type.constructor(pos, data)
            } catch (t: Throwable) {
                LOGGER.log(Level.SEVERE, "Failed to initialize vanilla tile entity pos=$pos, data=$data")
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
                LOGGER.log(Level.SEVERE, "Failed to initialize tile entity at $pos because there is no block state")
                return
            }
            
            val block = blockState.block as? NovaTileEntityBlock
            if (blockState.block == DefaultBlocks.UNKNOWN) {
                // nova:unknown is the only non-tile-entity block that is allowed to have tile-entity data
                continue
            } else if (block == null) {
                LOGGER.log(Level.SEVERE, "Failed to initialize tile entity at $pos because ${blockState.block} is not a tile entity type")
                return
            }
            
            try {
                tileEntities[pos] = block.tileEntityConstructor(pos, blockState, data)
            } catch (t: Throwable) {
                LOGGER.log(Level.SEVERE, "Failed to initialize tile entity pos=$pos, blockState=$blockState, data=$data", t)
            }
        }
    }
    
    /**
     * Gets the [NovaBlockState] at the given [pos].
     */
    fun getBlockState(pos: BlockPos): NovaBlockState? = lock.withLock {
        return getSection(pos.y)[pos.x and 0xF, pos.y and 0xF, pos.z and 0xF]
    }
    
    /**
     * Sets the [BlockState][state] at the given [pos].
     */
    fun setBlockState(pos: BlockPos, state: NovaBlockState?) = lock.withLock {
        val section = getSection(pos.y)
        section[pos.x and 0xF, pos.y and 0xF, pos.z and 0xF] = state
        if (state != null) {
            tileEntities[pos]?.blockState = state
            
            // if this is the first block in the chunk, we may need to start ticking it
            if (sectionsEmpty) {
                sectionsEmpty = false
                if (shouldTick && !isTicking) {
                    startTicking()
                }
            }
        } else if (!sectionsEmpty && section.isEmpty()) {
            // this section is now empty, so we check if the whole chunk is empty
            sectionsEmpty = sections.all { it.isEmpty() }
            if (sectionsEmpty && isTicking) {
                stopTicking()
                shouldTick = true
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
                vte.handleEnable()
            }
        }
        
        previous?.handleDisable()
        
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
                tileEntity.handleEnable()
                launchAndRegisterAsyncTicker(asyncTickerSupervisor!!, tileEntity)
            }
        }
        
        if (previous != null) {
            previous.handleDisable()
            previous.isEnabled = false
            cancelAndUnregisterAsyncTicker(previous)
        }
        
        return previous
    }
    
    /**
     * Gets the [RegionChunkSection] at the given [y] coordinate.
     */
    private fun getSection(y: Int): RegionChunkSection<NovaBlockState> {
        require(y in minHeight..maxHeight) { "Invalid y coordinate $y" }
        return sections[(y - minHeight) shr 4]
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
            
            // load models
            for ((pos, tileEntity) in tileEntities)
                tileEntity.blockState.modelProvider.load(pos)
            
            // It is assumed that (vanilla-) tile-entities will not update this RegionChunk's tile-entity map
            // during handleEnable, as that would cause the new tile-entity to not be enabled properly.
            for (tileEntity in tileEntities.values) {
                tileEntity.isEnabled = true
                tileEntity.handleEnable()
            }
            vanillaTileEntities.values.removeIf { vte ->
                // verify vte validity (the vanilla block state might've been changed without block updates)
                if (vte.meetsBlockStateRequirement()) {
                    vte.handleEnable()
                    return@removeIf false
                }
                return@removeIf true
            }
            
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
            
            for ((pos, tileEntity) in tileEntities) {
                tileEntity.isEnabled = false
                tileEntity.blockState.modelProvider.unload(pos)
                tileEntity.handleDisable()
            }
            
            for (vte in vanillaTileEntities.values) {
                vte.handleDisable()
            }
            
            isEnabled = false
        }
    }
    
    /**
     * Starts ticking this [RegionChunk].
     */
    fun startTicking(): Unit = lock.withLock {
        if (isTicking)
            return
        
        shouldTick = true
        
        if (sectionsEmpty)
            return
        
        // enable sync ticking
        syncTicker = runTaskTimer(0, 1, ::tick)
        
        // enable async ticking
        val supervisor = SupervisorJob(AsyncExecutor.SUPERVISOR)
        asyncTickerSupervisor = supervisor
        tileEntityAsyncTickers = tileEntities.values.asSequence()
            .filter { it.block.asyncTickrate > 0 }
            .associateWithTo(HashMap()) { launchAsyncTicker(supervisor, it) }
        
        isTicking = true
    }
    
    /**
     * Stops ticking this [RegionChunk].
     */
    fun stopTicking(): Unit = lock.withLock {
        if (!isTicking)
            return
        
        // disable sync ticking
        syncTicker?.cancel()
        syncTicker = null
        
        // disable async ticking
        asyncTickerSupervisor?.cancel("Chunk ticking disabled")
        tileEntityAsyncTickers = null
        
        shouldTick = false
        isTicking = false
    }
    
    private fun tick() = lock.withLock {
        tick++
        
        // tile-entity ticks
        for (tileEntity in tileEntities.values) {
            val tickRate = tileEntity.block.syncTickrate
            if (tickRate == 0)
                continue
            
            val interval = 20 - tickRate
            if (interval == 0 || tick % interval == 0) {
                try {
                    if (tileEntity.isEnabled) // this should prevent tile-entities that were removed during ticking from being ticked
                        tileEntity.handleTick()
                } catch (t: Throwable) {
                    LOGGER.log(Level.SEVERE, "An exception occurred while ticking tile entity $tileEntity", t)
                }
            }
        }
        
        // random ticks
        val randomTickSpeed = level.gameRules.getInt(GameRules.RULE_RANDOMTICKING)
        if (randomTickSpeed > 0) {
            for (section in sections) {
                if (!section.isEmpty()) {
                    repeat(randomTickSpeed) {
                        val x = Random.nextInt(0, 16)
                        val y = Random.nextInt(0, 16)
                        val z = Random.nextInt(0, 16)
                        val blockState = section[x, y, z]
                        if (blockState != null) {
                            val pos = BlockPos(world, pos.x + x, minHeight + y, pos.z + z)
                            try {
                                blockState.block.handleRandomTick(pos, blockState)
                            } catch (t: Throwable) {
                                LOGGER.log(Level.SEVERE, "An exception occurred while ticking block $blockState at $pos", t)
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun launchAndRegisterAsyncTicker(context: CoroutineContext, tileEntity: TileEntity) {
        tileEntityAsyncTickers?.put(tileEntity, launchAsyncTicker(context, tileEntity))
    }
    
    private fun cancelAndUnregisterAsyncTicker(tileEntity: TileEntity) {
        tileEntityAsyncTickers?.remove(tileEntity)?.cancel("Tile entity removed")
    }
    
    private fun launchAsyncTicker(context: CoroutineContext, tileEntity: TileEntity): Job =
        CoroutineScope(context).launch {
            var startTime: Long
            while (true) {
                startTime = System.currentTimeMillis()
                
                withContext(NonCancellable) { tileEntity.handleAsyncTick() }
                
                val globalTickRate = Bukkit.getServerTickManager().tickRate
                val msPerTick = (1000 / tileEntity.block.asyncTickrate * (globalTickRate / 20)).roundToLong()
                val delayTime = msPerTick - (System.currentTimeMillis() - startTime)
                if (delayTime > 0)
                    delay(delayTime)
            }
        }
    
    /**
     * Writes this chunk to the given [writer].
     */
    override fun write(writer: ByteWriter): Boolean = lock.withLock {
        if (vanillaTileEntityData.isEmpty() && tileEntityData.isEmpty() && sections.all { it.isEmpty() })
            return false
        
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
        
        val sectionBitmask = BitSet(sectionCount)
        val sectionsBuffer = ByteArrayOutputStream()
        val sectionsWriter = ByteWriter.fromStream(sectionsBuffer)
        
        for ((sectionIdx, section) in sections.withIndex()) {
            sectionBitmask.set(sectionIdx, section.write(sectionsWriter))
        }
        
        writer.writeInt(sectionCount)
        writer.writeBytes(Arrays.copyOf(sectionBitmask.toByteArray(), sectionCount.ceilDiv(8)))
        writer.writeBytes(sectionsBuffer.toByteArray())
        
        return true
    }
    
    companion object : RegionizedChunkReader<RegionChunk>() {
        
        override fun read(pos: ChunkPos, reader: ByteReader): RegionChunk {
            val vanillaTileEntityData = readPosCompoundMap(pos, reader)
            val tileEntityData = readPosCompoundMap(pos, reader)
            
            val sectionCount = reader.readInt()
            val sectionsBitmask = BitSet.valueOf(reader.readBytes(sectionCount.ceilDiv(8)))
            
            val sections = Array(sectionCount) { sectionIdx ->
                if (sectionsBitmask.get(sectionIdx))
                    RegionChunkSection.read(BlockStateIdResolver, reader)
                else RegionChunkSection(BlockStateIdResolver)
            }
            
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