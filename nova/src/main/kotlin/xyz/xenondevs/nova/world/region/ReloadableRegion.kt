package xyz.xenondevs.nova.world.region

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.menu.item.VisualizeRegionItem
import java.util.*

abstract class ReloadableRegion(
    val uuid: UUID,
    private val createRegion: (Int) -> Region
) {
    
    abstract val size: Int
    
    private lateinit var _region: Region
    private var region: Region
        set(value) {
            _region = value
            VisualRegion.updateRegion(uuid, region)
        }
        get() {
            if (!::_region.isInitialized)
                _region = createRegion(size)
            return _region
        }
    
    //<editor-fold desc="delegated to region", defaultstate="collapsed">
    val world: World
        get() = region.world
    val min: Location
        get() = region.min
    val max: Location
        get() = region.max
    
    operator fun contains(loc: Location): Boolean = region.contains(loc)
    operator fun contains(block: Block): Boolean = region.contains(block)
    //</editor-fold>
    
    fun createVisualizeRegionItem(player: Player): VisualizeRegionItem {
        return VisualizeRegionItem(player, uuid, ::region)
    }
    
    fun showRegionOutline(player: Player) {
        VisualRegion.showRegion(player, uuid, region)
    }
    
    fun hideRegionOutline(player: Player) {
        VisualRegion.hideRegion(player, uuid)
    }
    
    fun updateRegion() {
        region = createRegion(size)
    }
    
}