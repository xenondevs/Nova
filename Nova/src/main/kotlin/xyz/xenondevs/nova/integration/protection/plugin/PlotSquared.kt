package xyz.xenondevs.nova.integration.protection.plugin

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.integration.InternalIntegration
import com.plotsquared.core.location.Location as PlotLocation

object PlotSquared : ProtectionIntegration, InternalIntegration {
    
    override val isInstalled = Bukkit.getPluginManager().getPlugin("PlotSquared") != null
    
    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location) = isAllowed(player, location)
    
    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location) = isAllowed(player, location)
    
    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location) = isAllowed(player, location)
    
    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location) = isAllowed(player, location)
    
    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) = isAllowed(player, entity.location)
    
    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?) = isAllowed(player, entity.location)
    
    private fun isAllowed(offlinePlayer: OfflinePlayer, location: Location): Boolean {
        if (!isInstalled) return true
        val plotLocation = location.toPlotLocation()
        if (plotLocation.isPlotRoad) return false
        return if (plotLocation.isPlotArea) {
            plotLocation.plotArea?.getPlot(plotLocation)?.isAdded(offlinePlayer.uniqueId) ?: false
        } else true
    }
    
    private fun Location.toPlotLocation(): PlotLocation =
        PlotLocation.at(world!!.name, blockX, blockY, blockZ, yaw, pitch)
    
}