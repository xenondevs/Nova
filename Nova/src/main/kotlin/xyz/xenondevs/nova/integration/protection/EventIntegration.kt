package xyz.xenondevs.nova.integration.protection

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import xyz.xenondevs.nova.api.event.protection.ProtectionCheckEvent
import xyz.xenondevs.nova.api.event.protection.ProtectionCheckEvent.*
import xyz.xenondevs.nova.api.event.protection.Source
import xyz.xenondevs.nova.api.event.protection.TileEntitySource
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.callEvent

object EventIntegration: ProtectionIntegration {
    
    override fun canBreak(player: OfflinePlayer, location: Location): Boolean {
        val event = ProtectionCheckEvent(Source(player), ProtectionType.BREAK, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canBreak(tileEntity: TileEntity, location: Location): Boolean {
        val event = ProtectionCheckEvent(TileEntitySource(tileEntity), ProtectionType.BREAK, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canPlace(player: OfflinePlayer, location: Location): Boolean {
        val event = ProtectionCheckEvent(Source(player), ProtectionType.PLACE, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canPlace(tileEntity: TileEntity, location: Location): Boolean {
        val event = ProtectionCheckEvent(TileEntitySource(tileEntity), ProtectionType.PLACE, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canUse(player: OfflinePlayer, location: Location): Boolean {
        val event = ProtectionCheckEvent(Source(player), ProtectionType.USE, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canUse(tileEntity: TileEntity, location: Location): Boolean {
        val event = ProtectionCheckEvent(TileEntitySource(tileEntity), ProtectionType.USE, location)
        callEvent(event)
        return event.allowed
    }
    
    override val isInstalled = true
}