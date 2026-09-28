package xyz.xenondevs.nova.serialization.kotlinx

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.set.RegistrySet
import io.papermc.paper.registry.tag.Tag
import io.papermc.paper.registry.tag.TagKey
import kotlinx.serialization.json.Json
import net.kyori.adventure.key.Key.key
import org.bukkit.inventory.ItemType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RegistryKeySetSerializerTest {
    
    companion object {
        
        private lateinit var serializer: RegistryKeySetSerializer<ItemType>
        
        @JvmStatic
        @BeforeAll
        fun mockBukkitSetUp() {
            MockBukkit.mock()
        }
        
        @JvmStatic
        @AfterAll
        fun mockBukkitTearDown() {
            MockBukkit.unmock()
        }
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            serializer = RegistryKeySetSerializer(RegistryKey.ITEM)
        }
        
    }
    
    @Test
    fun `serialize RegistryKeySet Direct (single)`() {
        val set = RegistrySet.keySet(RegistryKey.ITEM, listOf(TypedKey.create(RegistryKey.ITEM, key("minecraft", "diamond"))))
        val json = Json.encodeToString(serializer, set)
        assertEquals(""""minecraft:diamond"""", json)
    }
    
    @Test
    fun `serialize RegistryKeySet Direct (multi)`() {
        val set = RegistrySet.keySet(RegistryKey.ITEM, listOf(
            TypedKey.create(RegistryKey.ITEM, key("minecraft", "diamond")),
            TypedKey.create(RegistryKey.ITEM, key("minecraft", "emerald"))
        ))
        val json = Json.encodeToString(serializer, set)
        assertEquals("""["minecraft:diamond","minecraft:emerald"]""", json)
    }
    
    @Test
    fun `serialize RegistryKeySet Tag`() {
        val tag = RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.ITEM)
            .getTag(TagKey.create(RegistryKey.ITEM, key("minecraft", "wool")))
        val json = Json.encodeToString(serializer, tag)
        assertEquals(""""#minecraft:wool"""", json)
    }
    
    @Test
    fun `deserialize RegistryKeySet Direct (single)`() {
        val json = """"minecraft:diamond""""
        val set = Json.decodeFromString(serializer, json)
        assertEquals(
            RegistrySet.keySet(RegistryKey.ITEM, listOf(TypedKey.create(RegistryKey.ITEM, key("minecraft", "diamond")))),
            set
        )
    }
    
    @Test
    fun `deserialize RegistryKeySet Direct (multi)`() {
        val json = """["minecraft:diamond","minecraft:emerald"]"""
        val set = Json.decodeFromString(serializer, json)
        assertEquals(
            RegistrySet.keySet(RegistryKey.ITEM, listOf(
                TypedKey.create(RegistryKey.ITEM, key("minecraft", "diamond")),
                TypedKey.create(RegistryKey.ITEM, key("minecraft", "emerald"))
            )),
            set
        )
    }
    
    @Test
    fun `deserialize RegistryKeySet Tag`() {
        val json = """"#minecraft:wool""""
        val set = Json.decodeFromString(serializer, json)
        assertIs<Tag<ItemType>>(set)
    }
    
}

