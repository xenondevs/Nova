package xyz.xenondevs.nova.world.region

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutable.MutableProvider
import xyz.xenondevs.commons.provider.mutable.map
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import java.util.*

class DynamicRegion internal constructor(
    uuid: UUID,
    minSize: Provider<Int>,
    maxSize: Provider<Int>,
    size: MutableProvider<Int>,
    createRegion: (Int) -> Region,
) : ReloadableRegion(uuid, createRegion) {
    
    private val _displaySizeItem = lazy { DisplayNumberItem({ this.size }, "menu.nova.region.size") }
    private val _increaseSizeItem = lazy { AddNumberItem({ this.minSize..this.maxSize }, { this.size }, { this.size = it }, "menu.nova.region.increase") }
    private val _decreaseSizeItem = lazy { RemoveNumberItem({ this.minSize..this.maxSize }, { this.size }, { this.size = it }, "menu.nova.region.decrease") }
    
    val displaySizeItem by _displaySizeItem
    val increaseSizeItem by _increaseSizeItem
    val decreaseSizeItem by _decreaseSizeItem
    
    val minSize by minSize
    val maxSize by maxSize
    
    private val _size = size.map({ it.coerceIn(minSize.get(), maxSize.get()) }, { it })
    override var size by _size
    
    init {
        size.addUpdateHandler {
            updateSizeDisplay()
            updateSizeControls()
            updateRegion()
        }
        
        minSize.addChild(_size)
        maxSize.addChild(_size)
        
        minSize.addUpdateHandler { updateSizeControls()}
        maxSize.addUpdateHandler { updateSizeControls() }
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
    
}