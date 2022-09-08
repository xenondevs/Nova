package xyz.xenondevs.nova.integration.protection.plugin

import com.iridium.iridiumskyblock.PermissionType
import com.iridium.iridiumskyblock.database.Island
import com.iridium.iridiumskyblock.database.User
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.integration.InternalIntegration
import java.util.*
import com.iridium.iridiumskyblock.IridiumSkyblock as ISkyBlock


internal object IridiumSkyblock : ProtectionIntegration, InternalIntegration {

    override val isInstalled = Bukkit.getPluginManager().getPlugin("IridiumSkyblock") != null

    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean {
        return checkIridiumSkyblockPlayer(player, location, PermissionType.BLOCK_BREAK)
    }

    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location): Boolean {
        return checkIridiumSkyblockPlayer(player, location, PermissionType.BLOCK_PLACE)
    }

    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean {
        return checkIridiumSkyblockPlayer(player, location, PermissionType.OPEN_CONTAINERS)
    }

    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location): Boolean {
        return checkIridiumSkyblockPlayer(player, location, PermissionType.INTERACT)
    }

    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean {
        return checkIridiumSkyblockPlayer(player, entity.location, PermissionType.INTERACT_ENTITIES)
    }

    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean {
        return checkIridiumSkyblockPlayer(player, entity.location, PermissionType.INTERACT_ENTITIES)
    }

    private fun checkIridiumSkyblockPlayer(player: OfflinePlayer, location: Location, permissionType: PermissionType): Boolean {
        val instance = ISkyBlock.getInstance()
        val user: User = instance.userManager.getUser(player)
        val island: Optional<Island> =
            instance.islandManager.getIslandViaLocation(location)
        if (!island.isPresent) return true
        return instance.islandManager
            .getIslandPermission(island.get(), user, permissionType)
    }

}