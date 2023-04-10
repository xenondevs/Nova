package xyz.xenondevs.nova.util

import net.milkbowl.vault.permission.Permission
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.World
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PermissionUtils {
    
    private val PERMISSIONS = if (Bukkit.getPluginManager().getPlugin("Vault") != null)
        Bukkit.getServer().servicesManager.getRegistration(Permission::class.java)!!.provider else null
    
    private val offlinePermissionCache = ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, ConcurrentHashMap<String, Boolean>>>()
    
    init {
        if (PERMISSIONS != null) runAsyncTaskTimer(6000L, 6000L, ::updateOfflinePermissions)
    }
    
    fun hasPermission(world: World, player: OfflinePlayer, permission: String): Boolean {
        return if (player.isOnline) player.player!!.hasPermission(permission)
        else hasOfflinePermission(world, player, permission)
    }
    
    fun hasPermission(world: World, uuid: UUID, permission: String): Boolean {
        return hasPermission(world, Bukkit.getOfflinePlayer(uuid), permission)
    }
    
    private fun updateOfflinePermissions() {
        require(PERMISSIONS != null)
        
        offlinePermissionCache.forEach { (playerUUID, worldMap) ->
            val player = Bukkit.getOfflinePlayer(playerUUID)
            worldMap.forEach { (worldUUID, permissionMap) ->
                val world = Bukkit.getWorld(worldUUID)
                if (world != null)
                    permissionMap.keys.forEach { permissionMap[it] = PERMISSIONS.playerHas(world.name, player, it) }
                else worldMap -= worldUUID
            }
        }
    }
    
    @Suppress("LiftReturnOrAssignment")
    private fun hasOfflinePermission(world: World, player: OfflinePlayer, permission: String): Boolean {
        if (PERMISSIONS == null) return false
        
        val permissionMap = offlinePermissionCache
            .getOrPut(player.uniqueId, ::ConcurrentHashMap)
            .getOrPut(world.uid, ::ConcurrentHashMap)
        
        if (permissionMap.containsKey(permission)) {
            return permissionMap[permission] ?: false
        } else if (MINECRAFT_SERVER.serverThread != Thread.currentThread()) {
            val result = PERMISSIONS.playerHas(world.name, player, permission)
            permissionMap[permission] = result
            return result
        } else {
            runAsyncTask { permissionMap[permission] = PERMISSIONS.playerHas(world.name, player, permission) }
            return false
        }
    }
    
}