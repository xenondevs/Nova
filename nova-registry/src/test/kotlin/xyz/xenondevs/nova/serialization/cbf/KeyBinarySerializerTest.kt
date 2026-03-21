package xyz.xenondevs.nova.serialization.cbf

import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.junit.jupiter.api.Test
import xyz.xenondevs.cbf.serializer.read
import xyz.xenondevs.cbf.serializer.write
import kotlin.test.assertEquals

class KeyBinarySerializerTest {
    
    @Test
    fun `round-trip Key`() {
        val key = Key.key("some_namespace", "some/key_value")
        val bytes = KeyBinarySerializer.write(key)
        val deserialized = KeyBinarySerializer.read(bytes)!!
        assertEquals(key, deserialized)
    }
    
    @Test
    fun `round-trip NamespacedKey`() {
        val key = NamespacedKey("some_namespace", "some/key_value")
        val bytes = NamespacedKeyBinarySerializer.write(key)
        val deserialized = NamespacedKeyBinarySerializer.read(bytes)!!
        assertEquals(key, deserialized)
    }
    
}

