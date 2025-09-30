package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.Test
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import kotlin.test.assertEquals

class BlockStateDefinitionTest {
    
    @Test
    fun testGetMultipartModels() {
        val blockState = deserializeBlockState("oak_fence")
        
        assertEquals(
            listOf(listOf(BlockStateDefinition.Model(model = ResourcePath(ResourceType.Model, "minecraft", "block/oak_fence_post")))),
            blockState.getModels(mapOf())
        )
        
        assertEquals(
            listOf(
                BlockStateDefinition.Model(model = ResourcePath(ResourceType.Model, "minecraft", "block/oak_fence_post")),
                BlockStateDefinition.Model(
                    model = ResourcePath(ResourceType.Model, "minecraft", "block/oak_fence_side"),
                    uvLock = true
                ),
                BlockStateDefinition.Model(
                    model = ResourcePath(ResourceType.Model, "minecraft", "block/oak_fence_side"),
                    uvLock = true,
                    y = 270
                )
            ).map { listOf(it) },
            blockState.getModels(mapOf("north" to "true", "west" to "true"))
        )
    }
    
    @Test
    fun testGetMixedVariantMultipartModels() {
        val blockState = deserializeBlockState("mixed_variant_multipart")
        
        assertEquals(
            listOf(
                BlockStateDefinition.Model(
                    model = ResourcePath(ResourceType.Model, "minecraft", "block/mushroom_block_inside"),
                ),
                BlockStateDefinition.Model(
                    model = ResourcePath(ResourceType.Model, "minecraft", "block/mushroom_block_inside"),
                    y = 90
                ),
                BlockStateDefinition.Model(
                    model = ResourcePath(ResourceType.Model, "minecraft", "block/mushroom_block_inside"),
                    y = 180
                ),
                BlockStateDefinition.Model(
                    model = ResourcePath(ResourceType.Model, "minecraft", "block/mushroom_block_inside"),
                    y = 270
                ),
                BlockStateDefinition.Model(
                    model = ResourcePath(ResourceType.Model, "minecraft", "block/mushroom_block_inside"),
                    x = 270
                ),
                BlockStateDefinition.Model(
                    model = ResourcePath(ResourceType.Model, "minecraft", "block/mushroom_block_inside"),
                    x = 90,
                ),
            ).map { listOf(it) },
            blockState.getModels(mapOf(
                "north" to "false",
                "east" to "false",
                "south" to "false",
                "west" to "false",
                "up" to "false",
                "down" to "false"
            ))
        )
        
        assertEquals(
            listOf(listOf(BlockStateDefinition.Model(model = ResourcePath(ResourceType.Model, "minecraft", "block/emerald_block")))),
            blockState.getModels(mapOf(
                "north" to "true",
                "east" to "true",
                "south" to "true",
                "west" to "true",
                "up" to "true",
                "down" to "true"
            ))
        )
    }
    
    @OptIn(ExperimentalSerializationApi::class)
    private fun deserializeBlockState(name: String): BlockStateDefinition =
        javaClass.getResourceAsStream("/blockstates/$name.json")?.use { Json.decodeFromStream(it)!! }
            ?: throw IllegalArgumentException("BlockState $name not found")
    
}