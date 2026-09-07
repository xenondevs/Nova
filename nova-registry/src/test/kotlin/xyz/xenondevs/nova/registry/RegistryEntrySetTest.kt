package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key.key
import org.bukkit.inventory.ItemType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockbukkit.mockbukkit.MockBukkit
import org.opentest4j.TestAbortedException
import xyz.xenondevs.nova.registry.entries.ItemTypeEntries
import xyz.xenondevs.nova.registry.entries.ItemTypeTags
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegistryEntrySetTest {
    
    private class TestElement(override val entry: RegistryEntry.Nova<TestElement>) : NovaRegistryElement<TestElement>
    
    companion object {
        
        private lateinit var registry: MutableNovaRegistry<TestElement>
        private lateinit var novaEntry1: RegistryEntry.Nova<TestElement>
        private lateinit var novaEntry2: RegistryEntry.Nova<TestElement>
        private lateinit var novaEntry3: RegistryEntry.Nova<TestElement>
        private lateinit var novaTag: RegistryEntrySet.Nova.Tag<TestElement>
        
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
            registry = MutableNovaRegistry(key("nova", "contains_test"), true)
            
            novaEntry1 = registerElement("element1")
            novaEntry2 = registerElement("element2")
            novaEntry3 = registerElement("element3")
            
            novaTag = registerTag("test_tag", setOf(novaEntry1, novaEntry2))
            
            registry.freeze()
        }
        
        private fun registerElement(name: String): RegistryEntry.Nova<TestElement> {
            val key = key("nova", name)
            val entry = registry[key]
            val element = TestElement(entry)
            registry[key] = element
            return entry
        }
        
        private fun registerTag(
            name: String,
            entries: Set<RegistryEntry.Nova<TestElement>>
        ): RegistryEntrySet.Nova.Tag<TestElement> {
            val key = key("nova", name)
            val tag = registry.getTag(key)
            registry[key] = buildNovaTagEntries { add(entries) }
            return tag
        }
        
    }
    
    @AfterEach
    fun resetBootstrapContext() {
        TestRegistryContext.reset()
    }
    
    // --- Nova.Direct contains(Nova) ---
    
    @Test
    fun `Nova Direct contains Nova entry that is in the set`() {
        val set = registryEntrySetOf(novaEntry1, novaEntry2)
        assertTrue(novaEntry1 in set)
    }
    
    @Test
    fun `Nova Direct does not contain Nova entry that is not in the set`() {
        val set = registryEntrySetOf(novaEntry1, novaEntry2)
        assertFalse(novaEntry3 in set)
    }
    
    @Test
    fun `Nova Direct does not contain null Nova entry`() {
        val set = registryEntrySetOf(novaEntry1)
        assertFalse(null as RegistryEntry.Nova<TestElement>? in set)
    }
    
    
    // --- Nova.Tag contains(Nova) ---
    
    @Test
    fun `Nova Tag contains Nova entry that is in the tag`() {
        assertTrue(novaEntry1 in novaTag)
    }
    
    @Test
    fun `Nova Tag does not contain Nova entry that is not in the tag`() {
        assertFalse(novaEntry3 in novaTag)
    }
    
    @Test
    fun `Nova Tag does not contain null Nova entry`() {
        assertFalse(null as RegistryEntry.Nova<TestElement>? in novaTag)
    }
    
    
    // --- Paper.Direct contains(Paper) ---
    
    @Test
    fun `Paper Direct contains Paper entry that is in the set`() {
        val set = registryEntrySetOf(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD)
        assertTrue(ItemTypeEntries.DIAMOND in set)
    }
    
    @Test
    fun `Paper Direct does not contain Paper entry that is not in the set`() {
        val set = registryEntrySetOf(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD)
        assertFalse(ItemTypeEntries.GOLD_INGOT in set)
    }
    
    @Test
    fun `Paper Direct does not contain null Paper entry`() {
        val set = registryEntrySetOf(ItemTypeEntries.DIAMOND)
        assertFalse(null as RegistryEntry.Paper<ItemType>? in set)
    }
    
    
    // --- Paper.Tag contains(Paper) ---
    
    @Test
    fun `Paper Tag contains Paper entry that is in the tag`() {
        // ItemTypeTags.WOOL is a Paper.Tag; white_wool should be in it
        assertTrue(ItemTypeEntries.WHITE_WOOL in ItemTypeTags.WOOL)
    }
    
    @Test
    fun `Paper Tag does not contain Paper entry that is not in the tag`() {
        assertFalse(ItemTypeEntries.DIAMOND in ItemTypeTags.WOOL)
    }
    
    @Test
    fun `Paper Tag does not contain null Paper entry`() {
        assertFalse(null as RegistryEntry.Paper<ItemType>? in ItemTypeTags.WOOL)
    }
    
    
    // --- Paper.Direct from TypedKeys bootstrap behavior ---
    
    @Test
    fun `registryEntrySetOf(TypedKeys) during bootstrap with invalid key does not throw`() {
        TestRegistryContext.inBootstrapPhase = true
        val invalidKey = TypedKey.create(RegistryKey.ITEM, key("test", "nonexistent"))
        assertDoesNotThrow { registryEntrySetOf(invalidKey) }
    }
    
    @Test
    fun `registryEntrySetOf(TypedKeys) during bootstrap with invalid key throws on resolution`() {
        TestRegistryContext.inBootstrapPhase = true
        val invalidKey = TypedKey.create(RegistryKey.ITEM, key("test", "nonexistent"))
        val set = registryEntrySetOf(invalidKey)
        assertThrows<NoSuchElementException> { set.get() }
    }
    
    @Test
    fun `registryEntrySetOf(TypedKeys) after bootstrap with invalid key throws immediately`() {
        TestRegistryContext.inBootstrapPhase = false
        val invalidKey = TypedKey.create(RegistryKey.ITEM, key("test", "nonexistent"))
        assertThrows<NoSuchElementException> { registryEntrySetOf(invalidKey) }
    }
    
    // --- Paper.Tag bootstrap behavior ---
    
    @Test
    fun `registryEntrySetOf(TagKey) during bootstrap with invalid tag does not throw`() {
        TestRegistryContext.inBootstrapPhase = true
        val invalidTag = TagKey.create(RegistryKey.ITEM, key("test", "nonexistent_paper_tag_bootstrap"))
        assertDoesNotThrow { registryEntrySetOf(invalidTag) }
    }
    
    @Test
    fun `registryEntrySetOf(TagKey) during bootstrap with invalid tag throws on resolution`() {
        TestRegistryContext.inBootstrapPhase = true
        val invalidTag = TagKey.create(RegistryKey.ITEM, key("test", "nonexistent_paper_tag_resolve"))
        val set = registryEntrySetOf(invalidTag)
        assertThrowsOrAborts<NoSuchElementException> { set.get() }
    }
    
    @Test
    fun `registryEntrySetOf(TagKey) after bootstrap with invalid tag throws immediately`() {
        TestRegistryContext.inBootstrapPhase = false
        val invalidTag = TagKey.create(RegistryKey.ITEM, key("test", "nonexistent_paper_tag_post"))
        assertThrowsOrAborts<NoSuchElementException> { registryEntrySetOf(invalidTag) }
    }
    
    
    /**
     * Asserts that [block] throws [T]. If [block] throws a [TestAbortedException] instead
     * (e.g. MockBukkit's [UnimplementedOperationException][org.mockbukkit.mockbukkit.exception.UnimplementedOperationException]),
     * it is rethrown so that the test is aborted rather than failed.
     */
    private inline fun <reified T : Throwable> assertThrowsOrAborts(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected ${T::class.simpleName} to be thrown, but nothing was thrown")
        } catch (e: Throwable) {
            if (e is T) return // expected
            if (e is TestAbortedException) throw e // let the test abort
            throw AssertionError("Expected ${T::class.simpleName} but got ${e::class.simpleName}", e)
        }
    }
    
}
