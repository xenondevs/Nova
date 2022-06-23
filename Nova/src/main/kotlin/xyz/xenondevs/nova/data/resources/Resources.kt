package xyz.xenondevs.nova.data.resources

import kotlinx.coroutines.runBlocking
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsLoader
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.builder.GUIData
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.material.ModelData

internal object Resources : Initializable() {
    
    override val inMainThread = false
    override val dependsOn = setOf(AddonsLoader)
    
    private lateinit var modelDataLookup: Map<String, Pair<ModelData?, ModelData?>>
    private lateinit var guiDataLookup: Map<String, GUIData>
    internal lateinit var languageLookup: Map<String, Map<String, String>>
    
    override fun init() {
        LOGGER.info("Loading resources")
        if (
            PermanentStorage.retrieveOrNull<Int>("addonsHashCode") == AddonManager.addonsHashCode
            && PermanentStorage.has("modelDataLookup")
            && PermanentStorage.has("guiDataLookup")
            && PermanentStorage.has("languageLookup")
        ) {
            // Load from PermanentStorage
            modelDataLookup = PermanentStorage.retrieveOrNull<HashMap<String, Pair<ModelData?, ModelData?>>>("modelDataLookup")!!
            languageLookup = PermanentStorage.retrieveOrNull<HashMap<String, HashMap<String, String>>>("languageLookup")!!
            guiDataLookup = PermanentStorage.retrieveOrNull<HashMap<String, GUIData>>("guiDataLookup")!!
        } else {
            // Create ResourcePack
            ResourcePackBuilder.buildPack()
            AutoUploadManager.wasRegenerated = true
            // Store addonsHashCode
            PermanentStorage.store("addonsHashCode", AddonManager.addonsHashCode)
        }
    }
    
    fun createResourcePack() {
        val file = ResourcePackBuilder.buildPack()
        
        if (AutoUploadManager.enabled) {
            runBlocking {
                val url = AutoUploadManager.uploadPack(file)
                if (url == null)
                    LOGGER.warning("The resource pack was not uploaded. (Misconfigured auto uploader?)")
            }
        }
    }
    
    internal fun updateModelDataLookup(modelDataLookup: Map<String, Pair<ModelData?, ModelData?>>) {
        this.modelDataLookup = modelDataLookup
        PermanentStorage.store("modelDataLookup", modelDataLookup)
    }
    
    internal fun updateGuiDataLookup(guiDataLookup: Map<String, GUIData>) {
        this.guiDataLookup = guiDataLookup
        PermanentStorage.store("guiDataLookup", guiDataLookup)
    }
    
    internal fun updateLanguageLookup(languageLookup: Map<String, Map<String, String>>) {
        this.languageLookup = languageLookup
        PermanentStorage.store("languageLookup", languageLookup)
    }
    
    fun getModelDataOrNull(id: NamespacedId): Pair<ModelData?, ModelData?>? {
        return modelDataLookup[id.toString()]
    }
    
    fun getModelData(id: NamespacedId): Pair<ModelData?, ModelData?> {
        return modelDataLookup[id.toString()]!!
    }
    
    fun getGUIDataOrNull(id: NamespacedId): GUIData? {
        return guiDataLookup[id.toString()]
    }
    
    fun getGUIData(id: NamespacedId): GUIData {
        return guiDataLookup[id.toString()]!!
    }
    
    fun getModelDataOrNull(id: String): Pair<ModelData?, ModelData?>? {
        return modelDataLookup[id]
    }
    
    fun getModelData(id: String): Pair<ModelData?, ModelData?> {
        return modelDataLookup[id]!!
    }
    
    fun getGUIDataOrNull(id: String): GUIData? {
        return guiDataLookup[id]
    }
    
    fun getGUIData(id: String): GUIData {
        return guiDataLookup[id]!!
    }
    
}