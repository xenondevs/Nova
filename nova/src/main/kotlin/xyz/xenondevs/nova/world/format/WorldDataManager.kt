@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.world.format

import kotlinx.coroutines.runBlocking
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldSaveEvent
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.item.recipe.RecipeManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    runAfter = [RecipeManager::class] // tile-entities may need recipes
)
object WorldDataManager : Listener {
    
    private val worlds = ConcurrentHashMap<UUID, WorldDataStorage>()
    private var initialized = false
    private var disabled = false
    
    @InitFun
    private fun init() = runBlocking {
        initialized = true
        registerEvents()
    }
    
    @DisableFun(
        runAfter = [NetworkManager::class], // all tasks need to be processed before saving network regions
        runBefore = [ProtectionManager::class] // tile-entities may access protection, so we need to disable them before ProtectionManager
    )
    private fun disable() = runBlocking {
        for (world in worlds.values) {
            world.shutdownAndWait()
        }
        disabled = true
    }
    
    @EventHandler
    private fun handleWorldSave(event: WorldSaveEvent) {
        runBlocking { worlds[event.world.uid]?.save() }
    }
    
    internal fun getWorldStorage(world: World): WorldDataStorage =
        worlds.computeIfAbsent(world.uid) { WorldDataStorage(world) }
    
}