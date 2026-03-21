package xyz.xenondevs.nova.serialization.configurate

import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key.key
import org.bukkit.inventory.ItemType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import xyz.xenondevs.nova.registry.MutableNovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RegistryElementConfigurateSerializerTest {
    
    private class TestElement(override val entry: RegistryEntry.Nova<TestElement>) : NovaRegistryElement<TestElement>
    
    companion object {
        
        private lateinit var registry: MutableNovaRegistry<TestElement>
        private lateinit var element1: TestElement
        private lateinit var loader: YamlConfigurationLoader
        
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
        fun setupRegistry() {
            registry = MutableNovaRegistry(key("nova", "config_element_test"), true)
            element1 = registerElement("element1")
            registry.freeze()
            
            loader = YamlConfigurationLoader.builder()
                .defaultOptions { opts ->
                    opts.serializers {
                        it.register(NovaRegistryElementConfigurateSerializer(registry))
                        it.register(PaperRegistryElementConfigurateSerializer<ItemType>(RegistryKey.ITEM))
                    }
                }
                .build()
        }
        
        private fun registerElement(name: String): TestElement {
            val key = key("nova", name)
            val entry = registry[key]
            val element = TestElement(entry)
            registry[key] = element
            return element
        }
        
        private fun createNode() = loader.createNode()
        
    }
    
    // --- Nova ---
    
    @Test
    fun `serialize NovaRegistryElement`() {
        val node = createNode()
        node.set(TestElement::class.java, element1)
        assertEquals("nova:element1", node.string)
    }
    
    @Test
    fun `deserialize NovaRegistryElement`() {
        val node = createNode()
        node.raw("nova:element1")
        val element = node.get(TestElement::class.java)
        assertSame(element1, element)
    }
    
    // --- Paper ---
    
    @Test
    fun `serialize PaperRegistryElement`() {
        val node = createNode()
        node.set(ItemType::class.java, ItemType.DIAMOND)
        assertEquals("minecraft:diamond", node.string)
    }
    
    @Test
    fun `deserialize PaperRegistryElement`() {
        val node = createNode()
        node.raw("minecraft:diamond")
        val element = node.get(ItemType::class.java)
        assertEquals(ItemType.DIAMOND, element)
    }
    
}
