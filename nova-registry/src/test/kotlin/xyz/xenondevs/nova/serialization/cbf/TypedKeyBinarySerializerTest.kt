package xyz.xenondevs.nova.serialization.cbf

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key.key
import org.bukkit.inventory.ItemType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import xyz.xenondevs.cbf.serializer.read
import xyz.xenondevs.cbf.serializer.write
import kotlin.test.assertEquals

class TypedKeyBinarySerializerTest {
    
    companion object {
        
        private lateinit var serializer: TypedKeyBinarySerializer<ItemType>
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            serializer = TypedKeyBinarySerializer(RegistryKey.ITEM)
        }
        
    }
    
    @Test
    fun `round-trip TypedKey`() {
        val typedKey = TypedKey.create(RegistryKey.ITEM, key("minecraft", "diamond"))
        val bytes = serializer.write(typedKey)
        val deserialized = serializer.read(bytes)!!
        assertEquals(typedKey, deserialized)
    }
    
}

