package xyz.xenondevs.nova.integration.protection.plugin

import com.griefdefender.api.GriefDefender
import com.griefdefender.api.claim.TrustType
import com.griefdefender.api.claim.TrustTypes
import com.griefdefender.api.permission.flag.Flag
import com.griefdefender.api.permission.flag.Flags
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import xyz.xenondevs.nova.integration.protection.ProtectionIntegration
import xyz.xenondevs.nova.util.getBlockName

object GriefDefender : ProtectionIntegration {
    
    override fun isInstalled() = Bukkit.getPluginManager().getPlugin("GriefDefender") != null
    
    private fun testFlag(player: OfflinePlayer, location: Location, flag: Flag, trustType: TrustType): Boolean {
        val user = GriefDefender.getCore().getUser(player.uniqueId)
        val claim = GriefDefender.getCore().getClaimAt(location)
        
        return GriefDefender
            .getPermissionManager()
            .getActiveFlagPermissionValue(claim, user, flag, null, location.getBlockName(), hashSetOf(), trustType, false)
            .asBoolean()
    }
    
    override fun canBreak(player: OfflinePlayer, location: Location) =
        testFlag(player, location, Flags.BLOCK_BREAK, TrustTypes.BUILDER)
    
    override fun canPlace(player: OfflinePlayer, location: Location) =
        testFlag(player, location, Flags.BLOCK_PLACE, TrustTypes.BUILDER)
    
    override fun canUse(player: OfflinePlayer, location: Location) =
        testFlag(player, location, Flags.INTERACT_BLOCK_SECONDARY, TrustTypes.CONTAINER)
    
}