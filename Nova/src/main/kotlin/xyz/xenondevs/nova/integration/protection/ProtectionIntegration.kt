package xyz.xenondevs.nova.integration.protection

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import xyz.xenondevs.nova.integration.Integration
import xyz.xenondevs.nova.tileentity.TileEntity

interface ProtectionIntegration : Integration {
    
    fun canBreak(player: OfflinePlayer, location: Location): Boolean
    
    fun canBreak(tileEntity: TileEntity, location: Location) = canBreak(tileEntity.owner, location)
    
    fun canPlace(player: OfflinePlayer, location: Location): Boolean
    
    fun canPlace(tileEntity: TileEntity, location: Location) = canPlace(tileEntity.owner, location)
    
    fun canUse(player: OfflinePlayer, location: Location): Boolean
    
    fun canUse(tileEntity: TileEntity, location: Location) = canUse(tileEntity.owner, location)
    
}