package xyz.xenondevs.nova.hooks.permission

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.craftbukkit.v1_19_R3.CraftServer
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.nova.util.runAsyncTaskTimer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PermissionManager {
    
    internal val integrations = ArrayList<PermissionIntegration>()
    private val offlinePermissionCache = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, ConcurrentHashMap<String, Boolean>>>()
    
    init {
        runAsyncTaskTimer(6000L, 6000L, ::updateOfflinePermissions)
    }
    
    fun hasPermission(world: World, player: OfflinePlayer, permission: String): Boolean {
        println("getting permission for ${player.name}: $permission")
        return if (player.isOnline) player.player!!.hasPermission(permission)
        else hasOfflinePermission(world, player, permission)
    }
    
    fun hasPermission(world: World, uuid: UUID, permission: String): Boolean {
        return hasPermission(world, Bukkit.getOfflinePlayer(uuid), permission)
    }
    
    private fun updateOfflinePermissions() {
        offlinePermissionCache.forEach { (playerUUID, worldMap) ->
            val player = Bukkit.getOfflinePlayer(playerUUID)
            worldMap.forEach { (worldUUID, permissionMap) ->
                val world = Bukkit.getWorld(worldUUID)
                if (world != null)
                    permissionMap.keys.forEach { permissionMap[it] = checkPermissionIntegrations(world, player, it) }
                else worldMap -= worldUUID
            }
        }
    }
    
    @Suppress("LiftReturnOrAssignment")
    private fun hasOfflinePermission(world: World, player: OfflinePlayer, permission: String): Boolean {
        println("checking permission for offline player ${player.name}: $permission")
        val permissionMap = offlinePermissionCache
            .getOrPut(player.uniqueId, ::ConcurrentHashMap)
            .getOrPut(world.uid, ::ConcurrentHashMap)
        
        if (permissionMap.containsKey(permission)) {
            return permissionMap[permission] ?: false
        } else if ((Bukkit.getServer() as CraftServer).server.serverThread != Thread.currentThread()) {
            val result = checkPermissionIntegrations(world, player, permission)
            permissionMap[permission] = result
            return result
        } else {
            runAsyncTask { permissionMap[permission] = checkPermissionIntegrations(world, player, permission) }
            return false
        }
    }
    
    private fun checkPermissionIntegrations(world: World, player: OfflinePlayer, permission: String): Boolean =
        integrations.firstNotNullOfOrNull { it.hasPermission(world, player, permission) } ?: false
    
}