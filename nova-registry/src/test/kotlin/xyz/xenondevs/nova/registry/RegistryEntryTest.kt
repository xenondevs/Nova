
package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.keys.ItemTypeKeys
import net.kyori.adventure.key.Key.key
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockbukkit.mockbukkit.MockBukkit
import xyz.xenondevs.nova.registry.entries.ItemTypeEntries
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RegistryEntryTest {
    
    private class TestElement(override val entry: RegistryEntry.Nova<TestElement>) : NovaRegistryElement<TestElement>
    
    companion object {
        
        private lateinit var registry: MutableNovaRegistry<TestElement>
        private lateinit var novaEntry1: RegistryEntry.Nova<TestElement>
        private lateinit var novaEntry2: RegistryEntry.Nova<TestElement>
        private lateinit var novaEntryDiamond: RegistryEntry.Nova<TestElement>
        
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
            registry = MutableNovaRegistry(key("nova", "equality_test"), true)
            novaEntry1 = registerElement("element1")
            novaEntry2 = registerElement("element2")
            novaEntryDiamond = registerElement("minecraft", "diamond")
            registry.freeze()
        }
        
        private fun registerElement(name: String): RegistryEntry.Nova<TestElement> =
            registerElement("nova", name)
        
        private fun registerElement(namespace: String, name: String): RegistryEntry.Nova<TestElement> {
            val key = key(namespace, name)
            val entry = registry[key]
            val element = TestElement(entry)
            registry[key] = element
            return entry
        }
        
    }
    
    @AfterEach
    fun resetBootstrapContext() {
        TestRegistryContext.reset()
    }
    
    // --- Paper bootstrap behavior ---
    
    @Test
    fun `paper() during bootstrap returns entry without throwing`() {
        TestRegistryContext.inBootstrapPhase = true
        assertDoesNotThrow { RegistryEntry.paper(ItemTypeKeys.DIAMOND) }
    }
    
    @Test
    fun `paper() during bootstrap tracks unresolved entry`() {
        TestRegistryContext.inBootstrapPhase = true
        val entry = RegistryEntry.paper(ItemTypeKeys.DIAMOND)
        assertTrue(entry.key in TestRegistryContext.trackedEntries)
    }
    
    @Test
    fun `paper() during bootstrap resolves to correct value`() {
        TestRegistryContext.inBootstrapPhase = true
        val entry = RegistryEntry.paper(ItemTypeKeys.DIAMOND)
        val expected = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getOrThrow(ItemTypeKeys.DIAMOND)
        assertSame(expected, entry.get())
    }
    
    @Test
    fun `paper() during bootstrap with invalid key does not throw`() {
        TestRegistryContext.inBootstrapPhase = true
        val invalidKey = TypedKey.create(RegistryKey.ITEM, key("test", "nonexistent"))
        assertDoesNotThrow { RegistryEntry.paper(invalidKey) }
    }
    
    @Test
    fun `paper() during bootstrap with invalid key throws on resolution`() {
        TestRegistryContext.inBootstrapPhase = true
        val invalidKey = TypedKey.create(RegistryKey.ITEM, key("test", "nonexistent"))
        val entry = RegistryEntry.paper(invalidKey)
        assertThrows<NoSuchElementException> { entry.get() }
    }
    
    @Test
    fun `paper() after bootstrap returns entry with resolved value`() {
        TestRegistryContext.inBootstrapPhase = false
        val entry = RegistryEntry.paper(ItemTypeKeys.DIAMOND)
        val expected = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getOrThrow(ItemTypeKeys.DIAMOND)
        assertSame(expected, entry.get())
    }
    
    @Test
    fun `paper() after bootstrap with invalid key throws immediately`() {
        TestRegistryContext.inBootstrapPhase = false
        val invalidKey = TypedKey.create(RegistryKey.ITEM, key("test", "nonexistent"))
        assertThrows<NoSuchElementException> { RegistryEntry.paper(invalidKey) }
    }
    
    // --- Nova == Nova ---
    
    @Test
    fun `Nova entries with same registry and key are equal`() {
        val sameEntry = registry[key("nova", "element1")]
        assertEquals(novaEntry1, sameEntry)
    }
    
    @Test
    fun `Nova entries with different keys are not equal`() {
        assertNotEquals(novaEntry1, novaEntry2)
    }
    
    @Test
    fun `Nova entries from different registries with same key are not equal`() {
        val otherRegistry = MutableNovaRegistry<TestElement>(key("nova", "equality_test_other"), false)
        val otherEntry = otherRegistry[key("nova", "element1")]
        assertNotEquals(novaEntry1, otherEntry)
    }
    
    // --- Paper == Paper ---
    
    @Test
    fun `Paper entries with same registry and key are equal`() {
        assertEquals(ItemTypeEntries.DIAMOND, ItemTypeEntries.DIAMOND)
    }
    
    @Test
    fun `Paper entries with different keys are not equal`() {
        assertNotEquals(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD)
    }
    
    
    
    
    // --- Nova != Paper ---
    
    @Test
    fun `Nova entry is not equal to Paper entry with same key`() {
        assertNotEquals<Any>(novaEntryDiamond, ItemTypeEntries.DIAMOND)
    }
    
    // --- hashCode consistency ---
    
    @Test
    fun `equal Nova entries have same hashCode`() {
        val sameEntry = registry[key("nova", "element1")]
        assertEquals(novaEntry1.hashCode(), sameEntry.hashCode())
    }
    
}


