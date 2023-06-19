package xyz.xenondevs.nova.data.resources.builder.task.material.info

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.resources.model.blockstate.BlockStateConfigType
import xyz.xenondevs.nova.data.resources.model.blockstate.BrownMushroomBlockStateConfig
import xyz.xenondevs.nova.data.resources.model.blockstate.MushroomStemBlockStateConfig
import xyz.xenondevs.nova.data.resources.model.blockstate.NoteBlockStateConfig
import xyz.xenondevs.nova.data.resources.model.blockstate.RedMushroomBlockStateConfig

internal enum class BlockModelType(vararg val configTypes: BlockStateConfigType<*>?) {
    
    DEFAULT(null),
    SOLID(RedMushroomBlockStateConfig, BrownMushroomBlockStateConfig, MushroomStemBlockStateConfig, NoteBlockStateConfig, null);
    
}

internal enum class BlockDirection(val char: Char, val x: Int, val y: Int) {
    
    NORTH('n', 0, 0),
    EAST('e', 0, 90),
    SOUTH('s', 0, 180),
    WEST('w', 0, 270),
    UP('u', -90, 0),
    DOWN('d', 90, 0);
    
    val blockFace = BlockFace.valueOf(name)
    
    companion object {
        
        fun of(s: String): List<BlockDirection> {
            if (s.equals("all", true))
                return values().toList()
            
            return s.toCharArray().map { c -> BlockDirection.values().first { it.char == c } }
        }
        
    }
    
}

internal class BlockModelInformation(
    override val id: ResourceLocation,
    type: BlockModelType?,
    hitboxType: Material?,
    override val models: List<String>,
    directions: List<BlockDirection>?,
    val priority: Int
) : ModelInformation {
    
    val type = type ?: BlockModelType.DEFAULT
    val hitboxType = hitboxType ?: DEFAULT_HITBOX_TYPE
    val directions = directions ?: listOf(BlockDirection.NORTH)
    
    companion object {
        val DEFAULT_HITBOX_TYPE = Material.BARRIER
    }
    
}