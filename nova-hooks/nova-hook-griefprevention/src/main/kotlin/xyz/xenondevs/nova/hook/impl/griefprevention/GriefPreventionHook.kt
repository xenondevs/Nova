package xyz.xenondevs.nova.hook.impl.griefprevention

import me.ryanhamshire.GriefPrevention.GriefPrevention
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.integration.Hook
import xyz.xenondevs.nova.util.FakeOnlinePlayer

@Hook(plugins = ["GriefPrevention"])
internal object GriefPreventionHook : ProtectionIntegration {
    
    private val GRIEF_PREVENTION: GriefPrevention = GriefPrevention.instance
    
    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location) =
        GRIEF_PREVENTION.allowBreak(FakeOnlinePlayer(player, location), location.block, location) == null
    
    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location) =
        GRIEF_PREVENTION.allowBuild(FakeOnlinePlayer(player, location), location) == null
    
    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location) =
        canBreak(player, item, location)
    
    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location) =
        canBreak(player, item, location)
    
    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) =
        canBreak(player, item, entity.location)
    
    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) =
        canBreak(player, item, entity.location)
    
}