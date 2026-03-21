package xyz.xenondevs.nova.serialization.kotlinx

import kotlinx.serialization.json.Json
import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KeySerializerTest {
    
    @Test
    fun `serialize Key`() {
        val key = Key.key("nova", "test")
        val json = Json.encodeToString(KeySerializer, key)
        assertEquals(""""nova:test"""", json)
    }
    
    @Test
    fun `deserialize Key`() {
        val json = """"nova:test""""
        val key = Json.decodeFromString(KeySerializer, json)
        assertEquals(Key.key("nova", "test"), key)
    }
    
    @Test
    fun `serialize NamespacedKey`() {
        val key = NamespacedKey.minecraft("test")
        val json = Json.encodeToString(NamespacedKeySerializer, key)
        assertEquals(""""minecraft:test"""", json)
    }
    
    @Test
    fun `deserialize NamespacedKey`() {
        val json = """"minecraft:test""""
        val key = Json.decodeFromString(NamespacedKeySerializer, json)
        assertEquals(NamespacedKey.minecraft("test"), key)
    }
    
}
