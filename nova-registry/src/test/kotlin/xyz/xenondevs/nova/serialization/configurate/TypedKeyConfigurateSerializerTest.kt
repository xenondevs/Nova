package xyz.xenondevs.nova.serialization.configurate

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key.key
import org.bukkit.inventory.ItemType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import kotlin.test.assertEquals

class TypedKeyConfigurateSerializerTest {
    
    companion object {
        
        private lateinit var loader: YamlConfigurationLoader
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            loader = YamlConfigurationLoader.builder()
                .defaultOptions { opts ->
                    opts.serializers {
                        it.register(TypedKeyConfigurateSerializer<ItemType>(RegistryKey.ITEM))
                    }
                }
                .build()
        }
        
        private fun createNode() = loader.createNode()
        
    }
    
    private val typeToken = geantyrefTypeTokenOf<TypedKey<ItemType>>()
    
    @Test
    fun `serialize TypedKey`() {
        val node = createNode()
        val typedKey = TypedKey.create(RegistryKey.ITEM, key("minecraft", "diamond"))
        node.set(typeToken, typedKey)
        assertEquals("minecraft:diamond", node.string)
    }
    
    @Test
    fun `deserialize TypedKey`() {
        val node = createNode()
        node.raw("minecraft:diamond")
        val typedKey = node.get(typeToken)
        assertEquals(TypedKey.create(RegistryKey.ITEM, key("minecraft", "diamond")), typedKey)
    }
    
}

