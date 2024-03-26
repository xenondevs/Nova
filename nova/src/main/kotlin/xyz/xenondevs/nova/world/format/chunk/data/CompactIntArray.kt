package xyz.xenondevs.nova.world.format.chunk.data

import xyz.xenondevs.cbf.io.ByteWriter
import java.nio.ByteBuffer

internal interface CompactIntArray : CompactIntStorage {
    
    /**
     * The size of the array.
     */
    val size: Int
    
    /**
     * Fills the array with the given [value].
     */
    fun fill(value: Int)
    
    /**
     * Converts or retrieves the array to/as a [ByteArray].
     */
    fun toByteArray(): ByteArray
    
    override fun write(writer: ByteWriter) = writer.writeBytes(toByteArray())
    
    companion object {
        
        /**
         * Creates a new [CompactIntArray] of size [size] with the given [bitWidth].
         */
        fun create(size: Int, bitWidth: Int) = when {
            bitWidth <= 2 -> CompactByteArrayBackedIntArray(2, size)
            bitWidth <= 4 -> CompactByteArrayBackedIntArray(4, size)
            bitWidth <= 8 -> CompactByteArrayWrappingIntArray(size)
            bitWidth <= 16 -> CompactShortArrayWrappingIntArray(size)
            else -> throw IllegalArgumentException("Bit width must be <= 16")
        }
        
        /**
         * Creates a new [CompactIntArray] from the given [array] of size [size] with the given [bitWidth].
         */
        fun fromByteArray(size: Int, bitWidth: Int, array: ByteArray) = when {
            bitWidth <= 2 -> CompactByteArrayBackedIntArray(2, size, array)
            bitWidth <= 4 -> CompactByteArrayBackedIntArray(4, size, array)
            bitWidth <= 8 -> CompactByteArrayWrappingIntArray(size, array)
            bitWidth <= 16 -> CompactShortArrayWrappingIntArray(size, array)
            else -> throw IllegalArgumentException("Bit width must be <= 16")
        }
        
    }
    
}

private class CompactByteArrayBackedIntArray(
    private val width: Int,
    override val size: Int,
    array: ByteArray? = null
) : CompactIntArray {
    
    private val mask = (1 shl width) - 1
    private val perByte = 8 / width
    private val modMax = perByte - 1
    
    private val array = array ?: ByteArray(size / perByte)
    
    init {
        require(size % perByte == 0) { "Size must be a multiple of $perByte" }
    }
    
    override fun get(index: Int): Int {
        val arrayIndex = index / perByte
        val byte = array[arrayIndex].toInt()
        val shift = (modMax - index % perByte) * width
        return byte ushr shift and mask
    }
    
    override fun set(index: Int, value: Int) {
        val arrayIndex = index / perByte
        var byte = array[arrayIndex].toInt()
        val shift = (modMax - index % perByte) * width
        byte = byte and (mask shl shift).inv()
        byte = byte or ((value and mask) shl shift)
        array[arrayIndex] = byte.toByte()
    }
    
    override fun fill(value: Int) {
        val bits = value and mask
        var filled = 0
        for (i in 0..<perByte) {
            filled = filled or (bits shl (i * width))
        }
        array.fill(filled.toByte())
    }
    
    override fun toByteArray(): ByteArray = array
    
}

private class CompactByteArrayWrappingIntArray(
    override val size: Int,
    array: ByteArray? = null,
) : CompactIntArray {
    
    private val array = array ?: ByteArray(size)
    
    override fun get(index: Int): Int {
        return array[index].toInt() and 0xFF
    }
    
    override fun set(index: Int, value: Int) {
        array[index] = value.toByte()
    }
    
    override fun fill(value: Int) {
        array.fill(value.toByte())
    }
    
    override fun toByteArray(): ByteArray = array
    
}

private class CompactShortArrayWrappingIntArray(
    override val size: Int,
    array: ByteArray? = null
) : CompactIntArray {
    
    private val array = ShortArray(size)
    
    init {
        if (array != null)
            ByteBuffer.wrap(array).asShortBuffer().get(this.array)
    }
    
    override fun get(index: Int): Int {
        return array[index].toInt() and 0xFFFF
    }
    
    override fun set(index: Int, value: Int) {
        array[index] = value.toShort()
    }
    
    override fun fill(value: Int) {
        array.fill(value.toShort())
    }
    
    override fun toByteArray(): ByteArray {
        val byteBuffer = ByteBuffer.allocate(size * 2)
        byteBuffer.asShortBuffer().put(array)
        return byteBuffer.array()
    }
    
}