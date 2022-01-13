package xyz.xenondevs.nova.integration.protection

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.integration.Integration
import xyz.xenondevs.nova.tileentity.TileEntity

interface ProtectionIntegration : Integration {
    
    /**
     * Checks if that [player] can break a block at that [location] using that [item]
     */
    fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean
    
    /**
     * Checks if that [tileEntity] can break a block at that [location] using that [item]
     */
    fun canBreak(tileEntity: TileEntity, item: ItemStack?, location: Location) = canBreak(tileEntity.owner, item, location)
    
    /**
     * Checks if the [player] can place that [item] at that [location]
     */
    fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location): Boolean
    
    /**
     * Checks if the [tileEntity] can place that [item] at that [location]
     */
    fun canPlace(tileEntity: TileEntity, item: ItemStack, location: Location) = canPlace(tileEntity.owner, item, location)
    
    /**
     * Checks if the [player] can interact with a block at that [location] using that [item]
     */
    fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean
    
    /**
     * Checks if the [tileEntity] can interact with a block at that [location] using that [item]
     */
    fun canUseBlock(tileEntity: TileEntity, item: ItemStack?, location: Location) = canUseBlock(tileEntity.owner, item, location)
    
    /**
     * Checks if the [player] can use that [item] at that [location]
     */
    fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location): Boolean
    
    /**
     * Checks if the [tileEntity] can use that [item] at that [location]
     */
    fun canUseItem(tileEntity: TileEntity, item: ItemStack, location: Location) = canUseItem(tileEntity.owner, item, location)
    
}