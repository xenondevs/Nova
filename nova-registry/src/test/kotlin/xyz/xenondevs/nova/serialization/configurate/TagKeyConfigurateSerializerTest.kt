package xyz.xenondevs.nova.serialization.configurate

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key.key
import org.bukkit.inventory.ItemType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TagKeyConfigurateSerializerTest {
    
    companion object {
        
        private lateinit var loader: YamlConfigurationLoader
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            loader = YamlConfigurationLoader.builder()
                .defaultOptions { opts ->
                    opts.serializers {
                        it.register(TagKeyConfigurateSerializer<ItemType>(RegistryKey.ITEM))
                    }
                }
                .build()
        }
        
        private fun createNode() = loader.createNode()
        
    }
    
    private val typeToken = geantyrefTypeTokenOf<TagKey<ItemType>>()
    
    @Test
    fun `serialize TagKey`() {
        val node = createNode()
        val tagKey = TagKey.create(RegistryKey.ITEM, key("minecraft", "wool"))
        node.set(typeToken, tagKey)
        assertEquals("#minecraft:wool", node.string)
    }
    
    @Test
    fun `deserialize TagKey`() {
        val node = createNode()
        node.raw("#minecraft:wool")
        val tagKey = node.get(typeToken)
        assertEquals(TagKey.create(RegistryKey.ITEM, key("minecraft", "wool")), tagKey)
    }
    
    @Test
    fun `deserialize TagKey without hash prefix fails`() {
        val node = createNode()
        node.raw("minecraft:wool")
        assertFailsWith<Exception> {
            node.get(typeToken)
        }
    }
    
}


