package xyz.xenondevs.nova.serialization.cbf

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.set.RegistrySet
import io.papermc.paper.registry.tag.Tag
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key.key
import org.bukkit.inventory.ItemType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import xyz.xenondevs.cbf.serializer.read
import xyz.xenondevs.cbf.serializer.write
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RegistryKeySetBinarySerializerTest {
    
    companion object {
        
        private lateinit var serializer: RegistryKeySetBinarySerializer<ItemType>
        
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
            serializer = RegistryKeySetBinarySerializer(RegistryKey.ITEM)
        }
        
    }
    
    @Test
    fun `round-trip RegistryKeySet Direct (single)`() {
        val set = RegistrySet.keySet(RegistryKey.ITEM, listOf(TypedKey.create(RegistryKey.ITEM, key("minecraft", "diamond"))))
        val bytes = serializer.write(set)
        val deserialized = serializer.read(bytes)!!
        
        assertEquals(set, deserialized)
    }
    
    @Test
    fun `round-trip RegistryKeySet Direct (multi)`() {
        val set = RegistrySet.keySet(RegistryKey.ITEM, listOf(
            TypedKey.create(RegistryKey.ITEM, key("minecraft", "diamond")),
            TypedKey.create(RegistryKey.ITEM, key("minecraft", "emerald"))
        ))
        val bytes = serializer.write(set)
        val deserialized = serializer.read(bytes)!!
        
        assertEquals(set, deserialized)
    }
    
    @Test
    fun `round-trip RegistryKeySet Tag`() {
        val tag = RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.ITEM)
            .getTag(TagKey.create(RegistryKey.ITEM, key("minecraft", "wool")))
        val bytes = serializer.write(tag)
        val deserialized = serializer.read(bytes)!!
        
        assertIs<Tag<ItemType>>(deserialized)
        assertEquals(tag, deserialized)
    }
    
}


