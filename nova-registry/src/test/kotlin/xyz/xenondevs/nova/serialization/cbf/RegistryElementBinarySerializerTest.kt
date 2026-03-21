package xyz.xenondevs.nova.serialization.cbf

import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key.key
import org.bukkit.inventory.ItemType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import xyz.xenondevs.cbf.serializer.read
import xyz.xenondevs.cbf.serializer.write
import xyz.xenondevs.nova.registry.MutableNovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RegistryElementBinarySerializerTest {
    
    private class TestElement(override val entry: RegistryEntry.Nova<TestElement>) : NovaRegistryElement<TestElement>
    
    companion object {
        
        private lateinit var registry: MutableNovaRegistry<TestElement>
        private lateinit var element1: TestElement
        private lateinit var novaSerializer: NovaRegistryElementBinarySerializer<TestElement>
        private lateinit var paperSerializer: PaperRegistryElementBinarySerializer<ItemType>
        
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
            registry = MutableNovaRegistry(key("nova", "element_test"), true)
            element1 = registerElement("element1")
            registry.freeze()
            novaSerializer = NovaRegistryElementBinarySerializer(registry)
            paperSerializer = PaperRegistryElementBinarySerializer(RegistryKey.ITEM)
        }
        
        private fun registerElement(name: String): TestElement {
            val key = key("nova", name)
            val entry = registry[key]
            val element = TestElement(entry)
            registry[key] = element
            return element
        }
        
    }
    
    @Test
    fun `round-trip NovaRegistryElement`() {
        val bytes = novaSerializer.write(element1)
        val deserialized = novaSerializer.read(bytes)!!
        assertSame(element1, deserialized)
    }
    
    @Test
    fun `round-trip PaperRegistryElement`() {
        val bytes = paperSerializer.write(ItemType.DIAMOND)
        val deserialized = paperSerializer.read(bytes)!!
        assertEquals(ItemType.DIAMOND, deserialized)
    }
    
}
