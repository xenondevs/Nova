package xyz.xenondevs.nova.hooks.permission

import org.bukkit.OfflinePlayer
import org.bukkit.World
import java.util.concurrent.CompletableFuture

interface PermissionIntegration {
    
    fun hasPermission(world: World, player: OfflinePlayer, permission: String): CompletableFuture<Boolean?>
    
}