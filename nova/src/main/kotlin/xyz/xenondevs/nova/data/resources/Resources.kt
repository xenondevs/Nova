package xyz.xenondevs.nova.data.resources

import de.studiocode.inventoryaccess.util.DataUtils
import kotlinx.coroutines.runBlocking
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsLoader
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.content.FontChar
import xyz.xenondevs.nova.data.resources.model.data.BlockModelData
import xyz.xenondevs.nova.data.resources.model.data.ItemModelData
import xyz.xenondevs.nova.data.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.util.data.getResourceAsStream
import xyz.xenondevs.nova.util.data.update
import java.security.MessageDigest

private const val RESOURCES_HASH = "resourcesHash"
private const val MODEL_DATA_LOOKUP = "modelDataLookup"
private const val LANGUAGE_LOOKUP = "languageLookup"
private const val TEXTURE_ICON_LOOKUP = "textureIconLookup"
private const val GUI_DATA_LOOKUP = "guiDataLookup"
private const val WAILA_DATA_LOOKUP = "wailaDataLookup"

private val ASSET_INDEX_FILES = listOf("assets/materials.json", "assets/guis.json")

object Resources : Initializable() {
    
    override val initializationStage = InitializationStage.PRE_WORLD
    override val dependsOn = setOf(AddonsLoader, AutoUploadManager)
    
    private lateinit var modelDataLookup: Map<String, Pair<ItemModelData?, BlockModelData?>>
    private lateinit var guiDataLookup: Map<String, FontChar>
    private lateinit var wailaDataLookup: Map<String, FontChar>
    private lateinit var textureIconLookup: Map<String, FontChar>
    internal lateinit var languageLookup: Map<String, Map<String, String>>
    
