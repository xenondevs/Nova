package xyz.xenondevs.nova.serialization.configurate

import net.kyori.adventure.key.Key
import org.bukkit.NamespacedKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import kotlin.test.assertEquals

class KeyConfigurateSerializerTest {
    
    companion object {
        
        private lateinit var loader: YamlConfigurationLoader
        
        @BeforeAll
        @JvmStatic
        fun setup() {
            loader = YamlConfigurationLoader.builder()
                .defaultOptions { opts ->
                    opts.serializers {
                        it.register(NamespacedKeyConfigurateSerializer)
                        it.register(KeyConfigurateSerializer)
                    }
                }
                .build()
        }
        
        private fun createNode() = loader.createNode()
        
    }
    
    @Test
    fun `serialize Key`() {
        val node = createNode()
        node.set(Key::class.java, Key.key("nova", "test"))
        assertEquals("nova:test", node.string)
    }
    
    @Test
    fun `deserialize Key`() {
        val node = createNode()
        node.raw("nova:test")
        val key = node.get(Key::class.java)
        assertEquals(Key.key("nova", "test"), key)
    }
    
    @Test
    fun `serialize NamespacedKey`() {
        val node = createNode()
        node.set(NamespacedKey::class.java, NamespacedKey.minecraft("test"))
        assertEquals("minecraft:test", node.string)
    }
    
    @Test
    fun `deserialize NamespacedKey`() {
        val node = createNode()
        node.raw("minecraft:test")
        val key = node.get(NamespacedKey::class.java)
        assertEquals(NamespacedKey.minecraft("test"), key)
    }
    
}
