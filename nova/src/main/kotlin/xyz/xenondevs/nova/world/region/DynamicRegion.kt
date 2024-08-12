package xyz.xenondevs.nova.world.region

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutable.MutableProvider
import xyz.xenondevs.commons.provider.mutable.map
import xyz.xenondevs.nova.ui.menu.item.AddNumberItem
import xyz.xenondevs.nova.ui.menu.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.menu.item.RemoveNumberItem
import xyz.xenondevs.nova.ui.menu.item.VisualizeRegionItem
import java.util.*

class DynamicRegion internal constructor(
    val uuid: UUID,
    minSize: Provider<Int>,
    maxSize: Provider<Int>,
    size: MutableProvider<Int>,
    private val createRegion: (Int) -> Region,
) {
    
    private val _displaySizeItem = lazy { DisplayNumberItem({ this.size }, "menu.nova.region.size") }
    private val _increaseSizeItem = lazy { AddNumberItem({ this.minSize..this.maxSize }, { this.size }, { this.size = it }, "menu.nova.region.increase") }
    private val _decreaseSizeItem = lazy { RemoveNumberItem({ this.minSize..this.maxSize }, { this.size }, { this.size = it }, "menu.nova.region.decrease") }
    
    val displaySizeItem by _displaySizeItem
    val increaseSizeItem by _increaseSizeItem
    val decreaseSizeItem by _decreaseSizeItem
    
    val minSize by minSize
    val maxSize by maxSize
    
    private val _size = size.map({ it.coerceIn(minSize.get(), maxSize.get()) }, { it })
    var size by _size
    
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
    
    init {
        size.subscribe {
            updateSizeDisplay()
            updateSizeControls()
            updateRegion()
        }
        
        minSize.addChild(_size)
        maxSize.addChild(_size)
        
        minSize.subscribe { updateSizeControls() }
        maxSize.subscribe { updateSizeControls() }
    }
    
    fun createVisualizeRegionItem(player: Player): VisualizeRegionItem {
        return VisualizeRegionItem(player, uuid, ::region)
    }
    
    fun showRegionOutline(player: Player) {
        VisualRegion.showRegion(player, uuid, region)
    }
    
    fun hideRegionOutline(player: Player) {
        VisualRegion.hideRegion(player, uuid)
    }
    
    private fun updateRegion() {
        region = createRegion(size)
    }
    
    private fun updateSizeDisplay() {
        if (_displaySizeItem.isInitialized())
            displaySizeItem.notifyWindows()
    }
    
    private fun updateSizeControls() {
        if (_increaseSizeItem.isInitialized())
            increaseSizeItem.notifyWindows()
        if (_decreaseSizeItem.isInitialized())
            decreaseSizeItem.notifyWindows()
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
    
}