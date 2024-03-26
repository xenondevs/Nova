package xyz.xenondevs.nova.world.format.chunk.data

import it.unimi.dsi.fastutil.shorts.Short2ByteMap
import it.unimi.dsi.fastutil.shorts.Short2ByteOpenHashMap
import it.unimi.dsi.fastutil.shorts.Short2ShortMap
import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter

internal inline fun MappedCompactIntStorage.forEach(action: (Int, Int) -> Unit) = when(this) {
    is Short2ByteMapCompactIntStorage -> map.short2ByteEntrySet().forEach { action(it.shortKey.toInt(), it.byteValue.toInt()) }
    is Short2ShortMapCompactIntStorage -> map.short2ShortEntrySet().forEach { action(it.shortKey.toInt(), it.shortValue.toInt()) }
}

internal sealed interface MappedCompactIntStorage : CompactIntStorage {
    
    /**
     * A read-only view of the underlying map.
     */
    val map: Map<Short, *>
    
    /**
     * Clears the storage.
     */
    fun clear()
    
    companion object {
        
        /**
         * Creates a new [MappedCompactIntStorage] with the given [bitWidth].
         */
        fun create(bitWidth: Int): MappedCompactIntStorage {
            return when (bitWidth) {
                8 -> Short2ByteMapCompactIntStorage(Short2ByteOpenHashMap())
                16 -> Short2ShortMapCompactIntStorage(Short2ShortOpenHashMap())
                else -> throw UnsupportedOperationException()
            }
        }
        
        /**
         * Reads a new [MappedCompactIntStorage] from the given [reader] with the given [bitWidth].
         */
        fun read(reader: ByteReader, bitWidth: Int): MappedCompactIntStorage {
            val size = reader.readVarInt()
            return when (bitWidth) {
                8 -> {
                    val map = Short2ByteOpenHashMap()
                    repeat(size) { map.put(reader.readShort(), reader.readByte()) }
                    Short2ByteMapCompactIntStorage(map)
                }
                
                16 -> {
                    val map = Short2ShortOpenHashMap()
                    repeat(size) { map.put(reader.readShort(), reader.readShort()) }
                    Short2ShortMapCompactIntStorage(map)
                }
                
                else -> throw UnsupportedOperationException()
            }
        }
        
    }
    
}

internal class Short2ByteMapCompactIntStorage(override val map: Short2ByteMap) : MappedCompactIntStorage {
    
    override fun get(index: Int): Int {
        return map.get(index.toShort()).toInt()
    }
    
    override fun set(index: Int, value: Int) {
        if (value == 0)
            map.remove(index.toShort())
        else map.put(index.toShort(), value.toByte())
    }
    
    override fun write(writer: ByteWriter) {
        writer.writeVarInt(map.size)
        for ((index, value) in map.short2ByteEntrySet()) {
            writer.writeShort(index)
            writer.writeByte(value)
        }
    }
    
    override fun clear() {
        map.clear()
    }
    
}

internal class Short2ShortMapCompactIntStorage(override val map: Short2ShortMap) : MappedCompactIntStorage {
    
    override fun get(index: Int): Int {
        return map.get(index.toShort()).toInt()
    }
    
    override fun set(index: Int, value: Int) {
        if (value == 0)
            map.remove(index.toShort())
        else map.put(index.toShort(), value.toShort())
    }
    
    override fun write(writer: ByteWriter) {
        writer.writeVarInt(map.size)
        for ((index, value) in map.short2ShortEntrySet()) {
            writer.writeShort(index)
            writer.writeShort(value)
        }
    }
    
    override fun clear() {
        map.clear()
    }
    
}