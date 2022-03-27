package xyz.xenondevs.nova.data.world

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldUnloadEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.world.block.state.BlockState
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos

object WorldDataManager : Listener {
    
    private val worlds = HashMap<World, WorldDataStorage>()
    
    init {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
    }
    
    @Synchronized
    fun storeBlockStates(pos: ChunkPos, blockStates: HashMap<BlockPos, BlockState>) {
        val chunk = getChunk(pos)
        chunk.blockStates = blockStates
    }
    
    @Synchronized
    fun getBlockStates(pos: ChunkPos): Map<BlockPos, BlockState> = getChunk(pos).blockStates
    
    @Synchronized
    fun getBlockState(pos: BlockPos): BlockState? = getChunk(pos.chunkPos).blockStates[pos]
    
    @Synchronized
    fun setBlockState(pos: BlockPos, state: BlockState) {
        getChunk(pos.chunkPos).blockStates[pos] = state
    }
    
    @Synchronized
    fun removeBlockState(pos: BlockPos) {
        getChunk(pos.chunkPos).blockStates -= pos
    }
    
    @Synchronized
    fun saveWorld(world: World) {
        LOGGER.info("Saving world ${world.name}...")
        worlds[world]?.saveAll()
    }
    
    @Synchronized
    private fun getWorldStorage(world: World): WorldDataStorage =
        worlds.getOrPut(world) { WorldDataStorage(world) }
    
    @Synchronized
    private fun getChunk(pos: ChunkPos): RegionChunk =
        getWorldStorage(pos.world!!).getRegion(pos).getChunk(pos)
    
    @Synchronized
    @EventHandler
    private fun handleWorldUnload(event: WorldUnloadEvent) {
        worlds -= event.world
    }
    
}