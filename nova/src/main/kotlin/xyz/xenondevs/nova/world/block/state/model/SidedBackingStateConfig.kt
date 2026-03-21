package xyz.xenondevs.nova.world.block.state.model

import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockType
import xyz.xenondevs.commons.collections.enumSet
import xyz.xenondevs.commons.collections.mapToBooleanArray
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.util.MathUtils
import xyz.xenondevs.nova.util.nmsBlock

private val POSSIBLE_FACES = arrayOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)

internal abstract class SidedBackingStateConfig(
    val faces: Set<BlockFace>,
    override val blockType: RegistryEntry.Paper<BlockType>
) : BackingStateConfig() {
    
    override val id = getIdOf(faces)
    override val waterlogged = false
    override val variantMap = POSSIBLE_FACES.associate { it.name.lowercase() to (it in faces).toString() }
    override val vanillaBlockState: BlockState by blockType.map {
        it.nmsBlock.defaultBlockState()
            .setValue(BlockStateProperties.NORTH, BlockFace.NORTH in faces)
            .setValue(BlockStateProperties.EAST, BlockFace.EAST in faces)
            .setValue(BlockStateProperties.SOUTH, BlockFace.SOUTH in faces)
            .setValue(BlockStateProperties.WEST, BlockFace.WEST in faces)
            .setValue(BlockStateProperties.UP, BlockFace.UP in faces)
            .setValue(BlockStateProperties.DOWN, BlockFace.DOWN in faces)
    }
    
    companion object {
        fun getIdOf(faces: Collection<BlockFace>): Int {
            return MathUtils.convertBooleanArrayToInt(POSSIBLE_FACES.mapToBooleanArray { it in faces })
        }
    }
    
}

internal abstract class SidedBackingStateConfigType<T : SidedBackingStateConfig>(
    private val constructor: (Set<BlockFace>) -> T,
    fileName: String
) : DefaultingBackingStateConfigType<T>(63, fileName) {
    
    override val blockedIds = setOf(63)
    override val defaultStateConfig = of(63)
    override val properties = hashSetOf("north", "east", "south", "west", "up", "down")
    override val isWaterloggable = false
    
    final override fun of(id: Int, waterlogged: Boolean): T {
        if (waterlogged)
            throw UnsupportedOperationException("${this.javaClass.simpleName} cannot be waterlogged")
        
        var i = id
        val faces = enumSet<BlockFace>()
        repeat(POSSIBLE_FACES.size) {
            if (i and 1 == 1)
                faces += POSSIBLE_FACES[POSSIBLE_FACES.lastIndex - it]
            
            i = i shr 1
        }
        
        return constructor(faces)
    }
    
    final override fun of(properties: Map<String, String>): T {
        val faces = properties.entries.mapNotNullTo(enumSet()) { (face, enabled) ->
            BlockFace.valueOf(face.uppercase()).takeIf { enabled.toBoolean() }
        }
        
        return constructor(faces)
    }
    
}