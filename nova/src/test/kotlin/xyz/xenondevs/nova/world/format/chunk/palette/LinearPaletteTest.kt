package xyz.xenondevs.nova.world.format.chunk.palette

import org.junit.jupiter.api.Test
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.nova.byteWriter
import xyz.xenondevs.nova.world.format.MockIdResolver
import xyz.xenondevs.nova.world.format.NotImplementedIdResolver
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinearPaletteTest {
    
    @Test
    fun testRead() {
        val idResolver = MockIdResolver()
        
        val bin = byteWriter { repeat(LinearPalette.MAX_SIZE) { writeVarInt(idResolver.data[it].second) } }
        
        val palette = LinearPalette(idResolver, ByteReader.fromByteArray(bin))
        
        assertNull(palette.getValue(0))
        for (i in 1..LinearPalette.MAX_SIZE) {
            assertEquals(idResolver.data[i - 1].first, palette.getValue(i))
        }
    }
    
    @Test
    fun testWrite() {
        val idResolver = MockIdResolver()
        
        val palette = LinearPalette(idResolver)
        for (i in 0..<LinearPalette.MAX_SIZE) {
            palette.putValue(idResolver.data[i].first)
        }
        
        val expected = byteWriter { repeat(LinearPalette.MAX_SIZE) { writeVarInt(idResolver.data[it].second) } }
        val actual = byteWriter { palette.write(this) }
        
        assertContentEquals(expected, actual)
    }
    
    @Test
    fun testReadWrite() {
        val idResolver = MockIdResolver()
        val binIn = byteWriter { repeat(LinearPalette.MAX_SIZE) { writeVarInt(idResolver.data[it].second) } }
        val palette = LinearPalette(idResolver, ByteReader.fromByteArray(binIn))
        val binOut = byteWriter { palette.write(this) }
        assertContentEquals(binIn, binOut)
    }
    
    @Test
    fun testToFromPalletizedId() {
        val palette = LinearPalette(NotImplementedIdResolver())
        
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
    
    @Test
    fun testToHashPalette() {
        val idResolver = MockIdResolver()
        val linearPalette = LinearPalette(idResolver)
        repeat(LinearPalette.MAX_SIZE) { linearPalette.putValue(idResolver.data[it].first) }
        val hashPalette = linearPalette.toHashPalette()
        
        assertNull(hashPalette.getValue(0))
        for (i in 1..LinearPalette.MAX_SIZE) {
            assertEquals(idResolver.data[i - 1].first, hashPalette.getValue(i))
        }
    }
    
}