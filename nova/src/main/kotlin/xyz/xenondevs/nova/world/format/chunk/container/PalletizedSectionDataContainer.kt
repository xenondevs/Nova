package xyz.xenondevs.nova.world.format.chunk.container

import xyz.xenondevs.nova.world.format.IdResolver
import xyz.xenondevs.nova.world.format.chunk.data.CompactIntStorage
import xyz.xenondevs.nova.world.format.chunk.palette.HashPalette
import xyz.xenondevs.nova.world.format.chunk.palette.LinearPalette
import xyz.xenondevs.nova.world.format.chunk.palette.Palette

internal sealed class PalletizedSectionDataContainer<T>(idResolver: IdResolver<T>) : SectionDataContainer<T>(idResolver) {
    
    protected abstract var palette: Palette<T>
    protected abstract val data: CompactIntStorage
    protected var bitsPerEntry = 0
    
    /**
     * Gets or creates the palletized id for the given [value], while also performing upkeep on the palette.
     * 
     * Upkeep includes:
     * - Remaking the [palette] if the next id would be greater than 0xFFFF, i.e. larger than 2 bytes.
     * - Converting a [LinearPalette] to a [HashPalette] if the next id would exceed [LinearPalette.MAX_SIZE].
     * - Resizing the data if [bitsPerEntry] changed.
     */
    fun toPalletizedId(value: T?): Int {
        if (value == null)
            return 0
        
        val existingId = palette.getId(value)
        if (existingId != 0)
            return existingId
        
        val oldBitsPerEntry = bitsPerEntry
        
        if (palette.size >= 0xFFFF)
            remakePalette()
        
        if (palette is LinearPalette && palette.size >= LinearPalette.MAX_SIZE)
            this.palette = (palette as LinearPalette<T>).toHashPalette()
        
        val newId = palette.putValue(value)
        
        val newBitsPerEntry = determineBitsPerEntry()
        if (newBitsPerEntry != oldBitsPerEntry) {
            resizeData(oldBitsPerEntry, newBitsPerEntry)
            this.bitsPerEntry = newBitsPerEntry
        }
        
        return newId
    }
    
    /**
     * Remakes the [palette], thereby eliminating any unused entries, then resizes the [data] if possible.
     */
    protected fun remakePaletteResizeData() {
        val oldBitsPerEntry = bitsPerEntry
        remakePalette()
        val newBitsPerEntry = determineBitsPerEntry()
        if (oldBitsPerEntry != newBitsPerEntry) {
            this.bitsPerEntry = newBitsPerEntry
            resizeData(oldBitsPerEntry, newBitsPerEntry)
        }
    }
    
    /**
     * Remakes the [palette] and migrates palletized ids in [data].
     * This process eliminates unused entries.
     * Does not update the [bitsPerEntry] or resize the data.
     */
    private fun remakePalette() {
        val currentPalette = palette
        var newPalette: Palette<T> = LinearPalette(idResolver)
        
        val data = data
        for (packedPos in 0..<SECTION_SIZE) {
            val oldPalletizedId = data[packedPos]
            if (oldPalletizedId == 0)
                continue
            
            val value = currentPalette.getValue(oldPalletizedId)!!
            var newPalletizedId = newPalette.getId(value)
            
            if (newPalletizedId == 0) {
                if (newPalette is LinearPalette && newPalette.size >= LinearPalette.MAX_SIZE)
                    newPalette = newPalette.toHashPalette()
                
                newPalletizedId = newPalette.putValue(value)
            }
            
            data[packedPos] = newPalletizedId
        }
        
        this.palette = newPalette
    }
    
    /**
     * Resizes the data from [oldBitsPerEntry] to [newBitsPerEntry].
     */
    protected abstract fun resizeData(oldBitsPerEntry: Int, newBitsPerEntry: Int)
    
    /**
     * Determines the number of bits per entry that should be used for [data].
     */
    protected abstract fun determineBitsPerEntry(): Int
    
}