package xyz.xenondevs.nova.serialization.kotlinx

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.tag.TagKey
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import net.kyori.adventure.key.Key.key
import org.bukkit.inventory.ItemType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TagKeySerializerTest {
    
    companion object {
        
        private lateinit var serializer: TagKeySerializer<ItemType>
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            serializer = TagKeySerializer(RegistryKey.ITEM)
        }
        
    }
    
    @Test
    fun `serialize TagKey`() {
        val tagKey = TagKey.create(RegistryKey.ITEM, key("minecraft", "wool"))
        val json = Json.encodeToString(serializer, tagKey)
        assertEquals(""""#minecraft:wool"""", json)
    }
    
    @Test
    fun `deserialize TagKey`() {
        val json = """"#minecraft:wool""""
        val tagKey = Json.decodeFromString(serializer, json)
        assertEquals(TagKey.create(RegistryKey.ITEM, key("minecraft", "wool")), tagKey)
    }
    
    @Test
    fun `deserialize TagKey without hash prefix fails`() {
        val json = """"minecraft:wool""""
        assertFailsWith<SerializationException> {
            Json.decodeFromString(serializer, json)
        }
    }
    
}


