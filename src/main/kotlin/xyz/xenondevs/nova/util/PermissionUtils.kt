package xyz.xenondevs.nova.util

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