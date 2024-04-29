@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.world.format

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
        VanillaTileEntityManager::class
    ]
)
object WorldDataManager : Listener {
    
    private val worlds = ConcurrentHashMap<UUID, WorldDataStorage>()
    
    @InitFun
    private fun init() = runBlocking {
        registerEvents()
        Bukkit.getWorlds().asSequence()
            .flatMap { it.loadedChunks.asSequence() }
            .forEach { getOrLoadChunk(it.pos).enable() }
    }
    
    @DisableFun
    private fun disable() = runBlocking {
        for (world in worlds.values) {
            world.disableAllChunks()
            world.save()
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
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
        // TODO: don't use global scope
        GlobalScope.launch { worlds.values.forEach { it.loadAsync(pos) } }
    }
    
    internal fun startTicking(pos: ChunkPos) {
        runBlocking { getOrLoadChunk(pos).startTicking() }
    }
    
    internal fun stopTicking(pos: ChunkPos) {
        runBlocking { getOrLoadChunk(pos).stopTicking() }
    }
    
    fun getBlockState(pos: BlockPos): NovaBlockState? =
        runBlocking { getOrLoadChunk(pos.chunkPos).getBlockState(pos) }
    
    fun setBlockState(pos: BlockPos, state: NovaBlockState?) =
        runBlocking { getOrLoadChunk(pos.chunkPos).setBlockState(pos, state) }
    
    fun getTileEntity(pos: BlockPos): TileEntity? =
        runBlocking { getOrLoadChunk(pos.chunkPos).getTileEntity(pos) }
    
    fun getTileEntities(pos: ChunkPos): List<TileEntity> =
        runBlocking { getOrLoadChunk(pos).getTileEntities() }
    
    fun getTileEntities(world: World): List<TileEntity> =
        runBlocking { getWorldStorage(world).getTileEntities() }
    
    fun getTileEntities(): List<TileEntity> =
        runBlocking { worlds.values.flatMap { it.getTileEntities() } }
    
    fun setTileEntity(pos: BlockPos, tileEntity: TileEntity?): TileEntity? =
        runBlocking { getOrLoadChunk(pos.chunkPos).setTileEntity(pos, tileEntity) }
    
    internal fun getVanillaTileEntity(pos: BlockPos): VanillaTileEntity? =
        runBlocking { getOrLoadChunk(pos.chunkPos).getVanillaTileEntity(pos) }
    
    internal fun getVanillaTileEntities(pos: ChunkPos): List<VanillaTileEntity> =
        runBlocking { getOrLoadChunk(pos).getVanillaTileEntities() }
    
    internal fun getVanillaTileEntities(world: World): List<VanillaTileEntity> =
        runBlocking { getWorldStorage(world).getVanillaTileEntities() }
    
    internal fun getVanillaTileEntities(): List<VanillaTileEntity> =
        runBlocking { worlds.values.flatMap { it.getVanillaTileEntities() } }
    
    internal fun setVanillaTileEntity(pos: BlockPos, tileEntity: VanillaTileEntity?): VanillaTileEntity? =
        runBlocking { getOrLoadChunk(pos.chunkPos).setVanillaTileEntity(pos, tileEntity) }
    
    private suspend fun getOrLoadChunk(pos: ChunkPos): RegionChunk =
        getOrLoadRegion(pos).getChunk(pos)
    
    private suspend fun getOrLoadRegion(pos: ChunkPos): RegionFile =
        getWorldStorage(pos.world!!).getOrLoadRegion(pos)
    
    internal fun getWorldStorage(world: World): WorldDataStorage =
        worlds.computeIfAbsent(world.uid) { WorldDataStorage(world) }
    
}