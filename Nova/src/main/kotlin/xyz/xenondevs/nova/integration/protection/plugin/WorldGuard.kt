package xyz.xenondevs.nova.integration.protection.plugin

import com.sk89q.worldedit.blocks.BaseItemStack
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.entity.BaseEntity
import com.sk89q.worldedit.extent.inventory.BlockBag
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.Vector3
import com.sk89q.worldedit.session.SessionKey
import com.sk89q.worldedit.util.HandSide
import com.sk89q.worldedit.world.World
import com.sk89q.worldedit.world.weather.WeatherType
import com.sk89q.worldguard.LocalPlayer
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.BukkitPlayer
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform
import com.sk89q.worldguard.protection.flags.Flags
import com.sk89q.worldguard.protection.flags.StateFlag
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.api.protection.ProtectionIntegration.ExecutionMode
import xyz.xenondevs.nova.integration.InternalIntegration
import xyz.xenondevs.nova.util.PermissionUtils
import java.util.*
import org.bukkit.World as BWorld

object WorldGuard : ProtectionIntegration, InternalIntegration {
    
    private val PLUGIN: WorldGuardPlugin?
    private val PLATFORM: WorldGuardPlatform?
    
    init {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            PLUGIN = WorldGuardPlugin.inst()
            PLATFORM = WorldGuard.getInstance().platform
        } else {
            PLUGIN = null
            PLATFORM = null
        }
    }
    
    override val isInstalled = PLATFORM != null && PLUGIN != null
    
    override val executionMode = ExecutionMode.NONE
    
    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location) = runQuery(player, location, Flags.BLOCK_BREAK)
    
    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location) = runQuery(player, location, Flags.BLOCK_PLACE)
    
    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location) = runQuery(player, location, Flags.USE)
    
    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location) = runQuery(player, location, Flags.USE)
    
    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) = runQuery(player, entity.location, Flags.INTERACT)
    
    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) = runQuery(player, entity.location, Flags.DAMAGE_ANIMALS)
    
    private fun runQuery(offlinePlayer: OfflinePlayer, location: Location, vararg flags: StateFlag): Boolean {
        if (PLATFORM == null || PLUGIN == null) return true
        
        val world = BukkitAdapter.adapt(location.world)
        val localPlayer = BetterBukkitOfflinePlayer(location.world!!, world, PLUGIN, offlinePlayer)
        
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
        if (PLATFORM == null || PLUGIN == null) return false
        val regionManager = PLATFORM.regionContainer.get(world) ?: return true
        return regionManager.getApplicableRegions(vector).size() > 0
    }
    
    private fun hasBypass(world: World, player: LocalPlayer): Boolean {
        if (PLATFORM == null || PLUGIN == null) return false
        
        val sessionManager = PLATFORM.sessionManager
        val session = sessionManager.getIfPresent(player)
        if (session?.hasBypassDisabled() == true) return false
        
        return player.hasPermission("worldguard.region.bypass.${world.name}")
    }
    
    private class BetterBukkitOfflinePlayer(
        private val bukkitWorld: BWorld,
        private val world: World,
        plugin: WorldGuardPlugin,
        private val player: OfflinePlayer
    ) : BukkitPlayer(plugin, player.player) {
        
        override fun getName(): String {
            return player.name!!
        }
        
        override fun getUniqueId(): UUID {
            return player.uniqueId
        }
        
        override fun hasGroup(group: String): Boolean {
            return plugin.inGroup(player, group)
        }
        
        override fun getGroups(): Array<String> {
            return plugin.getGroups(player)
        }
        
        override fun hasPermission(perm: String): Boolean {
            if (PLATFORM == null || PLUGIN == null) return false
            if (player.isOp) return PLATFORM.globalStateManager[world].opPermissions
            return PermissionUtils.hasPermission(bukkitWorld, player, perm)
        }
        
        override fun getWorld(): World {
            return world
        }
        
        override fun kick(msg: String) {
            throw UnsupportedOperationException()
        }
        
        override fun ban(msg: String) {
            throw UnsupportedOperationException()
        }
        
        override fun getHealth(): Double {
            throw UnsupportedOperationException()
        }
        
        override fun setHealth(health: Double) {
            throw UnsupportedOperationException()
        }
        
        override fun getMaxHealth(): Double {
            throw UnsupportedOperationException()
        }
        
        override fun getFoodLevel(): Double {
            throw UnsupportedOperationException()
        }
        
        override fun setFoodLevel(foodLevel: Double) {
            throw UnsupportedOperationException()
        }
        
        override fun getSaturation(): Double {
            throw UnsupportedOperationException()
        }
        
        override fun setSaturation(saturation: Double) {
            throw UnsupportedOperationException()
        }
        
        override fun getExhaustion(): Float {
            throw UnsupportedOperationException()
        }
        
        override fun setExhaustion(exhaustion: Float) {
            throw UnsupportedOperationException()
        }
        
        override fun getPlayerWeather(): WeatherType {
            throw UnsupportedOperationException()
        }
        
        override fun setPlayerWeather(weather: WeatherType) {
            throw UnsupportedOperationException()
        }
        
        override fun resetPlayerWeather() {
            throw UnsupportedOperationException()
        }
        
        override fun isPlayerTimeRelative(): Boolean {
            throw UnsupportedOperationException()
        }
        
        override fun getPlayerTimeOffset(): Long {
            throw UnsupportedOperationException()
        }
        
        override fun setPlayerTime(time: Long, relative: Boolean) {
            throw UnsupportedOperationException()
        }
        
        override fun resetPlayerTime() {
            throw UnsupportedOperationException()
        }
        
        override fun printRaw(msg: String) {
            throw UnsupportedOperationException()
        }
        
        override fun printDebug(msg: String) {
            throw UnsupportedOperationException()
        }
        
        override fun print(msg: String) {
            throw UnsupportedOperationException()
        }
        
        override fun printError(msg: String) {
            throw UnsupportedOperationException()
        }
        
        override fun getItemInHand(handSide: HandSide): BaseItemStack {
            throw UnsupportedOperationException()
        }
        
        override fun giveItem(itemStack: BaseItemStack) {
            throw UnsupportedOperationException()
        }
        
        override fun getInventoryBlockBag(): BlockBag {
            throw UnsupportedOperationException()
        }
        
        override fun setPosition(pos: Vector3, pitch: Float, yaw: Float) {
            throw UnsupportedOperationException()
        }
        
        override fun getState(): BaseEntity? {
            throw UnsupportedOperationException()
        }
        
        override fun getLocation(): com.sk89q.worldedit.util.Location {
            throw UnsupportedOperationException()
        }
        
        override fun setCompassTarget(location: com.sk89q.worldedit.util.Location) {
            throw UnsupportedOperationException()
        }
        
        override fun getSessionKey(): SessionKey {
            throw UnsupportedOperationException()
        }
        
        override fun <T> getFacet(cls: Class<out T?>): T? {
            throw UnsupportedOperationException()
        }
    }
    
}