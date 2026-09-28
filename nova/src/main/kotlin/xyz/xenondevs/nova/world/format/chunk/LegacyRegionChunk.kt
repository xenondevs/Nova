package xyz.xenondevs.nova.world.format.chunk

import net.minecraft.world.level.block.state.BlockState
import org.bukkit.block.Block
import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.util.ceilDiv
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.legacy.world.v2.LegacyBlockStateIdResolver
import java.util.*

internal class LegacyRegionChunk private constructor(
    val pos: ChunkPos,
    private val minSection: Int,
    private val sections: Array<RegionChunkSection<BlockState>>,
    val vanillaTileEntityData: Map<Block, Compound>,
    val tileEntityData: Map<Block, Compound>
) : RegionizedChunk {
    
    val isEmpty: Boolean
        get() = vanillaTileEntityData.isEmpty() &&
            tileEntityData.isEmpty() &&
            sections.all(RegionChunkSection<BlockState>::isEmpty)
    
    fun forEachBlock(action: (Block, BlockState) -> Unit) {
        for ([sectionIndex, section] in sections.withIndex()) {
            val sectionY = (minSection + sectionIndex) shl 4
            section.forEachNonEmpty { x, y, z, state ->
                action(
                    pos.world!!.getBlockAt((pos.x shl 4) + x, sectionY + y, (pos.z shl 4) + z),
                    state
                )
            }
        }
    }
    
    override fun write(writer: ByteWriter): Boolean =
        throw UnsupportedOperationException("Legacy region chunks are read-only")
    
    companion object : RegionizedChunkReader<LegacyRegionChunk>() {
        
        override fun read(pos: ChunkPos, reader: ByteReader): LegacyRegionChunk {
            val minSection = reader.readInt()
            val maxSection = reader.readInt()
            val sectionCount = maxSection - minSection
            val sectionBitmask = BitSet.valueOf(reader.readBytes(sectionCount.ceilDiv(8)))
            val sections = Array(sectionCount) { sectionIndex ->
                if (sectionBitmask.get(sectionIndex))
                    RegionChunkSection.read(LegacyBlockStateIdResolver, reader)
                else RegionChunkSection(LegacyBlockStateIdResolver)
            }
            
            return LegacyRegionChunk(
                pos,
                minSection,
                sections,
                readPosCompoundMap(pos, reader),
                readPosCompoundMap(pos, reader)
            )
        }
        
        override fun createEmpty(pos: ChunkPos): LegacyRegionChunk =
            LegacyRegionChunk(pos, 0, emptyArray(), emptyMap(), emptyMap())
        
        private fun readPosCompoundMap(chunkPos: ChunkPos, reader: ByteReader): Map<Block, Compound> {
            val map = HashMap<Block, Compound>()
            repeat(reader.readVarInt()) {
                val block = unpackBlockPos(chunkPos, reader.readInt())
                map[block] = requireNotNull(Cbf.read<Compound>(reader))
            }
            return map
        }
        
    }
    
}
