package xyz.xenondevs.nova.serialization.kotlinx

import io.papermc.paper.registry.RegistryKey
import kotlinx.serialization.KSerializer
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
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.buildNovaTagEntries
import xyz.xenondevs.nova.registry.emptyRegistryEntrySet
import xyz.xenondevs.nova.registry.entries.ItemTypeEntries
import xyz.xenondevs.nova.registry.entries.ItemTypeTags
import xyz.xenondevs.nova.registry.registryEntrySetOf
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RegistryEntrySetSerializerTest {
    
    private class TestElement(override val entry: RegistryEntry.Nova<TestElement>) : NovaRegistryElement<TestElement>
    
    companion object {
        
        private lateinit var registry: MutableNovaRegistry<TestElement>
        private lateinit var el1: RegistryEntry.Nova<TestElement>
        private lateinit var el2: RegistryEntry.Nova<TestElement>
        private lateinit var el3: RegistryEntry.Nova<TestElement>
        private lateinit var tag: RegistryEntrySet.Nova.Tag<TestElement>
        private lateinit var novaSerializer: KSerializer<RegistryEntrySet.Nova<TestElement>>
        private lateinit var paperSerializer: KSerializer<RegistryEntrySet.Paper<ItemType>>
        private lateinit var mixedSerializer: KSerializer<RegistryEntrySet.Mixed<TestElement, ItemType>>
        
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
            registry = MutableNovaRegistry(key("nova", "test"), true)
            
            el1 = registerElement("element1")
            el2 = registerElement("element2")
            el3 = registerElement("element3")
            
            tag = registerTag("tag1", setOf(el1, el3))
            
            registry.freeze()
            
            novaSerializer = NovaRegistryEntrySetSerializer(registry)
            paperSerializer = PaperRegistryEntrySetSerializer(RegistryKey.ITEM)
            mixedSerializer = MixedRegistryEntrySetSerializer(registry, RegistryKey.ITEM)
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
    fun `serialize RegistryEntrySet Nova Direct (single)`() {
        val set = registryEntrySetOf(el1)
        val json = Json.encodeToString(novaSerializer, set)
        assertEquals(""""nova:element1"""", json)
    }
    
    @Test
    fun `serialize RegistryEntrySet Nova Direct (multi)`() {
        val set = registryEntrySetOf(el1, el2)
        val json = Json.encodeToString(novaSerializer, set)
        assertEquals("""["nova:element1","nova:element2"]""", json)
    }
    
    @Test
    fun `serialize RegistryEntrySet Nova Tag`() {
        val json = Json.encodeToString(novaSerializer, tag)
        assertEquals(""""#nova:tag1"""", json)
    }
    
    @Test
    fun `serialize RegistryEntrySet Nova Tag (all entries)`() {
        val json = Json.encodeToString(novaSerializer, registry.entrySet)
        assertEquals(""""#nova:test"""", json)
    }
    
    @Test
    fun `deserialize RegistryEntrySet Nova Direct (single)`() {
        val json = """"nova:element1""""
        val set = Json.decodeFromString(novaSerializer, json)
        
        assertIs<RegistryEntrySet.Nova.Direct<TestElement>>(set)
        assertEquals(registryEntrySetOf(el1), set)
    }
    
    @Test
    fun `deserialize RegistryEntrySet Nova Direct (multi)`() {
        val json = """["nova:element1","nova:element2"]"""
        val set = Json.decodeFromString(novaSerializer, json)
        
        assertIs<RegistryEntrySet.Nova.Direct<TestElement>>(set)
        assertEquals(registryEntrySetOf(el1, el2), set)
    }
    
    @Test
    fun `deserialize RegistryEntrySet Nova Tag`() {
        val json = """"#nova:tag1""""
        val set = Json.decodeFromString(novaSerializer, json)
        
        assertEquals(tag, set)
    }
    
    @Test
    fun `deserialize RegistryEntrySet Nova Tag (all entries)`() {
        val json = """"#nova:test""""
        val set = Json.decodeFromString(novaSerializer, json)
        assertEquals(registry.entrySet, set)
    }
    
    // --- Paper ---
    
    @Test
    fun `serialize RegistryEntrySet Paper Direct (single)`() {
        val set = registryEntrySetOf(ItemTypeEntries.DIAMOND)
        val json = Json.encodeToString(paperSerializer, set)
        assertEquals(""""minecraft:diamond"""", json)
    }
    
    @Test
    fun `serialize RegistryEntrySet Paper Direct (multi)`() {
        val set = registryEntrySetOf(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD)
        val json = Json.encodeToString(paperSerializer, set)
        assertEquals("""["minecraft:diamond","minecraft:emerald"]""", json)
    }
    
    @Test
    fun `serialize RegistryEntrySet Paper Tag`() {
        val json = Json.encodeToString(paperSerializer, ItemTypeTags.WOOL)
        assertEquals(""""#minecraft:wool"""", json)
    }
    
    @Test
    fun `serialize RegistryEntrySet Paper Tag (all entries)`() {
        val json = Json.encodeToString(paperSerializer, registryEntrySetOf(RegistryKey.ITEM))
        assertEquals(""""#minecraft:item"""", json)
    }
    
    @Test
    fun `deserialize RegistryEntrySet Paper Direct (single)`() {
        val json = """"minecraft:diamond""""
        val set = Json.decodeFromString(paperSerializer, json)
        
        assertIs<RegistryEntrySet.Paper.Direct<ItemType>>(set)
        assertEquals(registryEntrySetOf(ItemTypeEntries.DIAMOND), set)
    }
    
    @Test
    fun `deserialize RegistryEntrySet Paper Direct (multi)`() {
        val json = """["minecraft:diamond","minecraft:emerald"]"""
        val set = Json.decodeFromString(paperSerializer, json)
        
        assertIs<RegistryEntrySet.Paper.Direct<ItemType>>(set)
        assertEquals(registryEntrySetOf(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD), set)
    }
    
    @Test
    fun `deserialize RegistryEntrySet Paper Tag`() {
        val json = """"#minecraft:wool""""
        val set = Json.decodeFromString(paperSerializer, json)
        
        assertEquals(ItemTypeTags.WOOL, set)
    }
    
    @Test
    fun `deserialize RegistryEntrySet Paper Tag (all entries)`() {
        val json = """"#minecraft:item""""
        val set = Json.decodeFromString(paperSerializer, json)
        assertEquals(registryEntrySetOf(RegistryKey.ITEM), set)
    }
    
    // --- Mixed ---
    
    @Test
    fun `serialize RegistryEntrySet Mixed Direct (single nova)`() {
        val mixed = registryEntrySetOf(
            registryEntrySetOf(el1),
            emptyRegistryEntrySet(RegistryKey.ITEM)
        )
        val json = Json.encodeToString(mixedSerializer, mixed)
        assertEquals(""""nova:element1"""", json)
    }
    
    @Test
    fun `serialize RegistryEntrySet Mixed Direct (single paper)`() {
        val mixed = registryEntrySetOf(
            emptyRegistryEntrySet(registry),
            registryEntrySetOf(ItemTypeEntries.DIAMOND)
        )
        val json = Json.encodeToString(mixedSerializer, mixed)
        assertEquals(""""minecraft:diamond"""", json)
    }
    
    @Test
    fun `serialize RegistryEntrySet Mixed Direct (one nova, one paper)`() {
        val mixed = registryEntrySetOf(
            registryEntrySetOf(el1),
            registryEntrySetOf(ItemTypeEntries.DIAMOND)
        )
        val json = Json.encodeToString(mixedSerializer, mixed)
        assertEquals("""["nova:element1","minecraft:diamond"]""", json)
    }
    
    @Test
    fun `serialize RegistryEntrySet Mixed Direct (multi nova)`() {
        val mixed = registryEntrySetOf(
            registryEntrySetOf(el1, el2),
            emptyRegistryEntrySet(RegistryKey.ITEM)
        )
        val json = Json.encodeToString(mixedSerializer, mixed)
        assertEquals("""["nova:element1","nova:element2"]""", json)
    }
    
    @Test
    fun `serialize RegistryEntrySet Mixed Direct (multi paper)`() {
        val mixed = registryEntrySetOf(
            emptyRegistryEntrySet(registry),
            registryEntrySetOf(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD)
        )
        val json = Json.encodeToString(mixedSerializer, mixed)
        assertEquals("""["minecraft:diamond","minecraft:emerald"]""", json)
    }
    
    @Test
    fun `deserialize RegistryEntrySet Mixed Direct (single nova)`() {
        val json = """"nova:element1""""
        val mixed = Json.decodeFromString(mixedSerializer, json)
        
        assertEquals(
            registryEntrySetOf(
                registryEntrySetOf(el1),
                emptyRegistryEntrySet(RegistryKey.ITEM)
            ),
            mixed
        )
    }
    
    @Test
    fun `deserialize RegistryEntrySet Mixed Direct (single paper)`() {
        val json = """"minecraft:diamond""""
        val mixed = Json.decodeFromString(mixedSerializer, json)
        
        assertEquals(
            registryEntrySetOf(
                emptyRegistryEntrySet(registry),
                registryEntrySetOf(ItemTypeEntries.DIAMOND)
            ),
            mixed
        )
    }
    
    @Test
    fun `deserialize RegistryEntrySet Mixed Direct (one nova, one paper)`() {
        val json = """["nova:element1","minecraft:diamond"]"""
        val mixed = Json.decodeFromString(mixedSerializer, json)
        
        assertEquals(
            registryEntrySetOf(
                registryEntrySetOf(el1),
                registryEntrySetOf(ItemTypeEntries.DIAMOND)
            ),
            mixed
        )
    }
    
    @Test
    fun `deserialize RegistryEntrySet Mixed Direct (multi nova)`() {
        val json = """["nova:element1","nova:element2"]"""
        val mixed = Json.decodeFromString(mixedSerializer, json)
        
        assertEquals(
            registryEntrySetOf(
                registryEntrySetOf(el1, el2),
                emptyRegistryEntrySet(RegistryKey.ITEM)
            ),
            mixed
        )
    }
    
    @Test
    fun `deserialize RegistryEntrySet Mixed Direct (multi paper)`() {
        val json = """["minecraft:diamond","minecraft:emerald"]"""
        val mixed = Json.decodeFromString(mixedSerializer, json)
        
        assertEquals(
            registryEntrySetOf(
                emptyRegistryEntrySet(registry),
                registryEntrySetOf(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD)
            ),
            mixed
        )
    }
    
}