package xyz.xenondevs.nova.integration.protection

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.google.common.util.concurrent.ThreadFactoryBuilder
import kotlinx.coroutines.future.await
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.api.ApiTileEntityWrapper
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.api.protection.ProtectionIntegration.ExecutionMode
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockBreak
import xyz.xenondevs.nova.context.intention.BlockInteract
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.integration.HooksLoader
import xyz.xenondevs.nova.integration.permission.PermissionManager
import xyz.xenondevs.nova.util.concurrent.CombinedBooleanFuture
import xyz.xenondevs.nova.util.concurrent.isServerThread
import xyz.xenondevs.nova.util.isBetweenXZ
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.pos
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

//<editor-fold desc="ProtectionArgs classes", defaultstate="collapsed">
private interface ProtectionArgs {
    val player: OfflinePlayer
    val location: Location
}

private interface ProtectionArgsTileEntity : ProtectionArgs {
    
    val tileEntity: TileEntity
    override val player get() = tileEntity.owner!!
    val apiTileEntity get() = ApiTileEntityWrapper(tileEntity)
}

private data class CanPlaceUserArgs(override val player: OfflinePlayer, val item: ItemStack, override val location: Location) : ProtectionArgs
private data class CanPlaceTileArgs(override val tileEntity: TileEntity, val item: ItemStack, override val location: Location) : ProtectionArgsTileEntity
private data class CanBreakUserArgs(override val player: OfflinePlayer, val item: ItemStack?, override val location: Location) : ProtectionArgs
private data class CanBreakTileArgs(override val tileEntity: TileEntity, val item: ItemStack?, override val location: Location) : ProtectionArgsTileEntity
private data class CanUseBlockUserArgs(override val player: OfflinePlayer, val item: ItemStack?, override val location: Location) : ProtectionArgs
private data class CanUseBlockTileArgs(override val tileEntity: TileEntity, val item: ItemStack?, override val location: Location) : ProtectionArgsTileEntity
private data class CanUseItemUserArgs(override val player: OfflinePlayer, val item: ItemStack, override val location: Location) : ProtectionArgs
private data class CanUseItemTileArgs(override val tileEntity: TileEntity, val item: ItemStack, override val location: Location) : ProtectionArgsTileEntity
private data class CanInteractWithEntityUserArgs(override val player: OfflinePlayer, val entity: Entity, val item: ItemStack?) : ProtectionArgs {
    override val location: Location = entity.location
}

private data class CanInteractWithEntityTileArgs(override val tileEntity: TileEntity, val entity: Entity, val item: ItemStack?) : ProtectionArgsTileEntity {
    override val location: Location = entity.location
}

private data class CanHurtEntityUserArgs(override val player: OfflinePlayer, val entity: Entity, val item: ItemStack?) : ProtectionArgs {
    override val location: Location = entity.location
}

private data class CanHurtEntityTileArgs(override val tileEntity: TileEntity, val entity: Entity, val item: ItemStack?) : ProtectionArgsTileEntity {
    override val location: Location = entity.location
}
//</editor-fold>

/**
 * Handles protection checks using registered [ProtectionIntegrations][ProtectionIntegration].
 *
 * Protection checks are cached for 60s after the last access.
 * Results of integrations that [can be called asynchronously][ExecutionMode.NONE] are refreshed every 30s in the background.
 */
object ProtectionManager {
    
    private val threadAgnosticIntegrations = CopyOnWriteArrayList<ProtectionIntegration>()
    private val serverThreadOnlyIntegrations = CopyOnWriteArrayList<ProtectionIntegration>()
    
    private val caches = CopyOnWriteArrayList<ProtectionCache<*>>()
    
    private lateinit var executor: ExecutorService
    private lateinit var cacheCanPlaceUser: ProtectionCache<CanPlaceUserArgs>
    private lateinit var cacheCanPlaceTile: ProtectionCache<CanPlaceTileArgs>
    private lateinit var cacheCanBreakUser: ProtectionCache<CanBreakUserArgs>
    private lateinit var cacheCanBreakTile: ProtectionCache<CanBreakTileArgs>
    private lateinit var cacheCanUseBlockUser: ProtectionCache<CanUseBlockUserArgs>
    private lateinit var cacheCanUseBlockTile: ProtectionCache<CanUseBlockTileArgs>
    private lateinit var cacheCanUseItemUser: ProtectionCache<CanUseItemUserArgs>
    private lateinit var cacheCanUseItemTile: ProtectionCache<CanUseItemTileArgs>
    private lateinit var cacheCanInteractWithEntityUser: ProtectionCache<CanInteractWithEntityUserArgs>
    private lateinit var cacheCanInteractWithEntityTile: ProtectionCache<CanInteractWithEntityTileArgs>
    private lateinit var cacheCanHurtEntityUser: ProtectionCache<CanHurtEntityUserArgs>
    private lateinit var cacheCanHurtEntityTile: ProtectionCache<CanHurtEntityTileArgs>
    
