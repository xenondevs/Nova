package xyz.xenondevs.nova.api.protection

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.tileentity.TileEntity

interface ProtectionIntegration {
    
    /**
     * Checks if that [player] can break a block at that [location] using that [item]
     */
    fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean
    
    /**
     * Checks if that [tileEntity] can break a block at that [location] using that [item]
     */
    fun canBreak(tileEntity: TileEntity, item: ItemStack?, location: Location): Boolean =
        canBreak(tileEntity.owner, item, location)
    
    /**
     * Checks if the [player] can place that [item] at that [location]
     */
    fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location): Boolean
    
    /**
     * Checks if the [tileEntity] can place that [item] at that [location]
     */
    fun canPlace(tileEntity: TileEntity, item: ItemStack, location: Location): Boolean =
        canPlace(tileEntity.owner, item, location)
    
    /**
     * Checks if the [player] can interact with a block at that [location] using that [item]
     */
    fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean
    
    /**
     * Checks if the [tileEntity] can interact with a block at that [location] using that [item]
     */
    fun canUseBlock(tileEntity: TileEntity, item: ItemStack?, location: Location): Boolean =
        canUseBlock(tileEntity.owner, item, location)
    
    /**
     * Checks if the [player] can use that [item] at that [location]
     */
    fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location): Boolean
    
    /**
     * Checks if the [tileEntity] can use that [item] at that [location]
     */
    fun canUseItem(tileEntity: TileEntity, item: ItemStack, location: Location): Boolean =
        canUseItem(tileEntity.owner, item, location)
    
    /**
     * Checks if the [player] can interact with the [entity] while holding [item]
     */
    fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean
    
    /**
     * Checks if the [tileEntity] can interact with the [entity] wile holding [item]
     */
    fun canInteractWithEntity(tileEntity: TileEntity, entity: Entity, item: ItemStack?): Boolean =
        canInteractWithEntity(tileEntity.owner, entity, item)
    
    /**
     * Checks if the [player] can hurt the [entity] with this [item]
     */
    fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean
    
    /**
     * Checks if the [tileEntity] can hurt the [entity] with this [item]
     */
    fun canHurtEntity(tileEntity: TileEntity, entity: Entity, item: ItemStack?): Boolean =
        canHurtEntity(tileEntity.owner, entity, item)
    
}