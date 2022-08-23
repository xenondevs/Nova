package xyz.xenondevs.nova.world.region

import xyz.xenondevs.nova.data.config.ValueReloadable
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import java.util.*

open class DynamicRegion internal constructor(
    uuid: UUID,
    minSize: ValueReloadable<Int>,
    maxSize: ValueReloadable<Int>,
    size: Int,
    private val createRegion: (Int) -> Region,
) : ReloadableRegion(uuid) {
    
    private val _displaySizeItem = lazy { DisplayNumberItem({ this.size }, "menu.nova.region.size") }
    private val _increaseSizeItem = lazy { AddNumberItem({ this.minSize..this.maxSize }, { this.size }, { this.size = it }, "menu.nova.region.increase") }
    private val _decreaseSizeItem = lazy { RemoveNumberItem({ this.minSize..this.maxSize }, { this.size }, { this.size = it }, "menu.nova.region.decrease") }
    
    val displaySizeItem by _displaySizeItem
    val increaseSizeItem by _increaseSizeItem
    val decreaseSizeItem by _decreaseSizeItem
    
    val minSize by minSize
    open val maxSize by maxSize
    
    final override var region: Region = createRegion(size)
        private set
    
    var size = size
        set(value) {
            if (field != value) {
                if (value !in minSize..maxSize)
                    throw IllegalArgumentException("Illegal region size $value for minSize $minSize, maxSize $maxSize")
                
                field = value
                region = createRegion(value)
                
                if (_displaySizeItem.isInitialized())
                    displaySizeItem.notifyWindows()
                if (_increaseSizeItem.isInitialized())
                    increaseSizeItem.notifyWindows()
                if (_decreaseSizeItem.isInitialized())
                    decreaseSizeItem.notifyWindows()
                
                VisualRegion.updateRegion(uuid, region)
            }
        }
    
    override fun reload() {
        if (size !in minSize..maxSize)
            size = maxSize
    }
    
}