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
import xyz.xenondevs.nova.registry.entries.ItemTypeEntries
import kotlin.test.assertEquals

class RegistryEntrySerializerTest {
    
    private class TestElement(override val entry: RegistryEntry.Nova<TestElement>) : NovaRegistryElement<TestElement>
    
    companion object {
        
        private lateinit var novaRegistry: MutableNovaRegistry<TestElement>
        private lateinit var entry1: RegistryEntry.Nova<TestElement>
        private lateinit var novaSerializer: NovaRegistryEntrySerializer<TestElement>
        private lateinit var paperSerializer: PaperRegistryEntrySerializer<ItemType>
        private lateinit var eitherSerializer: EitherRegistryEntrySerializer<TestElement, ItemType>
        
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
            novaRegistry = MutableNovaRegistry(key("nova", "kotlinx_entry_test"), true)
            entry1 = registerElement("element1")
            novaRegistry.freeze()
            
            novaSerializer = NovaRegistryEntrySerializer(novaRegistry)
            paperSerializer = PaperRegistryEntrySerializer(RegistryKey.ITEM)
            eitherSerializer = EitherRegistryEntrySerializer(novaRegistry, RegistryKey.ITEM)
        }
        
        private fun registerElement(name: String): RegistryEntry.Nova<TestElement> {
            val key = key("nova", name)
            val entry = novaRegistry[key]
            val element = TestElement(entry)
            novaRegistry[key] = element
            return entry
        }
        
    }
    
    // --- Nova ---
    
    @Test
    fun `serialize RegistryEntry Nova`() {
        val json = Json.encodeToString(novaSerializer, entry1)
        assertEquals(""""nova:element1"""", json)
    }
    
    @Test
    fun `deserialize RegistryEntry Nova`() {
        val json = """"nova:element1""""
        val entry = Json.decodeFromString(novaSerializer, json)
        assertEquals(entry1, entry)
    }
    
    // --- Paper ---
    
    @Test
    fun `serialize RegistryEntry Paper`() {
        val json = Json.encodeToString(paperSerializer, ItemTypeEntries.DIAMOND)
        assertEquals(""""minecraft:diamond"""", json)
    }
    
    @Test
    fun `deserialize RegistryEntry Paper`() {
        val json = """"minecraft:diamond""""
        val entry = Json.decodeFromString(paperSerializer, json)
        assertEquals(ItemTypeEntries.DIAMOND, entry)
    }
    
    // --- Either ---
    
    @Test
    fun `serialize RegistryEntry Either Nova`() {
        val either = RegistryEntry.either(entry1, RegistryKey.ITEM)
        val json = Json.encodeToString(eitherSerializer, either)
        assertEquals(""""nova:element1"""", json)
    }
    
    @Test
    fun `deserialize RegistryEntry Either Nova`() {
        val json = """"nova:element1""""
        val entry = Json.decodeFromString(eitherSerializer, json)
        assertEquals(RegistryEntry.either(entry1, RegistryKey.ITEM), entry)
    }
    
    @Test
    fun `serialize RegistryEntry Either Paper`() {
        val either = RegistryEntry.either(novaRegistry, ItemTypeEntries.DIAMOND)
        val json = Json.encodeToString(eitherSerializer, either)
        assertEquals(""""minecraft:diamond"""", json)
    }
    
    @Test
    fun `deserialize RegistryEntry Either Paper`() {
        val json = """"minecraft:diamond""""
        val entry = Json.decodeFromString(eitherSerializer, json)
        assertEquals(RegistryEntry.either(novaRegistry, ItemTypeEntries.DIAMOND), entry)
    }
    
}
