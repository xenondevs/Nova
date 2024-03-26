package xyz.xenondevs.nova.world.format.chunk.data

import org.junit.jupiter.api.Test
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.nova.byteWriter
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class MappedCompactIntStorageTest {
    
    @Test
    fun testReadWriteWidth8() {
        val binIn = byteWriter {
            writeVarInt(3) // length
            
            // key-value pairs
            writeShort(0)
            writeByte(1) 
            
            writeShort(Short.MIN_VALUE)
            writeByte(Byte.MIN_VALUE)
            
            writeShort(Short.MAX_VALUE)
            writeByte(Byte.MAX_VALUE)
        }
        
        val storage = MappedCompactIntStorage.read(ByteReader.fromByteArray(binIn), 8)
        
        assertEquals(0, storage[1]) // default value
        assertEquals(1, storage[0])
        assertEquals(Byte.MIN_VALUE.toInt(), storage[Short.MIN_VALUE.toInt()])
        assertEquals(Byte.MAX_VALUE.toInt(), storage[Short.MAX_VALUE.toInt()])
        
        // should not be written to bin
        storage[1] = 0
        storage[2] = 0
        storage[3] = 0
        
        val binOut = byteWriter { storage.write(this) }
        
        assertContentEquals(binIn, binOut)
    }
    
    @Test
    fun testReadWriteWidth16() {
        val binIn = byteWriter {
            writeVarInt(3) // length
            
            // key-value pairs
            writeShort(0)
            writeShort(1) 
            
            writeShort(Short.MIN_VALUE)
            writeShort(Short.MIN_VALUE)
            
            writeShort(Short.MAX_VALUE)
            writeShort(Short.MAX_VALUE)
        }
        
        val storage = MappedCompactIntStorage.read(ByteReader.fromByteArray(binIn), 16)
        
        assertEquals(0, storage[1]) // default value
        assertEquals(1, storage[0])
        assertEquals(Short.MIN_VALUE.toInt(), storage[Short.MIN_VALUE.toInt()])
        assertEquals(Short.MAX_VALUE.toInt(), storage[Short.MAX_VALUE.toInt()])
        
        // should not be written to bin
        storage[1] = 0
        storage[2] = 0
        storage[3] = 0
        
        val binOut = byteWriter { storage.write(this) }
        
        assertContentEquals(binIn, binOut)
    }
    
}