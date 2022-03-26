package xyz.xenondevs.nova.data.world

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.util.data.readStringList
import xyz.xenondevs.nova.util.data.writeStringList
import xyz.xenondevs.nova.util.getOrSet
import xyz.xenondevs.nova.world.ChunkPos
import java.io.File

private fun ByteBuf.readBooleanArray(size: Int): BooleanArray {
    val booleans = BooleanArray(size)
    val bytes = ByteArray(size / 8).also { readBytes(it) }
    for (i in bytes.indices) {
        val byte = bytes[i]
        repeat(8) { booleans[i * 8 + it] = byte.toInt() shr (7 - it) and 1 == 1 }
    }
    
    return booleans
}

private fun ByteBuf.writeBooleanArray(booleans: BooleanArray) {
    val bytes = ByteArray(booleans.size / 8)
    for (i in booleans.indices) {
        val bit = if (booleans[i]) 1 else 0
        bytes[i / 8] = (bytes[i / 8].toInt() shl 1 or bit).toByte()
    }
    writeBytes(bytes)
}

class RegionFile(val world: WorldDataStorage, val file: File, val regionX: Int, val regionZ: Int) {
    
    private val chunks = arrayOfNulls<RegionChunk?>(1024)
    
    fun read(buf: ByteBuf) {
        buf.readByte() // File format version
        val palette = buf.readStringList()
        
        val availableChunks = buf.readBooleanArray(1024)
        repeat(1024) {
            if (availableChunks[it]) {
                val x = it shr 5
                val z = it and 0x1F
                
                val chunk = RegionChunk(this, x, z)
                chunk.read(buf, palette)
                chunks[it] = chunk
            }
        }
    }
    
    fun write(buf: ByteBuf) {
        buf.writeByte(0) // File format version
        
        val palette = chunks.asSequence().filterNotNull().flatMap { it.blockStates.values }.mapTo(HashSet()) { it.id }.toList()
        buf.writeStringList(palette)
        
        val availableChunks = BooleanArray(1024) {
            val chunk = chunks[it]
            chunk != null && chunk.hasData()
        }
        buf.writeBooleanArray(availableChunks)
        chunks.forEach { chunk -> if (chunk != null && chunk.hasData()) chunk.write(buf, palette) }
    }
    
    fun getChunk(pos: ChunkPos): RegionChunk {
        val dx = pos.x and 0x1F
        val dz = pos.z and 0x1F
        return chunks.getOrSet(dx shl 5 or dz) { RegionChunk(this, dx, dz) }
    }
    
    fun isAnyChunkLoaded(): Boolean {
        val minChunkX = regionX shl 5
        val minChunkZ = regionZ shl 5
        
        for (chunkX in minChunkX until (minChunkX + 32)) {
            for (chunkZ in minChunkZ until (minChunkZ + 32)) {
                if (world.world.isChunkLoaded(chunkX, chunkZ))
                    return true
            }
        }
        
        return false
    }
    
}
