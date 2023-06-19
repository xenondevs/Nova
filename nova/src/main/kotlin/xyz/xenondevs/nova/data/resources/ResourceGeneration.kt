package xyz.xenondevs.nova.data.resources

import kotlinx.coroutines.runBlocking
import net.minecraft.core.Registry
import net.minecraft.core.WritableRegistry
import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import xyz.xenondevs.inventoryaccess.util.DataUtils
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.task.armor.info.ArmorTexture
import xyz.xenondevs.nova.data.resources.builder.task.font.FontChar
import xyz.xenondevs.nova.data.resources.model.data.BlockModelData
import xyz.xenondevs.nova.data.resources.model.data.ItemModelData
import xyz.xenondevs.nova.data.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.HooksLoader
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.data.getResourceAsStream
import xyz.xenondevs.nova.util.data.update
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.util.toMap
import java.security.MessageDigest

private const val RESOURCES_HASH = "resourcesHash"
private const val MODEL_DATA_LOOKUP = "modelDataLookup"
private const val ARMOR_DATA_LOOKUP = "armorDataLookup"
private const val LANGUAGE_LOOKUP = "languageLookup"
private const val TEXTURE_ICON_LOOKUP = "textureIconLookup"
private const val Gui_DATA_LOOKUP = "guiDataLookup"
private const val WAILA_DATA_LOOKUP = "wailaDataLookup"

private val ASSET_INDEX_FILES = listOf("assets/materials.json", "assets/guis.json", "assets/armor.json")

/**
 * Handles resource pack generation on startup.
 * Decides whether the resource pack needs to be regenerated and splits the generation into two parts: PreWorld and PostWorld (async)
 */
internal object ResourceGeneration {
    
    private lateinit var resourcesHash: String
    private var builder: ResourcePackBuilder? = null
    
    @InternalInit(
        stage = InternalInitStage.PRE_WORLD,
        dependsOn = [AddonsInitializer::class]
    )
    object PreWorld {
        
        @InitFun
        private fun init() {
            resourcesHash = calculateResourcesHash()
            if (
                PermanentStorage.retrieveOrNull<String>(RESOURCES_HASH) == resourcesHash
                && PermanentStorage.has(MODEL_DATA_LOOKUP)
                && PermanentStorage.has(ARMOR_DATA_LOOKUP)
                && PermanentStorage.has(LANGUAGE_LOOKUP)
                && PermanentStorage.has(TEXTURE_ICON_LOOKUP)
                && PermanentStorage.has(WAILA_DATA_LOOKUP)
                && PermanentStorage.has(Gui_DATA_LOOKUP)
            ) {
                // Load from PermanentStorage
                loadLookupRegistry(MODEL_DATA_LOOKUP, NovaRegistries.MODEL_DATA_LOOKUP)
                loadLookupRegistry(ARMOR_DATA_LOOKUP, NovaRegistries.ARMOR_DATA_LOOKUP)
                loadLookupRegistry(LANGUAGE_LOOKUP, NovaRegistries.LANGUAGE_LOOKUP)
                loadLookupRegistry(TEXTURE_ICON_LOOKUP, NovaRegistries.TEXTURE_ICON_LOOKUP)
                loadLookupRegistry(WAILA_DATA_LOOKUP, NovaRegistries.WAILA_DATA_LOOKUP)
                loadLookupRegistry(Gui_DATA_LOOKUP, NovaRegistries.GUI_DATA_LOOKUP)
            } else {
                // Build resource pack
                LOGGER.info("Building resource pack")
                builder = ResourcePackBuilder()
                    .also(ResourcePackBuilder::buildPackPreWorld)
                LOGGER.info("Pre-world resource pack building done")
            }
        }
        
    }
    
    @InternalInit(
        stage = InternalInitStage.POST_WORLD_ASYNC,
        dependsOn = [HooksLoader::class]
    )
    object PostWorld {
    
        @InitFun
        private fun init() {
            val builder = builder
            if (builder != null) {
                LOGGER.info("Continuing to build resource pack")
                builder.buildPackPostWorld()
                AutoUploadManager.wasRegenerated = true
                PermanentStorage.store(RESOURCES_HASH, resourcesHash)
            }
        }
        
    }
    
