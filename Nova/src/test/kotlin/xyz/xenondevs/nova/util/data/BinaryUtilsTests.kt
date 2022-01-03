package xyz.xenondevs.nova.util.data

import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BinaryUtilsTests {
    
    @Test
    fun testByteArrayCompression() {
        var bytes = Random.Default.nextBytes(128)
        val compressed = bytes.compress()
        assertFalse { bytes.contentEquals(compressed) }
        assertContentEquals(bytes, compressed.decompress())
        bytes = ByteArray(128) { 1 }
        assertTrue { bytes.compress().size < bytes.size }
    }
}