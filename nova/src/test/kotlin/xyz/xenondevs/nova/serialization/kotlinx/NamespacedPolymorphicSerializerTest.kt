package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NamespacedPolymorphicSerializerTest {
    
    private val serializer = NamespacedPolymorphicSerializer(Value.serializer(), "minecraft")
    
    @Test
    fun `decodes non-namespaced type`() {
        assertEquals(Value.Example(1), Json.decodeFromString(serializer, """{"type":"example","value":1}"""))
    }
    
    @Test
    fun `decodes namespaced type`() {
        assertEquals(Value.Example(1), Json.decodeFromString(serializer, """{"type":"minecraft:example","value":1}"""))
    }
    
    @Test
    fun `encodes namespaced type`() {
        assertEquals("""{"type":"minecraft:example","value":1}""", Json.encodeToString(serializer, Value.Example(1)))
    }
    
    @Serializable
    private sealed interface Value {
        
        @Serializable
        @SerialName("minecraft:example")
        data class Example(val value: Int) : Value
        
    }
    
}