    private fun calculateResourcesHash(): String {
        val digest = MessageDigest.getInstance("MD5")
        
        // Nova version
        digest.update(NOVA.version.toString().toByteArray())
        // nova asset indices
        ASSET_INDEX_FILES.forEach { getResourceAsStream(it.replace("assets/", "assets/nova/"))?.let(digest::update) }
        
        // Addon id, version and asset indices
        AddonManager.loaders.forEach { (id, loader) ->
            // id and version
            digest.update(id.toByteArray())
            digest.update(loader.description.version.toByteArray())
            
            // asset indices
            ASSET_INDEX_FILES.forEach { getResourceAsStream(loader.file, it)?.let(digest::update) }
        }
        
        return DataUtils.toHexadecimalString(digest.digest())
    }
    
    internal fun createResourcePack() {
        ResourcePackBuilder().buildPackCompletely()
        
        if (AutoUploadManager.enabled) {
            runBlocking {
                val url = AutoUploadManager.uploadPack(ResourcePackBuilder.RESOURCE_PACK_FILE)
                if (url == null)
                    LOGGER.warning("The resource pack was not uploaded. (Misconfigured auto uploader?)")
            }
        }
    }
    
    internal fun updateModelDataLookup(modelDataLookup: Map<ResourceLocation, ModelData>) {
        loadLookupRegistry(modelDataLookup, NovaRegistries.MODEL_DATA_LOOKUP)
        storeLookupRegistry(MODEL_DATA_LOOKUP, NovaRegistries.MODEL_DATA_LOOKUP)
    }
    
    internal fun updateArmorDataLookup(armorDataLookup: Map<ResourceLocation, ArmorTexture>) {
        loadLookupRegistry(armorDataLookup, NovaRegistries.ARMOR_DATA_LOOKUP)
        storeLookupRegistry(ARMOR_DATA_LOOKUP, NovaRegistries.ARMOR_DATA_LOOKUP)
    }
    
    internal fun updateGuiDataLookup(guiDataLookup: Map<ResourceLocation, FontChar>) {
        loadLookupRegistry(guiDataLookup, NovaRegistries.GUI_DATA_LOOKUP)
        storeLookupRegistry(Gui_DATA_LOOKUP, NovaRegistries.GUI_DATA_LOOKUP)
    }
    
    internal fun updateWailaDataLookup(wailaDataLookup: Map<ResourceLocation, FontChar>) {
        loadLookupRegistry(wailaDataLookup, NovaRegistries.WAILA_DATA_LOOKUP)
        storeLookupRegistry(WAILA_DATA_LOOKUP, NovaRegistries.WAILA_DATA_LOOKUP)
    }
    
    internal fun updateTextureIconLookup(textureIconLookup: Map<ResourceLocation, FontChar>) {
        loadLookupRegistry(textureIconLookup, NovaRegistries.TEXTURE_ICON_LOOKUP)
        storeLookupRegistry(TEXTURE_ICON_LOOKUP, NovaRegistries.TEXTURE_ICON_LOOKUP)
    }
    
    internal fun updateLanguageLookup(languageLookup: Map<ResourceLocation, Map<String, String>>) {
        loadLookupRegistry(languageLookup, NovaRegistries.LANGUAGE_LOOKUP)
        storeLookupRegistry(LANGUAGE_LOOKUP, NovaRegistries.LANGUAGE_LOOKUP)
    }
    
    private inline fun <reified T> loadLookupRegistry(storageKey: String, registry: WritableRegistry<T>) {
        val map: Map<ResourceLocation, T> = PermanentStorage.retrieveOrNull(storageKey)!!
        loadLookupRegistry(map, registry)
    }
    
    private fun <T> loadLookupRegistry(map: Map<ResourceLocation, T>, registry: WritableRegistry<T>) {
        map.entries.forEach { (key, value) -> registry[key] = value }
    }
    
    private fun <T> storeLookupRegistry(storageKey: String, registry: Registry<T>) {
        PermanentStorage.store(storageKey, registry.toMap())
    }
    
}

data class ModelData(
    val item: Map<Material, ItemModelData>? = null,
    val block: BlockModelData? = null,
    val armor: ResourceLocation? = null
)