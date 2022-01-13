package xyz.xenondevs.nova.integration.protection

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.event.protection.ProtectionCheckEvent
import xyz.xenondevs.nova.api.event.protection.ProtectionCheckEvent.ProtectionType
import xyz.xenondevs.nova.api.event.protection.Source
import xyz.xenondevs.nova.api.event.protection.TileEntitySource
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.callEvent

object EventIntegration : ProtectionIntegration {
    
    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean {
        val event = ProtectionCheckEvent(Source(player), item, ProtectionType.BREAK, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canBreak(tileEntity: TileEntity, item: ItemStack?, location: Location): Boolean {
        val event = ProtectionCheckEvent(TileEntitySource(tileEntity), item, ProtectionType.BREAK, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location): Boolean {
        val event = ProtectionCheckEvent(Source(player), item, ProtectionType.PLACE, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canPlace(tileEntity: TileEntity, item: ItemStack, location: Location): Boolean {
        val event = ProtectionCheckEvent(TileEntitySource(tileEntity), item, ProtectionType.PLACE, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean {
        val event = ProtectionCheckEvent(Source(player), item, ProtectionType.USE_BLOCK, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canUseBlock(tileEntity: TileEntity, item: ItemStack?, location: Location): Boolean {
        val event = ProtectionCheckEvent(TileEntitySource(tileEntity), item, ProtectionType.USE_BLOCK, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canUseItem(tileEntity: TileEntity, item: ItemStack, location: Location): Boolean {
        val event = ProtectionCheckEvent(TileEntitySource(tileEntity), item, ProtectionType.USE_ITEM, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location): Boolean {
        val event = ProtectionCheckEvent(Source(player), item, ProtectionType.USE_ITEM, location)
        callEvent(event)
        return event.allowed
    }
    
    override val isInstalled = true
}