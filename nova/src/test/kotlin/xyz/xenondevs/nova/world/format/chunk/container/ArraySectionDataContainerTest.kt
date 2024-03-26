package xyz.xenondevs.nova.world.format.chunk.container

import org.junit.jupiter.api.Test
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.nova.byteWriter
import xyz.xenondevs.nova.world.format.MockIdResolver
import xyz.xenondevs.nova.world.format.chunk.data.CompactIntArray
import xyz.xenondevs.nova.world.format.chunk.palette.HashPalette
import xyz.xenondevs.nova.world.format.chunk.palette.LinearPalette
import xyz.xenondevs.nova.world.format.chunk.palette.Palette
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArraySectionDataContainerTest : AbstractSectionDataContainerTest() {
    
    @Test
    fun testReadWrite() {
        val idResolver = MockIdResolver()
        val palette = LinearPalette(idResolver)
        
        palette.putValue("a")
        palette.putValue("b")
        palette.putValue("c")
        
        val binIn = byteWriter { 
            Palette.write(palette, this)
            
            val arr = CompactIntArray.create(4096, 2)
            arr[0] = palette.getId("a")
            arr[1] = palette.getId("b")
            arr[2] = palette.getId("c")
            arr.write(this)
            
            writeVarInt(3) // non-empty block count
        }
        
        val container = ArraySectionDataContainer(idResolver, ByteReader.fromByteArray(binIn))
        
        assertEquals("a", container[0, 0, 0])
        assertEquals("b", container[0, 0, 1])
        assertEquals("c", container[0, 0, 2])
        assertNull(container[0, 0, 3])
        assertEquals(3, container.nonEmptyBlockCount)
        
        val binOut = byteWriter { container.write(this) }
        assertContentEquals(binIn, binOut)
    }
    
    @Test
    fun testResizeData() {
        val idResolver = MockIdResolver()
        val paletteIn = LinearPalette(idResolver)
        
        paletteIn.putValue("a")
        paletteIn.putValue("b")
        paletteIn.putValue("c")
        
        val binIn = byteWriter {
            Palette.write(paletteIn, this)
            
            val arr = CompactIntArray.create(4096, 2)
            arr[0] = paletteIn.getId("a")
            arr[1] = paletteIn.getId("b")
            arr[2] = paletteIn.getId("c")
            arr.write(this)
            
            writeVarInt(3) // non-empty block count
        }
        val container = ArraySectionDataContainer(idResolver, ByteReader.fromByteArray(binIn))
        
        container[0, 0, 3] = "d"
        
        val paletteOutExpected = LinearPalette(idResolver)
        paletteOutExpected.putValue("a")
        paletteOutExpected.putValue("b")
        paletteOutExpected.putValue("c")
        paletteOutExpected.putValue("d")
        val binOutExpected = byteWriter { 
            Palette.write(paletteOutExpected, this)
            
            val arr = CompactIntArray.create(4096, 4)
            arr[0] = paletteOutExpected.getId("a")
            arr[1] = paletteOutExpected.getId("b")
            arr[2] = paletteOutExpected.getId("c")
            arr[3] = paletteOutExpected.getId("d")
            arr.write(this)

            writeVarInt(4) // non-empty block count
        }
        
        val binOut = byteWriter { container.write(this) }
        assertContentEquals(binOutExpected, binOut)
    }
    
    @Test
    fun testSwapPalette() {
        val idResolver = MockIdResolver()
        val container = ArraySectionDataContainer(MockIdResolver())
        val alphabet = "abcdefghijklmnopqrstuvwxyz"
        
        for (i in 0..16)
            container[i / 16, 0, i % 16] = alphabet[i].toString()
        
        val binOut = byteWriter { container.write(this) }
        val expected = byteWriter {
            val palette = HashPalette(idResolver)
            for (i in 0..16)
                palette.putValue(alphabet[i].toString())
            
            val arr = CompactIntArray.create(4096, 8)
            for (i in 0..16)
                arr[i] = palette.getId(alphabet[i].toString())
            
            Palette.write(palette, this)
            arr.write(this)
            writeVarInt(17) // non-empty block count
        }
        
        assertContentEquals(expected, binOut)
    }
    
    
    @Test
    fun testNonEmptyBlockCount() {
        testNonEmptyBlockCount(ArraySectionDataContainer(MockIdResolver()))
    }
    
    @Test
    fun testFill() {
        testFill(ArraySectionDataContainer(MockIdResolver()))
    }
    
    @Test
    fun testForEachNonEmpty() {
        testForEachNonEmpty(ArraySectionDataContainer(MockIdResolver()))
    }
    
    @Test
    fun testIsMonotone() {
        testIsMonotone(ArraySectionDataContainer(MockIdResolver()))
    }
    
}