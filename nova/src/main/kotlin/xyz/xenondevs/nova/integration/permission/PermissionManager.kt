package xyz.xenondevs.nova.integration.permission

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.World
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.Dispatcher
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.HooksLoader
import xyz.xenondevs.nova.util.MINECRAFT_SERVER
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
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
@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dispatcher = Dispatcher.ASYNC,
    dependsOn = [HooksLoader::class]
)
object PermissionManager {
    
    internal val integrations = CopyOnWriteArrayList<PermissionIntegration>()
    
    private lateinit var executor: ExecutorService
    private lateinit var offlinePermissionCache: LoadingCache<PermissionArgs, Boolean>
    
    @InitFun
    private fun init() {
        executor = ThreadPoolExecutor(
            10, 10,
            0, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            ThreadFactoryBuilder().setNameFormat("Nova Permission Worker - %s").build()
        )
        
        offlinePermissionCache = Caffeine.newBuilder()
            .executor(executor)
            .expireAfterAccess(Duration.ofMinutes(30))
            .refreshAfterWrite(Duration.ofMinutes(1))
            .build { hasOfflinePermission(it.world, it.player, it.permission) }
        
        if (integrations.size > 1)
            LOGGER.warn("Multiple permission integrations have been registered: ${integrations.joinToString { it::class.simpleName!! }}, Nova will use the first one")
    }
    
    @DisableFun
    private fun disable() {
        executor.shutdown()
    }
    
    /**
     * Checks whether the player under the given [UUID][player] has the given [permission] in the given [world].
     *
     * This method will use [Player.hasPermission] if the player is online and otherwise use access the offline permission cache,
     * which accesses the registered [PermissionIntegrations][PermissionIntegration] asynchronously.
     */
    fun hasPermission(world: World, player: UUID, permission: String): CompletableFuture<Boolean> =
        hasPermission(world, Bukkit.getOfflinePlayer(player), permission)
    
    /**
     * Checks whether the given [player] has the given [permission] in the given [world].
     *
     * This method will use [Player.hasPermission] if the player is online and otherwise use access the offline permission cache,
     * which accesses the registered [PermissionIntegrations][PermissionIntegration] asynchronously.
     */
    fun hasPermission(world: World, player: OfflinePlayer, permission: String): CompletableFuture<Boolean> {
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