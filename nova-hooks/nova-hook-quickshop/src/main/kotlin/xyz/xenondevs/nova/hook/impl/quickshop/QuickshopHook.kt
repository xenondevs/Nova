package xyz.xenondevs.nova.hook.impl.quickshop

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import org.maxgamer.quickshop.QuickShop
import org.maxgamer.quickshop.api.shop.AbstractDisplayItem
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.integration.Hook

@Hook(plugins = ["QuickShop"])
internal object QuickshopHook : ProtectionIntegration {
    
    private val QUICK_SHOP = Bukkit.getPluginManager().getPlugin("QuickShop") as QuickShop
    
    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean =
        canModifyShopAt(location, player)
    
    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location): Boolean =
        canModifyShopAt(location, player)
    
    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean =
        canModifyShopAt(location, player)
    
    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean =
         entity !is Item || !AbstractDisplayItem.checkIsGuardItemStack(entity.itemStack)
    
    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean =
         entity !is Item || !AbstractDisplayItem.checkIsGuardItemStack(entity.itemStack)
    
    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location): Boolean = true
    
    private fun canModifyShopAt(location: Location, player: OfflinePlayer): Boolean {
        val shopOwner = QUICK_SHOP.shopManager?.getShopIncludeAttached(location)?.owner
        return shopOwner == null || shopOwner == player.uniqueId
    }
    
}