package xyz.xenondevs.nova.serialization.cbf

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key.key
import org.bukkit.inventory.ItemType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import xyz.xenondevs.cbf.serializer.read
import xyz.xenondevs.cbf.serializer.write
import kotlin.test.assertEquals

class TagKeyBinarySerializerTest {
    
    companion object {
        
        private lateinit var serializer: TagKeyBinarySerializer<ItemType>
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            serializer = TagKeyBinarySerializer(RegistryKey.ITEM)
        }
        
    }
    
    @Test
    fun `round-trip TagKey`() {
        val tagKey = TagKey.create(RegistryKey.ITEM, key("minecraft", "wool"))
        val bytes = serializer.write(tagKey)
        val deserialized = serializer.read(bytes)!!
        assertEquals(tagKey, deserialized)
    }
    
}

