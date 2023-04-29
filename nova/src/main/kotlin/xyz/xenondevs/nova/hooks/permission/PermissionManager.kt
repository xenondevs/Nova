package xyz.xenondevs.nova.hooks.permission

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.entity.Player
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.hooks.HooksLoader
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private data class PermissionArgs(val world: World, val player: OfflinePlayer, val permission: String)

/**
 * A permission manager that can retrieve permissions for offline players via registered [PermissionIntegrations][PermissionIntegration].
 *
 * [OfflinePlayer] permissions will be cached for 30 minutes after the last access and commonly accessed permissions will be refreshed every minute.
 */
@InternalInit(stage = InitializationStage.POST_WORLD_ASYNC, dependsOn = [HooksLoader::class])
object PermissionManager {
    
    internal val integrations = ArrayList<PermissionIntegration>()
    
    private lateinit var executor: ExecutorService
    private lateinit var offlinePermissionCache: LoadingCache<PermissionArgs, Boolean>
    
    @InitFun
    private fun init() {
        executor = ThreadPoolExecutor(
            10, 10,
            0, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            ThreadFactoryBuilder().setNameFormat("Nova Protection Worker - %s").build()
        )
        
        offlinePermissionCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .refreshAfterWrite(Duration.ofMinutes(1))
            .build { hasOfflinePermission(it.world, it.player, it.permission) }
        
        if (integrations.size > 1)
            LOGGER.warning("Multiple permission integrations have been registered: ${integrations.joinToString { it::class.simpleName!! }}, Nova will use the first one")
    }
    
    @DisableFun
    private fun disable() {
        executor.shutdown()
    }
    
    /**
     * Checks whether the player under the given [UUID][player] has the given [permission] in the given [world].
     *
     * This method will use [Player.hasPermission] if the player is online and otherwise use access the offline
     * permission cache. If the permission is not cached yet, this method will return false and initiate a cache load.
     */
    fun hasPermission(world: World, player: UUID, permission: String): Boolean =
        hasPermission(world, Bukkit.getOfflinePlayer(player), permission)
    
    /**
     * Checks whether the given [player] has the given [permission] in the given [world].
     *
     * This method will use [Player.hasPermission] if the player is online and otherwise use access the offline
     * permission cache. If the permission is not cached yet, this method will return false and initiate a cache load.
     */
    fun hasPermission(world: World, player: OfflinePlayer, permission: String): Boolean {
        // online-player permissions are cached by the permissions plugin
        if (player.isOnline)
            return player.player!!.hasPermission(permission)
        
        val args = PermissionArgs(world, player, permission)
        
        // don't initiate cache loads, as that would cause lag spikes due to database access from the main thread
        val cachedResult = offlinePermissionCache.getIfPresent(args)
        if (cachedResult == null) {
            // load cache async
            offlinePermissionCache.refresh(args)
            // return false as we don't know the result yet
            return false
        }
        
        return cachedResult
    }
    
    /**
     * Checks whether the player under the given [UUID][player] has the given [permission] in the given [world].
     * 
     * This method will use [Player.hasPermission] if the player is online and otherwise use access the offline permission cache,
     * which accessed the registered [PermissionIntegrations][PermissionIntegration] asynchronously.
     */
    fun hasPermissionAsync(world: World, player: UUID, permission: String): CompletableFuture<Boolean> =
        hasPermissionAsync(world, Bukkit.getOfflinePlayer(player), permission)
    
    /**
     * Checks whether the given [player] has the given [permission] in the given [world].
     * 
     * This method will use [Player.hasPermission] if the player is online and otherwise use access the offline permission cache,
     * which accessed the registered [PermissionIntegrations][PermissionIntegration] asynchronously.
     */
    fun hasPermissionAsync(world: World, player: OfflinePlayer, permission: String): CompletableFuture<Boolean> {
        // online-player permissions are cached by the permissions plugin
        if (player.isOnline)
            return CompletableFuture.completedFuture(player.player!!.hasPermission(permission))
        
        val args = PermissionArgs(world, player, permission)
        val cachedResult = offlinePermissionCache.getIfPresent(args)
        if (cachedResult != null)
            return CompletableFuture.completedFuture(cachedResult)
        
        return offlinePermissionCache.refresh(args)
    }
    
    private fun hasOfflinePermission(world: World, player: OfflinePlayer, permission: String): Boolean {
        require(Thread.currentThread() != MINECRAFT_SERVER.serverThread) { "Offline player permissions should never be checked from the main thread" }
        return integrations[0].hasPermission(world, player, permission).get() ?: false
    }
    
}