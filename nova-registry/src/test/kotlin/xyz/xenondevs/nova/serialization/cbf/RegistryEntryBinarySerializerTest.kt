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
import xyz.xenondevs.nova.registry.entries.ItemTypeEntries
import kotlin.test.assertEquals

class RegistryEntryBinarySerializerTest {
    
    private class TestElement(override val entry: RegistryEntry.Nova<TestElement>) : NovaRegistryElement<TestElement>
    
    companion object {
        
        private lateinit var registry: MutableNovaRegistry<TestElement>
        private lateinit var entry1: RegistryEntry.Nova<TestElement>
        private lateinit var novaSerializer: NovaRegistryEntryBinarySerializer<TestElement>
        private lateinit var paperSerializer: PaperRegistryEntryBinarySerializer<ItemType>
        private lateinit var eitherSerializer: EitherRegistryEntryBinarySerializer<TestElement, ItemType>
        
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
            registry = MutableNovaRegistry(key("nova", "cbf_entry_test"), true)
            entry1 = registerElement("element1")
            registry.freeze()
            novaSerializer = NovaRegistryEntryBinarySerializer(registry)
            paperSerializer = PaperRegistryEntryBinarySerializer(RegistryKey.ITEM)
            eitherSerializer = EitherRegistryEntryBinarySerializer(registry, RegistryKey.ITEM)
        }
        
        private fun registerElement(name: String): RegistryEntry.Nova<TestElement> {
            val key = key("nova", name)
            val entry = registry[key]
            val element = TestElement(entry)
            registry[key] = element
            return entry
        }
        
    }
    
    @Test
    fun `round-trip RegistryEntry Nova`() {
        val bytes = novaSerializer.write(entry1)
        val deserialized = novaSerializer.read(bytes)!!
        assertEquals(entry1.key, deserialized.key)
    }
    
    @Test
    fun `round-trip RegistryEntry Paper`() {
        val bytes = paperSerializer.write(ItemTypeEntries.DIAMOND)
        val deserialized = paperSerializer.read(bytes)!!
        assertEquals(ItemTypeEntries.DIAMOND, deserialized)
    }
    
    @Test
    fun `round-trip RegistryEntry Either Nova`() {
        val either = RegistryEntry.either(entry1, RegistryKey.ITEM)
        val bytes = eitherSerializer.write(either)
        val deserialized = eitherSerializer.read(bytes)!!
        assertEquals(RegistryEntry.either(entry1, RegistryKey.ITEM), deserialized)
    }
    
    @Test
    fun `round-trip RegistryEntry Either Paper`() {
        val either = RegistryEntry.either(registry, ItemTypeEntries.DIAMOND)
        val bytes = eitherSerializer.write(either)
        val deserialized = eitherSerializer.read(bytes)!!
        assertEquals(RegistryEntry.either(registry, ItemTypeEntries.DIAMOND), deserialized)
    }
    
}
