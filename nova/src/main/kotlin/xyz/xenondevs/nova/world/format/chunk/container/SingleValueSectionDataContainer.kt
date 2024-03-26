package xyz.xenondevs.nova.world.format.chunk.container

import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.world.format.IdResolver

internal class SingleValueSectionDataContainer<T> : SectionDataContainer<T> {
    
    var value: T?
        private set
    
    override var nonEmptyBlockCount = 0
        private set
    
    constructor(idResolver: IdResolver<T>, value: T?) : super(idResolver) {
        this.value = value
        nonEmptyBlockCount = if (value == null) 0 else SECTION_SIZE
    }
    
    constructor(idResolver: IdResolver<T>, reader: ByteReader) : super(idResolver) {
        value = idResolver.fromId(reader.readVarInt())
        nonEmptyBlockCount = if (value == null) 0 else SECTION_SIZE
    }
    
    override fun write(writer: ByteWriter) {
        writer.writeVarInt(idResolver.toId(value))
    }
    
    override fun get(x: Int, y: Int, z: Int): T? = value
    
    override fun set(x: Int, y: Int, z: Int, value: T?) = throw UnsupportedOperationException()
    
    override fun isMonotone(): Boolean = true
    
    override fun forEachNonEmpty(action: (Int, Int, Int, T) -> Unit) {
        val value = value
        if (value != null) {
            for (x in 0..15) {
                for (y in 0..15) {
                    for (z in 0..15) {
                        action(x, y, z, value)
                    }
                }
            }
        }
    }
    
    override fun fill(value: T?) {
        this.value = value
    }
    
}