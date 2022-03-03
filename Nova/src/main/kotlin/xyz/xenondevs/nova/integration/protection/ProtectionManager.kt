package xyz.xenondevs.nova.integration.protection

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.integration.protection.plugin.*
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.data.ArrayKey
import xyz.xenondevs.nova.util.isBetweenXZ
import xyz.xenondevs.nova.world.pos
import java.util.concurrent.TimeUnit

object ProtectionManager {
    
    val integrations: MutableList<ProtectionIntegration> =
        listOf(GriefPrevention, PlotSquared, WorldGuard, Towny, ProtectionStones)
            .filterTo(ArrayList(), InternalProtectionIntegration::isInstalled)
    
    private val PROTECTION_CACHE: Cache<ArrayKey<Any?>, Boolean> =
        CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build()
    
    fun canPlace(tileEntity: TileEntity, item: ItemStack, location: Location): Boolean =
        PROTECTION_CACHE.get(ArrayKey(0, tileEntity.uuid, item, location.pos)) {
            !isVanillaProtected(tileEntity.owner, location)
                && integrations.all { it.canPlace(tileEntity, item, location) }
        }
    
    fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location): Boolean =
        PROTECTION_CACHE.get(ArrayKey(0, player.uniqueId, item, location.pos)) {
            !isVanillaProtected(player, location)
                && integrations.all { it.canPlace(player, item, location) }
        }
    
    fun canBreak(tileEntity: TileEntity, item: ItemStack?, location: Location): Boolean =
        PROTECTION_CACHE.get(ArrayKey(1, tileEntity.uuid, item, location.pos)) {
            !isVanillaProtected(tileEntity.owner, location)
                && integrations.all { it.canBreak(tileEntity, item, location) }
        }
    
    fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean =
        PROTECTION_CACHE.get(ArrayKey(1, player.uniqueId, item, location.pos)) {
            !isVanillaProtected(player, location)
                && integrations.all { it.canBreak(player, item, location) }
        }
    
    fun canUseBlock(tileEntity: TileEntity, item: ItemStack?, location: Location): Boolean =
        PROTECTION_CACHE.get(ArrayKey(2, tileEntity.uuid, item, location.pos)) {
            !isVanillaProtected(tileEntity.owner, location)
                && integrations.all { it.canUseBlock(tileEntity, item, location) }
        }
    
    fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean =
        PROTECTION_CACHE.get(ArrayKey(2, player.uniqueId, item, location.pos)) {
            !isVanillaProtected(player, location)
                && integrations.all { it.canUseBlock(player, item, location) }
        }
    
    fun canUseItem(tileEntity: TileEntity, item: ItemStack, location: Location): Boolean =
        PROTECTION_CACHE.get(ArrayKey(3, tileEntity.uuid, item, location.pos)) {
            !isVanillaProtected(tileEntity.owner, location)
                && integrations.all { it.canUseBlock(tileEntity, item, location) }
        }
    
    fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location): Boolean =
        PROTECTION_CACHE.get(ArrayKey(3, player.uniqueId, item, location.pos)) {
            !isVanillaProtected(player, location)
                && integrations.all { it.canUseBlock(player, item, location) }
        }
    
    fun canInteractWithEntity(tileEntity: TileEntity, entity: Entity, item: ItemStack?): Boolean =
        PROTECTION_CACHE.get(ArrayKey(4, tileEntity.uuid, entity, item, entity.location.pos)) {
            !isVanillaProtected(tileEntity.owner, entity.location)
                && integrations.all { it.canInteractWithEntity(tileEntity, entity, item) }
        }
    
    fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean =
        PROTECTION_CACHE.get(ArrayKey(4, player.uniqueId, entity, item, entity.location.pos)) {
            !isVanillaProtected(player, entity.location)
                && integrations.all { it.canInteractWithEntity(player, entity, item) }
        }
    
    fun canHurtEntity(tileEntity: TileEntity, entity: Entity, item: ItemStack?): Boolean =
        PROTECTION_CACHE.get(ArrayKey(5, tileEntity.uuid, entity, item, entity.location.pos)) {
            !isVanillaProtected(tileEntity.owner, entity.location)
                && integrations.all { it.canHurtEntity(tileEntity, entity, item) }
        }
    
    fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean =
        PROTECTION_CACHE.get(ArrayKey(5, player.uniqueId, entity, item, entity.location.pos)) {
            !isVanillaProtected(player, entity.location)
                && integrations.all { it.canHurtEntity(player, entity, item) }
        }
    
    private fun isVanillaProtected(offlinePlayer: OfflinePlayer, location: Location): Boolean {
        val spawnRadius = Bukkit.getServer().spawnRadius.toDouble()
        val world = location.world!!
        return world.name == "world"
            && spawnRadius > 0
            && !offlinePlayer.isOp
            && location.isBetweenXZ(
            world.spawnLocation.subtract(spawnRadius, 0.0, spawnRadius),
            world.spawnLocation.add(spawnRadius, 0.0, spawnRadius)
        )
    }
    
}