    @InternalInit(
        stage = POST_WORLD,
        dispatcher = ASYNC,
        runAfter = [
            HooksLoader::class,
            PermissionManager::class,
        ]
    )
    private fun init() {
        executor = ThreadPoolExecutor(
            10, 10,
            0, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            ThreadFactoryBuilder().setNameFormat("Nova Protection Worker - %s").build()
        )
        
        cacheCanPlaceUser = ProtectionCache { canPlace(it.player, it.item, it.location) }
        cacheCanPlaceTile = ProtectionCache { canPlace(it.apiTileEntity, it.item, it.location) }
        cacheCanBreakUser = ProtectionCache { canBreak(it.player, it.item, it.location) }
        cacheCanBreakTile = ProtectionCache { canBreak(it.apiTileEntity, it.item, it.location) }
        cacheCanUseBlockUser = ProtectionCache { canUseBlock(it.player, it.item, it.location) }
        cacheCanUseBlockTile = ProtectionCache { canUseBlock(it.apiTileEntity, it.item, it.location) }
        cacheCanUseItemUser = ProtectionCache { canUseItem(it.player, it.item, it.location) }
        cacheCanUseItemTile = ProtectionCache { canUseItem(it.apiTileEntity, it.item, it.location) }
        cacheCanInteractWithEntityUser = ProtectionCache { canInteractWithEntity(it.player, it.entity, it.item) }
        cacheCanInteractWithEntityTile = ProtectionCache { canInteractWithEntity(it.apiTileEntity, it.entity, it.item) }
        cacheCanHurtEntityUser = ProtectionCache { canHurtEntity(it.player, it.entity, it.item) }
        cacheCanHurtEntityTile = ProtectionCache { canHurtEntity(it.apiTileEntity, it.entity, it.item) }
    }
    
    @DisableFun(runBefore = [PermissionManager::class])
    private fun disable() {
        executor.shutdown()
    }
    
    /**
     * Checks whether the given [ctx] passes place permission checks.
     */
    suspend fun canPlace(ctx: Context<BlockPlace>): Boolean {
        val block = ctx[BlockPlace.BLOCK]
        val blockItem = ctx[BlockPlace.BLOCK_ITEM_STACK] ?: ItemStack.empty()
        
        val tileEntity = ctx[BlockPlace.SOURCE_TILE_ENTITY]
        if (tileEntity != null)
            return canPlace(tileEntity, blockItem, block)
        
        val responsiblePlayer = ctx[BlockPlace.RESPONSIBLE_PLAYER]
        if (responsiblePlayer != null)
            return canPlace(responsiblePlayer, blockItem, block)
        
        return true
    }
    
