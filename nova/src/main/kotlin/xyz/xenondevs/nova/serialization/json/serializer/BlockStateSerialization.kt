@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import kotlin.jvm.optionals.getOrNull

internal object BlockStateSerialization : TypeAdapter<BlockState>() {
    
    override fun write(writer: JsonWriter, blockState: BlockState) {
        val block = blockState.block
        val id = BuiltInRegistries.BLOCK.getKey(block)
        
        writer.beginObject()
        writer.name("id")
        writer.value(id.toString())
        for ((property, value) in blockState.values) {
            property as Property<Any>
            writer.name(property.name)
            writer.value(property.getName(value))
        }
        writer.endObject()
    }
    
    override fun read(reader: JsonReader): BlockState {
        reader.beginObject()
        reader.nextName()
        
        val id = ResourceLocation.parse(reader.nextString())
        var blockState = BuiltInRegistries.BLOCK.getValue(id).defaultBlockState()
        val properties = blockState.properties.associateBy { it.name }
        
        while (reader.peek() == JsonToken.NAME) {
            val propertyName = reader.nextName()
            val valueName = reader.nextString()
            blockState = blockState.setValue<String, String>(id, properties, propertyName, valueName)
        }
        
        reader.endObject()
        
        return blockState
    }
    
    private fun <T : Comparable<T>, V : T> BlockState.setValue(
        id: ResourceLocation,
        properties: Map<String, Property<*>>,
        propertyName: String,
        valueName: String
    ): BlockState {
        val property = properties[propertyName] as? Property<T>
            ?: throw NoSuchElementException("Block $id does not have property named $propertyName")
        val value = property.getValue(valueName).getOrNull() as? V
            ?: throw NoSuchElementException("Block $id property $propertyName does not have value named $valueName")
        
        return setValue(property, value)
    }
    
}