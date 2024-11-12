package xyz.xenondevs.nova.resources.layout.block

import org.bukkit.Axis
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.model.Model
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.resources.builder.task.model.ModelContent
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.property.BlockStateProperty
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties

@RegistryElementBuilderDsl
open class BlockSelectorScope internal constructor(
    private val blockState: NovaBlockState
) {
    
    /**
     * Checks whether the current block state has the given [property].
     */
    fun <T : Any> hasProperty(property: BlockStateProperty<T>): Boolean =
        property in blockState.properties
    
    /**
     * Gets the value of the given [property] of the current block state or
     * null if the current block state does not have the given [property].
     */
    fun <T : Any> getPropertyValueOrNull(property: BlockStateProperty<T>): T? =
        blockState[property]
    
    /**
     * Gets the value of the given [property] of the current block state or
     * throws an exception if the current block state does not have the given [property].
     */
    fun <T : Any> getPropertyValueOrThrow(property: BlockStateProperty<T>): T =
        getPropertyValueOrNull(property) ?: throw IllegalArgumentException("$blockState does not have property $property")
    
}

class BlockModelSelectorScope internal constructor(
    blockState: NovaBlockState,
    val resourcePackBuilder: ResourcePackBuilder,
    val modelContent: ModelContent
) : BlockSelectorScope(blockState) {
    
    /**
     * The ID of the block.
     */
    val id = blockState.block.id
    
    /**
     * The default model for this block under `namespace:block/name` or a new model
     * with parent `minecraft:block/cube_all` and `"all": "namespace:block/name"`.
     */
    val defaultModel: ModelBuilder by lazy {
        val path = ResourcePath(id.namespace, "block/${id.path}")
        modelContent[path]
            ?.let(::ModelBuilder)
            ?: createCubeModel(ResourcePath(id.namespace, "block/${id.path}"))
    }
    
    /**
     * Gets the model under the given [path] or throws an exception if it does not exist.
     */
    fun getModel(path: ResourcePath): ModelBuilder =
        modelContent[path]
            ?.let(::ModelBuilder)
            ?: throw IllegalArgumentException("Model $path does not exist")
    
    /**
     * Gets the model under the given [path] after or throws an exception if it does not exist.
     */
    fun getModel(path: String): ModelBuilder =
        getModel(ResourcePath.of(path, id.namespace))
    
    /**
     * Rotates the builder based on the built-in facing [BlockStateProperties][BlockStateProperty]:
     * [DefaultBlockStateProperties.FACING], [DefaultBlockStateProperties.AXIS]
     */
    fun ModelBuilder.rotated(uvLock: Boolean = false): ModelBuilder =
        when (getPropertyValueOrNull(DefaultBlockStateProperties.FACING)
            ?: getPropertyValueOrNull(DefaultBlockStateProperties.AXIS)
        ) {
            BlockFace.NORTH, Axis.Z -> this
            BlockFace.NORTH_NORTH_WEST -> rotateY(22.5, uvLock)
            BlockFace.NORTH_WEST -> rotateY(45.0, uvLock)
            BlockFace.WEST_NORTH_WEST -> rotateY(67.5, uvLock)
            BlockFace.WEST -> rotateY(90.0, uvLock)
            BlockFace.WEST_SOUTH_WEST -> rotateY(112.5, uvLock)
            BlockFace.SOUTH_WEST -> rotateY(135.0, uvLock)
            BlockFace.SOUTH_SOUTH_WEST -> rotateY(157.5, uvLock)
            BlockFace.SOUTH -> rotateY(180.0, uvLock)
            BlockFace.SOUTH_SOUTH_EAST -> rotateY(202.5, uvLock)
            BlockFace.SOUTH_EAST -> rotateY(225.0, uvLock)
            BlockFace.EAST_SOUTH_EAST -> rotateY(247.5, uvLock)
            BlockFace.EAST, Axis.X -> rotateY(270.0, uvLock)
            BlockFace.EAST_NORTH_EAST -> rotateY(292.5, uvLock)
            BlockFace.NORTH_EAST -> rotateY(315.0, uvLock)
            BlockFace.NORTH_NORTH_EAST -> rotateY(337.5, uvLock)
            BlockFace.UP -> rotateX(-90.0, uvLock)
            BlockFace.DOWN, Axis.Y -> rotateX(90.0, uvLock)
            else -> this
        }
    
    // TODO: utility methods to generate cube models from textures
    
    fun createCubeModel(all: String): ModelBuilder =
        createCubeModel(ResourcePath.of(all, id.namespace))
    
    fun createCubeModel(all: ResourcePath): ModelBuilder = ModelBuilder(
        Model(
            parent = ResourcePath("minecraft", "block/cube_all"),
            textures = mapOf("all" to all.toString())
        )
    )
    
}