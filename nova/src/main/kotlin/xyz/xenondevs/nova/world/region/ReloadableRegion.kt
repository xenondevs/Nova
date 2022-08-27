package xyz.xenondevs.nova.world.region

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.item.VisualizeRegionItem
import java.util.*

abstract class ReloadableRegion(val uuid: UUID): Iterable<Block> {
    
    abstract val region: Region
    
    val visualizeRegionItem by lazy { VisualizeRegionItem(uuid) { region } }
    
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
    
    abstract fun reload()
    
    fun showRegionOutline(player: Player) {
        VisualRegion.showRegion(player, uuid, region)
    }
    
    fun hideRegionOutline(player: Player) {
        VisualRegion.removeRegionViewer(player, uuid)
    }
    
}