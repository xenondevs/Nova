package xyz.xenondevs.nova.integration.protection

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.google.common.util.concurrent.ThreadFactoryBuilder
import kotlinx.coroutines.future.await
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
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
import xyz.xenondevs.nova.initialize.Dispatcher
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.HooksLoader
import xyz.xenondevs.nova.integration.permission.PermissionManager
import xyz.xenondevs.nova.util.concurrent.CombinedBooleanFuture
import xyz.xenondevs.nova.util.concurrent.isServerThread
import xyz.xenondevs.nova.util.isBetweenXZ
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
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
 * If all protection integrations [can be called asynchronously][ExecutionMode], commonly used protection checks will be refreshed
 * every 30s.
 */
@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dispatcher = Dispatcher.ASYNC,
    dependsOn = [HooksLoader::class, PermissionManager::class]
)
object ProtectionManager {
    
    internal val integrations = CopyOnWriteArrayList<ProtectionIntegration>()
    
    private lateinit var executor: ExecutorService
    private lateinit var cacheCanPlaceUser: LoadingCache<CanPlaceUserArgs, CompletableFuture<Boolean>>
    private lateinit var cacheCanPlaceTile: LoadingCache<CanPlaceTileArgs, CompletableFuture<Boolean>>
    private lateinit var cacheCanBreakUser: LoadingCache<CanBreakUserArgs, CompletableFuture<Boolean>>
    private lateinit var cacheCanBreakTile: LoadingCache<CanBreakTileArgs, CompletableFuture<Boolean>>
    private lateinit var cacheCanUseBlockUser: LoadingCache<CanUseBlockUserArgs, CompletableFuture<Boolean>>
    private lateinit var cacheCanUseBlockTile: LoadingCache<CanUseBlockTileArgs, CompletableFuture<Boolean>>
    private lateinit var cacheCanUseItemUser: LoadingCache<CanUseItemUserArgs, CompletableFuture<Boolean>>
    private lateinit var cacheCanUseItemTile: LoadingCache<CanUseItemTileArgs, CompletableFuture<Boolean>>
    private lateinit var cacheCanInteractWithEntityUser: LoadingCache<CanInteractWithEntityUserArgs, CompletableFuture<Boolean>>
    private lateinit var cacheCanInteractWithEntityTile: LoadingCache<CanInteractWithEntityTileArgs, CompletableFuture<Boolean>>
    private lateinit var cacheCanHurtEntityUser: LoadingCache<CanHurtEntityUserArgs, CompletableFuture<Boolean>>
    private lateinit var cacheCanHurtEntityTile: LoadingCache<CanHurtEntityTileArgs, CompletableFuture<Boolean>>
    
    @InitFun
    private fun init() {
        executor = ThreadPoolExecutor(
            10, 10,
            0, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            ThreadFactoryBuilder().setNameFormat("Nova Protection Worker - %s").build()
        )
        
        val cacheBuilder = Caffeine.newBuilder()
            .executor(executor)
            .expireAfterAccess(Duration.ofSeconds(60))
        
        // only configure refreshing if the action won't block the main thread
        if (integrations.none { it.executionMode == ExecutionMode.SERVER })
            cacheBuilder.refreshAfterWrite(Duration.ofSeconds(30))
        
        cacheCanPlaceUser = cacheBuilder.build { checkProtection(it) { canPlace(it.player, it.item, it.location) } }
        cacheCanPlaceTile = cacheBuilder.build { checkProtection(it) { canPlace(it.apiTileEntity, it.item, it.location) } }
        cacheCanBreakUser = cacheBuilder.build { checkProtection(it) { canBreak(it.player, it.item, it.location) } }
        cacheCanBreakTile = cacheBuilder.build { checkProtection(it) { canBreak(it.apiTileEntity, it.item, it.location) } }
        cacheCanUseBlockUser = cacheBuilder.build { checkProtection(it) { canUseBlock(it.player, it.item, it.location) } }
        cacheCanUseBlockTile = cacheBuilder.build { checkProtection(it) { canUseBlock(it.apiTileEntity, it.item, it.location) } }
        cacheCanUseItemUser = cacheBuilder.build { checkProtection(it) { canUseItem(it.player, it.item, it.location) } }
        cacheCanUseItemTile = cacheBuilder.build { checkProtection(it) { canUseItem(it.apiTileEntity, it.item, it.location) } }
        cacheCanInteractWithEntityUser = cacheBuilder.build { checkProtection(it) { canInteractWithEntity(it.player, it.entity, it.item) } }
        cacheCanInteractWithEntityTile = cacheBuilder.build { checkProtection(it) { canInteractWithEntity(it.apiTileEntity, it.entity, it.item) } }
        cacheCanHurtEntityUser = cacheBuilder.build { checkProtection(it) { canHurtEntity(it.player, it.entity, it.item) } }
        cacheCanHurtEntityTile = cacheBuilder.build { checkProtection(it) { canHurtEntity(it.apiTileEntity, it.entity, it.item) } }
    }
    
