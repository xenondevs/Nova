package xyz.xenondevs.nova.world.format.chunk

import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.RegionizedFile

/**
 * A chunk inside a [RegionizedFile].
 */
internal sealed interface RegionizedChunk {
    
    /**
     * Writes this [RegionizedChunk] to the given [writer], then
     * returns whether any data was written.
     */
    fun write(writer: ByteWriter): Boolean
    
}

/**
 * A reader for [RegionizedChunks][RegionizedChunk].
 */
internal abstract class RegionizedChunkReader<C : RegionizedChunk> {
    
    /**
     * Reads a [C], which is a [RegionizedChunk] at [pos] from the given [reader].
     */
    abstract fun read(pos: ChunkPos, reader: ByteReader): C
    
    /**
     * Creates an empty [C] with given [pos].
     */
    abstract fun createEmpty(pos: ChunkPos): C
    
}