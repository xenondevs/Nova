package xyz.xenondevs.nova.hook.impl.plotsquared

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.integration.Hook
import com.plotsquared.core.location.Location as PlotLocation

@Hook(plugins = ["PlotSquared"])
internal object PlotSquaredHook : ProtectionIntegration {
    
    override fun canBreak(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean =
        isAllowed(player, location)
    
    override fun canPlace(player: OfflinePlayer, item: ItemStack, location: Location): Boolean =
        isAllowed(player, location)
    
    override fun canUseBlock(player: OfflinePlayer, item: ItemStack?, location: Location): Boolean =
        isAllowed(player, location)
    
    override fun canUseItem(player: OfflinePlayer, item: ItemStack, location: Location): Boolean =
        isAllowed(player, location)
    
    override fun canInteractWithEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean =
        isAllowed(player, entity.location)
    
    override fun canHurtEntity(player: OfflinePlayer, entity: Entity, item: ItemStack?): Boolean =
        isAllowed(player, entity.location)
    
    private fun isAllowed(offlinePlayer: OfflinePlayer, location: Location): Boolean {
        val plotLocation = location.toPlotLocation()
        
        if (plotLocation.isPlotRoad)
            return false
        
        if (plotLocation.isPlotArea)
            return plotLocation.plotArea?.getPlot(plotLocation)?.isAdded(offlinePlayer.uniqueId) ?: false
        
        return true
    }
    
    private fun Location.toPlotLocation(): PlotLocation =
        PlotLocation.at(world!!.name, blockX, blockY, blockZ, yaw, pitch)
    
}