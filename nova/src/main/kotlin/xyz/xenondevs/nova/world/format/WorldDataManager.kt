@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.world.format

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.event.world.WorldSaveEvent
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.resources.ResourceGeneration
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntity
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.format.chunk.RegionChunk
import xyz.xenondevs.nova.world.format.legacy.LegacyFileConverter
import xyz.xenondevs.nova.world.pos
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [
        AddonsInitializer::class,
        ResourceGeneration.PreWorld::class,
        LegacyFileConverter::class,
        VanillaTileEntityManager::class,
        NetworkManager.Companion::class
    ]
)
object WorldDataManager : Listener {
    
    private val worlds = ConcurrentHashMap<UUID, WorldDataStorage>()
    private val chunkJobs = ConcurrentHashMap<ChunkPos, Job>()
    
    @InitFun
    private fun init() {
        registerEvents()
        Bukkit.getWorlds().asSequence()
            .flatMap { it.loadedChunks.asSequence() }
            .forEach { handleChunkLoad(it.pos) }
    }
    
    @DisableFun
    private fun disable() {
        for (world in worlds.values) {
            world.disableAllChunks()
            world.saveAllRegions()
        }
    }
    
    @EventHandler
    private fun handleChunkLoad(event: ChunkLoadEvent) =
        handleChunkLoad(event.chunk.pos)
    
    private fun handleChunkLoad(pos: ChunkPos) {
        chunkJobs.compute(pos) { _, prevJob ->
            CoroutineScope(Dispatchers.IO).launch {
                prevJob?.join()
                getOrLoadChunk(pos).enable()
            }
        }
    }
    
    @EventHandler
    private fun handleChunkUnload(event: ChunkUnloadEvent) =
        handleChunkUnload(event.chunk.pos)
    
    private fun handleChunkUnload(pos: ChunkPos) {
        chunkJobs.compute(pos) { _, prevJob ->
            CoroutineScope(Dispatchers.IO).launch {
                prevJob?.join()
                getOrLoadChunk(pos).disable()
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handleWorldSave(event: WorldSaveEvent) {
        CoroutineScope(Dispatchers.IO).launch { saveWorld(event.world) }
    }
    
    private fun saveWorld(world: World) =
        worlds[world.uid]?.saveAllRegions()
    
    fun getBlockState(pos: BlockPos): NovaBlockState? =
        getOrLoadChunk(pos.chunkPos).getBlockState(pos)
    
    fun setBlockState(pos: BlockPos, state: NovaBlockState?) =
        getOrLoadChunk(pos.chunkPos).setBlockState(pos, state)
    
    fun getTileEntity(pos: BlockPos): TileEntity? =
        getOrLoadChunk(pos.chunkPos).getTileEntity(pos)
    
    fun getTileEntities(pos: ChunkPos): List<TileEntity> =
        getOrLoadChunk(pos).getTileEntities()
    
    fun getTileEntities(world: World): List<TileEntity> =
        getWorldStorage(world).getTileEntities()
    
    fun getTileEntities(): List<TileEntity> =
        worlds.values.flatMap { it.getTileEntities() }
    
    fun setTileEntity(pos: BlockPos, tileEntity: TileEntity?): TileEntity? =
        getOrLoadChunk(pos.chunkPos).setTileEntity(pos, tileEntity)
    
    internal fun getVanillaTileEntity(pos: BlockPos): VanillaTileEntity? =
        getOrLoadChunk(pos.chunkPos).getVanillaTileEntity(pos)
    
    internal fun getVanillaTileEntities(pos: ChunkPos): List<VanillaTileEntity> =
        getOrLoadChunk(pos).getVanillaTileEntities()
    
    internal fun getVanillaTileEntities(world: World): List<VanillaTileEntity> =
        getWorldStorage(world).getVanillaTileEntities()
    
    internal fun getVanillaTileEntities(): List<VanillaTileEntity> =
        worlds.values.flatMap { it.getVanillaTileEntities() }
    
    internal fun setVanillaTileEntity(pos: BlockPos, tileEntity: VanillaTileEntity?) =
        getOrLoadChunk(pos.chunkPos).setVanillaTileEntity(pos, tileEntity)
    
    private fun getWorldStorage(world: World): WorldDataStorage =
        worlds.computeIfAbsent(world.uid) { WorldDataStorage(world) }
    
    private fun getOrLoadRegion(pos: ChunkPos): RegionFile =
        getWorldStorage(pos.world!!).getOrLoadRegion(pos)
    
    private fun getOrLoadChunk(pos: ChunkPos): RegionChunk =
        getOrLoadRegion(pos).getChunk(pos)
    
}