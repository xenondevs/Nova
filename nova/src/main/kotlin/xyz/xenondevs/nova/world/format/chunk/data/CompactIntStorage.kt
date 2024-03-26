package xyz.xenondevs.nova.world.format.chunk.data

import xyz.xenondevs.cbf.io.ByteWriter

interface CompactIntStorage {
    
    /**
     * Retrieves the value at the given [index].
     */
    operator fun get(index: Int): Int
    
    /**
     * Sets the [value] at the given [index].
     */
    operator fun set(index: Int, value: Int)
    
    /**
     * Writes the storage to the given [writer].
     */
    fun write(writer: ByteWriter)
    
}