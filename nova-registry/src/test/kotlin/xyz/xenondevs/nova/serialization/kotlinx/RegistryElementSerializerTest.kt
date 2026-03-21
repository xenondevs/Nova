package xyz.xenondevs.nova.serialization.kotlinx

import io.papermc.paper.registry.RegistryKey
import kotlinx.serialization.json.Json
import net.kyori.adventure.key.Key.key
import org.bukkit.inventory.ItemType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import xyz.xenondevs.nova.registry.MutableNovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RegistryElementSerializerTest {
    
    private class TestElement(override val entry: RegistryEntry.Nova<TestElement>) : NovaRegistryElement<TestElement>
    
    companion object {
        
        private lateinit var registry: MutableNovaRegistry<TestElement>
        private lateinit var element1: TestElement
        private lateinit var novaSerializer: NovaRegistryElementSerializer<TestElement>
        private lateinit var paperSerializer: PaperRegistryElementSerializer<ItemType>
        
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
            registry = MutableNovaRegistry(key("nova", "kotlinx_element_test"), true)
            element1 = registerElement("element1")
            registry.freeze()
            
            novaSerializer = NovaRegistryElementSerializer(registry)
            paperSerializer = PaperRegistryElementSerializer(RegistryKey.ITEM)
        }
        
        private fun registerElement(name: String): TestElement {
            val key = key("nova", name)
            val entry = registry[key]
            val element = TestElement(entry)
            registry[key] = element
            return element
        }
        
    }
    
    // --- Nova ---
    
    @Test
    fun `serialize NovaRegistryElement`() {
        val json = Json.encodeToString(novaSerializer, element1)
        assertEquals(""""nova:element1"""", json)
    }
    
    @Test
    fun `deserialize NovaRegistryElement`() {
        val json = """"nova:element1""""
        val element = Json.decodeFromString(novaSerializer, json)
        assertSame(element1, element)
    }
    
    // --- Paper ---
    
    @Test
    fun `serialize PaperRegistryElement`() {
        val json = Json.encodeToString(paperSerializer, ItemType.DIAMOND)
        assertEquals(""""minecraft:diamond"""", json)
    }
    
    @Test
    fun `deserialize PaperRegistryElement`() {
        val json = """"minecraft:diamond""""
        val element = Json.decodeFromString(paperSerializer, json)
        assertEquals(ItemType.DIAMOND, element)
    }
    
}
