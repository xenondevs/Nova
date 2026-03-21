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
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.buildNovaTagEntries
import xyz.xenondevs.nova.registry.emptyRegistryEntrySet
import xyz.xenondevs.nova.registry.entries.ItemTypeEntries
import xyz.xenondevs.nova.registry.entries.ItemTypeTags
import xyz.xenondevs.nova.registry.registryEntrySetOf
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RegistryEntrySetBinarySerializerTest {
    
    private class TestElement(override val entry: RegistryEntry.Nova<TestElement>) : NovaRegistryElement<TestElement>
    
    companion object {
        
        private lateinit var registry: MutableNovaRegistry<TestElement>
        private lateinit var entry1: RegistryEntry.Nova<TestElement>
        private lateinit var entry2: RegistryEntry.Nova<TestElement>
        private lateinit var entry3: RegistryEntry.Nova<TestElement>
        private lateinit var tag: RegistryEntrySet.Nova.Tag<TestElement>
        private lateinit var novaSerializer: NovaRegistryEntrySetBinarySerializer<TestElement>
        private lateinit var paperSerializer: PaperRegistryEntrySetBinarySerializer<ItemType>
        private lateinit var mixedSerializer: MixedRegistryEntrySetBinarySerializer<TestElement, ItemType>
        
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
            registry = MutableNovaRegistry(key("nova", "cbf_entry_set_test"), true)
            
            entry1 = registerElement("element1")
            entry2 = registerElement("element2")
            entry3 = registerElement("element3")
            
            tag = registerTag("tag1", setOf(entry1, entry3))
            
            registry.freeze()
            
            novaSerializer = NovaRegistryEntrySetBinarySerializer(registry)
            paperSerializer = PaperRegistryEntrySetBinarySerializer(RegistryKey.ITEM)
            mixedSerializer = MixedRegistryEntrySetBinarySerializer(registry, RegistryKey.ITEM)
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
    
    // --- Nova ---
    
    @Test
    fun `round-trip RegistryEntrySet Nova Direct (single)`() {
        val set = registryEntrySetOf(entry1)
        val bytes = novaSerializer.write(set)
        val deserialized = novaSerializer.read(bytes)!!
        
        assertIs<RegistryEntrySet.Nova.Direct<TestElement>>(deserialized)
        assertEquals(setOf(entry1), deserialized.entries)
    }
    
    @Test
    fun `round-trip RegistryEntrySet Nova Direct (multi)`() {
        val set = registryEntrySetOf(entry1, entry2)
        val bytes = novaSerializer.write(set)
        val deserialized = novaSerializer.read(bytes)!!
        
        assertIs<RegistryEntrySet.Nova.Direct<TestElement>>(deserialized)
        assertEquals(setOf(entry1, entry2), deserialized.entries)
    }
    
    @Test
    fun `round-trip RegistryEntrySet Nova Tag`() {
        val bytes = novaSerializer.write(tag)
        val deserialized = novaSerializer.read(bytes)!!
        
        assertIs<RegistryEntrySet.Nova.Tag<TestElement>>(deserialized)
        assertEquals(tag, deserialized)
    }
    
    @Test
    fun `round-trip RegistryEntrySet Nova Tag (all entries)`() {
        val bytes = novaSerializer.write(registry.entrySet)
        val deserialized = novaSerializer.read(bytes)!!
        
        assertIs<RegistryEntrySet.Nova.Tag<TestElement>>(deserialized)
        assertEquals(registry.entrySet, deserialized)
    }
    
    // --- Paper ---
    
    @Test
    fun `round-trip RegistryEntrySet Paper Direct (single)`() {
        val set = registryEntrySetOf(ItemTypeEntries.DIAMOND)
        val bytes = paperSerializer.write(set)
        val deserialized = paperSerializer.read(bytes)!!
        
        assertIs<RegistryEntrySet.Paper.Direct<ItemType>>(deserialized)
        assertEquals(registryEntrySetOf(ItemTypeEntries.DIAMOND), deserialized)
    }
    
    @Test
    fun `round-trip RegistryEntrySet Paper Direct (multi)`() {
        val set = registryEntrySetOf(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD)
        val bytes = paperSerializer.write(set)
        val deserialized = paperSerializer.read(bytes)!!
        
        assertIs<RegistryEntrySet.Paper.Direct<ItemType>>(deserialized)
        assertEquals(registryEntrySetOf(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD), deserialized)
    }
    
    @Test
    fun `round-trip RegistryEntrySet Paper Tag`() {
        val bytes = paperSerializer.write(ItemTypeTags.WOOL)
        val deserialized = paperSerializer.read(bytes)!!
        
        assertIs<RegistryEntrySet.Paper.Tag<ItemType>>(deserialized)
        assertEquals(ItemTypeTags.WOOL, deserialized)
    }
    
    @Test
    fun `round-trip RegistryEntrySet Paper Tag (all entries)`() {
        val set = registryEntrySetOf(RegistryKey.ITEM)
        val bytes = paperSerializer.write(set)
        val deserialized = paperSerializer.read(bytes)!!
        
        assertIs<RegistryEntrySet.Paper.Tag<ItemType>>(deserialized)
        assertEquals(registryEntrySetOf(RegistryKey.ITEM), deserialized)
    }
    
    // --- Mixed ---
    
    @Test
    fun `round-trip RegistryEntrySet Mixed Direct (single nova)`() {
        val mixed = registryEntrySetOf(
            registryEntrySetOf(entry1),
            emptyRegistryEntrySet(RegistryKey.ITEM)
        )
        val bytes = mixedSerializer.write(mixed)
        val deserialized = mixedSerializer.read(bytes)!!
        
        assertIs<RegistryEntrySet.Mixed.Direct<TestElement, ItemType>>(deserialized)
        assertEquals(mixed, deserialized)
    }
    
    @Test
    fun `round-trip RegistryEntrySet Mixed Direct (single paper)`() {
        val mixed = registryEntrySetOf(
            emptyRegistryEntrySet(registry),
            registryEntrySetOf(ItemTypeEntries.DIAMOND)
        )
        val bytes = mixedSerializer.write(mixed)
        val deserialized = mixedSerializer.read(bytes)!!
        
        assertIs<RegistryEntrySet.Mixed.Direct<TestElement, ItemType>>(deserialized)
        assertEquals(mixed, deserialized)
    }
    
    @Test
    fun `round-trip RegistryEntrySet Mixed Direct (one nova, one paper)`() {
        val mixed = registryEntrySetOf(
            registryEntrySetOf(entry1),
            registryEntrySetOf(ItemTypeEntries.DIAMOND)
        )
        val bytes = mixedSerializer.write(mixed)
        val deserialized = mixedSerializer.read(bytes)!!
        
        assertIs<RegistryEntrySet.Mixed.Direct<TestElement, ItemType>>(deserialized)
        assertEquals(mixed, deserialized)
    }
    
    @Test
    fun `round-trip RegistryEntrySet Mixed Direct (multi nova)`() {
        val mixed = registryEntrySetOf(
            registryEntrySetOf(entry1, entry2),
            emptyRegistryEntrySet(RegistryKey.ITEM)
        )
        val bytes = mixedSerializer.write(mixed)
        val deserialized = mixedSerializer.read(bytes)!!
        
        assertIs<RegistryEntrySet.Mixed.Direct<TestElement, ItemType>>(deserialized)
        assertEquals(mixed, deserialized)
    }
    
    @Test
    fun `round-trip RegistryEntrySet Mixed Direct (multi paper)`() {
        val mixed = registryEntrySetOf(
            emptyRegistryEntrySet(registry),
            registryEntrySetOf(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD)
        )
        val bytes = mixedSerializer.write(mixed)
        val deserialized = mixedSerializer.read(bytes)!!
        
        assertIs<RegistryEntrySet.Mixed.Direct<TestElement, ItemType>>(deserialized)
        assertEquals(mixed, deserialized)
    }
    
}
