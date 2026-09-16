package xyz.xenondevs.nova.world.block.state.model

import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockType
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.util.CubeFaceSet
import xyz.xenondevs.nova.util.nmsBlock

internal abstract class SidedBackingStateConfig(
    val faces: CubeFaceSet,
    override val blockType: RegistryEntry.Paper<BlockType>
) : BackingStateConfig() {
    
    override val id = faces.data.toInt()
    override val waterlogged = false
    override val variantMap = CubeFaceSet.ALL.map { it.name.lowercase() to (it in faces).toString() }.toMap()
    override val vanillaBlockState: BlockState by blockType.map {
        it.nmsBlock.defaultBlockState()
            .setValue(BlockStateProperties.NORTH, BlockFace.NORTH in faces)
            .setValue(BlockStateProperties.EAST, BlockFace.EAST in faces)
            .setValue(BlockStateProperties.SOUTH, BlockFace.SOUTH in faces)
            .setValue(BlockStateProperties.WEST, BlockFace.WEST in faces)
            .setValue(BlockStateProperties.UP, BlockFace.UP in faces)
            .setValue(BlockStateProperties.DOWN, BlockFace.DOWN in faces)
    }
    override val maskedBlockState: BlockState by blockType.map { it.nmsBlock.defaultBlockState }
    
}

internal abstract class SidedBackingStateConfigType<T : SidedBackingStateConfig>(
    private val constructor: (CubeFaceSet) -> T,
    fileName: String
) : DefaultingBackingStateConfigType<T>(63, fileName) {
    
    override val blockedIds = setOf(63)
    override val defaultStateConfig = of(63)
    override val properties = hashSetOf("north", "east", "south", "west", "up", "down")
    override val isWaterloggable = false
    
    final override fun of(id: Int, waterlogged: Boolean): T {
        if (waterlogged)
            throw UnsupportedOperationException("${this.javaClass.simpleName} cannot be waterlogged")
        
        return constructor(CubeFaceSet(id))
    }
    
    final override fun of(properties: Map<String, String>): T {
        var faces = CubeFaceSet.NONE
        for ([faceName, enabled] in properties) {
            val face = BlockFace.valueOf(faceName.uppercase())
            if (enabled.toBoolean())
                faces += face
        }
        
        return constructor(faces)
    }
    
}
