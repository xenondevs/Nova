package xyz.xenondevs.nova.integration.protection

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.api.ApiTileEntityWrapper
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.api.protection.ProtectionIntegration.ExecutionMode
import xyz.xenondevs.nova.integration.HooksLoader
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.concurrent.CombinedBooleanFuture
import xyz.xenondevs.nova.util.concurrent.completeServerThread
import xyz.xenondevs.nova.util.isBetweenXZ
import java.time.Duration
import java.util.concurrent.CompletableFuture
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
@InternalInit(stage = InternalInitStage.POST_WORLD_ASYNC, dependsOn = [HooksLoader::class])
object ProtectionManager {
    
    internal val integrations = ArrayList<ProtectionIntegration>()
    
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
        //<editor-fold desc="executor service">
        executor = ThreadPoolExecutor(
            10, 10,
            0, TimeUnit.MILLISECONDS,
            LinkedBlockingQueue(),
            ThreadFactoryBuilder().setNameFormat("Nova Protection Worker - %s").build()
        )
        //</editor-fold>
        
        //<editor-fold desc="cache">
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
        //</editor-fold>
    }
    
    @DisableFun
    private fun disable() {
        executor.shutdown()
    }
    
    /**
     * Checks if the [tileEntity] can place that [item] at that [location]
     */
    fun canPlace(tileEntity: TileEntity, item: ItemStack, location: Location): CompletableFuture<Boolean> {
        if (tileEntity.owner == null) return CompletableFuture.completedFuture(true)
        return cacheCanPlaceTile.get(CanPlaceTileArgs(tileEntity, item.clone(), location.clone()))
    }
    
    /**
     * Checks if the [player] can place that [item] at that [location]
     */
    fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location): CompletableFuture<Boolean> =
        cacheCanPlaceUser.get(CanPlaceUserArgs(player, item.clone(), location.clone()))
    
    /**
     * Checks if that [tileEntity] can break a block at that [location] using that [item]
     */
    fun canBreak(tileEntity: TileEntity, item: ItemStack?, location: Location): CompletableFuture<Boolean> {
        if (tileEntity.owner == null) return CompletableFuture.completedFuture(true)
        return cacheCanBreakTile.get(CanBreakTileArgs(tileEntity, item?.clone(), location.clone()))
    }
    
    /**
     * Checks if that [player] can break a block at that [location] using that [item]
     */
    fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location): CompletableFuture<Boolean> =
        cacheCanBreakUser.get(CanBreakUserArgs(player, item?.clone(), location.clone()))
    
    /**
     * Checks if the [tileEntity] can interact with a block at that [location] using that [item]
     */
    fun canUseBlock(tileEntity: TileEntity, item: ItemStack?, location: Location): CompletableFuture<Boolean> {
        if (tileEntity.owner == null) return CompletableFuture.completedFuture(true)
        return cacheCanUseBlockTile.get(CanUseBlockTileArgs(tileEntity, item?.clone(), location.clone()))
    }
    
    /**
     * Checks if the [player] can interact with a block at that [location] using that [item]
     */
    fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location): CompletableFuture<Boolean> =
        cacheCanUseBlockUser.get(CanUseBlockUserArgs(player, item?.clone(), location.clone()))
    
    /**
     * Checks if the [tileEntity] can use that [item] at that [location]
     */
    fun canUseItem(tileEntity: TileEntity, item: ItemStack, location: Location): CompletableFuture<Boolean> {
        if (tileEntity.owner == null) return CompletableFuture.completedFuture(true)
        return cacheCanUseItemTile.get(CanUseItemTileArgs(tileEntity, item.clone(), location.clone()))
    }
    
    /**
     * Checks if the [player] can use that [item] at that [location]
     */
    fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location): CompletableFuture<Boolean> =
        cacheCanUseItemUser.get(CanUseItemUserArgs(player, item.clone(), location.clone()))
    
    /**
     * Checks if the [tileEntity] can interact with the [entity] wile holding that [item]
     */
    fun canInteractWithEntity(tileEntity: TileEntity, entity: Entity, item: ItemStack?): CompletableFuture<Boolean> {
        if (tileEntity.owner == null) return CompletableFuture.completedFuture(true)
        return cacheCanInteractWithEntityTile.get(CanInteractWithEntityTileArgs(tileEntity, entity, item?.clone()))
    }
    
    /**
     * Checks if the [player] can interact with the [entity] while holding that [item]
     */
    fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): CompletableFuture<Boolean> =
        cacheCanInteractWithEntityUser.get(CanInteractWithEntityUserArgs(player, entity, item?.clone()))
    
    /**
     * Checks if the [tileEntity] can hurt the [entity] with this [item]
     */
    fun canHurtEntity(tileEntity: TileEntity, entity: Entity, item: ItemStack?): CompletableFuture<Boolean> {
        if (tileEntity.owner == null) return CompletableFuture.completedFuture(true)
        return cacheCanHurtEntityTile.get(CanHurtEntityTileArgs(tileEntity, entity, item?.clone()))
    }
    
    /**
     * Checks if the [player] can hurt the [entity] with this [item]
     */
    fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): CompletableFuture<Boolean> =
        cacheCanHurtEntityUser.get(CanHurtEntityUserArgs(player, entity, item?.clone()))
    
    private fun checkProtection(
        args: ProtectionArgs,
        check: ProtectionIntegration.() -> Boolean
    ): CompletableFuture<Boolean> {
        if (!NOVA.isEnabled)
            return CompletableFuture.completedFuture(false)
        
        if (integrations.isNotEmpty()) {
            val futures = checkIntegrations(check)
            futures += CompletableFuture.completedFuture(!isVanillaProtected(args.player, args.location))
            return CombinedBooleanFuture(futures)
        } else {
            return CompletableFuture.completedFuture(!isVanillaProtected(args.player, args.location))
        }
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
    
    private fun checkIntegrations(check: ProtectionIntegration.() -> Boolean): MutableList<CompletableFuture<Boolean>> =
        integrations.mapTo(ArrayList()) {
            when (it.executionMode) {
                ExecutionMode.NONE -> CompletableFuture.completedFuture(it.check())
                ExecutionMode.ASYNC -> CompletableFuture<Boolean>().apply { completeAsync({ it.check() }, executor) }
                ExecutionMode.SERVER -> CompletableFuture<Boolean>().apply { completeServerThread { it.check() } }
            }
        }
    
}

