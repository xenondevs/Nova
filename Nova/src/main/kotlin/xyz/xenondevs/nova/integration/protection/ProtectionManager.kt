package xyz.xenondevs.nova.integration.protection

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.api.protection.ProtectionIntegration.ExecutionMode
import xyz.xenondevs.nova.integration.InternalIntegration
import xyz.xenondevs.nova.integration.protection.plugin.*
import xyz.xenondevs.nova.integration.protection.plugin.GriefPrevention
import xyz.xenondevs.nova.integration.protection.plugin.PlotSquared
import xyz.xenondevs.nova.integration.protection.plugin.ProtectionStones
import xyz.xenondevs.nova.integration.protection.plugin.QuickShop
import xyz.xenondevs.nova.integration.protection.plugin.Towny
import xyz.xenondevs.nova.integration.protection.plugin.WorldGuard
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.concurrent.CombinedBooleanFuture
import xyz.xenondevs.nova.util.data.ArrayKey
import xyz.xenondevs.nova.util.isBetweenXZ
import xyz.xenondevs.nova.util.minecraftServer
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.world.pos
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object ProtectionManager {
    
    internal val integrations: MutableList<ProtectionIntegration> =
        listOf(GriefPrevention, PlotSquared, WorldGuard, Towny, ProtectionStones, QuickShop, Residence, IridiumSkyblock)
            .filterTo(ArrayList(), InternalIntegration::isInstalled)
    
    private val PROTECTION_CHECK_EXECUTOR = ThreadPoolExecutor(
        10, 10,
        0, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(),
        ThreadFactoryBuilder().setNameFormat("Nova Protection Worker - %s").build()
    )
    
    private val PROTECTION_CACHE: Cache<ArrayKey<Any?>, CompletableFuture<Boolean>> =
        CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build()
    
    init {
        NOVA.disableHandlers += { PROTECTION_CHECK_EXECUTOR.shutdown() }
    }
    
    fun canPlace(tileEntity: TileEntity, item: ItemStack, location: Location): CompletableFuture<Boolean> =
        PROTECTION_CACHE.get(ArrayKey(0, tileEntity.uuid, item, location.pos)) {
            checkProtection(tileEntity.owner, location) { canPlace(tileEntity, item, location) }
        }
    
    fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location): CompletableFuture<Boolean> =
        PROTECTION_CACHE.get(ArrayKey(0, player.uniqueId, item, location.pos)) {
            checkProtection(player, location) { canPlace(player, item, location) }
        }
    
    fun canBreak(tileEntity: TileEntity, item: ItemStack?, location: Location): CompletableFuture<Boolean> =
        PROTECTION_CACHE.get(ArrayKey(1, tileEntity.uuid, item, location.pos)) {
            checkProtection(tileEntity.owner, location) { canBreak(tileEntity, item, location) }
        }
    
    fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location): CompletableFuture<Boolean> =
        PROTECTION_CACHE.get(ArrayKey(1, player.uniqueId, item, location.pos)) {
            checkProtection(player, location) { canBreak(player, item, location) }
        }
    
    fun canUseBlock(tileEntity: TileEntity, item: ItemStack?, location: Location): CompletableFuture<Boolean> =
        PROTECTION_CACHE.get(ArrayKey(2, tileEntity.uuid, item, location.pos)) {
            checkProtection(tileEntity.owner, location) { canUseBlock(tileEntity, item, location) }
        }
    
    fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location): CompletableFuture<Boolean> =
        PROTECTION_CACHE.get(ArrayKey(2, player.uniqueId, item, location.pos)) {
            checkProtection(player, location) { canUseBlock(player, item, location) }
        }
    
    fun canUseItem(tileEntity: TileEntity, item: ItemStack, location: Location): CompletableFuture<Boolean> =
        PROTECTION_CACHE.get(ArrayKey(3, tileEntity.uuid, item, location.pos)) {
            checkProtection(tileEntity.owner, location) { canUseBlock(tileEntity, item, location) }
        }
    
    fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location): CompletableFuture<Boolean> =
        PROTECTION_CACHE.get(ArrayKey(3, player.uniqueId, item, location.pos)) {
            checkProtection(player, location) { canUseBlock(player, item, location) }
        }
    
    fun canInteractWithEntity(tileEntity: TileEntity, entity: Entity, item: ItemStack?): CompletableFuture<Boolean> =
        PROTECTION_CACHE.get(ArrayKey(4, tileEntity.uuid, entity, item, entity.location.pos)) {
            checkProtection(tileEntity.owner, entity.location) { canInteractWithEntity(tileEntity, entity, item) }
        }
    
    fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): CompletableFuture<Boolean> =
        PROTECTION_CACHE.get(ArrayKey(4, player.uniqueId, entity, item, entity.location.pos)) {
            checkProtection(player, entity.location) { canInteractWithEntity(player, entity, item) }
        }
    
    fun canHurtEntity(tileEntity: TileEntity, entity: Entity, item: ItemStack?): CompletableFuture<Boolean> =
        PROTECTION_CACHE.get(ArrayKey(5, tileEntity.uuid, entity, item, entity.location.pos)) {
            checkProtection(tileEntity.owner, entity.location) { canHurtEntity(tileEntity, entity, item) }
        }
    
    fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): CompletableFuture<Boolean> =
        PROTECTION_CACHE.get(ArrayKey(5, player.uniqueId, entity, item, entity.location.pos)) {
            checkProtection(player, entity.location) { canHurtEntity(player, entity, item) }
        }
    
    private fun checkProtection(
        player: OfflinePlayer,
        location: Location,
        check: ProtectionIntegration.() -> Boolean
    ): CompletableFuture<Boolean> {
        if (!NOVA.isEnabled) return CompletableFuture.completedFuture(false)
        
        val futures = checkIntegrations(check)
        futures += CompletableFuture.completedFuture(!isVanillaProtected(player, location))
        return CombinedBooleanFuture(futures)
    }
    
    private fun isVanillaProtected(player: OfflinePlayer, location: Location): Boolean {
        val spawnRadius = Bukkit.getServer().spawnRadius.toDouble()
        val world = location.world!!
        return world.name == "world"
            && spawnRadius > 0
            && !player.isOp
            && location.isBetweenXZ(
            world.spawnLocation.subtract(spawnRadius, 0.0, spawnRadius),
            world.spawnLocation.add(spawnRadius, 0.0, spawnRadius)
        )
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun checkIntegrations(check: ProtectionIntegration.() -> Boolean): MutableList<CompletableFuture<Boolean>> {
        val isMainThread = Thread.currentThread() == minecraftServer.serverThread
        val futures = ArrayList<CompletableFuture<Boolean>>()
        
        integrations.forEach { integration ->
            when (integration.executionMode) {
                
                ExecutionMode.NONE -> futures += CompletableFuture.completedFuture(integration.check())
                
                ExecutionMode.ASYNC -> {
                    val future = CompletableFuture<Boolean>()
                    futures += future
                    PROTECTION_CHECK_EXECUTOR.submit { future.complete(integration.check()) }
                }
                
                ExecutionMode.SERVER -> {
                    val future = CompletableFuture<Boolean>()
                    futures += future
                    
                    if (isMainThread) future.complete(integration.check())
                    else runTask { future.complete(integration.check()) }
                }
                
            }
        }
        
        return futures
    }
    
}

