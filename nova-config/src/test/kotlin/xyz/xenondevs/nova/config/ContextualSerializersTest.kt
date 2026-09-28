package xyz.xenondevs.nova.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ContextualSerializersTest {
    
    @Serializable
    private enum class Foo { BAR }
    
    private class FooSerializer : KSerializer<Foo> {
        override val descriptor = String.serializer().descriptor
        override fun deserialize(decoder: Decoder) = throw UnsupportedOperationException()
        override fun serialize(encoder: Encoder, value: Foo) {
            encoder.encodeString("CONTEXTUAL")
        }
    }
    
    private val serializersModule = SerializersModule {
        contextual(Foo::class, FooSerializer())
    }
    
    @Test
    fun `contextualSerializer  prioritizes contextual over default serializer`() {
        assertEquals(""""BAR"""", Json.encodeToString(Foo.BAR))
        assertEquals(
            """"CONTEXTUAL"""",
            Json.encodeToString(serializersModule.contextualSerializer<Foo>(), Foo.BAR)
        )
    }
    
    @Test
    fun `contextualSerializer preserves nullability`() {
        val serializer = serializersModule.contextualSerializer<Foo?>()
        
        assertEquals("null", Json.encodeToString(serializer, null))
        assertEquals(""""CONTEXTUAL"""", Json.encodeToString(serializer, Foo.BAR))
    }
    
    @Test
    fun `contextualSerializer uses contextual serializer for generic arguments`() {
        val serializer = serializersModule.contextualSerializer<List<Foo>>()
        
        assertEquals(
            """["CONTEXTUAL"]""",
            Json.encodeToString(serializer, listOf(Foo.BAR))
        )
    }
    
    @Test
    fun `contextualSerializer recursively resolves generic arguments and nullability`() {
        val serializer = serializersModule.contextualSerializer<Map<String, List<Foo?>>>()
        
        assertEquals(
            """{"foo":["CONTEXTUAL",null]}""",
            Json.encodeToString(
                serializer,
                mapOf("foo" to listOf(Foo.BAR, null)),
            )
        )
    }
    
}