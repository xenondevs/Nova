package xyz.xenondevs.nova.world.format.chunk.palette

import org.junit.jupiter.api.Test
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.nova.byteWriter
import xyz.xenondevs.nova.world.format.MockIdResolver
import xyz.xenondevs.nova.world.format.NotImplementedIdResolver
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class HashPaletteTest {
    
    @Test
    fun testRead() {
        val idResolver = MockIdResolver()
        
        val bin = byteWriter {
            writeVarInt(3) // size
            
            // global ids
            writeVarInt(idResolver.toId("a"))
            writeVarInt(idResolver.toId("b"))
            writeVarInt(idResolver.toId("c"))
        }
        
        val palette = HashPalette(idResolver, ByteReader.fromByteArray(bin))
        
        assertEquals("a", palette.getValue(1))
        assertEquals("b", palette.getValue(2))
        assertEquals("c", palette.getValue(3))
    }
    
    @Test
    fun testWrite() {
        val idResolver = MockIdResolver()
        val palette = HashPalette(idResolver)
        
        assertEquals(1, palette.putValue("a"))
        assertEquals(2, palette.putValue("b"))
        assertEquals(3, palette.putValue("c"))
        
        val expected = byteWriter {
            writeVarInt(3) // size
            
            // global ids
            writeVarInt(idResolver.toId("a"))
            writeVarInt(idResolver.toId("b"))
            writeVarInt(idResolver.toId("c"))
        }
        val actual = byteWriter { palette.write(this) }
        
        assertContentEquals(expected, actual)
    }
    
    @Test
    fun testReadWrite() {
        val idResolver = MockIdResolver()
        
        val binIn = byteWriter {
            writeVarInt(3) // size
            
            // global ids
            writeVarInt(idResolver.toId("a"))
            writeVarInt(idResolver.toId("b"))
            writeVarInt(idResolver.toId("c"))
        }
        
        val palette = HashPalette(idResolver, ByteReader.fromByteArray(binIn))
        
        val binOut = byteWriter { palette.write(this) }
        
        assertContentEquals(binIn, binOut)
    }
    
    @Test
    fun testToFromPalletizedId() {
        val palette = HashPalette(NotImplementedIdResolver())
        
        assertEquals(1, palette.putValue("a"))
        assertEquals(2, palette.putValue("b"))
        assertEquals(3, palette.putValue("c"))
        
        assertEquals(0, palette.getId(null))
        assertEquals(1, palette.getId("a"))
        assertEquals(2, palette.getId("b"))
        assertEquals(3, palette.getId("c"))
        
        assertEquals(null, palette.getValue(0))
        assertEquals("a", palette.getValue(1))
        assertEquals("b", palette.getValue(2))
        assertEquals("c", palette.getValue(3))
    }
    
}