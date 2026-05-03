package xyz.xenondevs.nova.world.format

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SectionMatchResultTest {
    
    @Test
    fun testNoneIsAllFalse() {
        val none = SectionMatchResult.NONE
        for (i in 0..<4096) {
            assertFalse(none[i])
        }
    }
    
    @Test
    fun testAllIsAllTrue() {
        val all = SectionMatchResult.ALL
        for (i in 0..<4096) {
            assertTrue(all[i])
        }
    }
    
    @Test
    fun testGetByCoordinatesMatchesGetByIndex() {
        val data = LongArray(64)
        // pack: (y shl 8) or (x shl 4) or z
        val packed = (5 shl 8) or (3 shl 4) or 7
        SectionMatchResult.set(data, packed, true)
        val result = SectionMatchResult(data)
        
        assertTrue(result[3, 5, 7])
        assertTrue(result[packed])
        assertFalse(result[0, 0, 0])
        assertFalse(result[1, 1, 1])
    }
    
    @Test
    fun testSetTrueSetsBit() {
        val data = LongArray(64)
        SectionMatchResult.set(data, 0, true)
        SectionMatchResult.set(data, 63, true)
        SectionMatchResult.set(data, 64, true)
        SectionMatchResult.set(data, 4095, true)
        
        val result = SectionMatchResult(data)
        assertTrue(result[0])
        assertTrue(result[63])
        assertTrue(result[64])
        assertTrue(result[4095])
        assertFalse(result[1])
        assertFalse(result[62])
        assertFalse(result[65])
        assertFalse(result[4094])
    }
    
    @Test
    fun testSetFalseDoesNothing() {
        val data = LongArray(64)
        SectionMatchResult.set(data, 5, true)
        SectionMatchResult.set(data, 5, false) // should NOT clear the bit
        
        val result = SectionMatchResult(data)
        assertTrue(result[5])
    }
    
    @Test
    fun testBuildPassesPackedCoordinates() {
        // pick a few packed values and verify the build callback sees the same index
        // that get() will use
        val target = setOf(0, 1, 16, 256, 4095, (7 shl 8) or (4 shl 4) or 2)
        val result = SectionMatchResult.build { packed -> packed in target }
        
        for (i in 0..<4096) {
            assertEquals(i in target, result[i], "mismatch at index $i")
        }
    }
    
}
