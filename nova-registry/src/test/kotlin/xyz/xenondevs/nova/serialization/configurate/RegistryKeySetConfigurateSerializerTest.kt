package xyz.xenondevs.nova.serialization.configurate

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.set.RegistryKeySet
import io.papermc.paper.registry.set.RegistrySet
import io.papermc.paper.registry.tag.Tag
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.key.Key.key
import org.bukkit.inventory.ItemType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RegistryKeySetConfigurateSerializerTest {
    
    companion object {
        
        private lateinit var loader: YamlConfigurationLoader
        private val typeToken = geantyrefTypeTokenOf<RegistryKeySet<ItemType>>()
        
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
        fun setup() {
            loader = YamlConfigurationLoader.builder()
                .defaultOptions { opts ->
                    opts.serializers {
                        it.register(typeToken, RegistryKeySetConfigurateSerializer(RegistryKey.ITEM))
                    }
                }
                .build()
        }
        
        private fun createNode() = loader.createNode()
        
    }
    
    @Test
    fun `serialize RegistryKeySet Direct (single)`() {
        val set = RegistrySet.keySet(RegistryKey.ITEM, listOf(TypedKey.create(RegistryKey.ITEM, key("minecraft", "diamond"))))
        val node = createNode()
        node.set(typeToken, set)
        assertEquals("minecraft:diamond", node.string)
    }
    
    @Test
    fun `serialize RegistryKeySet Direct (multi)`() {
        val set = RegistrySet.keySet(RegistryKey.ITEM, listOf(
            TypedKey.create(RegistryKey.ITEM, key("minecraft", "diamond")),
            TypedKey.create(RegistryKey.ITEM, key("minecraft", "emerald"))
        ))
        val node = createNode()
        node.set(typeToken, set)
        val list = node.getList(String::class.java)
        assertEquals(listOf("minecraft:diamond", "minecraft:emerald"), list)
    }
    
    @Test
    fun `serialize RegistryKeySet Tag`() {
        val tag = RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.ITEM)
            .getTag(TagKey.create(RegistryKey.ITEM, key("minecraft", "wool")))
        val node = createNode()
        node.set(typeToken, tag)
        assertEquals("#minecraft:wool", node.string)
    }
    
    @Test
    fun `deserialize RegistryKeySet Direct (single)`() {
        val node = createNode()
        node.raw("minecraft:diamond")
        val set = node.get(typeToken)
        assertEquals(
            RegistrySet.keySet(RegistryKey.ITEM, listOf(TypedKey.create(RegistryKey.ITEM, key("minecraft", "diamond")))),
            set
        )
    }
    
    @Test
    fun `deserialize RegistryKeySet Direct (multi)`() {
        val node = createNode()
        node.setList(String::class.java, listOf("minecraft:diamond", "minecraft:emerald"))
        val set = node.get(typeToken)
        assertEquals(
            RegistrySet.keySet(RegistryKey.ITEM, listOf(
                TypedKey.create(RegistryKey.ITEM, key("minecraft", "diamond")),
                TypedKey.create(RegistryKey.ITEM, key("minecraft", "emerald"))
            )),
            set
        )
    }
    
    @Test
    fun `deserialize RegistryKeySet Tag`() {
        val node = createNode()
        node.raw("#minecraft:wool")
        val set = node.get(typeToken)
        assertIs<Tag<ItemType>>(set)
    }
    
}

