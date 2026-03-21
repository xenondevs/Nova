package xyz.xenondevs.nova.serialization.kotlinx

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import kotlinx.serialization.json.Json
import net.kyori.adventure.key.Key.key
import org.bukkit.inventory.ItemType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TypedKeySerializerTest {
    
    companion object {
        
        private lateinit var serializer: TypedKeySerializer<ItemType>
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            serializer = TypedKeySerializer(RegistryKey.ITEM)
        }
        
    }
    
    @Test
    fun `serialize TypedKey`() {
        val typedKey = TypedKey.create(RegistryKey.ITEM, key("minecraft", "diamond"))
        val json = Json.encodeToString(serializer, typedKey)
        assertEquals(""""minecraft:diamond"""", json)
    }
    
    @Test
    fun `deserialize TypedKey`() {
        val json = """"minecraft:diamond""""
        val typedKey = Json.decodeFromString(serializer, json)
        assertEquals(TypedKey.create(RegistryKey.ITEM, key("minecraft", "diamond")), typedKey)
    }
    
}

