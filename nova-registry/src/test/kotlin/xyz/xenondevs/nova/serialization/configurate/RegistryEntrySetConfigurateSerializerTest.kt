package xyz.xenondevs.nova.serialization.configurate

import io.leangen.geantyref.TypeToken
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key.key
import org.bukkit.inventory.ItemType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
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

class RegistryEntrySetConfigurateSerializerTest {
    
    private class TestElement(override val entry: RegistryEntry.Nova<TestElement>) : NovaRegistryElement<TestElement>
    
    companion object {
        
        private lateinit var registry: MutableNovaRegistry<TestElement>
        private lateinit var entry1: RegistryEntry.Nova<TestElement>
        private lateinit var entry2: RegistryEntry.Nova<TestElement>
        private lateinit var entry3: RegistryEntry.Nova<TestElement>
        private lateinit var tag: RegistryEntrySet.Nova.Tag<TestElement>
        private lateinit var novaTypeToken: TypeToken<RegistryEntrySet.Nova<TestElement>>
        private lateinit var paperTypeToken: TypeToken<RegistryEntrySet.Paper<ItemType>>
        private lateinit var mixedTypeToken: TypeToken<RegistryEntrySet.Mixed<TestElement, ItemType>>
        private lateinit var loader: YamlConfigurationLoader
        
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
            registry = MutableNovaRegistry(key("nova", "config_entry_set_test"), true)
            
            entry1 = registerElement("element1")
            entry2 = registerElement("element2")
            entry3 = registerElement("element3")
            
            tag = registerTag("tag1", setOf(entry1, entry3))
            
            registry.freeze()
            
            novaTypeToken = geantyrefTypeTokenOf()
            paperTypeToken = geantyrefTypeTokenOf()
            mixedTypeToken = geantyrefTypeTokenOf()
            loader = YamlConfigurationLoader.builder()
                .defaultOptions { opts ->
                    opts.serializers {
                        it.register(novaTypeToken, NovaRegistryEntrySetConfigurateSerializer(registry))
                        it.register(paperTypeToken, PaperRegistryEntrySetConfigurateSerializer(RegistryKey.ITEM))
                        it.register(mixedTypeToken, MixedRegistryEntrySetConfigurateSerializer(registry, RegistryKey.ITEM))
                    }
                }
                .build()
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
        
        private fun createNode() = loader.createNode()
        
    }
    
    // --- Nova ---
    
    @Test
    fun `serialize RegistryEntrySet Nova Direct (single)`() {
        val set = registryEntrySetOf(entry1)
        val node = createNode()
        node.set(novaTypeToken, set)
        assertEquals("nova:element1", node.string)
    }
    
    @Test
    fun `serialize RegistryEntrySet Nova Direct (multi)`() {
        val set = registryEntrySetOf(entry1, entry2)
        val node = createNode()
        node.set(novaTypeToken, set)
        val list = node.getList(String::class.java)
        assertEquals(listOf("nova:element1", "nova:element2"), list)
    }
    
    @Test
    fun `serialize RegistryEntrySet Nova Tag`() {
        val node = createNode()
        node.set(novaTypeToken, tag)
        assertEquals("#nova:tag1", node.string)
    }
    
    @Test
    fun `deserialize RegistryEntrySet Nova Direct (single)`() {
        val node = createNode()
        node.raw("nova:element1")
        val set = node.get(novaTypeToken)
        
        assertIs<RegistryEntrySet.Nova.Direct<TestElement>>(set)
        assertEquals(setOf(entry1), set.entries)
    }
    
    @Test
    fun `deserialize RegistryEntrySet Nova Direct (multi)`() {
        val node = createNode()
        node.setList(String::class.java, listOf("nova:element1", "nova:element2"))
        val set = node.get(novaTypeToken)
        
        assertIs<RegistryEntrySet.Nova.Direct<TestElement>>(set)
        assertEquals(setOf(entry1, entry2), set.entries)
    }
    
    @Test
    fun `deserialize RegistryEntrySet Nova Tag`() {
        val node = createNode()
        node.raw("#nova:tag1")
        val set = node.get(novaTypeToken)
        
        assertIs<RegistryEntrySet.Nova.Tag<TestElement>>(set)
        assertEquals(tag, set)
    }
    
    // --- Paper ---
    
    @Test
    fun `serialize RegistryEntrySet Paper Direct (single)`() {
        val set = registryEntrySetOf(ItemTypeEntries.DIAMOND)
        val node = createNode()
        node.set(paperTypeToken, set)
        assertEquals("minecraft:diamond", node.string)
    }
    
    @Test
    fun `serialize RegistryEntrySet Paper Direct (multi)`() {
        val set = registryEntrySetOf(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD)
        val node = createNode()
        node.set(paperTypeToken, set)
        val list = node.getList(String::class.java)
        assertEquals(listOf("minecraft:diamond", "minecraft:emerald"), list)
    }
    
    @Test
    fun `serialize RegistryEntrySet Paper Tag`() {
        val node = createNode()
        node.set(paperTypeToken, ItemTypeTags.WOOL)
        assertEquals("#minecraft:wool", node.string)
    }
    
