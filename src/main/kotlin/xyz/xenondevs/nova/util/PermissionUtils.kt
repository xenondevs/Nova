package xyz.xenondevs.nova.util

import net.milkbowl.vault.permission.Permission
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.World
import java.util.*

object PermissionUtils {
    
    fun hasPermission(world: World, player: OfflinePlayer, permission: String): Boolean {
        return if (player.isOnline) {
            player.player!!.hasPermission(permission)
        } else {
            VaultUtils.hasPermission(world, player, permission)
        }
    }
    
    fun hasPermission(world: World, uuid: UUID, permission: String): Boolean {
        return hasPermission(world, Bukkit.getOfflinePlayer(uuid), permission)
    }
    
}

object VaultUtils {
    
    private val PERMISSIONS = if (Bukkit.getPluginManager().getPlugin("Vault") != null)
        Bukkit.getServer().servicesManager.getRegistration(Permission::class.java)!!.provider else null
    
    fun hasPermission(world: World, offlinePlayer: OfflinePlayer, permission: String): Boolean {
        if (PERMISSIONS == null) return false
        return PERMISSIONS.playerHas(world.name, offlinePlayer, permission)
    }
    
}