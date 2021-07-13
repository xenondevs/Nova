package xyz.xenondevs.nova.util.protection.plugin

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import xyz.xenondevs.nova.util.protection.ProtectionPlugin
import com.plotsquared.core.location.Location as PlotLocation

object PlotSquared : ProtectionPlugin() {
    
    private val HAS_PLOT_SQUARED = Bukkit.getPluginManager().getPlugin("PlotSquared") != null
    
    override fun canBreak(player: OfflinePlayer, location: Location) = isAllowed(player, location)
    
    override fun canPlace(player: OfflinePlayer, location: Location) = isAllowed(player, location)
    
    override fun canUse(player: OfflinePlayer, location: Location) = isAllowed(player, location)
    
    fun isAllowed(offlinePlayer: OfflinePlayer, location: Location): Boolean {
        if (!HAS_PLOT_SQUARED) return true
        val plotLocation = location.toPlotLocation()
        if (plotLocation.isPlotRoad) return false
        return if (plotLocation.isPlotArea) {
            plotLocation.plotArea.getPlot(plotLocation)?.isAdded(offlinePlayer.uniqueId) ?: false
        } else true
    }
    
    private fun Location.toPlotLocation() =
        PlotLocation(world!!.name, blockX, blockY, blockZ, yaw, pitch)
    
    
}