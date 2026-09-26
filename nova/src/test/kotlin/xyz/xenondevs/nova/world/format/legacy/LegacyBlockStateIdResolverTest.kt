package xyz.xenondevs.nova.world.format.legacy

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import xyz.xenondevs.nova.world.format.legacy.world.v2.LegacyBlockStateIdResolver
import kotlin.test.assertEquals

class LegacyBlockStateIdResolverTest {
    
    @Test
    fun convertsNoteBlockToVanillaState() {
        val state = resolve(
            "nova:note_block",
            "nova:instrument" to "harp",
            "nova:note" to "12",
            "nova:powered" to "true"
        )
        
        assertEquals(Blocks.NOTE_BLOCK, state.block)
        assertProperties(state, "instrument" to "harp", "note" to "12", "powered" to "true")
    }
    
    @Test
    fun convertsTripwireToVanillaState() {
        val state = resolve(
            "nova:tripwire",
            "nova:north" to "true",
            "nova:east" to "false",
            "nova:south" to "true",
            "nova:west" to "false",
            "nova:attached" to "true",
            "nova:disarmed" to "false",
            "nova:powered" to "true"
        )
        
        assertEquals(Blocks.TRIPWIRE, state.block)
        assertProperties(
            state,
            "north" to "true",
            "east" to "false",
            "south" to "true",
            "west" to "false",
            "attached" to "true",
            "disarmed" to "false",
            "powered" to "true"
        )
    }
    
    @Test
    fun convertsLeavesToVanillaStates() {
        val blocks = mapOf(
            "nova:oak_leaves" to Blocks.OAK_LEAVES,
            "nova:spruce_leaves" to Blocks.SPRUCE_LEAVES,
            "nova:birch_leaves" to Blocks.BIRCH_LEAVES,
            "nova:jungle_leaves" to Blocks.JUNGLE_LEAVES,
            "nova:acacia_leaves" to Blocks.ACACIA_LEAVES,
            "nova:dark_oak_leaves" to Blocks.DARK_OAK_LEAVES,
            "nova:mangrove_leaves" to Blocks.MANGROVE_LEAVES,
            "nova:cherry_leaves" to Blocks.CHERRY_LEAVES,
            "nova:azalea_leaves" to Blocks.AZALEA_LEAVES,
            "nova:flowering_azalea_leaves" to Blocks.FLOWERING_AZALEA_LEAVES,
            "nova:pale_oak_leaves" to Blocks.PALE_OAK_LEAVES
        )
        
        for ([legacyId, vanillaBlock] in blocks) {
            val state = resolve(
                legacyId,
                "nova:distance" to "4",
                "nova:persistent" to "true",
                "nova:waterlogged" to "true"
            )
            
            assertEquals(vanillaBlock, state.block, legacyId)
            assertProperties(
                state,
                "distance" to "4",
                "persistent" to "true",
                "waterlogged" to "true"
            )
        }
    }
    
    private fun resolve(block: String, vararg properties: Pair<String, String>): BlockState =
        LegacyBlockStateIdResolver.resolve(JsonObject(mapOf(
            "block" to JsonPrimitive(block),
            "properties" to JsonObject(properties.associate { [key, value] -> key to JsonPrimitive(value) })
        )))
    
    private fun assertProperties(state: BlockState, vararg expected: Pair<String, String>) {
        for ([propertyName, value] in expected) {
            val property = requireNotNull(state.block.stateDefinition.getProperty(propertyName))
            assertEquals(value, getPropertyValue(state, property), propertyName)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun getPropertyValue(state: BlockState, property: Property<*>): String {
        val typedProperty = property as Property<Comparable<Any>>
        return typedProperty.getName(state.getValue(typedProperty))
    }
    
    companion object {
        
        @BeforeAll
        @JvmStatic
        fun bootstrapMinecraft() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
        
    }
    
}
