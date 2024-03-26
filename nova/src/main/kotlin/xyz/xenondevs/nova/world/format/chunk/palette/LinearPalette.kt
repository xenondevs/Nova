package xyz.xenondevs.nova.world.format.chunk.palette

import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.world.format.IdResolver

internal class LinearPalette<T>(private val idResolver: IdResolver<T>) : Palette<T> {
    
    @Suppress("UNCHECKED_CAST")
    private val palette = arrayOfNulls<Any?>(MAX_SIZE) as Array<T?>
    override var size: Int = 0
        private set
    
    constructor(idResolver: IdResolver<T>, reader: ByteReader) : this(idResolver) {
        repeat(MAX_SIZE) {
            val value = idResolver.fromId(reader.readVarInt())
            if (value != null) {
                size++
                palette[it] = value
            }
        }
    }
    
    override fun write(writer: ByteWriter) {
        for (value in palette) writer.writeVarInt(idResolver.toId(value))
    }
    
    override fun getValue(id: Int): T? {
        if (id == 0)
            return null
        
        return palette[id - 1]
    }
    
    override fun getId(value: T?): Int {
        if (value == null)
            return 0
        
        return palette.indexOf(value) + 1
    }
    
    override fun putValue(value: T): Int {
        palette[size++] = value
        return size
    }
    
    fun toHashPalette(): HashPalette<T> {
        val hashPalette = HashPalette(idResolver)
        for (value in palette) {
            if (value != null)
                hashPalette.putValue(value)
        }
        
        return hashPalette
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as LinearPalette<T>
        
        if (idResolver != other.idResolver) return false
        if (!palette.contentEquals(other.palette)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = idResolver.hashCode()
        result = 31 * result + palette.contentHashCode()
        return result
    }
    
    companion object {
        /**
         * The maximum number of entries in a [LinearPalette], not including null als 0.
         */
        const val MAX_SIZE = 16
    }
    
}