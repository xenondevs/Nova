package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key.key
import org.bukkit.inventory.ItemType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
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

    // --- Nova.Direct contains(Either) ---

    @Test
    fun `Nova Direct contains Either entry whose key matches a contained Nova entry`() {
        val set = registryEntrySetOf(novaEntry1, novaEntry2)
        val either = RegistryEntry.either(novaEntry1, RegistryKey.ITEM)
        assertTrue(either in set)
    }

    @Test
    fun `Nova Direct does not contain Either entry whose key is not in the set`() {
        val set = registryEntrySetOf(novaEntry1, novaEntry2)
        val either = RegistryEntry.either(novaEntry3, RegistryKey.ITEM)
        assertFalse(either in set)
    }

    @Test
    fun `Nova Direct does not contain null Either entry`() {
        val set = registryEntrySetOf(novaEntry1)
        assertFalse(null as RegistryEntry.Either<TestElement, *>? in set)
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

    // --- Nova.Tag contains(Either) ---

    @Test
    fun `Nova Tag contains Either entry whose key matches a tagged Nova entry`() {
        val either = RegistryEntry.either(novaEntry1, RegistryKey.ITEM)
        assertTrue(either in novaTag)
    }

    @Test
    fun `Nova Tag does not contain Either entry whose key is not in the tag`() {
        val either = RegistryEntry.either(novaEntry3, RegistryKey.ITEM)
        assertFalse(either in novaTag)
    }

    @Test
    fun `Nova Tag does not contain null Either entry`() {
        assertFalse(null as RegistryEntry.Either<TestElement, *>? in novaTag)
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

    // --- Paper.Direct contains(Either) ---

    @Test
    fun `Paper Direct contains Either entry whose key matches a contained Paper entry`() {
        val set = registryEntrySetOf(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD)
        val either = RegistryEntry.either(registry, ItemTypeEntries.DIAMOND)
        assertTrue(either in set)
    }

    @Test
    fun `Paper Direct does not contain Either entry whose key is not in the set`() {
        val set = registryEntrySetOf(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD)
        val either = RegistryEntry.either(registry, ItemTypeEntries.GOLD_INGOT)
        assertFalse(either in set)
    }

    @Test
    fun `Paper Direct does not contain null Either entry`() {
        val set = registryEntrySetOf(ItemTypeEntries.DIAMOND)
        assertFalse(null as RegistryEntry.Either<*, ItemType>? in set)
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

    // --- Paper.Tag contains(Either) ---

    @Test
    fun `Paper Tag contains Either entry whose key matches a tagged Paper entry`() {
        val either = RegistryEntry.either(registry, ItemTypeEntries.WHITE_WOOL)
        assertTrue(either in ItemTypeTags.WOOL)
    }

    @Test
    fun `Paper Tag does not contain Either entry whose key is not in the tag`() {
        val either = RegistryEntry.either(registry, ItemTypeEntries.DIAMOND)
        assertFalse(either in ItemTypeTags.WOOL)
    }

    @Test
    fun `Paper Tag does not contain null Either entry`() {
        assertFalse(null as RegistryEntry.Either<*, ItemType>? in ItemTypeTags.WOOL)
    }

    // --- Mixed.Direct contains(Either) ---

    @Test
    fun `Mixed Direct contains Either entry that is in the set`() {
        val set = registryEntrySetOf(
            registryEntrySetOf(novaEntry1),
            registryEntrySetOf(ItemTypeEntries.DIAMOND)
        )
        val either = RegistryEntry.either(novaEntry1, RegistryKey.ITEM)
        assertTrue(either in set)
    }

    @Test
    fun `Mixed Direct does not contain Either entry that is not in the set`() {
        val set = registryEntrySetOf(
            registryEntrySetOf(novaEntry1),
            registryEntrySetOf(ItemTypeEntries.DIAMOND)
        )
        val either = RegistryEntry.either(novaEntry3, RegistryKey.ITEM)
        assertFalse(either in set)
    }

    @Test
    fun `Mixed Direct does not contain null Either entry`() {
        val set = registryEntrySetOf(
            registryEntrySetOf(novaEntry1),
            emptyRegistryEntrySet(RegistryKey.ITEM)
        )
        assertFalse(null as RegistryEntry.Either<TestElement, ItemType>? in set)
    }

    // --- Mixed.Direct contains(Nova) ---

    @Test
    fun `Mixed Direct contains Nova entry that is in the set`() {
        val set = registryEntrySetOf(
            registryEntrySetOf(novaEntry1, novaEntry2),
            emptyRegistryEntrySet(RegistryKey.ITEM)
        )
        assertTrue(novaEntry1 in set)
    }

    @Test
    fun `Mixed Direct does not contain Nova entry that is not in the set`() {
        val set = registryEntrySetOf(
            registryEntrySetOf(novaEntry1),
            emptyRegistryEntrySet(RegistryKey.ITEM)
        )
        assertFalse(novaEntry3 in set)
    }

    @Test
    fun `Mixed Direct does not contain null Nova entry`() {
        val set = registryEntrySetOf(
            registryEntrySetOf(novaEntry1),
            emptyRegistryEntrySet(RegistryKey.ITEM)
        )
        assertFalse(null as RegistryEntry.Nova<TestElement>? in set)
    }

    // --- Mixed.Direct contains(Paper) ---

    @Test
    fun `Mixed Direct contains Paper entry that is in the set`() {
        val set = registryEntrySetOf(
            emptyRegistryEntrySet(registry),
            registryEntrySetOf(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD)
        )
        assertTrue(ItemTypeEntries.DIAMOND in set)
    }

    @Test
    fun `Mixed Direct does not contain Paper entry that is not in the set`() {
        val set = registryEntrySetOf(
            emptyRegistryEntrySet(registry),
            registryEntrySetOf(ItemTypeEntries.DIAMOND)
        )
        assertFalse(ItemTypeEntries.GOLD_INGOT in set)
    }

    @Test
    fun `Mixed Direct does not contain null Paper entry`() {
        val set = registryEntrySetOf(
            emptyRegistryEntrySet(registry),
            registryEntrySetOf(ItemTypeEntries.DIAMOND)
        )
        assertFalse(null as RegistryEntry.Paper<ItemType>? in set)
    }

    // --- Mixed.Tag contains(Either) ---

    @Test
    fun `Mixed Tag contains Either entry whose key matches a tagged entry`() {
        val mixedTag = registryEntrySetOf(
            novaTag,
            RegistryKey.ITEM
        )
        val either = RegistryEntry.either(novaEntry1, RegistryKey.ITEM)
        assertTrue(either in mixedTag)
    }

    @Test
    fun `Mixed Tag does not contain Either entry whose key is not in the tag`() {
        val mixedTag = registryEntrySetOf(
            novaTag,
            RegistryKey.ITEM
        )
        val either = RegistryEntry.either(novaEntry3, RegistryKey.ITEM)
        assertFalse(either in mixedTag)
    }

    @Test
    fun `Mixed Tag does not contain null Either entry`() {
        val mixedTag = registryEntrySetOf(
            novaTag,
            RegistryKey.ITEM
        )
        assertFalse(null as RegistryEntry.Either<TestElement, ItemType>? in mixedTag)
    }

    // --- Mixed.Tag contains(Nova) ---

    @Test
    fun `Mixed Tag contains Nova entry that is in the tag`() {
        val mixedTag = registryEntrySetOf(
            novaTag,
            RegistryKey.ITEM
        )
        assertTrue(novaEntry1 in mixedTag)
    }

    @Test
    fun `Mixed Tag does not contain Nova entry that is not in the tag`() {
        val mixedTag = registryEntrySetOf(
            novaTag,
            RegistryKey.ITEM
        )
        assertFalse(novaEntry3 in mixedTag)
    }

    @Test
    fun `Mixed Tag does not contain null Nova entry`() {
        val mixedTag = registryEntrySetOf(
            novaTag,
            RegistryKey.ITEM
        )
        assertFalse(null as RegistryEntry.Nova<TestElement>? in mixedTag)
    }

    // --- Mixed.Tag contains(Paper) ---

    @Test
    fun `Mixed Tag contains Paper entry that is in the tag`() {
        val mixedTag = registryEntrySetOf(
            registry,
            ItemTypeTags.WOOL
        )
        assertTrue(ItemTypeEntries.WHITE_WOOL in mixedTag)
    }

    @Test
    fun `Mixed Tag does not contain Paper entry that is not in the tag`() {
        val mixedTag = registryEntrySetOf(
            registry,
            ItemTypeTags.WOOL
        )
        assertFalse(ItemTypeEntries.DIAMOND in mixedTag)
    }

    @Test
    fun `Mixed Tag does not contain null Paper entry`() {
        val mixedTag = registryEntrySetOf(
            registry,
            ItemTypeTags.WOOL
        )
        assertFalse(null as RegistryEntry.Paper<ItemType>? in mixedTag)
    }

}
