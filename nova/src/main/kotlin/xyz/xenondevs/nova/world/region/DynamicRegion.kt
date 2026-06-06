package xyz.xenondevs.nova.world.region

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.nova.ui.menu.item.VisualizeRegionItem
import xyz.xenondevs.nova.ui.menu.item.addNumberItem
import xyz.xenondevs.nova.ui.menu.item.displayNumberItem
import xyz.xenondevs.nova.ui.menu.item.removeNumberItem
import java.util.*

/**
 * A [Region] that can be dynamically resized between [minSize] and [maxSize],
 * storing the current size in [size] and creating a new [Region] via [createRegion]
 * every time the size changes.
 * 
 * Uses [uuid] to register in [VisualRegion].
 * 
 * Also provides some convenience UI-items like [displaySizeItem], [increaseSizeItem], [decreaseSizeItem] and [visualizeRegionItem]
 * for resizing and visualizing the region.
 */
class DynamicRegion internal constructor(
    val uuid: UUID,
    minSize: Provider<Int>,
    maxSize: Provider<Int>,
    size: MutableProvider<Int>,
    private val createRegion: (Int) -> Region,
) {
    
    private val _minSize = minSize
    private val _maxSize = maxSize
    private val _size = size
    
    /**
     * The current minimum allowed size of the region.
     */
    val minSize by minSize
    
    /**
     * The current maximum allowed size of the region.
     */
    val maxSize by maxSize
    
    /**
     * The current size of the region.
     */
    var size by size
    
    /**
     * A [UI Item][Item] that displays the current [size] as a number.
     * @see displayNumberItem
     */
    val displaySizeItem: Item
        get() = displayNumberItem(_size, "menu.nova.region.size")
    
    /**
     * A [UI Item][Item] in the shape of a plus button that increases the [size] when clicked.
     * @see addNumberItem
     */
    val increaseSizeItem: Item
        get() = addNumberItem(
            combinedProvider(_minSize, _maxSize) { a, b -> a..b },
            _size,
            "menu.nova.region.increase"
        )
    
    /**
     * A [UI Item][Item] in the shape of a minus button that decreases the [size] when clicked.
     * @see removeNumberItem
     */
    val decreaseSizeItem: Item
        get() = removeNumberItem(
            combinedProvider(_minSize, _maxSize) { a, b -> a..b },
            _size,
            "menu.nova.region.decrease"
        )
    
    /**
     * A [UI Item][Item] that shows the region outline when clicked.
     * @see VisualRegion
     */
    val visualizeRegionItem: Item
        get() = VisualizeRegionItem(uuid, ::region)
    
    private lateinit var _region: Region
    private var region: Region
        set(value) {
            _region = value
            VisualRegion.updateRegion(uuid, value)
        }
        get() {
            if (!::_region.isInitialized)
                _region = createRegion(size)
            return _region
        }
    
    init {
        _size.subscribe { updateRegion() }
        _minSize.subscribe { this.size = this.size.coerceIn(it, this.maxSize) }
        _maxSize.subscribe { this.size = this.size.coerceIn(this.minSize, it) }
    }
    
    /**
     * Shows the region outline for [player].
     * @see VisualRegion
     */
    fun showRegionOutline(player: Player) {
        VisualRegion.showRegion(player, uuid, region)
    }
    
    /**
     * Hides the region outline for [player].
     * @see VisualRegion
     */
    fun hideRegionOutline(player: Player) {
        VisualRegion.hideRegion(player, uuid)
    }
    
    private fun updateRegion() {
        region = createRegion(size)
    }
    
    //<editor-fold desc="delegated to region", defaultstate="collapsed">
    /**
     * The [World] that this region is in.
     * @see Region.world
     */
    val world: World
        get() = region.world
    
    /**
     * The start of the region, inclusive.
     * @see Region.min
     */
    val min: Location
        get() = region.min
    
    /**
     * The end of the region, inclusive.
     * @see Region.max
     */
    val max: Location
        get() = region.max
    
    /**
     * Checks whether [loc] is inside the region.
     * @see Region.contains
     */
    operator fun contains(loc: Location): Boolean = region.contains(loc)
    
    /**
     * Checks whether [block] is inside the region.
     * @see Region.contains
     */
    operator fun contains(block: Block): Boolean = region.contains(block)
    
    /**
     * Converts this region to a [BoundingBox].
     * @see Region.toBoundingBox
     */
    fun toBoundingBox(): BoundingBox = region.toBoundingBox()
    //</editor-fold>
    
}