    /**
     * Checks if the [tileEntity] can place that [item] at that [block].
     */
    fun canPlaceAsync(tileEntity: TileEntity, item: ItemStack, block: Block): CompletableFuture<Boolean> {
        if (tileEntity.owner == null)
            return CompletableFuture.completedFuture(true)
        if (!::cacheCanPlaceTile.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanPlaceTile.get(CanPlaceTileArgs(tileEntity, item.clone(), block.location))
    }
    
    /**
     * Checks if the [tileEntity] can place that [item] at that [block].
     */
    suspend fun canPlace(tileEntity: TileEntity, item: ItemStack, block: Block): Boolean =
        canPlaceAsync(tileEntity, item, block).await()
    
    /**
     * Checks if the [player] can place that [item] at that [block].
     */
    fun canPlaceAsync(player: OfflinePlayer, item: ItemStack, block: Block): CompletableFuture<Boolean> {
        if (!::cacheCanPlaceUser.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanPlaceUser.get(CanPlaceUserArgs(player, item.clone(), block.location))
    }
    
    /**
     * Checks if the [player] can place that [item] at that [pos].
     */
    suspend fun canPlace(player: OfflinePlayer, item: ItemStack, block: Block): Boolean =
        canPlaceAsync(player, item, block).await()
    
    /**
     * Checks if the [player] can place that [item] at that [pos].
     */
    fun canPlace(player: Player, item: ItemStack, block: Block): Boolean =
        canPlaceAsync(player, item, block).get()
    
    /**
     * Checks whether the given [ctx] passes break permission checks.
     */
    suspend fun canBreak(ctx: Context<BlockBreak>): Boolean {
        val block = ctx[BlockBreak.BLOCK]
        val tool = ctx[BlockBreak.TOOL_ITEM_STACK]
        
        val tileEntity = ctx[BlockBreak.SOURCE_TILE_ENTITY]
        if (tileEntity != null)
            return canBreak(tileEntity, tool, block)
        
        val responsiblePlayer = ctx[BlockBreak.RESPONSIBLE_PLAYER]
        if (responsiblePlayer != null)
            return canBreak(responsiblePlayer, tool, block)
        
        return true
    }
    
    /**
     * Checks if that [tileEntity] can break [block] using that [item].
     */
    fun canBreakAsync(tileEntity: TileEntity, item: ItemStack?, block: Block): CompletableFuture<Boolean> {
        if (tileEntity.owner == null)
            return CompletableFuture.completedFuture(true)
        if (!::cacheCanBreakTile.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanBreakTile.get(CanBreakTileArgs(tileEntity, item?.clone(), block.location))
    }
    
    /**
     * Checks if that [tileEntity] can break a block at that [pos] using that [item].
     */
    suspend fun canBreak(tileEntity: TileEntity, item: ItemStack?, block: Block): Boolean =
        canBreakAsync(tileEntity, item, block).await()
    
    /**
     * Checks if that [player] can break [block] using that [item].
     */
    fun canBreakAsync(player: OfflinePlayer, item: ItemStack?, block: Block): CompletableFuture<Boolean> {
        if (!::cacheCanBreakUser.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanBreakUser.get(CanBreakUserArgs(player, item?.clone(), block.location))
    }
    
    /**
     * Checks if that [player] can break a block at that [pos] using that [item].
     */
    suspend fun canBreak(player: OfflinePlayer, item: ItemStack?, block: Block): Boolean =
        canBreakAsync(player, item, block).await()
    
    /**
     * Checks if that [player] can break a block at that [pos] using that [item].
     */
    fun canBreak(player: Player, item: ItemStack?, block: Block): Boolean =
        canBreakAsync(player, item, block).get()
    
    /**
     * Checks whether the given [ctx] passes block interaction permission checks.
     */
    suspend fun canUseBlock(ctx: Context<BlockInteract>): Boolean {
        val block = ctx[BlockInteract.BLOCK]
        val item = ctx[BlockInteract.HELD_ITEM_STACK]
        
        val tileEntity = ctx[BlockInteract.SOURCE_TILE_ENTITY]
        if (tileEntity != null)
            return canUseBlock(tileEntity, item, block)
        
        val responsiblePlayer = ctx[BlockInteract.RESPONSIBLE_PLAYER]
        if (responsiblePlayer != null)
            return canUseBlock(responsiblePlayer, item, block)
        
        return true
    }
    
    /**
     * Checks if the [tileEntity] can interact with [block] using that [item].
     */
    fun canUseBlockAsync(tileEntity: TileEntity, item: ItemStack?, block: Block): CompletableFuture<Boolean> {
        if (tileEntity.owner == null)
            return CompletableFuture.completedFuture(true)
        if (!::cacheCanUseBlockTile.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanUseBlockTile.get(CanUseBlockTileArgs(tileEntity, item?.clone(), block.location))
    }
    
    /**
     * Checks if the [tileEntity] can interact with a block at that [pos] using that [item].
     */
    suspend fun canUseBlock(tileEntity: TileEntity, item: ItemStack?, block: Block): Boolean =
        canUseBlockAsync(tileEntity, item, block).await()
    
    /**
     * Checks if the [player] can interact with [block] using that [item].
     */
    fun canUseBlockAsync(player: OfflinePlayer, item: ItemStack?, block: Block): CompletableFuture<Boolean> {
        if (!::cacheCanUseBlockUser.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanUseBlockUser.get(CanUseBlockUserArgs(player, item?.clone(), block.location))
    }
    
    /**
     * Checks if the [player] can interact with a block at that [pos] using that [item].
     */
    suspend fun canUseBlock(player: OfflinePlayer, item: ItemStack?, block: Block): Boolean =
        canUseBlockAsync(player, item, block).await()
    
    /**
     * Checks if the [player] can interact with a block at that [pos] using that [item].
     */
    fun canUseBlock(player: Player, item: ItemStack?, block: Block): Boolean =
        canUseBlockAsync(player, item, block).get()
    
    /**
     * Checks if the [tileEntity] can use that [item] at that [location].
     */
    fun canUseItemAsync(tileEntity: TileEntity, item: ItemStack, location: Location): CompletableFuture<Boolean> {
        if (tileEntity.owner == null)
            return CompletableFuture.completedFuture(true)
        if (!::cacheCanUseItemTile.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanUseItemTile.get(CanUseItemTileArgs(tileEntity, item.clone(), location))
    }
    
    /**
     * Checks if the [tileEntity] can use that [item] at that [location].
     */
    suspend fun canUseItem(tileEntity: TileEntity, item: ItemStack, location: Location): Boolean =
        canUseItemAsync(tileEntity, item, location).await()
    
    /**
     * Checks if the [player] can use that [item] at that [location].
     */
    fun canUseItemAsync(player: OfflinePlayer, item: ItemStack, location: Location): CompletableFuture<Boolean> {
        if (!::cacheCanUseItemUser.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanUseItemUser.get(CanUseItemUserArgs(player, item.clone(), location))
    }
    
    /**
     * Checks if the [player] can use that [item] at that [location].
     */
    suspend fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location): Boolean =
        canUseItemAsync(player, item, location).await()
    
    /**
     * Checks if the [player] can use that [item] at that [location].
     */
    fun canUseItem(player: Player, item: ItemStack, location: Location): Boolean =
        canUseItemAsync(player, item, location).get()
    
    /**
     * Checks if the [tileEntity] can interact with the [entity] while holding that [item].
     */
    fun canInteractWithEntityAsync(tileEntity: TileEntity, entity: Entity, item: ItemStack?): CompletableFuture<Boolean> {
        if (tileEntity.owner == null)
            return CompletableFuture.completedFuture(true)
        if (!::cacheCanInteractWithEntityTile.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanInteractWithEntityTile.get(CanInteractWithEntityTileArgs(tileEntity, entity, item?.clone()))
    }
    
    /**
     * Checks if the [tileEntity] can interact with the [entity] wile holding that [item].
     */
    suspend fun canInteractWithEntity(tileEntity: TileEntity, entity: Entity, item: ItemStack?): Boolean =
        canInteractWithEntityAsync(tileEntity, entity, item).await()
    
    /**
     * Checks if the [player] can interact with the [entity] while holding that [item].
     */
    fun canInteractWithEntityAsync(player: OfflinePlayer, entity: Entity, item: ItemStack?): CompletableFuture<Boolean> {
        if (!::cacheCanInteractWithEntityUser.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanInteractWithEntityUser.get(CanInteractWithEntityUserArgs(player, entity, item?.clone()))
    }
    
    /**
     * Checks if the [player] can interact with the [entity] while holding that [item].
     */
    suspend fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean =
        canInteractWithEntityAsync(player, entity, item).await()
    
    /**
     * Checks if the [player] can interact with the [entity] while holding that [item].
     */
    fun canInteractWithEntity(player: Player, entity: Entity, item: ItemStack?): Boolean =
        canInteractWithEntityAsync(player, entity, item).get()
    
    /**
     * Checks if the [tileEntity] can hurt the [entity] with this [item].
     */
    fun canHurtEntityAsync(tileEntity: TileEntity, entity: Entity, item: ItemStack?): CompletableFuture<Boolean> {
        if (tileEntity.owner == null)
            return CompletableFuture.completedFuture(true)
        if (!::cacheCanHurtEntityTile.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanHurtEntityTile.get(CanHurtEntityTileArgs(tileEntity, entity, item?.clone()))
    }
    
    /**
     * Checks if the [tileEntity] can hurt the [entity] with this [item].
     */
    suspend fun canHurtEntity(tileEntity: TileEntity, entity: Entity, item: ItemStack?): Boolean =
        canHurtEntityAsync(tileEntity, entity, item).await()
    
    /**
     * Checks if the [player] can hurt the [entity] with this [item].
     */
    fun canHurtEntityAsync(player: OfflinePlayer, entity: Entity, item: ItemStack?): CompletableFuture<Boolean> {
        if (!::cacheCanHurtEntityUser.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanHurtEntityUser.get(CanHurtEntityUserArgs(player, entity, item?.clone()))
    }
    
    /**
     * Checks if the [player] can hurt the [entity] with this [item].
     */
    suspend fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean =
        canHurtEntityAsync(player, entity, item).await()
    
    /**
     * Checks if the [player] can hurt the [entity] with this [item].
     */
    fun canHurtEntity(player: Player, entity: Entity, item: ItemStack?): Boolean =
        canHurtEntityAsync(player, entity, item).get()
    
    /**
     * Registers [integration] and discards all cached protection check results.
     */
    internal fun registerIntegration(integration: ProtectionIntegration) {
        if (integration.executionMode == ExecutionMode.SERVER) {
            serverThreadOnlyIntegrations += integration
        } else {
            threadAgnosticIntegrations += integration
        }
        caches.forEach { it.invalidateAll() }
    }
    
    internal fun isVanillaProtected(player: OfflinePlayer, location: Location): Boolean {
        val spawnRadius = Bukkit.getServer().spawnRadius.toDouble()
        val world = location.world!!
        val spawnMin = world.spawnLocation.subtract(spawnRadius, 0.0, spawnRadius)
        val spawnMax = world.spawnLocation.add(spawnRadius, 0.0, spawnRadius)
        
        return world.name == "world"
            && spawnRadius > 0
            && !player.isOp
            && location.isBetweenXZ(spawnMin, spawnMax)
    }
    
    /**
     * Caches the results of the protection check [check], separately for integrations with [ExecutionMode.NONE]
     * and [ExecutionMode.SERVER].
     */
    private class ProtectionCache<A : ProtectionArgs>(
        private val check: ProtectionIntegration.(A) -> Boolean
    ) {
        
        private val syncCache: LoadingCache<A, Boolean> = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .build(::checkServerThreadOnlyIntegrations)
        
        // only ExecutionMode.NONE integrations can be refreshed, sync refreshing would create unnecessary load & may risk deadlocks
        private val asyncCache: LoadingCache<A, CompletableFuture<Boolean>> = Caffeine.newBuilder()
            .executor(executor)
            .expireAfterAccess(Duration.ofSeconds(60))
            .refreshAfterWrite(Duration.ofSeconds(30))
            .build(::checkThreadAgnosticIntegrations)
        
        init {
            caches += this
        }
        
        fun invalidateAll() {
            asyncCache.invalidateAll()
            syncCache.invalidateAll()
        }
        
        fun get(args: A): CompletableFuture<Boolean> {
            if (!Nova.isEnabled || isVanillaProtected(args.player, args.location))
                return CompletableFuture.completedFuture(false)
            
            val anyThreadResult = asyncCache.get(args)
            if (serverThreadOnlyIntegrations.isEmpty())
                return anyThreadResult
            
            return anyThreadResult.thenCombine(getServerThreadOnlyIntegrationsResult(args)) { a, b -> a && b }
        }
        
        private fun getServerThreadOnlyIntegrationsResult(args: A): CompletableFuture<Boolean> {
            if (isServerThread)
                return CompletableFuture.completedFuture(syncCache.get(args))
            
            val cached = syncCache.getIfPresent(args)
            if (cached != null)
                return CompletableFuture.completedFuture(cached)
            
            val future = CompletableFuture<Boolean>()
            runTask {
                try {
                    future.complete(syncCache.get(args))
                } catch (t: Throwable) {
                    future.completeExceptionally(t)
                }
            }
            return future
        }
        
        private fun checkThreadAgnosticIntegrations(args: A): CompletableFuture<Boolean> {
            if (threadAgnosticIntegrations.isEmpty())
                return CompletableFuture.completedFuture(true)
            
            if (isServerThread && !args.player.isOnline) {
                val futures = threadAgnosticIntegrations.map { CompletableFuture.supplyAsync({ it.check(args) }, executor) }
                return CombinedBooleanFuture(futures)
            } else {
                // assumes that queries for online players can be performed on the server thread
                return CompletableFuture.completedFuture(threadAgnosticIntegrations.all { it.check(args) })
            }
        }
        
        private fun checkServerThreadOnlyIntegrations(args: A): Boolean {
            check(isServerThread)
            return serverThreadOnlyIntegrations.all { it.check(args) }
        }
        
    }
    
}
