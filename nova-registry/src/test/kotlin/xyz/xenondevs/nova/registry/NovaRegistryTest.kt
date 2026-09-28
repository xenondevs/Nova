package xyz.xenondevs.nova.registry

import io.mockk.mockk
import net.kyori.adventure.key.Key.key
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.provider
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private typealias MR = MutableNovaRegistry<NovaRegistryElement<*>>

class NovaRegistryTest {
    
    companion object {
        
        @JvmStatic
        fun registryProvider(): List<MR> = listOf(
            createRegistry("reloadable", reloadable = true),
            createRegistry("stable", reloadable = false),
        )
        
        @JvmStatic
        fun createRegistry(name: String, reloadable: Boolean): MR =
            MutableNovaRegistry(key("nova", name), reloadable)
        
    }
    
    private fun mockElement() = mockk<NovaRegistryElement<*>>()
    
    private fun tagEntries(
        vararg entries: RegistryEntry.Nova<NovaRegistryElement<*>>
    ): Set<NovaTagEntry<NovaRegistryElement<*>>> = entries.mapTo(HashSet()) { NovaTagEntry.Direct(it) }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `getValue(Key) throws IllegalStateException before freeze`(registry: MR) {
        val key = key("nova", "element")
        registry[key] = mockElement()
        
        assertThrows<IllegalStateException> { registry.getValue(key) }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `getValue(Key) returns value after freeze`(registry: MR) {
        val key = key("nova", "element")
        val value = mockElement()
        registry[key] = value
        registry.freeze()
        
        assertSame(value, registry.getValue(key))
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `getValue(Key) returns null for non-existent key after freeze`(registry: MR) {
        registry.freeze()
        
        assertNull(registry.getValue(key("nova", "nonexistent")))
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `getValueOrThrow(Key) throws IllegalStateException before freeze`(registry: MR) {
        val key = key("nova", "element")
        registry[key] = mockElement()
        
        assertThrows<IllegalStateException> { registry.getValueOrThrow(key) }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `getValueOrThrow(Key) returns value after freeze`(registry: MR) {
        val key = key("nova", "element")
        val value = mockElement()
        registry[key] = value
        registry.freeze()
        
        assertSame(value, registry.getValueOrThrow(key))
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `getValueOrThrow(Key) throws NoSuchElementException for non-existent key after freeze`(registry: MR) {
        registry.freeze()
        
        assertThrows<NoSuchElementException> { registry.getValueOrThrow(key("nova", "nonexistent")) }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `getValuesByName throws IllegalStateException before freeze`(registry: MR) {
        val key = key("nova", "element")
        registry[key] = mockElement()
        
        assertThrows<IllegalStateException> { registry.getValuesByName("element") }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `getValuesByName returns values with matching name after freeze`(registry: MR) {
        val value1 = mockElement()
        val value2 = mockElement()
        registry[key("nova", "element")] = value1
        registry[key("other", "element")] = value2
        registry[key("nova", "different")] = mockElement()
        registry.freeze()
        
        assertEquals(listOf(value1, value2), registry.getValuesByName("element"))
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `getValuesByName returns empty list for non-existent name after freeze`(registry: MR) {
        registry.freeze()
        
        assertTrue(registry.getValuesByName("nonexistent").isEmpty())
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `contains(Key) throws IllegalStateException before freeze`(registry: MR) {
        val key = key("nova", "element")
        registry[key] = mockElement()
        
        assertThrows<IllegalStateException> { key in registry }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `contains(Key) returns true for registered key after freeze`(registry: MR) {
        val key = key("nova", "element")
        registry[key] = mockElement()
        registry.freeze()
        
        assertTrue(key in registry)
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `contains(Key) returns false for non-existent key after freeze`(registry: MR) {
        registry.freeze()
        
        assertFalse(key("nova", "nonexistent") in registry)
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `contains(value) throws IllegalStateException before freeze`(registry: MR) {
        val key = key("nova", "element")
        val element = mockElement()
        registry[key] = element
        
        assertThrows<IllegalStateException> { element in registry }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `contains(value) returns true for registered value after freeze`(registry: MR) {
        val key = key("nova", "element")
        val element = mockElement()
        registry[key] = element
        registry.freeze()
        
        assertTrue(element in registry)
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `contains(value) returns false for unregistered value after freeze`(registry: MR) {
        registry.freeze()
        
        assertFalse(mockElement() in registry)
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `get can be called before freeze`(registry: MR) {
        val key = key("nova", "element")
        
        val entry = registry[key]
        assertEquals(key, entry.key)
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `get throws IllegalArgumentException for unregistered key after freeze`(registry: MR) {
        registry.freeze()
        
        assertThrows<IllegalArgumentException> { registry[key("nova", "nonexistent")] }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `get entry resolves to correct value after freeze`(registry: MR) {
        val key = key("nova", "element")
        val entry = registry[key]
        val element = mockElement()
        registry[key] = element
        registry.freeze()
        
        assertSame(element, entry.get())
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `getOptional entry resolves to correct value after freeze`(registry: MR) {
        val key = key("nova", "element")
        val entry = registry.getOptional(key)
        registry[key] = mockElement()
        registry.freeze()
        
        assertSame(registry[key], entry.get())
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `getOptional entry resolves to null after freeze`(registry: MR) {
        val key = key("nova", "element")
        val entry = registry.getOptional(key)
        registry.freeze()
        
        assertNull(entry.get())
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `getTag can be called before freeze`(registry: MR) {
        assertNotNull(registry.getTag(key("nova", "test_tag")))
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `freeze throws IllegalStateException for tag without definition`(registry: MR) {
        registry.getTag(key("nova", "test_tag"))
        
        assertThrows<IllegalStateException> { registry.freeze() }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `getTag throws IllegalArgumentException for unregistered tag key after freeze`(registry: MR) {
        registry.freeze()
        
        assertThrows<IllegalArgumentException> { registry.getTag(key("nova", "nonexistent_tag")) }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `getTag tag resolves to correct values after freeze`(registry: MR) {
        val key1 = key("nova", "element1")
        val key2 = key("nova", "element2")
        val tagKey = key("nova", "test_tag")
        val element1 = mockElement()
        val element2 = mockElement()
        registry[key1] = element1
        registry[key2] = element2
        registry[key("nova", "element3")] = mockElement()
        val entry1 = registry[key1]
        val entry2 = registry[key2]
        registry[tagKey] = buildNovaTagEntries { add(entry1, entry2) }
        registry.freeze()
        
        val tag = registry.getTag(tagKey)
        assertEquals(setOf(element1, element2), tag.get())
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `entrySet contains all entries after freeze`(registry: MR) {
        registry[key("nova", "element1")] = mockElement()
        registry[key("nova", "element2")] = mockElement()
        registry.freeze()
        
        assertEquals(2, registry.entrySet.get().size)
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `set throws IllegalStateException after freeze`(registry: MR) {
        registry.freeze()
        
        assertThrows<IllegalStateException> { registry[key("nova", "element")] = mockElement() }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `set throws IllegalArgumentException for duplicate key`(registry: MR) {
        val key = key("nova", "element")
        registry[key] = mockElement()
        
        assertThrows<IllegalArgumentException> { registry[key] = mockElement() }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `set throws IllegalArgumentException for duplicate value`(registry: MR) {
        val element = mockElement()
        registry[key("nova", "element1")] = element
        
        assertThrows<IllegalArgumentException> {
            registry[key("nova", "element2")] = element
        }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `setKnown throws IllegalStateException after freeze`(registry: MR) {
        registry.freeze()
        
        assertThrows<IllegalStateException> {
            registry.setKnown(key("nova", "element"))
        }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `tag set throws IllegalStateException after freeze`(registry: MR) {
        registry.freeze()
        
        assertThrows<IllegalStateException> {
            registry[key("nova", "tag")] = buildNovaTagEntries {}
        }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `tag set throws IllegalArgumentException for reserved key`(registry: MR) {
        assertThrows<IllegalArgumentException> {
            registry[registry.key] = buildNovaTagEntries {}
        }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `tag set throws IllegalArgumentException for duplicate key`(registry: MR) {
        val tagKey = key("nova", "tag")
        registry[tagKey] = buildNovaTagEntries {}
        
        assertThrows<IllegalArgumentException> {
            registry[tagKey] = buildNovaTagEntries {}
        }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `tag contents from another registry throw IllegalArgumentException when resolved`(registry: MR) {
        val otherRegistry = createRegistry("other", reloadable = false)
        val entry = otherRegistry[key("nova", "element")]
        otherRegistry[entry.key] = mockElement()
        registry[key("nova", "tag")] = provider(tagEntries(entry))
        
        assertThrows<IllegalArgumentException> { registry.freeze() }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `freeze throws IllegalStateException if registry is already frozen`(registry: MR) {
        registry.freeze()
        
        assertThrows<IllegalStateException> { registry.freeze() }
    }
    
    @Test
    fun `reload throws UnsupportedOperationException for stable registry`() {
        val registry = createRegistry("stable", reloadable = false)
        registry.freeze()
        
        assertThrows<UnsupportedOperationException> { registry.reload { } }
    }
    
    @Test
    fun `reload throws IllegalStateException before freeze`() {
        val registry = createRegistry("reloadable", reloadable = true)
        
        assertThrows<IllegalStateException> { registry.reload {} }
    }
    
    @Test
    fun `reload throws IllegalStateException while already reloading`() {
        val registry = createRegistry("reloadable", reloadable = true)
        registry.freeze()
        
        assertThrows<IllegalStateException> {
            registry.reload {
                reload {}
            }
        }
    }
    
    @Test
    fun `reload allows modification of reloadable registry`() {
        val registry = createRegistry("reloadable", reloadable = true)
        val key = key("nova", "element")
        registry[key] = mockElement()
        registry.freeze()
        
        assertDoesNotThrow {
            registry.reload { this[key] = mockElement() }
        }
    }
    
    @Test
    fun `reload requires elements to be reregistered`() {
        val registry = createRegistry("reloadable", reloadable = true)
        registry[key("nova", "element")] = mockElement()
        registry.freeze()
        
        assertThrows<IllegalStateException> { registry.reload { } }
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `unmodifiableView returns NovaRegistry with same key`(registry: MR) {
        assertEquals(registry.key, registry.unmodifiableView.key)
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `unmodifiableView has same values after freeze`(registry: MR) {
        registry[key("nova", "element1")] = mockElement()
        registry[key("nova", "element2")] = mockElement()
        registry.freeze()
        
        assertEquals(registry.entrySet.get(), registry.unmodifiableView.entrySet.get())
    }
    
    @Test
    fun `isReloadable returns correct value`() {
        assertTrue(createRegistry("reloadable", reloadable = true).isReloadable)
        assertFalse(createRegistry("stable", reloadable = false).isReloadable)
    }
    
    @Test
    fun `setKnown does nothing without unknown-entry factory`() {
        val registry = createRegistry("stable", reloadable = false)
        val elementKey = key("nova", "element")
        
        registry.setKnown(elementKey)
        registry.freeze()
        
        assertFalse(elementKey in registry)
    }
    
    @Test
    fun `setKnown creates value through unknown-entry factory`() {
        val unknownElement = mockElement()
        var invocations = 0
        val registry: MR = MutableNovaRegistry(key("nova", "stable"), reloadable = false) {
            invocations++
            unknownElement
        }
        val elementKey = key("nova", "element")
        
        registry.setKnown(elementKey)
        registry.freeze()
        
        assertSame(unknownElement, registry.getValueOrThrow(elementKey))
        assertEquals(1, invocations)
    }
    
    @Test
    fun `registered value takes precedence over unknown-entry factory`() {
        val registeredElement = mockElement()
        var invocations = 0
        val registry: MR = MutableNovaRegistry(key("nova", "stable"), reloadable = false) {
            invocations++
            mockElement()
        }
        val elementKey = key("nova", "element")
        
        registry.setKnown(elementKey)
        registry[elementKey] = registeredElement
        registry.freeze()
        
        assertSame(registeredElement, registry.getValueOrThrow(elementKey))
        assertEquals(0, invocations)
    }
    
    @Test
    fun `unknown-entry factory does not create value for unmarked reference`() {
        val registry: MR = MutableNovaRegistry(key("nova", "stable"), reloadable = false) { mockElement() }
        registry[key("nova", "element")]
        
        assertThrows<IllegalStateException> { registry.freeze() }
    }
    
    @Test
    fun `reload creates omitted entry through unknown-entry factory`() {
        val initialElement = mockElement()
        val unknownElement = mockElement()
        val registry: MR = MutableNovaRegistry(key("nova", "reloadable"), reloadable = true) { unknownElement }
        val elementKey = key("nova", "element")
        registry[elementKey] = initialElement
        val entry = registry[elementKey]
        registry.freeze()
        
        registry.reload {}
        
        assertSame(unknownElement, entry.get())
    }
    
    @Test
    fun `reload fails when entry is omitted without unknown-entry factory`() {
        val registry = createRegistry("reloadable", reloadable = true)
        registry[key("nova", "element")] = mockElement()
        registry.freeze()
        
        assertThrows<IllegalStateException> { registry.reload {} }
    }
    
    @Test
    fun `unknown-entry factory exception fails freeze`() {
        val registry: MR = MutableNovaRegistry(key("nova", "stable"), reloadable = false) {
            throw IllegalArgumentException("failed")
        }
        registry.setKnown(key("nova", "element"))
        
        assertThrows<IllegalArgumentException> { registry.freeze() }
    }
    
    @Test
    fun `reloading is reflected in entry`() {
        val registry = createRegistry("reloadable", reloadable = true)
        val key = key("nova", "element")
        val element1 = mockElement()
        registry[key] = element1
        val entry = registry[key]
        registry.freeze()
        
        assertSame(element1, entry.get())
        
        val element2 = mockElement()
        registry.reload {
            this[key] = element2
        }
        
        assertSame(element2, entry.get())
    }
    
    @Test
    fun `reloading is reflected in optional entry`() {
        val registry = createRegistry("reloadable", reloadable = true)
        val key = key("nova", "element")
        val element1 = mockElement()
        val element2 = mockElement()
        
        val optEntry = registry.getOptional(key)
        registry.freeze()
        
        assertNull(optEntry.get())
        
        registry.reload {
            this[key] = element1
        }
        
        assertSame(element1, optEntry.get()?.get())
        
        registry.reload {
            this[key] = element2
        }
        
        assertSame(element2, optEntry.get()?.get())
    }
    
    @Test
    fun `reloading is reflected in tag`() {
        val registry = createRegistry("reloadable", reloadable = true)
        val key1 = key("nova", "element1")
        val key2 = key("nova", "element2")
        val tagKey = key("nova", "test_tag")
        val element1 = mockElement()
        val element2 = mockElement()
        registry[key1] = element1
        registry[key2] = element2
        val entry1 = registry[key1]
        val entry2 = registry[key2]
        registry[tagKey] = buildNovaTagEntries { add(entry1, entry2) }
        registry.freeze()
        
        val tag = registry.getTag(tagKey)
        assertEquals(setOf(element1, element2), tag.get())
        
        val element3 = mockElement()
        registry.reload {
            this[key1] = element3
            this[key2] = element2
            this[tagKey] = buildNovaTagEntries { add(entry1) }
        }
        
        assertEquals(setOf(element3), tag.get())
    }
    
    @Test
    fun `reloading is reflected in optional tag`() {
        val registry = createRegistry("reloadable", reloadable = true)
        val key1 = key("nova", "element1")
        val key2 = key("nova", "element2")
        val tagKey = key("nova", "test_tag")
        val element1 = mockElement()
        val element2 = mockElement()
        
        val optTag = registry.getOptionalTag(tagKey)
        registry.freeze()
        
        assertNull(optTag.get())
        
        registry.reload {
            this[key1] = element1
            this[key2] = element2
            val entry1 = this[key1]
            val entry2 = this[key2]
            this[tagKey] = buildNovaTagEntries { add(entry1, entry2) }
        }
        
        assertEquals(setOf(element1, element2), optTag.get()?.get())
        
        registry.reload {
            this[key1] = element1
            this[key2] = element2
            val entry1 = this[key1]
            this[tagKey] = buildNovaTagEntries { add(entry1) }
        }
        
        assertEquals(setOf(element1), optTag.get()?.get())
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `tag including another tag resolves to combined entries after freeze`(registry: MR) {
        val key1 = key("nova", "element1")
        val key2 = key("nova", "element2")
        val key3 = key("nova", "element3")
        val innerTagKey = key("nova", "inner_tag")
        val outerTagKey = key("nova", "outer_tag")
        val element1 = mockElement()
        val element2 = mockElement()
        val element3 = mockElement()
        registry[key1] = element1
        registry[key2] = element2
        registry[key3] = element3
        val entry1 = registry[key1]
        val entry2 = registry[key2]
        val entry3 = registry[key3]
        val innerTag = registry.getTag(innerTagKey)
        registry[innerTagKey] = buildNovaTagEntries { add(entry1, entry2) }
        registry[outerTagKey] = buildNovaTagEntries { add(innerTag); add(entry3) }
        registry.freeze()
        
        val outerTag = registry.getTag(outerTagKey)
        assertEquals(setOf(element1, element2, element3), outerTag.get())
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `tag including only another tag resolves to same entries after freeze`(registry: MR) {
        val key1 = key("nova", "element1")
        val key2 = key("nova", "element2")
        val innerTagKey = key("nova", "inner_tag")
        val outerTagKey = key("nova", "outer_tag")
        val element1 = mockElement()
        val element2 = mockElement()
        registry[key1] = element1
        registry[key2] = element2
        val entry1 = registry[key1]
        val entry2 = registry[key2]
        val innerTag = registry.getTag(innerTagKey)
        registry[innerTagKey] = buildNovaTagEntries { add(entry1, entry2) }
        registry[outerTagKey] = buildNovaTagEntries { add(innerTag) }
        registry.freeze()
        
        val outerTag = registry.getTag(outerTagKey)
        assertEquals(setOf(element1, element2), outerTag.get())
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `tag including nested tags resolves transitively after freeze`(registry: MR) {
        val key1 = key("nova", "element1")
        val key2 = key("nova", "element2")
        val key3 = key("nova", "element3")
        val tag1Key = key("nova", "tag1")
        val tag2Key = key("nova", "tag2")
        val tag3Key = key("nova", "tag3")
        val element1 = mockElement()
        val element2 = mockElement()
        val element3 = mockElement()
        registry[key1] = element1
        registry[key2] = element2
        registry[key3] = element3
        val entry1 = registry[key1]
        val entry2 = registry[key2]
        val entry3 = registry[key3]
        val tag1 = registry.getTag(tag1Key)
        val tag2 = registry.getTag(tag2Key)
        registry[tag1Key] = buildNovaTagEntries { add(entry1) }
        registry[tag2Key] = buildNovaTagEntries { add(tag1); add(entry2) }
        registry[tag3Key] = buildNovaTagEntries { add(tag2); add(entry3) }
        registry.freeze()
        
        val tag3 = registry.getTag(tag3Key)
        assertEquals(setOf(element1, element2, element3), tag3.get())
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `tag with remove excludes direct entries after freeze`(registry: MR) {
        val key1 = key("nova", "element1")
        val key2 = key("nova", "element2")
        val tagKey = key("nova", "test_tag")
        val element1 = mockElement()
        val element2 = mockElement()
        registry[key1] = element1
        registry[key2] = element2
        val entry1 = registry[key1]
        val entry2 = registry[key2]
        registry[tagKey] = buildNovaTagEntries { add(entry1, entry2); remove(entry2) }
        registry.freeze()
        
        val tag = registry.getTag(tagKey)
        assertEquals(setOf(element1), tag.get())
    }
    
    @Test
    fun `tags provider includes new tags after reloading`() {
        val registry = createRegistry("reloadable", reloadable = true)
        val key1 = key("nova", "element1")
        val key2 = key("nova", "element2")
        val tag1Key = key("nova", "tag1")
        val tag2Key = key("nova", "tag2")
        val element1 = mockElement()
        val element2 = mockElement()
        val entry1 = registry[key1]
        val entry2 = registry[key2]
        
        registry[key1] = element1
        registry[key2] = element2
        registry[tag1Key] = buildNovaTagEntries { add(entry1) }
        registry.freeze()
        
        val tags = registry.tags
        
        val tagKeys = tags.get().map { it.tagKey }.toSet()
        assertTrue(tag1Key in tagKeys)
        assertFalse(tag2Key in tagKeys)
        
        registry.reload {
            this[key1] = element1
            this[key2] = element2
            this[tag1Key] = buildNovaTagEntries { add(entry1) }
            this[tag2Key] = buildNovaTagEntries { add(entry2) }
        }
        
        val updatedTagKeys = tags.get().map { it.tagKey }.toSet()
        assertTrue(tag1Key in updatedTagKeys)
        assertTrue(tag2Key in updatedTagKeys)
    }
    
    @Test
    fun `reloading is reflected in tag including another tag`() {
        val registry = createRegistry("reloadable", reloadable = true)
        val key1 = key("nova", "element1")
        val key2 = key("nova", "element2")
        val key3 = key("nova", "element3")
        val innerTagKey = key("nova", "inner_tag")
        val outerTagKey = key("nova", "outer_tag")
        val element1 = mockElement()
        val element2 = mockElement()
        val element3 = mockElement()
        registry[key1] = element1
        registry[key2] = element2
        registry[key3] = element3
        val entry1 = registry[key1]
        val entry2 = registry[key2]
        val entry3 = registry[key3]
        val innerTag = registry.getTag(innerTagKey)
        registry[innerTagKey] = buildNovaTagEntries { add(entry1, entry2) }
        registry[outerTagKey] = buildNovaTagEntries { add(innerTag); add(entry3) }
        registry.freeze()
        
        val outerTag = registry.getTag(outerTagKey)
        assertEquals(setOf(element1, element2, element3), outerTag.get())
        
        val element4 = mockElement()
        val element5 = mockElement()
        registry.reload {
            this[key1] = element4
            this[key2] = element5
            this[key3] = element3
            this[innerTagKey] = buildNovaTagEntries { add(entry1) }
            this[outerTagKey] = buildNovaTagEntries { add(innerTag); add(entry3) }
        }
        
        assertEquals(setOf(element4, element3), outerTag.get())
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `provider-backed tag contents update after freeze`(registry: MR) {
        val key1 = key("nova", "element1")
        val key2 = key("nova", "element2")
        val tagKey = key("nova", "test_tag")
        val element1 = mockElement()
        val element2 = mockElement()
        registry[key1] = element1
        registry[key2] = element2
        val entry1 = registry[key1]
        val entry2 = registry[key2]
        val tagEntries = mutableProvider(tagEntries(entry1))
        registry[tagKey] = tagEntries
        registry.freeze()
        
        val tag = registry.getTag(tagKey)
        assertEquals(setOf(element1), tag.get())
        
        tagEntries.set(tagEntries(entry2))
        assertEquals(setOf(element2), tag.get())
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `provider-backed tag builder operations update after freeze`(registry: MR) {
        val key1 = key("nova", "element1")
        val key2 = key("nova", "element2")
        val tagKey = key("nova", "test_tag")
        val element1 = mockElement()
        val element2 = mockElement()
        registry[key1] = element1
        registry[key2] = element2
        val entry1 = registry[key1]
        val entry2 = registry[key2]
        val addedEntries = mutableProvider<Iterable<RegistryEntry.Nova<NovaRegistryElement<*>>>>(setOf(entry1))
        val removedEntries = mutableProvider<Iterable<RegistryEntry.Nova<NovaRegistryElement<*>>>>(emptySet())
        registry[tagKey] = buildNovaTagEntries {
            add(addedEntries)
            remove(removedEntries)
        }
        registry.freeze()
        
        val tag = registry.getTag(tagKey)
        assertEquals(setOf(element1), tag.get())
        
        addedEntries.set(setOf(entry1, entry2))
        assertEquals(setOf(element1, element2), tag.get())
        
        removedEntries.set(setOf(entry1))
        assertEquals(setOf(element2), tag.get())
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `provider-backed entry vararg operation updates after freeze`(registry: MR) {
        val key1 = key("nova", "element1")
        val key2 = key("nova", "element2")
        val tagKey = key("nova", "test_tag")
        val element1 = mockElement()
        val element2 = mockElement()
        registry[key1] = element1
        registry[key2] = element2
        val entry = mutableProvider<RegistryEntry.Nova<NovaRegistryElement<*>>>(registry[key1])
        registry[tagKey] = buildNovaTagEntries { add(entry) }
        registry.freeze()
        
        val tag = registry.getTag(tagKey)
        assertEquals(setOf(element1), tag.get())
        
        entry.set(registry[key2])
        assertEquals(setOf(element2), tag.get())
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `provider-backed nested tag builder operation updates after freeze`(registry: MR) {
        val key1 = key("nova", "element1")
        val key2 = key("nova", "element2")
        val tag1Key = key("nova", "tag1")
        val tag2Key = key("nova", "tag2")
        val outerTagKey = key("nova", "outer_tag")
        val element1 = mockElement()
        val element2 = mockElement()
        registry[key1] = element1
        registry[key2] = element2
        val entry1 = registry[key1]
        val entry2 = registry[key2]
        val tag1 = registry.getTag(tag1Key)
        val tag2 = registry.getTag(tag2Key)
        val nestedTag = mutableProvider<RegistryEntrySet.Nova.Tag<NovaRegistryElement<*>>>(tag1)
        registry[tag1Key] = buildNovaTagEntries { add(entry1) }
        registry[tag2Key] = buildNovaTagEntries { add(entry2) }
        registry[outerTagKey] = buildNovaTagEntries { add(nestedTag) }
        registry.freeze()
        
        val outerTag = registry.getTag(outerTagKey)
        assertEquals(setOf(element1), outerTag.get())
        
        nestedTag.set(tag2)
        assertEquals(setOf(element2), outerTag.get())
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `provider-backed nested tag contents update transitively`(registry: MR) {
        val key1 = key("nova", "element1")
        val key2 = key("nova", "element2")
        val innerTagKey = key("nova", "inner_tag")
        val outerTagKey = key("nova", "outer_tag")
        val element1 = mockElement()
        val element2 = mockElement()
        registry[key1] = element1
        registry[key2] = element2
        val entry1 = registry[key1]
        val entry2 = registry[key2]
        val innerTag = registry.getTag(innerTagKey)
        val innerTagEntries = mutableProvider(tagEntries(entry1))
        registry[innerTagKey] = innerTagEntries
        registry[outerTagKey] = buildNovaTagEntries { add(innerTag) }
        registry.freeze()
        
        val outerTag = registry.getTag(outerTagKey)
        assertEquals(setOf(element1), outerTag.get())
        
        innerTagEntries.set(tagEntries(entry2))
        assertEquals(setOf(element2), outerTag.get())
    }
    
    @ParameterizedTest
    @MethodSource("registryProvider")
    fun `cyclic tags resolve to least fixed point`(registry: MR) {
        val key1 = key("nova", "element1")
        val key2 = key("nova", "element2")
        val tag1Key = key("nova", "tag1")
        val tag2Key = key("nova", "tag2")
        val element1 = mockElement()
        val element2 = mockElement()
        registry[key1] = element1
        registry[key2] = element2
        val entry1 = registry[key1]
        val entry2 = registry[key2]
        val tag1 = registry.getTag(tag1Key)
        val tag2 = registry.getTag(tag2Key)
        registry[tag1Key] = buildNovaTagEntries { add(entry1); add(tag2) }
        registry[tag2Key] = buildNovaTagEntries { add(entry2); add(tag1) }
        registry.freeze()
        
        assertEquals(setOf(element1, element2), tag1.get())
        assertEquals(setOf(element1, element2), tag2.get())
    }
    
    @Test
    fun `registry reload disconnects previous tag definition providers`() {
        val registry = createRegistry("reloadable", reloadable = true)
        val key1 = key("nova", "element1")
        val key2 = key("nova", "element2")
        val tagKey = key("nova", "test_tag")
        val element1 = mockElement()
        val element2 = mockElement()
        val entry1 = registry[key1]
        val entry2 = registry[key2]
        registry[key1] = element1
        registry[key2] = element2
        val oldEntries = mutableProvider(tagEntries(entry1))
        registry[tagKey] = oldEntries
        registry.freeze()
        val tag = registry.getTag(tagKey)
        
        val newEntries = mutableProvider(tagEntries(entry2))
        registry.reload {
            this[key1] = element1
            this[key2] = element2
            this[tagKey] = newEntries
        }
        assertEquals(setOf(element2), tag.get())
        
        oldEntries.set(tagEntries(entry1, entry2))
        assertEquals(setOf(element2), tag.get())
        
        newEntries.set(tagEntries(entry1))
        assertEquals(setOf(element1), tag.get())
    }
    
    @Test
    fun `omitted tag becomes empty and disconnects previous definition provider`() {
        val registry = createRegistry("reloadable", reloadable = true)
        val elementKey = key("nova", "element")
        val tagKey = key("nova", "test_tag")
        val element = mockElement()
        registry[elementKey] = element
        val entry = registry[elementKey]
        val oldEntries = mutableProvider(tagEntries(entry))
        registry[tagKey] = oldEntries
        registry.freeze()
        val tag = registry.getTag(tagKey)
        
        registry.reload {
            this[elementKey] = element
        }
        
        assertTrue(tag.get().isEmpty())
        oldEntries.set(tagEntries(entry))
        assertTrue(tag.get().isEmpty())
    }
    
}