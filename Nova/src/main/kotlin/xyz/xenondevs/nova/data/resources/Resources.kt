package xyz.xenondevs.nova.data.resources

import kotlinx.coroutines.runBlocking
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsLoader
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.content.FontChar
import xyz.xenondevs.nova.data.resources.builder.content.GUIFontChar
import xyz.xenondevs.nova.data.resources.model.data.BlockModelData
import xyz.xenondevs.nova.data.resources.model.data.ItemModelData
import xyz.xenondevs.nova.data.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.initialize.Initializable

object Resources : Initializable() {
    
    override val inMainThread = false
    override val dependsOn = setOf(AddonsLoader)
    
    private lateinit var modelDataLookup: Map<String, Pair<ItemModelData?, BlockModelData?>>
    private lateinit var guiDataLookup: Map<String, GUIFontChar>
    private lateinit var wailaDataLookup: Map<String, FontChar>
    private lateinit var textureIconLookup: Map<String, FontChar>
    internal lateinit var languageLookup: Map<String, Map<String, String>>
    
    override fun init() {
        if (
            PermanentStorage.retrieveOrNull<Int>("addonsHashCode") == AddonManager.addonsHashCode
            && PermanentStorage.has("modelDataLookup")
            && PermanentStorage.has("guiDataLookup")
            && PermanentStorage.has("wailaDataLookup")
            && PermanentStorage.has("languageLookup")
        ) {
            // Load from PermanentStorage
            modelDataLookup = PermanentStorage.retrieveOrNull<HashMap<String, Pair<ItemModelData?, BlockModelData?>>>("modelDataLookup")!!
            languageLookup = PermanentStorage.retrieveOrNull<HashMap<String, HashMap<String, String>>>("languageLookup")!!
            textureIconLookup = PermanentStorage.retrieveOrNull<HashMap<String, FontChar>>("textureIconLookup")!!
            wailaDataLookup = PermanentStorage.retrieveOrNull<HashMap<String, FontChar>>("wailaDataLookup")!!
            guiDataLookup = PermanentStorage.retrieveOrNull<HashMap<String, GUIFontChar>>("guiDataLookup")!!
        } else {
            // Create ResourcePack
            ResourcePackBuilder.buildPack()
            AutoUploadManager.wasRegenerated = true
            // Store addonsHashCode
            PermanentStorage.store("addonsHashCode", AddonManager.addonsHashCode)
        }
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
    
    internal fun updateGuiDataLookup(guiDataLookup: Map<String, GUIFontChar>) {
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
    
    fun getModelData(id: String): Pair<ItemModelData?, BlockModelData?> {
        return modelDataLookup[id]!!
    }
    
    fun getModelDataOrNull(id: NamespacedId): Pair<ItemModelData?, BlockModelData?>? {
        return modelDataLookup[id.toString()]
    }
    
    fun getModelDataOrNull(id: String): Pair<ItemModelData?, BlockModelData?>? {
        return modelDataLookup[id]
    }
    
    fun getGUIChar(id: NamespacedId): GUIFontChar {
        return guiDataLookup[id.toString()]!!
    }
    
    fun getGUIChar(id: String): GUIFontChar {
        return guiDataLookup[id]!!
    }
    
    fun getGUICharOrNull(id: NamespacedId): GUIFontChar? {
        return guiDataLookup[id.toString()]
    }
    
    fun getGUICharOrNull(id: String): GUIFontChar? {
        return guiDataLookup[id]
    }
    
    fun getWailaIconChar(id: NamespacedId): FontChar {
        return wailaDataLookup[id.toString()]!!
    }
    
    fun getWailaIconChar(id: String): FontChar {
        return wailaDataLookup[id]!!
    }
    
    fun getWailaIconCharOrNull(id: NamespacedId): FontChar? {
        return wailaDataLookup[id.toString()]
    }
    
    fun getWailaIconCharOrNull(id: String): FontChar? {
        return wailaDataLookup[id]
    }
    
    fun getTextureIconChar(id: NamespacedId): FontChar {
        return textureIconLookup[id.toString()]!!
    }
    
    fun getTextureIconChar(id: String): FontChar {
        return textureIconLookup[id]!!
    }
    
    fun getTextureIconCharOrNull(id: NamespacedId): FontChar? {
        return textureIconLookup[id.toString()]
    }
    
    fun getTextureIconCharOrNull(id: String): FontChar? {
        return textureIconLookup[id]
    }
    
}