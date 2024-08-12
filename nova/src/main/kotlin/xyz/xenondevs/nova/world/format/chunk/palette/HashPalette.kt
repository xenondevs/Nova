package xyz.xenondevs.nova.world.format.chunk.palette

import it.unimi.dsi.fastutil.objects.Object2ShortMap
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.world.format.IdResolver

internal class HashPalette<T>(private val idResolver: IdResolver<T>) : Palette<T> {
    
    private val fromId: MutableList<T> = ArrayList()
    private val toId: Object2ShortMap<T> = Object2ShortOpenHashMap()
    
    override val size: Int
        get() = fromId.size
    
    constructor(idResolver: IdResolver<T>, reader: ByteReader) : this(idResolver) {
        val size = reader.readVarInt()
        repeat(size) {
            val globalId = reader.readVarInt()
            val value = idResolver.fromId(globalId)
            
            if (value != null) {
                fromId.add(value)
                toId.put(value, fromId.size.toShort())
            }
        }
    }
    
    override fun write(writer: ByteWriter) {
        writer.writeVarInt(fromId.size)
        for (value in fromId)
            writer.writeVarInt(idResolver.toId(value))
    }
    
    override fun getValue(id: Int): T? {
        if (id == 0)
            return null
        
        return if (id <= fromId.size) fromId[id - 1] else null
    }
    
    override fun getId(value: T?): Int {
        if (value == null)
            return 0
        
        return toId.getShort(value).toInt()
    }
    
    override fun putValue(value: T): Int {
        fromId.add(value)
        val paletteId = fromId.size
        toId.put(value, paletteId.toShort())
        
        return paletteId
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as HashPalette<*>
        
        if (idResolver != other.idResolver) return false
        if (fromId != other.fromId) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = idResolver.hashCode()
        result = 31 * result + fromId.hashCode()
        return result
    }
    
}