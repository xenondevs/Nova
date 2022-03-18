package xyz.xenondevs.nova.integration.protection.plugin

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import org.maxgamer.quickshop.QuickShop
import org.maxgamer.quickshop.api.shop.AbstractDisplayItem
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.integration.InternalIntegration

object QuickShop : ProtectionIntegration, InternalIntegration {
    
    private val QUICK_SHOP = Bukkit.getPluginManager().getPlugin("QuickShop") as? QuickShop
    override val isInstalled = QUICK_SHOP != null
    
    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean =
        canModifyShopAt(location, player)
    
    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location): Boolean =
        canModifyShopAt(location, player)
    
    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean =
        canModifyShopAt(location, player)
    
    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean {
        return entity !is Item || !AbstractDisplayItem.checkIsGuardItemStack(entity.itemStack)
    }
    
    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean {
        return entity !is Item || !AbstractDisplayItem.checkIsGuardItemStack(entity.itemStack)
    }
    
    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location) = true
    
    private fun canModifyShopAt(location: Location, player: OfflinePlayer): Boolean {
        val shopOwner = QUICK_SHOP!!.shopManager?.getShopIncludeAttached(location)?.owner
        return shopOwner == null || shopOwner == player.uniqueId
    }
    
}