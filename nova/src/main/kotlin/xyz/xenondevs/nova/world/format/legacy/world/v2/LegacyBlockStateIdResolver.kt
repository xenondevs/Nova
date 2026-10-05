package xyz.xenondevs.nova.world.format.legacy.world.v2

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.kyori.adventure.key.Key
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import xyz.xenondevs.nova.registry.KnownRegistryEntries.BlockConfiguration
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.format.IdResolver

private val LEGACY_VANILLA_BLOCKS by lazy {
    mapOf(
        "nova:note_block" to Blocks.NOTE_BLOCK,
        "nova:tripwire" to Blocks.TRIPWIRE,
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
}

private val HORIZONTAL_FACING_VALUES = setOf("north", "east", "south", "west")
private val CARTESIAN_FACING_VALUES = HORIZONTAL_FACING_VALUES + setOf("up", "down")
private val ROTATION_VALUES = listOf(
    "south", "south_south_west", "south_west", "west_south_west",
    "west", "west_north_west", "north_west", "north_north_west",
    "north", "north_north_east", "north_east", "east_north_east",
    "east", "east_south_east", "south_east", "south_south_east"
)
private val ROTATION_VALUE_SET = ROTATION_VALUES.toSet()
private val PROPERTY_NAME_SANITIZER = Regex("""[:_\-./]""")

internal class LegacyBlockStateIdResolver(
    private val serializedStates: Map<Int, JsonObject>
) : IdResolver<BlockState> {
    
    private val convertedProperties = serializedStates.values
        .groupBy { it.getValue("block").jsonPrimitive.content }
        .mapValues { [_, states] -> convertProperties(states) }
    private val resolvedStates = Int2ObjectOpenHashMap<BlockState>()
    
    override val size: Int
        get() = serializedStates.size
    
    override fun fromId(id: Int): BlockState? {
        val serializedState = serializedStates[id] ?: return null
        return resolvedStates.getOrPut(id) { resolve(serializedState) }
    }
    
    private fun resolve(serializedState: JsonObject): BlockState {
        val blockId = serializedState.getValue("block").jsonPrimitive.content
        val vanillaBlock = LEGACY_VANILLA_BLOCKS[blockId]
        val block = vanillaBlock
            ?: BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockId)) as? NovaBlock
            ?: throw IllegalArgumentException("Nova block $blockId is no longer registered")
        val definitions = if (vanillaBlock == null) convertedProperties.getValue(blockId) else null
        
        var state = block.defaultBlockState()
        for ([propertyId, propertyValue] in serializedState["properties"]?.jsonObject.orEmpty()) {
            val name = definitions?.getValue(propertyId)?.nmsName ?: propertyId.substringAfter(':')
            val property = block.stateDefinition.getProperty(name)
                ?: throw IllegalArgumentException("Block $blockId has no property $name")
            val value = convertPropertyValue(propertyId, name, propertyValue.jsonPrimitive.content)
            state = setProperty(state, property, value)
        }
        return state
    }
    
    private fun <T : Comparable<T>> setProperty(state: BlockState, property: Property<T>, value: String): BlockState {
        val typedValue = property.getValue(value).orElseThrow {
            IllegalArgumentException("Value $value is not valid for property ${property.name}")
        }
        return state.setValue(property, typedValue)
    }
    
    override fun toId(value: BlockState?) = throw UnsupportedOperationException()
    
    companion object {
        
        fun convertProperties(states: Collection<JsonObject>): Map<String, BlockConfiguration.Property> {
            val properties = LinkedHashMap<String, MutableSet<String>>()
            for (state in states) {
                for ([id, value] in state["properties"]?.jsonObject.orEmpty()) {
                    properties.getOrPut(id, ::LinkedHashSet) += value.jsonPrimitive.content
                }
            }
            return properties.mapValues { [id, values] -> convertProperty(id, values) }
        }
        
        private fun convertProperty(id: String, values: Set<String>): BlockConfiguration.Property {
            val vanillaName = when (id) {
                "nova:axis" -> "axis"
                "nova:waterlogged" -> "waterlogged"
                "nova:powered" -> "powered"
                "nova:facing" -> when (values) {
                    HORIZONTAL_FACING_VALUES, CARTESIAN_FACING_VALUES -> "facing"
                    ROTATION_VALUE_SET -> "rotation"
                    else -> null
                }
                
                else -> null
            }
            val name = vanillaName ?: id.replace(PROPERTY_NAME_SANITIZER, "_")
            return BlockConfiguration.Property(
                if (vanillaName != null) Key.key("minecraft", vanillaName) else Key.key(id),
                name,
                values.map { convertPropertyValue(id, name, it) }
            )
        }
        
        private fun convertPropertyValue(id: String, name: String, value: String): String =
            if (id == "nova:facing" && name == "rotation") ROTATION_VALUES.indexOf(value).toString() else value
        
    }
    
}
