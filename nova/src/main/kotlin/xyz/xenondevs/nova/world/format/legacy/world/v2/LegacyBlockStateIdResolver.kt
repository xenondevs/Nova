package xyz.xenondevs.nova.world.format.legacy.world.v2

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.property.BlockStateProperty
import xyz.xenondevs.nova.world.format.IdResolver
import java.util.*

private const val ID_MAP_KEY = "block_state_id_map"

private val LEGACY_VANILLA_BLOCKS = mapOf(
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

private val LEGACY_PROPERTY_ALIASES = mapOf(
    "nova:facing" to setOf("nova:facing", "minecraft:facing", "minecraft:horizontal_facing", "minecraft:rotation"),
    "nova:axis" to setOf("minecraft:axis", "minecraft:horizontal_axis"),
    "nova:waterlogged" to setOf("minecraft:waterlogged"),
    "nova:powered" to setOf("minecraft:powered")
)

internal object LegacyBlockStateIdResolver : IdResolver<BlockState> {
    
    private val serializedStates: Map<Int, JsonObject> by lazy {
        PermanentStorage.retrieve(ID_MAP_KEY)
            ?: throw IllegalStateException("Legacy region files exist, but the legacy block-state id map is missing")
    }
    private val resolvedStates = HashMap<Int, BlockState>()
    
    override val size: Int
        get() = serializedStates.size
    
    override fun toId(value: BlockState?): Int =
        throw UnsupportedOperationException("Legacy block states are read-only")
    
    override fun fromId(id: Int): BlockState? {
        val serializedState = serializedStates[id] ?: return null
        return resolvedStates.getOrPut(id) { resolve(serializedState) }
    }
    
    internal fun resolve(serializedState: JsonObject): BlockState {
        val blockId = serializedState.getValue("block").jsonPrimitive.content
        val properties = serializedState["properties"]?.jsonObject ?: JsonObject(emptyMap())
        
        val vanillaBlock = LEGACY_VANILLA_BLOCKS[blockId]
        if (vanillaBlock != null)
            return resolveVanillaState(vanillaBlock, properties)
        
        val block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockId)) as? NovaBlock
            ?: throw IllegalArgumentException("Nova block $blockId is no longer registered")
        
        var state = block.defaultBlockState()
        for ([propertyId, propertyValue] in properties) {
            val value = propertyValue.jsonPrimitive.content
            val property = findProperty(block, propertyId, value)
                ?: throw IllegalArgumentException(
                    "Nova block $blockId no longer has a property compatible with $propertyId=$value"
                )
            state = setProperty(state, propertyId, property, value)
        }
        return state
    }
    
    private fun resolveVanillaState(block: Block, properties: JsonObject): BlockState {
        var state = block.defaultBlockState()
        for ([propertyId, propertyValue] in properties) {
            val propertyName = propertyId.substringAfter(':')
            val property = block.stateDefinition.getProperty(propertyName)
                ?: throw IllegalArgumentException("Vanilla block $block has no property $propertyName")
            state = setVanillaProperty(state, property, propertyValue.jsonPrimitive.content)
        }
        return state
    }
    
    private fun findProperty(block: NovaBlock, legacyId: String, legacyValue: String): BlockStateProperty<*>? {
        val acceptedIds = LEGACY_PROPERTY_ALIASES[legacyId] ?: setOf(legacyId)
        return block.stateProperties.firstOrNull { property ->
            property.key.asString() in acceptedIds && canReadLegacyValue(legacyId, property, legacyValue)
        }
    }
    
    private fun canReadLegacyValue(
        legacyId: String,
        property: BlockStateProperty<*>,
        value: String
    ): Boolean {
        if (legacyId == "nova:facing" && property.key.asString() == "minecraft:rotation")
            return runCatching { BlockFace.valueOf(value.uppercase(Locale.ROOT)) }.isSuccess
        return property.isValidString(value)
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun setProperty(
        state: BlockState,
        legacyId: String,
        property: BlockStateProperty<*>,
        value: String
    ): BlockState {
        val typedProperty = property as BlockStateProperty<Comparable<Any>>
        val typedValue = if (legacyId == "nova:facing" && property.key.asString() == "minecraft:rotation") {
            BlockFace.valueOf(value.uppercase(Locale.ROOT))
        } else {
            typedProperty.stringToValue(value)
        }
        return typedProperty.set(state, typedValue as Comparable<Any>)
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun setVanillaProperty(state: BlockState, property: Property<*>, value: String): BlockState {
        val typedProperty = property as Property<Comparable<Any>>
        val typedValue = typedProperty.getValue(value).orElseThrow {
            IllegalArgumentException("Value $value is not valid for vanilla property " + property.name)
        }
        return state.setValue(typedProperty, typedValue)
    }
    
}
