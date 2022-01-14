package xyz.xenondevs.nova.integration.protection

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.event.protection.ProtectionCheckEvent
import xyz.xenondevs.nova.api.event.protection.ProtectionCheckEvent.ProtectionType
import xyz.xenondevs.nova.api.event.protection.Source
import xyz.xenondevs.nova.api.event.protection.TileEntitySource
import xyz.xenondevs.nova.api.tileentity.TileEntity
import xyz.xenondevs.nova.util.callEvent

object EventIntegration : InternalProtectionIntegration {
    
    override val isInstalled = true
    
    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean {
        val event = ProtectionCheckEvent(Source(player), ProtectionType.BREAK, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canBreak(tileEntity: TileEntity, item: ItemStack?, location: Location): Boolean {
        val event = ProtectionCheckEvent(TileEntitySource(tileEntity), ProtectionType.BREAK, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location): Boolean {
        val event = ProtectionCheckEvent(Source(player), ProtectionType.PLACE, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canPlace(tileEntity: TileEntity, item: ItemStack, location: Location): Boolean {
        val event = ProtectionCheckEvent(TileEntitySource(tileEntity), ProtectionType.PLACE, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean {
        val event = ProtectionCheckEvent(Source(player), ProtectionType.USE_BLOCK, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canUseBlock(tileEntity: TileEntity, item: ItemStack?, location: Location): Boolean {
        val event = ProtectionCheckEvent(TileEntitySource(tileEntity), ProtectionType.USE_BLOCK, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canUseItem(tileEntity: TileEntity, item: ItemStack, location: Location): Boolean {
        val event = ProtectionCheckEvent(TileEntitySource(tileEntity), ProtectionType.USE_ITEM, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location): Boolean {
        val event = ProtectionCheckEvent(Source(player), ProtectionType.USE_ITEM, location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canInteractWithEntity(tileEntity: TileEntity, entity: Entity, item: ItemStack?): Boolean {
        val event = ProtectionCheckEvent(TileEntitySource(tileEntity), ProtectionType.INTERACT_ENTITY, entity.location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean {
        val event = ProtectionCheckEvent(Source(player), ProtectionType.INTERACT_ENTITY, entity.location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canHurtEntity(tileEntity: TileEntity, entity: Entity, item: ItemStack?): Boolean {
        val event = ProtectionCheckEvent(TileEntitySource(tileEntity), ProtectionType.HURT_ENTITY, entity.location)
        callEvent(event)
        return event.allowed
    }
    
    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean {
        val event = ProtectionCheckEvent(Source(player), ProtectionType.HURT_ENTITY, entity.location)
        callEvent(event)
        return event.allowed
    }
    
}