package xyz.xenondevs.nova.world.block.limits

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.config.PermanentStorageMigrations
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import java.lang.Integer.max
import java.util.*

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [PermanentStorageMigrations::class]
)
internal object TileEntityTracker {
    
    private val BLOCK_COUNTER: HashMap<UUID, HashMap<Key, Int>> =
        PermanentStorage.retrieve("block_counter") ?: HashMap()
    private val BLOCK_WORLD_COUNTER: HashMap<UUID, HashMap<UUID, HashMap<Key, Int>>> =
        PermanentStorage.retrieve("block_world_counter") ?: HashMap()
    private val BLOCK_CHUNK_COUNTER: HashMap<UUID, HashMap<ChunkPos, HashMap<Key, Int>>> =
        PermanentStorage.retrieve("block_chunk_counter") ?: HashMap()
    
    @InitFun
    private fun init() {
        runTaskTimer(20 * 60, 20 * 60, ::saveCounters)
    }
    
    @DisableFun
    private fun saveCounters() {
        PermanentStorage.store("block_counter", BLOCK_COUNTER)
        PermanentStorage.store("block_world_counter", BLOCK_WORLD_COUNTER)
        PermanentStorage.store("block_chunk_counter", BLOCK_CHUNK_COUNTER)
    }
    
    internal fun handlePlace(block: NovaTileEntityBlock, ctx: Context<BlockPlace>) {
        val sourceUuid = ctx[BlockPlace.SOURCE_UUID] ?: return
        modifyCounters(sourceUuid, ctx[BlockPlace.BLOCK_POS], block.id, 1)
    }
    
    internal fun handleBreak(tileEntity: TileEntity, ctx: Context<BlockBreak>) {
        val ownerUuid = tileEntity.ownerUuid
        if (ownerUuid != null)
            modifyCounters(ownerUuid, ctx[BlockBreak.BLOCK_POS], tileEntity.block.id, -1)
    }
    
    private fun modifyCounters(player: UUID, pos: BlockPos, id: Key, add: Int) {
        val playerMap = BLOCK_COUNTER.getOrPut(player, ::HashMap)
        playerMap[id] = max(0, (playerMap[id] ?: 0) + add)
        
        val playerWorldMap = BLOCK_WORLD_COUNTER.getOrPut(player, ::HashMap).getOrPut(pos.world.uid, ::HashMap)
        playerWorldMap[id] = max(0, (playerWorldMap[id] ?: 0) + add)
        
        val playerChunkMap = BLOCK_CHUNK_COUNTER.getOrPut(player, ::HashMap).getOrPut(pos.chunkPos, ::HashMap)
        playerChunkMap[id] = max(0, (playerChunkMap[id] ?: 0) + add)
    }
    
    fun getBlocksPlacedAmount(player: UUID, blockId: Key): Int {
        return BLOCK_COUNTER[player]?.get(blockId) ?: 0
    }
    
    fun getBlocksPlacedAmount(player: UUID, world: UUID, blockId: Key): Int {
        return BLOCK_WORLD_COUNTER[player]?.get(world)?.get(blockId) ?: 0
    }
    
    fun getBlocksPlacedAmount(player: UUID, chunk: ChunkPos, blockId: Key): Int {
        return BLOCK_CHUNK_COUNTER[player]?.get(chunk)?.get(blockId) ?: 0
    }
    
    fun getBlocksPlacedAmount(player: UUID): Int {
        return BLOCK_COUNTER[player]?.values?.sum() ?: 0
    }
    
    fun getBlocksPlacedAmount(player: UUID, world: UUID): Int {
        return BLOCK_WORLD_COUNTER[player]?.get(world)?.values?.sum() ?: 0
    }
    
    fun getBlocksPlacedAmount(player: UUID, chunk: ChunkPos): Int {
        return BLOCK_CHUNK_COUNTER[player]?.get(chunk)?.values?.sum() ?: 0
    }
    
}