package xyz.xenondevs.nova.world.format.legacy.v1

import org.bukkit.World
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions
import xyz.xenondevs.nova.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.UUIDUtils
import xyz.xenondevs.nova.util.get
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.DefaultBlocks
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties
import xyz.xenondevs.nova.world.format.RegionFile
import xyz.xenondevs.nova.world.format.RegionizedFileReader
import xyz.xenondevs.nova.world.format.chunk.RegionChunk
import xyz.xenondevs.nova.world.format.legacy.LegacyConversionException
import xyz.xenondevs.nova.world.format.legacy.LegacyRegionizedFileReader
import java.io.File

internal object LegacyRegionFileReaderV1 : LegacyRegionizedFileReader<RegionChunk, RegionFile> {
    
    override fun read(file: File, reader: ByteReader, world: World, regionX: Int, regionZ: Int): RegionFile {
        val chunks = Array(1024) { RegionChunk.createEmpty(RegionizedFileReader.chunkIdxToPos(it, world, regionX, regionZ)) }
        while (reader.readByte() == 1.toByte()) {
            val chunk = chunks[reader.readUnsignedShort().toInt()]
            val data = reader.readBytes(reader.readVarInt())
            populateChunk(ByteReader.fromByteArray(data), chunk)
        }
        
        return RegionFile(file, world, regionX, regionZ, chunks)
    }
    
    private fun populateChunk(reader: ByteReader, chunk: RegionChunk) {
        val chunkPos = chunk.pos
        while (reader.readByte().toInt() == 1) {
            val relPos = reader.readUnsignedByte().toInt()
            val relX = relPos shr 4
            val relZ = relPos and 0xF
            val y = reader.readVarInt()
            val pos = BlockPos(chunkPos.world!!, (chunkPos.x shl 4) + relX, y, (chunkPos.z shl 4) + relZ)
            val type = reader.readString()
            reader.readVarInt() // data length, ignored
            
            val isVanillaBlock = type.startsWith("minecraft:")
            var blockType: NovaBlock? = null
            
            if (!isVanillaBlock && NovaRegistries.BLOCK[type]?.also { blockType = it } == null) {
                throw LegacyConversionException("Could not load block at $pos: Unknown id $type")
            }
            
            if (isVanillaBlock) {
                readPopulateVanilla(reader, pos, type, chunk)
            } else {
                readPopulateNova(reader, pos, blockType!!, chunk)
            }
        }
    }
    
    private fun readPopulateVanilla(reader: ByteReader, pos: BlockPos, type: String, chunk: RegionChunk) {
        val data = CBF.read<Compound>(reader)!!
        when (type) {
            "minecraft:note_block" -> {
                val blockState = DefaultBlocks.NOTE_BLOCK.defaultBlockState
                    .with(DefaultBlockStateProperties.NOTE_BLOCK_INSTRUMENT, data["instrument"]!!)
                    .with(DefaultBlockStateProperties.NOTE_BLOCK_NOTE, data["note"]!!)
                    .with(DefaultBlockStateProperties.POWERED, data["powered"]!!)
                chunk.setBlockState(pos, blockState)
            }
            else -> chunk.setVanillaTileEntityData(pos, data)
        }
    }
    
    private fun readPopulateNova(reader: ByteReader, pos: BlockPos, type: NovaBlock, chunk: RegionChunk) {
        val compound = CBF.read<Compound>(reader)!!
        val blockFacing = compound.get<BlockFace>("facing") // facing was the only built-in block property
        
        // place context is used to determine correct block state
        val placeCtx = Context.intention(DefaultContextIntentions.BlockPlace)
            .param(DefaultContextParamTypes.BLOCK_POS, pos)
            .param(DefaultContextParamTypes.BLOCK_TYPE_NOVA, type)
            .param(DefaultContextParamTypes.SOURCE_DIRECTION, (blockFacing ?: BlockFace.NORTH).oppositeFace.direction)
            .build()
        
        val blockState = type.chooseBlockState(placeCtx)
        chunk.setBlockState(pos, blockState)
        
        if (type is NovaTileEntityBlock) {
            val uuid = reader.readUUID()
            val ownerUUID = reader.readUUID().takeUnless(UUIDUtils.ZERO::equals)
            val data = CBF.read<Compound>(reader)!!
            
            data["uuid"] = uuid
            data["ownerUuid"] = ownerUUID
            
            val tileEntity = type.tileEntityConstructor(pos, blockState, data)
            chunk.setTileEntity(pos, tileEntity)
        }
    }
    
}