package xyz.xenondevs.nova.world.region

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import java.util.*

abstract class ReloadableRegion(
    val uuid: UUID,
    private val createRegion: (Int) -> Region
) : Iterable<Block> {
    
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
    val blocks
        get() = region.blocks
    val world
        get() = region.world
    
    operator fun contains(loc: Location): Boolean = region.contains(loc)
    operator fun contains(block: Block): Boolean = region.contains(block)
    operator fun get(index: Int): Block = blocks[index]
    override fun iterator(): Iterator<Block> = blocks.iterator()
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