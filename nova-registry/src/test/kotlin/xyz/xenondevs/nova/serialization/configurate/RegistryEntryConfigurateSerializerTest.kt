package xyz.xenondevs.nova.serialization.configurate

import io.leangen.geantyref.TypeToken
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
import xyz.xenondevs.nova.registry.entries.ItemTypeEntries
import kotlin.test.assertEquals

class RegistryEntryConfigurateSerializerTest {
    
    private class TestElement(override val entry: RegistryEntry.Nova<TestElement>) : NovaRegistryElement<TestElement>
    
    companion object {
        
        private lateinit var registry: MutableNovaRegistry<TestElement>
        private lateinit var entry1: RegistryEntry.Nova<TestElement>
        private lateinit var novaTypeToken: TypeToken<RegistryEntry.Nova<TestElement>>
        private lateinit var paperTypeToken: TypeToken<RegistryEntry.Paper<ItemType>>
        private lateinit var eitherTypeToken: TypeToken<RegistryEntry.Either<TestElement, ItemType>>
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
            registry = MutableNovaRegistry(key("nova", "config_entry_test"), true)
            entry1 = registerElement("element1")
            registry.freeze()
            
            novaTypeToken = geantyrefTypeTokenOf()
            paperTypeToken = geantyrefTypeTokenOf()
            eitherTypeToken = geantyrefTypeTokenOf()
            loader = YamlConfigurationLoader.builder()
                .defaultOptions { opts ->
                    opts.serializers {
                        it.register(NovaRegistryEntryConfigurateSerializer(registry))
                        it.register(PaperRegistryEntryConfigurateSerializer(RegistryKey.ITEM, geantyrefTypeTokenOf()))
                        it.register(EitherRegistryEntryConfigurateSerializer(registry, RegistryKey.ITEM, geantyrefTypeTokenOf()))
                    }
                }
                .build()
        }
        
        private fun registerElement(name: String): RegistryEntry.Nova<TestElement> {
            val key = key("nova", name)
            val entry = registry[key]
            val element = TestElement(entry)
            registry[key] = element
            return entry
        }
        
        private fun createNode() = loader.createNode()
        
    }
    
    // --- Nova ---
    
    @Test
    fun `serialize RegistryEntry Nova`() {
        val node = createNode()
        node.set(entry1)
        assertEquals("nova:element1", node.string)
    }
    
    @Test
    fun `deserialize RegistryEntry Nova`() {
        val node = createNode()
        node.raw("nova:element1")
        val entry = node.get(novaTypeToken)
        assertEquals(entry1.key, entry?.key)
    }
    
    // --- Paper ---
    
    @Test
    fun `serialize RegistryEntry Paper`() {
        val node = createNode()
        node.set(paperTypeToken, ItemTypeEntries.DIAMOND)
        assertEquals("minecraft:diamond", node.string)
    }
    
    @Test
    fun `deserialize RegistryEntry Paper`() {
        val node = createNode()
        node.raw("minecraft:diamond")
        val entry = node.get(paperTypeToken)
        assertEquals(ItemTypeEntries.DIAMOND, entry)
    }
    
    // --- Either ---
    
    @Test
    fun `serialize RegistryEntry Either Nova`() {
        val either = RegistryEntry.either(entry1, RegistryKey.ITEM)
        val node = createNode()
        node.set(eitherTypeToken, either)
        assertEquals("nova:element1", node.string)
    }
    
    @Test
    fun `deserialize RegistryEntry Either Nova`() {
        val node = createNode()
        node.raw("nova:element1")
        val entry = node.get(eitherTypeToken)
        assertEquals(RegistryEntry.either(entry1, RegistryKey.ITEM), entry)
    }
    
    @Test
    fun `serialize RegistryEntry Either Paper`() {
        val either = RegistryEntry.either(registry, ItemTypeEntries.DIAMOND)
        val node = createNode()
        node.set(eitherTypeToken, either)
        assertEquals("minecraft:diamond", node.string)
    }
    
    @Test
    fun `deserialize RegistryEntry Either Paper`() {
        val node = createNode()
        node.raw("minecraft:diamond")
        val entry = node.get(eitherTypeToken)
        assertEquals(RegistryEntry.either(registry, ItemTypeEntries.DIAMOND), entry)
    }
    
}
