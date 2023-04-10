package xyz.xenondevs.nova.world.region

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import java.util.*

open class DynamicRegion internal constructor(
    uuid: UUID,
    minSize: Provider<Int>,
    maxSize: Provider<Int>,
    size: Int,
    createRegion: (Int) -> Region,
) : ReloadableRegion(uuid, createRegion) {
    
    private val _displaySizeItem = lazy { DisplayNumberItem({ this.size }, "menu.nova.region.size") }
    private val _increaseSizeItem = lazy { AddNumberItem({ this.minSize..this.maxSize }, { this.size }, { this.size = it }, "menu.nova.region.increase") }
    private val _decreaseSizeItem = lazy { RemoveNumberItem({ this.minSize..this.maxSize }, { this.size }, { this.size = it }, "menu.nova.region.decrease") }
    
    val displaySizeItem by _displaySizeItem
    val increaseSizeItem by _increaseSizeItem
    val decreaseSizeItem by _decreaseSizeItem
    
    val minSize by minSize
    open val maxSize by maxSize
    
    override var size = size
        set(value) {
            if (field != value) {
                if (value !in minSize..maxSize)
                    throw IllegalArgumentException("Illegal region size $value for minSize $minSize, maxSize $maxSize")
                
                field = value
                updateRegion()
                
                if (_displaySizeItem.isInitialized())
                    displaySizeItem.notifyWindows()
                if (_increaseSizeItem.isInitialized())
                    increaseSizeItem.notifyWindows()
                if (_decreaseSizeItem.isInitialized())
                    decreaseSizeItem.notifyWindows()
            }
        }
    
    override fun reload() {
        if (size !in minSize..maxSize)
            size = maxSize
    }
    
}