@file:Suppress("OVERRIDE_DEPRECATION")

package xyz.xenondevs.nova.hook.impl.worldguard

import com.sk89q.worldedit.blocks.BaseItemStack
import com.sk89q.worldedit.entity.BaseEntity
import com.sk89q.worldedit.extent.inventory.BlockBag
import com.sk89q.worldedit.math.Vector3
import com.sk89q.worldedit.session.SessionKey
import com.sk89q.worldedit.util.HandSide
import com.sk89q.worldedit.util.Location
import com.sk89q.worldedit.world.World
import com.sk89q.worldedit.world.weather.WeatherType
import com.sk89q.worldguard.bukkit.BukkitPlayer
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform
import org.bukkit.OfflinePlayer
import xyz.xenondevs.nova.hook.permission.PermissionManager
import java.util.*
import org.bukkit.World as BukkitWorld

internal class BetterBukkitOfflinePlayer(
    private val bukkitWorld: BukkitWorld,
    private val world: World,
    private val platform: WorldGuardPlatform,
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
        return (player.isOp && platform.globalStateManager[world].opPermissions) || PermissionManager.hasPermission(bukkitWorld, player, perm)
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
    
    override fun getLocation(): Location {
        throw UnsupportedOperationException()
    }
    
    override fun setCompassTarget(location: Location) {
        throw UnsupportedOperationException()
    }
    
    override fun getSessionKey(): SessionKey {
        throw UnsupportedOperationException()
    }
    
    override fun <T> getFacet(cls: Class<out T?>): T? {
        throw UnsupportedOperationException()
    }
}