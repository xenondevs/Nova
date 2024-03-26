package xyz.xenondevs.nova.world.format.chunk.data

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CompactIntArrayTest {
    
    @Test
    fun testReadWidth2() {
        val bin = byteArrayOf(0b00011011.toByte(), 0b11100100.toByte())
        val arr = CompactIntArray.fromByteArray(8, 2, bin)
        
        assertEquals(0b00, arr[0])
        assertEquals(0b01, arr[1])
        assertEquals(0b10, arr[2])
        assertEquals(0b11, arr[3])
        assertEquals(0b11, arr[4])
        assertEquals(0b10, arr[5])
        assertEquals(0b01, arr[6])
        assertEquals(0b00, arr[7])
    }
    
    @Test
    fun testWriteWidth2() {
        val arr = CompactIntArray.create(8, 2)
        arr[0] = 0b00
        arr[1] = 0b01
        arr[2] = 0b10
        arr[3] = 0b11
        arr[4] = 0b11
        arr[5] = 0b10
        arr[6] = 0b01
        arr[7] = 0b00
        
        val bin = arr.toByteArray()
        assertEquals(0b00011011.toByte(), bin[0])
        assertEquals(0b11100100.toByte(), bin[1])
    }
    
    @Test
    fun testReadWriteWidth2() {
        val binIn = byteArrayOf(0b00011011.toByte(), 0b11100100.toByte())
        val arr = CompactIntArray.fromByteArray(8, 2, binIn)
        val binOut = arr.toByteArray()
        
        assertContentEquals(binIn, binOut)
    }
    
    @Test
    fun testReadWidth4() {
        val bin = byteArrayOf(0b00011011.toByte(), 0b11100100.toByte())
        val arr = CompactIntArray.fromByteArray(4, 4, bin)
        
        assertEquals(0b0001, arr[0])
        assertEquals(0b1011, arr[1])
        assertEquals(0b1110, arr[2])
        assertEquals(0b0100, arr[3])
    }
    
    @Test
    fun testWriteWidth4() {
        val arr = CompactIntArray.create(4, 4)
        arr[0] = 0b0001
        arr[1] = 0b1011
        arr[2] = 0b1110
        arr[3] = 0b0100
        
        val bin = arr.toByteArray()
        assertEquals(0b00011011.toByte(), bin[0])
        assertEquals(0b11100100.toByte(), bin[1])
    }
    
    @Test
    fun testReadWriteWidth4() {
        val binIn = byteArrayOf(0b00011011.toByte(), 0b11100100.toByte())
        val arr = CompactIntArray.fromByteArray(4, 4, binIn)
        val binOut = arr.toByteArray()
        
        assertContentEquals(binIn, binOut)
    }
    
    @Test
    fun testReadWidth8() {
        val bin = byteArrayOf(0b00011011.toByte(), 0b11100100.toByte())
        val arr = CompactIntArray.fromByteArray(2, 8, bin)
        
        assertEquals(0b00011011, arr[0])
        assertEquals(0b11100100, arr[1])
    }
    
    @Test
    fun testWriteWidth8() {
        val arr = CompactIntArray.create(2, 8)
        arr[0] = 0b00011011
        arr[1] = 0b11100100
        
        val bin = arr.toByteArray()
        assertEquals(0b00011011.toByte(), bin[0])
        assertEquals(0b11100100.toByte(), bin[1])
    }
    
    @Test
    fun testReadWriteWidth8() {
        val binIn = byteArrayOf(0b00011011.toByte(), 0b11100100.toByte())
        val arr = CompactIntArray.fromByteArray(2, 8, binIn)
        val binOut = arr.toByteArray()
        
        assertContentEquals(binIn, binOut)
    }
    
    @Test
    fun testReadWidth16() {
        val bin = byteArrayOf(0b00011011.toByte(), 0b11100100.toByte(), 0b11111111.toByte(), 0b00000000.toByte())
        val arr = CompactIntArray.fromByteArray(2, 16, bin)
        
        assertEquals(0b0001101111100100, arr[0])
        assertEquals(0b1111111100000000, arr[1])
    }
    
}