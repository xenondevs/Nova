package xyz.xenondevs.nova.util

import net.milkbowl.vault.permission.Permission
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.World

object VaultUtils {
    
    private val PERMISSIONS = if (Bukkit.getPluginManager().getPlugin("Vault") != null)
        Bukkit.getServer().servicesManager.getRegistration(Permission::class.java)!!.provider else null
    
    fun hasPermission(world: World, offlinePlayer: OfflinePlayer, permission: String): Boolean {
        if (PERMISSIONS == null) return false
        return PERMISSIONS.playerHas(world.name, offlinePlayer, permission)
    }
    
}