package xyz.xenondevs.nova.world.format.chunk

import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.world.BlockPos
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
    
    companion object {
        
        /**
         * Packs [pos] into a 32-bit integer, with the following format (from most to least significant bits):
         * - 24 bits for the y-coordinate
         * - 4 bits for the x-coordinate
         * - 4 bits for the z-coordinate
         * 
         * @see RegionizedChunkReader.unpackBlockPos
         */
        fun packBlockPos(pos: BlockPos): Int =
            (pos.y shl 8) or (pos.x and 0xF shl 4) or (pos.z and 0xF)
        
    }
    
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
    
    companion object {
        
        /**
         * Unpacks a 32-bit integer [value] into a [BlockPos] using the given [chunkPos].
         * 
         * @see RegionizedChunk.packBlockPos
         */
        fun unpackBlockPos(chunkPos: ChunkPos, value: Int): BlockPos {
            val y = value shr 8
            val x = (value shr 4) and 0xF
            val z = value and 0xF
            return BlockPos(chunkPos.world!!, (chunkPos.x shl 4) + x, y, (chunkPos.z shl 4) + z)
        }
        
    }
    
}