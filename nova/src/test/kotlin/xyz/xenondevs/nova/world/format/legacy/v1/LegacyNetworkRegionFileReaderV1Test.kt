package xyz.xenondevs.nova.world.format.legacy.v1

import org.bukkit.block.BlockFace
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LegacyNetworkRegionFileReaderV1Test {
    
    @Test
    fun testCubeFaceSetBitOrderConversion() {
        val faces = listOf(
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
        )
        
        for (legacyData in 0..0x3F) {
            val converted = convertLegacyCubeFaceSet(legacyData.toByte())
            for ([index, face] in faces.withIndex()) {
                val expected = legacyData and (1 shl (5 - index)) != 0
                assertEquals(expected, face in converted, "legacyData=$legacyData, face=$face")
            }
        }
    }
    
}
