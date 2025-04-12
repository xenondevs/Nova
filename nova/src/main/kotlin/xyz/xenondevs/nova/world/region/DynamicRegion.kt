package xyz.xenondevs.nova.world.region

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.invui.item.Item
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
    
    val displaySizeItem: Item by _displaySizeItem
    val increaseSizeItem: Item by _increaseSizeItem
    val decreaseSizeItem: Item by _decreaseSizeItem
    val visualizeRegionItem: Item by lazy { VisualizeRegionItem(uuid, ::region) }
    
    val minSize by minSize
    val maxSize by maxSize
    var size by size
    
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
        minSize.subscribe {
            size.set(size.get().coerceIn(it, maxSize.get()))
            updateSizeControls()
        }
        maxSize.subscribe {
            size.set(size.get().coerceIn(minSize.get(), it))
            updateSizeControls()
        }
    }
    
    @Deprecated(message= "Player is redundant", ReplaceWith("visualizeRegionItem"))
    fun createVisualizeRegionItem(player: Player): VisualizeRegionItem {
        return VisualizeRegionItem(uuid, ::region)
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
    
    fun toBoundingBox(): BoundingBox = region.toBoundingBox()
    //</editor-fold>
    
}