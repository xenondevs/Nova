package xyz.xenondevs.nova.world.format.chunk.palette

import org.junit.jupiter.api.Test
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.nova.byteWriter
import xyz.xenondevs.nova.world.format.IdResolver
import xyz.xenondevs.nova.world.format.MockIdResolver
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PaletteTest {
    
    @Test
    fun testIdentityPalette() {
        testReadWritePalette(::IdentityPalette, 0)
    }
    
    @Test
    fun testReadWriteLinearPalette() {
        testReadWritePalette(::LinearPalette, 1)
    }
    
    @Test
    fun testReadWriteHashPalette() {
        testReadWritePalette(::HashPalette, 2)
    }
    
    private fun testReadWritePalette(paletteConstructor: (IdResolver<String>) -> Palette<String>, identifier: Byte) {
        val idResolver = MockIdResolver()
        val palette = paletteConstructor(idResolver)
        
        palette.getId("a")
        palette.getId("b")
        palette.getId("c")
        
        val expectedBin = byteWriter {
            writeByte(identifier)
            palette.write(this)
        }
        val actualBin = byteWriter { Palette.write(palette, this) }
        assertContentEquals(expectedBin, actualBin)
        
        val readPalette = Palette.read(idResolver, ByteReader.fromByteArray(actualBin))
        assertEquals(palette, readPalette)
    }
    
}