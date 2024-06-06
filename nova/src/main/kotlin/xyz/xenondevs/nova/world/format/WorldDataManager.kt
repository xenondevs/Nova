@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.world.format

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.Chunk.LoadLevel
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
import xyz.xenondevs.nova.world.pos
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [
        AddonsInitializer::class,
        ResourceGeneration.PreWorld::class,
        VanillaTileEntityManager::class,
        NetworkManager::class
    ]
)
object WorldDataManager : Listener {
    
    private val worlds = ConcurrentHashMap<UUID, WorldDataStorage>()
    private var initialized = false
    
    @InitFun
    private fun init() = runBlocking {
        initialized = true
        registerEvents()
        Bukkit.getWorlds().asSequence()
            .flatMap { it.loadedChunks.asSequence() }
            .forEach {
                val regionChunk = getOrLoadChunk(it.pos)
                regionChunk.enable()
                if (it.loadLevel == LoadLevel.TICKING || it.loadLevel == LoadLevel.ENTITY_TICKING) {
                    regionChunk.startTicking()
                }
            }
    }
    
    @DisableFun
    private fun disable() = runBlocking {
        for (world in worlds.values) {
            world.disableAllChunks()
            world.save()
        }
    }
    
    @EventHandler(priority = EventPriority.LOW) // NetworkManager is LOWEST
    private fun handleChunkLoad(event: ChunkLoadEvent) {
        runBlocking { getOrLoadChunk(event.chunk.pos).enable() }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    private fun handleChunkUnload(event: ChunkUnloadEvent) {
        runBlocking { getOrLoadChunk(event.chunk.pos).disable() }
    }
    
    @EventHandler
    private fun handleWorldSave(event: WorldSaveEvent) {
        runBlocking { worlds[event.world.uid]?.save() }
    }
    
    internal fun loadAsync(pos: ChunkPos) {
        if (!initialized)
            return
        
        // TODO: don't use global scope
        GlobalScope.launch { worlds.values.forEach { it.loadAsync(pos) } }
    }
    
    internal fun startTicking(pos: ChunkPos) {
        if (!initialized)
            return
        
        runBlocking { getOrLoadChunk(pos).startTicking() }
    }
    
    internal fun stopTicking(pos: ChunkPos) {
        if (!initialized)
            return
        
        runBlocking { getOrLoadChunk(pos).stopTicking() }
    }
    
    fun getBlockState(pos: BlockPos): NovaBlockState? =
        getChunkOrThrow(pos.chunkPos).getBlockState(pos)
    
    fun setBlockState(pos: BlockPos, state: NovaBlockState?) =
        getChunkOrThrow(pos.chunkPos).setBlockState(pos, state)
    
    fun getTileEntity(pos: BlockPos): TileEntity? =
        getChunkOrThrow(pos.chunkPos).getTileEntity(pos)
    
    internal suspend fun getOrLoadTileEntity(pos: BlockPos): TileEntity? =
        getOrLoadChunk(pos.chunkPos).getTileEntity(pos)
    
    fun getTileEntities(pos: ChunkPos): List<TileEntity> =
        getChunkOrThrow(pos).getTileEntities()
    
    internal suspend fun getOrLoadTileEntities(pos: ChunkPos): List<TileEntity> =
        getOrLoadChunk(pos).getTileEntities()
    
    fun getTileEntities(world: World): List<TileEntity> =
        getWorldStorage(world).getTileEntities()
    
    fun getTileEntities(): List<TileEntity> =
        worlds.values.flatMap { it.getTileEntities() }
    
    fun setTileEntity(pos: BlockPos, tileEntity: TileEntity?): TileEntity? =
        getChunkOrThrow(pos.chunkPos).setTileEntity(pos, tileEntity)
    
    internal fun getVanillaTileEntity(pos: BlockPos): VanillaTileEntity? =
        getChunkOrThrow(pos.chunkPos).getVanillaTileEntity(pos)
    
    internal suspend fun getOrLoadVanillaTileEntity(pos: BlockPos): VanillaTileEntity? =
        getOrLoadChunk(pos.chunkPos).getVanillaTileEntity(pos)
    
    internal fun getVanillaTileEntities(pos: ChunkPos): List<VanillaTileEntity> =
        getChunkOrThrow(pos).getVanillaTileEntities()
    
    internal suspend fun getOrLoadVanillaTileEntities(pos: ChunkPos): List<VanillaTileEntity> =
        getOrLoadChunk(pos).getVanillaTileEntities()
    
    internal fun setVanillaTileEntity(pos: BlockPos, tileEntity: VanillaTileEntity?): VanillaTileEntity? =
        getChunkOrThrow(pos.chunkPos).setVanillaTileEntity(pos, tileEntity)
    
    internal fun getVanillaTileEntities(world: World): List<VanillaTileEntity> =
        getWorldStorage(world).getVanillaTileEntities()
    
    internal fun getVanillaTileEntities(): List<VanillaTileEntity> =
        worlds.values.flatMap { it.getVanillaTileEntities() }
    
    private suspend fun getOrLoadChunk(pos: ChunkPos): RegionChunk =
        getOrLoadRegion(pos).getChunk(pos)
    
    private fun getChunkOrThrow(pos: ChunkPos): RegionChunk {
        return getWorldStorage(pos.world!!).getBlockChunkOrThrow(pos)
    }
    
    private suspend fun getOrLoadRegion(pos: ChunkPos): RegionFile {
        return getWorldStorage(pos.world!!).getOrLoadRegion(pos)
    }
    
    internal fun getWorldStorage(world: World): WorldDataStorage =
        worlds.computeIfAbsent(world.uid) { WorldDataStorage(world) }
    
}