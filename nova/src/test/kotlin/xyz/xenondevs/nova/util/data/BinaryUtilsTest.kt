package xyz.xenondevs.nova.util.data

import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BinaryUtilsTest {
    
    @Test
    fun testCompressDecompress() {
        val bytes1 = Random.Default.nextBytes(128)
        assertContentEquals(bytes1, bytes1.compress().decompress())
        
        val bytes2 = Random.Default.nextBytes(128)
        assertContentEquals(bytes2, bytes2.compress().decompress())
    }
    
    @Test
    fun testCompressionWorking() {
        val bytes = ByteArray(128) { 0 }
        val compressed = bytes.compress()
        assertTrue { bytes.size > compressed.size }
    }
    
    @Test
    fun testCompressionChanges() {
        val bytes = Random.Default.nextBytes(128)
        val compressed = bytes.compress()
        assertFalse { bytes.contentEquals(compressed) }
    }
    
}