    @Test
    fun `deserialize RegistryEntrySet Paper Direct (single)`() {
        val node = createNode()
        node.raw("minecraft:diamond")
        val set = node.get(paperTypeToken)
        
        assertIs<RegistryEntrySet.Paper.Direct<ItemType>>(set)
        assertEquals(registryEntrySetOf(ItemTypeEntries.DIAMOND), set)
    }
    
    @Test
    fun `deserialize RegistryEntrySet Paper Direct (multi)`() {
        val node = createNode()
        node.setList(String::class.java, listOf("minecraft:diamond", "minecraft:emerald"))
        val set = node.get(paperTypeToken)
        
        assertIs<RegistryEntrySet.Paper.Direct<ItemType>>(set)
        assertEquals(registryEntrySetOf(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD), set)
    }
    
    @Test
    fun `deserialize RegistryEntrySet Paper Tag`() {
        val node = createNode()
        node.raw("#minecraft:wool")
        val set = node.get(paperTypeToken)
        
        assertIs<RegistryEntrySet.Paper.Tag<ItemType>>(set)
        assertEquals(ItemTypeTags.WOOL, set)
    }
    
    // --- Mixed ---
    
    @Test
    fun `serialize RegistryEntrySet Mixed Direct (single nova)`() {
        val mixed = registryEntrySetOf(
            registryEntrySetOf(entry1),
            emptyRegistryEntrySet(RegistryKey.ITEM)
        )
        val node = createNode()
        node.set(mixedTypeToken, mixed)
        assertEquals("nova:element1", node.string)
    }
    
    @Test
    fun `serialize RegistryEntrySet Mixed Direct (single paper)`() {
        val mixed = registryEntrySetOf(
            emptyRegistryEntrySet(registry),
            registryEntrySetOf(ItemTypeEntries.DIAMOND)
        )
        val node = createNode()
        node.set(mixedTypeToken, mixed)
        assertEquals("minecraft:diamond", node.string)
    }
    
    @Test
    fun `serialize RegistryEntrySet Mixed Direct (one nova, one paper)`() {
        val mixed = registryEntrySetOf(
            registryEntrySetOf(entry1),
            registryEntrySetOf(ItemTypeEntries.DIAMOND)
        )
        val node = createNode()
        node.set(mixedTypeToken, mixed)
        val list = node.getList(String::class.java)
        assertEquals(listOf("nova:element1", "minecraft:diamond"), list)
    }
    
    @Test
    fun `serialize RegistryEntrySet Mixed Direct (multi nova)`() {
        val mixed = registryEntrySetOf(
            registryEntrySetOf(entry1, entry2),
            emptyRegistryEntrySet(RegistryKey.ITEM)
        )
        val node = createNode()
        node.set(mixedTypeToken, mixed)
        val list = node.getList(String::class.java)
        assertEquals(listOf("nova:element1", "nova:element2"), list)
    }
    
    @Test
    fun `serialize RegistryEntrySet Mixed Direct (multi paper)`() {
        val mixed = registryEntrySetOf(
            emptyRegistryEntrySet(registry),
            registryEntrySetOf(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD)
        )
        val node = createNode()
        node.set(mixedTypeToken, mixed)
        val list = node.getList(String::class.java)
        assertEquals(listOf("minecraft:diamond", "minecraft:emerald"), list)
    }
    
    @Test
    fun `deserialize RegistryEntrySet Mixed Direct (single nova)`() {
        val node = createNode()
        node.raw("nova:element1")
        val mixed = node.get(mixedTypeToken)
        
        assertEquals(
            registryEntrySetOf(
                registryEntrySetOf(entry1),
                emptyRegistryEntrySet(RegistryKey.ITEM)
            ),
            mixed
        )
    }
    
    @Test
    fun `deserialize RegistryEntrySet Mixed Direct (single paper)`() {
        val node = createNode()
        node.raw("minecraft:diamond")
        val mixed = node.get(mixedTypeToken)
        
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
        val node = createNode()
        node.setList(String::class.java, listOf("nova:element1", "minecraft:diamond"))
        val mixed = node.get(mixedTypeToken)
        
        assertEquals(
            registryEntrySetOf(
                registryEntrySetOf(entry1),
                registryEntrySetOf(ItemTypeEntries.DIAMOND)
            ),
            mixed
        )
    }
    
    @Test
    fun `deserialize RegistryEntrySet Mixed Direct (multi nova)`() {
        val node = createNode()
        node.setList(String::class.java, listOf("nova:element1", "nova:element2"))
        val mixed = node.get(mixedTypeToken)
        
        assertEquals(
            registryEntrySetOf(
                registryEntrySetOf(entry1, entry2),
                emptyRegistryEntrySet(RegistryKey.ITEM)
            ),
            mixed
        )
    }
    
    @Test
    fun `deserialize RegistryEntrySet Mixed Direct (multi paper)`() {
        val node = createNode()
        node.setList(String::class.java, listOf("minecraft:diamond", "minecraft:emerald"))
        val mixed = node.get(mixedTypeToken)
        
        assertEquals(
            registryEntrySetOf(
                emptyRegistryEntrySet(registry),
                registryEntrySetOf(ItemTypeEntries.DIAMOND, ItemTypeEntries.EMERALD)
            ),
            mixed
        )
    }
    
}
