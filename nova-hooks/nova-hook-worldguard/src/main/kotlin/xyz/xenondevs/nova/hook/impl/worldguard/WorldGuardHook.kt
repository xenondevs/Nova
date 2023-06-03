package xyz.xenondevs.nova.hook.impl.worldguard

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.world.World
import com.sk89q.worldguard.LocalPlayer
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import com.sk89q.worldguard.protection.flags.Flags
import com.sk89q.worldguard.protection.flags.StateFlag
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.api.protection.ProtectionIntegration.ExecutionMode
import xyz.xenondevs.nova.integration.Hook

@Hook(plugins = ["WorldGuard"])
internal object WorldGuardHook : ProtectionIntegration {
    
    private val PLUGIN = WorldGuardPlugin.inst()
    private val PLATFORM = WorldGuard.getInstance().platform
    
    override fun getExecutionMode(): ExecutionMode = ExecutionMode.NONE
    
    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location) =
        runQuery(player, location, Flags.BLOCK_BREAK)
    
    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location) =
        runQuery(player, location, Flags.BLOCK_PLACE)
    
    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location) =
        runQuery(player, location, Flags.USE)
    
    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location) =
        runQuery(player, location, Flags.USE)
    
    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) =
        runQuery(player, entity.location, Flags.INTERACT)
    
    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) =
        runQuery(player, entity.location, Flags.DAMAGE_ANIMALS)
    
    private fun runQuery(offlinePlayer: OfflinePlayer, location: Location, vararg flags: StateFlag): Boolean {
        val world = BukkitAdapter.adapt(location.world)
        val localPlayer = BetterBukkitOfflinePlayer(location.world!!, world, PLATFORM, PLUGIN, offlinePlayer)
        
        return if (!hasBypass(world, localPlayer)) {
            val vector = BukkitAdapter.asBlockVector(location)
            return if (hasRegion(world, vector)) {
                val wrappedLocation = BukkitAdapter.adapt(location)
                val query = PLATFORM.regionContainer.createQuery()
                query.testBuild(wrappedLocation, localPlayer, *flags)
            } else true
        } else true
    }
    
    private fun hasRegion(world: World, vector: BlockVector3): Boolean {
        val regionManager = PLATFORM.regionContainer.get(world) ?: return true
        return regionManager.getApplicableRegions(vector).size() > 0
    }
    
    private fun hasBypass(world: World, player: LocalPlayer): Boolean {
        val sessionManager = PLATFORM.sessionManager
        val session = sessionManager.getIfPresent(player)
        if (session?.hasBypassDisabled() == true) return false
        
        return player.hasPermission("worldguard.region.bypass.${world.name}")
    }
    
}