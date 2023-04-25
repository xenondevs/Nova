package xyz.xenondevs.nova.hooks.permission

import org.bukkit.OfflinePlayer
import org.bukkit.World

interface PermissionIntegration {
    
    fun hasPermission(world: World, player: OfflinePlayer, permission: String): Boolean?
    
}