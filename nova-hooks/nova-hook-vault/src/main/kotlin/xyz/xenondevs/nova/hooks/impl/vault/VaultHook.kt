package xyz.xenondevs.nova.hooks.impl.vault

import net.milkbowl.vault.permission.Permission
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.World
import xyz.xenondevs.nova.hooks.Hook
import xyz.xenondevs.nova.hooks.permission.PermissionIntegration

@Hook(plugins = ["Vault"])
internal object VaultHook : PermissionIntegration {
    
    private val PERMISSIONS = Bukkit.getServicesManager().getRegistration(Permission::class.java)!!.provider
    
    override fun hasPermission(world: World, player: OfflinePlayer, permission: String): Boolean =
        PERMISSIONS.playerHas(world.name, player, permission)
    
}