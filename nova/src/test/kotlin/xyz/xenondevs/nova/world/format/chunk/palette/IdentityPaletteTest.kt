package xyz.xenondevs.nova.world.format.chunk.palette

import org.junit.jupiter.api.Test
import xyz.xenondevs.nova.byteWriter
import xyz.xenondevs.nova.world.format.MockIdResolver
import xyz.xenondevs.nova.world.format.NotImplementedIdResolver
import kotlin.test.assertEquals

class IdentityPaletteTest {
    
    @Test
    fun testWrite() {
        val palette = IdentityPalette(NotImplementedIdResolver())
        val out = byteWriter { palette.write(this) }
        assert(out.isEmpty()) { "IdentityPalette should not write anything" }
    }
    
    @Test
    fun testToFromPalletizedId() {
        val idResolver = MockIdResolver()
        val palette = IdentityPalette(idResolver)
        
        assertEquals(idResolver.size, palette.size)
        
        assertEquals(idResolver.fromId(0), palette.getValue(0))
        assertEquals(idResolver.fromId(Int.MIN_VALUE), palette.getValue(Int.MIN_VALUE))
    }
    
}