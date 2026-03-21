package xyz.xenondevs.nova.registry

import io.mockk.mockk
import net.kyori.adventure.key.Key.key
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
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
    
}