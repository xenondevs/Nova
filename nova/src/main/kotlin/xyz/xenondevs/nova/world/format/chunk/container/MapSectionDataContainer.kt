package xyz.xenondevs.nova.world.format.chunk.container

import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.world.format.IdResolver
import xyz.xenondevs.nova.world.format.chunk.data.MappedCompactIntStorage
import xyz.xenondevs.nova.world.format.chunk.data.forEach
import xyz.xenondevs.nova.world.format.chunk.palette.LinearPalette
import xyz.xenondevs.nova.world.format.chunk.palette.Palette

internal class MapSectionDataContainer<T> : PalletizedSectionDataContainer<T> {
    
    override var palette: Palette<T>
    override var data: MappedCompactIntStorage
    override val nonEmptyBlockCount: Int
        get() = data.map.size
    
    constructor(idResolver: IdResolver<T>) : super(idResolver) {
        palette = LinearPalette(idResolver)
        bitsPerEntry = determineBitsPerEntry()
        data = MappedCompactIntStorage.create(bitsPerEntry)
    }
    
    constructor(idResolver: IdResolver<T>, reader: ByteReader) : super(idResolver) {
        palette = Palette.read(idResolver, reader)
        bitsPerEntry = determineBitsPerEntry()
        data = MappedCompactIntStorage.read(reader, bitsPerEntry)
    }
    
    override fun write(writer: ByteWriter) {
        remakePaletteResizeData()
        
        Palette.write(palette, writer)
        data.write(writer)
    }
    
    override fun get(x: Int, y: Int, z: Int): T? {
        return palette.getValue(data[pack(x, y, z)])
    }
    
    override fun set(x: Int, y: Int, z: Int, value: T?): T? {
        val previous = get(x, y, z)
        if (previous == value)
            return previous
        
        val id = toPalletizedId(value)
        data[pack(x, y, z)] = id
        
        return previous
    }
    
    override fun fill(value: T?) {
        if (value != null) {
            val id = toPalletizedId(value)
            for (i in 0..<SECTION_SIZE) {
                data[i] = id
            }
        } else data.clear()
    }
    
    override fun forEachNonEmpty(action: (Int, Int, Int, T) -> Unit) {
        data.forEach { packedPos, value ->
            action(unpackX(packedPos), unpackY(packedPos), unpackZ(packedPos), palette.getValue(value)!!)
        }
    }
    
    override fun isMonotone(): Boolean {
        if (nonEmptyBlockCount == 0)
            return true
        
        var prev = 0
        data.forEach { _, value -> 
            if (prev == 0)
                prev = value
            else if (prev != value)
                return false
        }
        
        // if not returned earlier, the section can only be monotone if there are no empty blocks
        return nonEmptyBlockCount == SECTION_SIZE
    }
    
    override fun resizeData(oldBitsPerEntry: Int, newBitsPerEntry: Int) {
        val newMap = MappedCompactIntStorage.create(newBitsPerEntry)
        data.forEach { packedPos, value -> newMap[packedPos] = value }
        data = newMap
    }
    
    override fun determineBitsPerEntry(): Int = when {
        palette.size < 0xFF -> 8
        palette.size < 0xFFFF -> 16
        else -> throw UnsupportedOperationException()
    }
    
}