    @DisableFun(runBefore = [PermissionManager::class])
    private fun disable() {
        executor.shutdown()
    }
    
    /**
     * Checks whether the given [ctx] passes place permission checks.
     */
    suspend fun canPlace(ctx: Context<BlockPlace>): Boolean {
        val pos = ctx[BlockPlace.BLOCK_POS]
        val blockItem = ctx[BlockPlace.BLOCK_ITEM_STACK] ?: ItemStack(Material.AIR)
        
        val tileEntity = ctx[BlockPlace.SOURCE_TILE_ENTITY]
        if (tileEntity != null)
            return canPlace(tileEntity, blockItem, pos)
        
        val responsiblePlayer = ctx[BlockPlace.RESPONSIBLE_PLAYER]
        if (responsiblePlayer != null)
            return canPlace(responsiblePlayer, blockItem, pos)
        
        return true
    }
    
    /**
     * Checks if the [tileEntity] can place that [item] at that [pos].
     */
    fun canPlaceAsync(tileEntity: TileEntity, item: ItemStack, pos: BlockPos): CompletableFuture<Boolean> {
        if (tileEntity.owner == null)
            return CompletableFuture.completedFuture(true)
        if (!::cacheCanPlaceTile.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanPlaceTile.get(CanPlaceTileArgs(tileEntity, item.clone(), pos.location))
    }
    
    /**
     * Checks if the [tileEntity] can place that [item] at that [pos].
     */
    suspend fun canPlace(tileEntity: TileEntity, item: ItemStack, pos: BlockPos): Boolean =
        canPlaceAsync(tileEntity, item, pos).await()
    
    /**
     * Checks if the [player] can place that [item] at that [pos].
     */
    fun canPlaceAsync(player: OfflinePlayer, item: ItemStack, pos: BlockPos): CompletableFuture<Boolean> {
        if (!::cacheCanPlaceUser.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanPlaceUser.get(CanPlaceUserArgs(player, item.clone(), pos.location))
    }
    
    /**
     * Checks if the [player] can place that [item] at that [pos].
     */
    suspend fun canPlace(player: OfflinePlayer, item: ItemStack, pos: BlockPos): Boolean =
        canPlaceAsync(player, item, pos).await()
    
    /**
     * Checks if the [player] can place that [item] at that [pos].
     */
    fun canPlace(player: Player, item: ItemStack, pos: BlockPos): Boolean =
        canPlaceAsync(player, item, pos).get()
    
    /**
     * Checks whether the given [ctx] passes break permission checks.
     */
    suspend fun canBreak(ctx: Context<BlockBreak>): Boolean {
        val pos = ctx[BlockBreak.BLOCK_POS]
        val tool = ctx[BlockBreak.TOOL_ITEM_STACK]
        
        val tileEntity = ctx[BlockBreak.SOURCE_TILE_ENTITY]
        if (tileEntity != null)
            return canBreak(tileEntity, tool, pos)
        
        val responsiblePlayer = ctx[BlockBreak.RESPONSIBLE_PLAYER]
        if (responsiblePlayer != null)
            return canBreak(responsiblePlayer, tool, pos)
        
        return true
    }
    
    /**
     * Checks if that [tileEntity] can break a block at that [pos] using that [item].
     */
    fun canBreakAsync(tileEntity: TileEntity, item: ItemStack?, pos: BlockPos): CompletableFuture<Boolean> {
        if (tileEntity.owner == null)
            return CompletableFuture.completedFuture(true)
        if (!::cacheCanBreakTile.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanBreakTile.get(CanBreakTileArgs(tileEntity, item?.clone(), pos.location))
    }
    
    /**
     * Checks if that [tileEntity] can break a block at that [pos] using that [item].
     */
    suspend fun canBreak(tileEntity: TileEntity, item: ItemStack?, pos: BlockPos): Boolean =
        canBreakAsync(tileEntity, item, pos).await()
    
    /**
     * Checks if that [player] can break a block at that [pos] using that [item].
     */
    fun canBreakAsync(player: OfflinePlayer, item: ItemStack?, pos: BlockPos): CompletableFuture<Boolean> {
        if (!::cacheCanBreakUser.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanBreakUser.get(CanBreakUserArgs(player, item?.clone(), pos.location))
    }
    
    /**
     * Checks if that [player] can break a block at that [pos] using that [item].
     */
    suspend fun canBreak(player: OfflinePlayer, item: ItemStack?, pos: BlockPos): Boolean =
        canBreakAsync(player, item, pos).await()
    
    /**
     * Checks if that [player] can break a block at that [pos] using that [item].
     */
    fun canBreak(player: Player, item: ItemStack?, pos: BlockPos): Boolean =
        canBreakAsync(player, item, pos).get()
    
    /**
     * Checks whether the given [ctx] passes block interaction permission checks.
     */
    suspend fun canUseBlock(ctx: Context<BlockInteract>): Boolean {
        val pos = ctx[BlockInteract.BLOCK_POS]
        val item = ctx[BlockInteract.HELD_ITEM_STACK]
        
        val tileEntity = ctx[BlockInteract.SOURCE_TILE_ENTITY]
        if (tileEntity != null)
            return canUseBlock(tileEntity, item, pos)
        
        val responsiblePlayer = ctx[BlockInteract.RESPONSIBLE_PLAYER]
        if (responsiblePlayer != null)
            return canUseBlock(responsiblePlayer, item, pos)
        
        return true
    }
    
    /**
     * Checks if the [tileEntity] can interact with a block at that [pos] using that [item].
     */
    fun canUseBlockAsync(tileEntity: TileEntity, item: ItemStack?, pos: BlockPos): CompletableFuture<Boolean> {
        if (tileEntity.owner == null)
            return CompletableFuture.completedFuture(true)
        if (!::cacheCanUseBlockTile.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanUseBlockTile.get(CanUseBlockTileArgs(tileEntity, item?.clone(), pos.location))
    }
    
    /**
     * Checks if the [tileEntity] can interact with a block at that [pos] using that [item].
     */
    suspend fun canUseBlock(tileEntity: TileEntity, item: ItemStack?, pos: BlockPos): Boolean =
        canUseBlockAsync(tileEntity, item, pos).await()
    
    /**
     * Checks if the [player] can interact with a block at that [pos] using that [item].
     */
    fun canUseBlockAsync(player: OfflinePlayer, item: ItemStack?, pos: BlockPos): CompletableFuture<Boolean> {
        if (!::cacheCanUseBlockUser.isInitialized)
            return CompletableFuture.completedFuture(false)
        return cacheCanUseBlockUser.get(CanUseBlockUserArgs(player, item?.clone(), pos.location))
    }
    
    /**
     * Checks if the [player] can interact with a block at that [pos] using that [item].
     */
    suspend fun canUseBlock(player: OfflinePlayer, item: ItemStack?, pos: BlockPos): Boolean =
        canUseBlockAsync(player, item, pos).await()
    
    /**
     * Checks if the [player] can interact with a block at that [pos] using that [item].
     */
    fun canUseBlock(player: Player, item: ItemStack?, pos: BlockPos): Boolean =
        canUseBlockAsync(player, item, pos).get()
    
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
    
    private fun checkProtection(
        args: ProtectionArgs,
        check: ProtectionIntegration.() -> Boolean
    ): CompletableFuture<Boolean> {
        if (!Nova.isEnabled)
            return CompletableFuture.completedFuture(false)
        
        val player = args.player
        if (isVanillaProtected(player, args.location))
            return CompletableFuture.completedFuture(false)
        
        if (integrations.isNotEmpty()) {
            val futures = ArrayList<CompletableFuture<Boolean>>()
            for (integration in integrations) {
                // assumes that queries for online players can be performed on the main thread
                if (!player.isOnline && isServerThread && integration.executionMode != ExecutionMode.SERVER) {
                    // player offline, in main thread, async calls allowed -> check protection async 
                    futures += CompletableFuture.supplyAsync({ integration.check() }, executor)
                } else if (!isServerThread && integration.executionMode == ExecutionMode.SERVER) {
                    // not in main thread, no async calls allowed -> check protection on main thread
                    futures += CompletableFuture<Boolean>().apply { runTask { complete(integration.check()) } }
                } else {
                    // check protection in current thread (online player, already async, or no async calls allowed)
                    futures += CompletableFuture.completedFuture(integration.check())
                }
            }
            
            return CombinedBooleanFuture(futures)
        }
        
        return CompletableFuture.completedFuture(true)
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
    
}