    override fun init() {
        val resourcesHash = calculateResourcesHash()
        if (
            PermanentStorage.retrieveOrNull<String>(RESOURCES_HASH) == resourcesHash
            && PermanentStorage.has(MODEL_DATA_LOOKUP)
            && PermanentStorage.has(LANGUAGE_LOOKUP)
            && PermanentStorage.has(TEXTURE_ICON_LOOKUP)
            && PermanentStorage.has(WAILA_DATA_LOOKUP)
            && PermanentStorage.has(GUI_DATA_LOOKUP)
        ) {
            // Load from PermanentStorage
            modelDataLookup = PermanentStorage.retrieveOrNull<HashMap<String, Pair<ItemModelData?, BlockModelData?>>>(MODEL_DATA_LOOKUP)!!
            languageLookup = PermanentStorage.retrieveOrNull<HashMap<String, HashMap<String, String>>>(LANGUAGE_LOOKUP)!!
            textureIconLookup = PermanentStorage.retrieveOrNull<HashMap<String, FontChar>>(TEXTURE_ICON_LOOKUP)!!
            wailaDataLookup = PermanentStorage.retrieveOrNull<HashMap<String, FontChar>>(WAILA_DATA_LOOKUP)!!
            guiDataLookup = PermanentStorage.retrieveOrNull<HashMap<String, FontChar>>(GUI_DATA_LOOKUP)!!
        } else {
            // Create ResourcePack
            ResourcePackBuilder.buildPack()
            AutoUploadManager.wasRegenerated = true
            // Store resourcesHashCode
            PermanentStorage.store(RESOURCES_HASH, resourcesHash)
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
        val file = ResourcePackBuilder.buildPack()
        
        if (AutoUploadManager.enabled) {
            runBlocking {
                val url = AutoUploadManager.uploadPack(file)
                if (url == null)
                    LOGGER.warning("The resource pack was not uploaded. (Misconfigured auto uploader?)")
            }
        }
    }
    
    internal fun updateModelDataLookup(modelDataLookup: Map<String, Pair<ItemModelData?, BlockModelData?>>) {
        this.modelDataLookup = modelDataLookup
        PermanentStorage.store("modelDataLookup", modelDataLookup)
    }
    
    internal fun updateGuiDataLookup(guiDataLookup: Map<String, FontChar>) {
        this.guiDataLookup = guiDataLookup
        PermanentStorage.store("guiDataLookup", guiDataLookup)
    }
    
    internal fun updateWailaDataLookup(wailaDataLookup: Map<String, FontChar>) {
        this.wailaDataLookup = wailaDataLookup
        PermanentStorage.store("wailaDataLookup", wailaDataLookup)
    }
    
    internal fun updateTextureIconLookup(textureIconLookup: Map<String, FontChar>) {
        this.textureIconLookup = textureIconLookup
        PermanentStorage.store("textureIconLookup", textureIconLookup)
    }
    
    internal fun updateLanguageLookup(languageLookup: Map<String, Map<String, String>>) {
        this.languageLookup = languageLookup
        PermanentStorage.store("languageLookup", languageLookup)
    }
    
    fun getModelData(id: NamespacedId): Pair<ItemModelData?, BlockModelData?> {
        return modelDataLookup[id.toString()]!!
    }
    
    fun getModelData(path: ResourcePath): Pair<ItemModelData?, BlockModelData?> {
        return modelDataLookup[path.toString()]!!
    }
    
    fun getModelData(id: String): Pair<ItemModelData?, BlockModelData?> {
        return modelDataLookup[id]!!
    }
    
    fun getModelDataOrNull(id: NamespacedId): Pair<ItemModelData?, BlockModelData?>? {
        return modelDataLookup[id.toString()]
    }
    
    fun getModelDataOrNull(path: ResourcePath): Pair<ItemModelData?, BlockModelData?>? {
        return modelDataLookup[path.toString()]
    }
    
    fun getModelDataOrNull(id: String): Pair<ItemModelData?, BlockModelData?>? {
        return modelDataLookup[id]
    }
    
    fun getGUIChar(id: NamespacedId): FontChar {
        return guiDataLookup[id.toString()]!!
    }
    
    fun getGUIChar(path: ResourcePath): FontChar {
        return guiDataLookup[path.toString()]!!
    }
    
    fun getGUIChar(id: String): FontChar {
        return guiDataLookup[id]!!
    }
    
    fun getGUICharOrNull(id: NamespacedId): FontChar? {
        return guiDataLookup[id.toString()]
    }
    
    fun getGUICharOrNull(path: ResourcePath): FontChar? {
        return guiDataLookup[path.toString()]
    }
    
    fun getGUICharOrNull(id: String): FontChar? {
        return guiDataLookup[id]
    }
    
    fun getWailaIconChar(id: NamespacedId): FontChar {
        return wailaDataLookup[id.toString()]!!
    }
    
    fun getWailaIconChar(path: ResourcePath): FontChar {
        return wailaDataLookup[path.toString()]!!
    }
    
    fun getWailaIconChar(id: String): FontChar {
        return wailaDataLookup[id]!!
    }
    
    fun getWailaIconCharOrNull(id: NamespacedId): FontChar? {
        return wailaDataLookup[id.toString()]
    }
    
    fun getWailaIconCharOrNull(path: ResourcePath): FontChar? {
        return wailaDataLookup[path.toString()]
    }
    
    fun getWailaIconCharOrNull(id: String): FontChar? {
        return wailaDataLookup[id]
    }
    
    fun getTextureIconChar(id: NamespacedId): FontChar {
        return textureIconLookup[id.toString()]!!
    }
    
    fun getTextureIconChar(path: ResourcePath): FontChar {
        return textureIconLookup[path.toString()]!!
    }
    
    fun getTextureIconChar(id: String): FontChar {
        return textureIconLookup[id]!!
    }
    
    fun getTextureIconCharOrNull(id: NamespacedId): FontChar? {
        return textureIconLookup[id.toString()]
    }
    
    fun getTextureIconCharOrNull(path: ResourcePath): FontChar? {
        return textureIconLookup[path.toString()]
    }
    
    fun getTextureIconCharOrNull(id: String): FontChar? {
        return textureIconLookup[id]
    }
    
}