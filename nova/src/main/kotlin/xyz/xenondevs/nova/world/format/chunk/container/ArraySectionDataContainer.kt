package xyz.xenondevs.nova.world.format.chunk.container

import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.world.format.IdResolver
import xyz.xenondevs.nova.world.format.chunk.data.CompactIntArray
import xyz.xenondevs.nova.world.format.chunk.palette.LinearPalette
import xyz.xenondevs.nova.world.format.chunk.palette.Palette

internal class ArraySectionDataContainer<T> : PalletizedSectionDataContainer<T> {
    
    override var palette: Palette<T>
    override var data: CompactIntArray
    
    override var nonEmptyBlockCount: Int = 0
        private set
    
    constructor(idResolver: IdResolver<T>) : super(idResolver) {
        palette = LinearPalette(idResolver)
        bitsPerEntry = determineBitsPerEntry()
        data = CompactIntArray.create(SECTION_SIZE, bitsPerEntry)
    }
    
    constructor(idResolver: IdResolver<T>, reader: ByteReader) : super(idResolver) {
        palette = Palette.read(idResolver, reader)
        bitsPerEntry = determineBitsPerEntry()
        data = CompactIntArray.fromByteArray(
            SECTION_SIZE,
            bitsPerEntry,
            reader.readBytes(SECTION_SIZE * bitsPerEntry / 8)
        )
        nonEmptyBlockCount = reader.readVarInt()
    }
    
    override fun write(writer: ByteWriter) {
        remakePaletteResizeData()
        
        Palette.write(palette, writer)
        data.write(writer)
        writer.writeVarInt(nonEmptyBlockCount)
    }
    
    override fun get(x: Int, y: Int, z: Int): T? {
        return palette.getValue(data[pack(x, y, z)])
    }
    
    override fun set(x: Int, y: Int, z: Int, value: T?): T? {
        val previous = get(x, y, z)
        if (previous == value)
            return previous
        else if (previous == null)
            nonEmptyBlockCount++
        else if (value == null)
            nonEmptyBlockCount--
        
        val id = toPalletizedId(value)
        data[pack(x, y, z)] = id
        
        return previous
    }
    
    override fun fill(value: T?) {
        val id = toPalletizedId(value)
        data.fill(id)
    }
    
    override fun forEachNonEmpty(action: (Int, Int, Int, T) -> Unit) {
        for (x in 0..15) {
            for (y in 0..15) {
                for (z in 0..15) {
                    val value = get(x, y, z)
                    if (value != null)
                        action(x, y, z, value)
                }
            }
        }
    }
    
    override fun isMonotone(): Boolean {
        // empty section is monotone of air
        if (nonEmptyBlockCount == 0)
            return true
        
        // section with empty parts cannot be monotone
        if (nonEmptyBlockCount != SECTION_SIZE)
            return false
        
        var prev = data[0]
        for (i in 1..<SECTION_SIZE) {
            val now = data[i]
            if (now != prev)
                return false
            prev = now
        }
        
        return true
    }
    
    override fun resizeData(oldBitsPerEntry: Int, newBitsPerEntry: Int) {
        val oldData = data
        val newData = CompactIntArray.create(SECTION_SIZE, newBitsPerEntry)
        for (packedPos in 0..<SECTION_SIZE) {
            newData[packedPos] = oldData[packedPos]
        }
        data = newData
    }
    
    override fun determineBitsPerEntry(): Int {
        val size = palette.size
        return when {
            size <= 0b11 -> 2
            size <= 0xF -> 4
            size <= 0xFF -> 8
            size <= 0xFFFF -> 16
            else -> throw UnsupportedOperationException()
        }
    }
    
    
}