package xyz.xenondevs.nova.config

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import net.kyori.adventure.key.Key
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.version.Version
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.serialization.kotlinx.AbilityTypeEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.AbilityTypeSerializer
import xyz.xenondevs.nova.serialization.kotlinx.AttachmentTypeEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.AttachmentTypeSerializer
import xyz.xenondevs.nova.serialization.kotlinx.BlockStateSerializer
import xyz.xenondevs.nova.serialization.kotlinx.EquipmentEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.EquipmentSerializer
import xyz.xenondevs.nova.serialization.kotlinx.GuiTextureEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.GuiTextureSerializer
import xyz.xenondevs.nova.serialization.kotlinx.ItemFilterTypeEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.ItemFilterTypeSerializer
import xyz.xenondevs.nova.serialization.kotlinx.KeySerializer
import xyz.xenondevs.nova.serialization.kotlinx.NetworkTypeEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.NetworkTypeSerializer
import xyz.xenondevs.nova.serialization.kotlinx.NovaBlockEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.NovaBlockSerializer
import xyz.xenondevs.nova.serialization.kotlinx.NovaItemEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.NovaItemSerializer
import xyz.xenondevs.nova.serialization.kotlinx.RecipeTypeEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.RecipeTypeSerializer
import xyz.xenondevs.nova.serialization.kotlinx.ToolCategoryEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.ToolCategorySerializer
import xyz.xenondevs.nova.serialization.kotlinx.ToolTierEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.ToolTierSerializer
import xyz.xenondevs.nova.serialization.kotlinx.TooltipStyleEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.TooltipStyleSerializer
import xyz.xenondevs.nova.serialization.kotlinx.UUIDSerializer
import xyz.xenondevs.nova.serialization.kotlinx.VersionSerializer
import xyz.xenondevs.nova.serialization.kotlinx.WailaInfoProviderEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.WailaInfoProviderSerializer
import xyz.xenondevs.nova.serialization.kotlinx.WailaToolIconProviderEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.WailaToolIconProviderSerializer
import xyz.xenondevs.nova.util.data.readJson
import xyz.xenondevs.nova.util.data.writeJson
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.reflect.KType

internal object PermanentStorage {
    
    val JSON = Json {
        allowStructuredMapKeys = true
        serializersModule = SerializersModule {
            contextual(UUID::class, UUIDSerializer)
            contextual(Key::class, KeySerializer)
            contextual(Version::class, VersionSerializer)
            contextual(BlockState::class, BlockStateSerializer)
            contextual(RegistryEntry.Nova::class) { typeArgs ->
                when (typeArgs.single()) {
                    NovaBlockSerializer -> NovaBlockEntrySerializer
                    NovaItemSerializer -> NovaItemEntrySerializer
                    EquipmentSerializer -> EquipmentEntrySerializer
                    ToolTierSerializer -> ToolTierEntrySerializer
                    ToolCategorySerializer -> ToolCategoryEntrySerializer
                    NetworkTypeSerializer -> NetworkTypeEntrySerializer
                    AbilityTypeSerializer -> AbilityTypeEntrySerializer
                    AttachmentTypeSerializer -> AttachmentTypeEntrySerializer
                    RecipeTypeSerializer -> RecipeTypeEntrySerializer
                    GuiTextureSerializer -> GuiTextureEntrySerializer
                    WailaInfoProviderSerializer -> WailaInfoProviderEntrySerializer
                    WailaToolIconProviderSerializer -> WailaToolIconProviderEntrySerializer
                    ItemFilterTypeSerializer -> ItemFilterTypeEntrySerializer
                    TooltipStyleSerializer -> TooltipStyleEntrySerializer
                    else -> error("No RegistryEntry.Nova serializer for element serializer: ${typeArgs.single().descriptor.serialName}")
                }
            }
        }
    }
    
    private val dir = Path("plugins/Nova/.internal_data/storage/")
    
    fun has(key: String): Boolean =
        getPath(key).exists()
    
    fun remove(key: String) =
        getPath(key).deleteIfExists()
    
    fun <T> store(key: String, serializer: SerializationStrategy<T>, data: T): Unit =
        getPath(key).also { it.createParentDirectories() }.writeJson(serializer, data, JSON)
    
    fun <T> retrieve(key: String, deserializer: DeserializationStrategy<T>): T? =
        getPath(key).takeIf { it.exists() }?.readJson(deserializer, JSON)
    
    @Suppress("UNCHECKED_CAST")
    fun <T> store(key: String, type: KType, data: T): Unit =
        store(key, JSON.serializersModule.serializer(type) as KSerializer<T>, data)
    
    @Suppress("UNCHECKED_CAST")
    fun <T> retrieve(key: String, type: KType): T? =
        retrieve(key, JSON.serializersModule.serializer(type) as KSerializer<T>)
    
    inline fun <reified T> store(key: String, data: T): Unit =
        getPath(key).also { it.createParentDirectories() }.writeJson(data, JSON)
    
    inline fun <reified T> retrieve(key: String): T? =
        getPath(key).takeIf { it.exists() }?.readJson(JSON)
    
    inline fun <reified T> storedValue(key: String, noinline alternativeProvider: () -> T): MutableProvider<T> =
        mutableProvider(
            { retrieve<T>(key) ?: alternativeProvider().also { store(key, it) } },
            { store<T>(key, it) }
        )
    
    fun <T> storedValue(key: String, serializer: KSerializer<T>, alternativeProvider: () -> T): MutableProvider<T> =
        mutableProvider(
            { retrieve(key, serializer) ?: alternativeProvider().also { store(key, serializer, it) } },
            { store(key, serializer, it) }
        )
    
    fun getPath(key: String): Path =
        dir.resolve("$key.json")
    
}