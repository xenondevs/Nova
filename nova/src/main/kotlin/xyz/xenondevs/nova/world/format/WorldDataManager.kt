package xyz.xenondevs.nova.world.format

import kotlinx.coroutines.runBlocking
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldSaveEvent
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal object WorldDataManager : Listener {
    
    private val worlds = ConcurrentHashMap<UUID, WorldDataStorage>()
    
    @InternalInit(stage = PRE_WORLD)
    private fun init() {
        registerEvents()
    }
    
    @DisableFun(
        runAfter = [NetworkManager::class], // all tasks need to be processed before saving network regions
    )
    private fun disable() = runBlocking {
        for (world in worlds.values) {
            world.shutdownAndWait()
        }
    }
    
    @EventHandler
    private fun handleWorldSave(event: WorldSaveEvent) {
        runBlocking { worlds[event.world.uid]?.save() }
    }
    
    internal fun getWorldStorage(world: World): WorldDataStorage =
        worlds.computeIfAbsent(world.uid) { WorldDataStorage(world) }
    
}