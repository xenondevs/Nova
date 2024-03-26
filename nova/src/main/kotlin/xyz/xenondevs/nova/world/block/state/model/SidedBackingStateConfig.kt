package xyz.xenondevs.nova.world.block.state.model

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.mapToBooleanArray
import xyz.xenondevs.nova.util.MathUtils

private val POSSIBLE_FACES = arrayOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)

internal abstract class SidedBackingStateConfig(val faces: List<BlockFace>, block: Block) : BackingStateConfig() {
    
    override val id = getIdOf(faces)
    override val variantString = POSSIBLE_FACES.joinToString(",") { "${it.name.lowercase()}=${it in faces}" }
    override val vanillaBlockState: BlockState = block.defaultBlockState()
        .setValue(BlockStateProperties.NORTH, BlockFace.NORTH in faces)
        .setValue(BlockStateProperties.EAST, BlockFace.EAST in faces)
        .setValue(BlockStateProperties.SOUTH, BlockFace.SOUTH in faces)
        .setValue(BlockStateProperties.WEST, BlockFace.WEST in faces)
        .setValue(BlockStateProperties.UP, BlockFace.UP in faces)
        .setValue(BlockStateProperties.DOWN, BlockFace.DOWN in faces)
    
    companion object {
        fun getIdOf(faces: Collection<BlockFace>): Int {
            return MathUtils.convertBooleanArrayToInt(POSSIBLE_FACES.mapToBooleanArray { it in faces })
        }
    }
    
}

internal abstract class SidedBackingStateConfigType<T : SidedBackingStateConfig>(
    private val constructor: (List<BlockFace>) -> T
) : DefaultingBackingStateConfigType<T>() {
    
    override val maxId = 63
    override val blockedIds = setOf(63)
    override val defaultStateConfig = of(63)
    
    final override fun of(id: Int): T {
        var i = id
        val faces = ArrayList<BlockFace>()
        repeat(POSSIBLE_FACES.size) {
            if (i and 1 == 1)
                faces += POSSIBLE_FACES[POSSIBLE_FACES.lastIndex - it]
            
            i = i shr 1
        }
        
        return constructor(faces)
    }
    
    final override fun of(variantString: String): T {
        val faces = variantString.split(',').mapNotNull {
            val s = it.split('=')
            if (s[1].toBoolean())
                BlockFace.valueOf(s[0].uppercase())
            else null
        }
        
        return constructor(faces)
    }
    
}