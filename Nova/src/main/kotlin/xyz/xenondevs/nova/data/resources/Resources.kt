package xyz.xenondevs.nova.data.resources

import kotlinx.coroutines.runBlocking
import org.bukkit.event.HandlerList
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsLoader
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.builder.GUIData
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.model.data.BlockModelData
import xyz.xenondevs.nova.data.resources.model.data.ItemModelData
import xyz.xenondevs.nova.data.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.util.registerEventsFirst

internal object Resources : Initializable() {
    
    override val inMainThread = false
    override val dependsOn = setOf(AddonsLoader)
    
    private lateinit var modelDataLookup: Map<String, Pair<ItemModelData?, BlockModelData?>>
    private lateinit var guiDataLookup: Map<String, GUIData>
    internal lateinit var languageLookup: Map<String, Map<String, String>>
    
    override fun init() {
        if (DEFAULT_CONFIG.getBoolean("resource_pack.freeze_loading_players"))
            registerEventsFirst(PlayerFreezer)
        LOGGER.info("Loading resources")
        if (
            PermanentStorage.retrieveOrNull<Int>("addonsHashCode") == AddonManager.addonsHashCode
            && PermanentStorage.has("modelDataLookup")
            && PermanentStorage.has("guiDataLookup")
            && PermanentStorage.has("languageLookup")
        ) {
            // Load from PermanentStorage
            modelDataLookup = PermanentStorage.retrieveOrNull<HashMap<String, Pair<ItemModelData?, BlockModelData?>>>("modelDataLookup")!!
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
    
    fun reload() {
        val freezerEnabled = DEFAULT_CONFIG.getBoolean("resource_pack.freeze_loading_players")
        if (freezerEnabled && !PlayerFreezer.enabled) {
            PlayerFreezer.enabled = true
            registerEventsFirst(PlayerFreezer)
        } else if (!freezerEnabled && PlayerFreezer.enabled) {
            PlayerFreezer.enabled = false
            PlayerFreezer.clearPlayers()
            HandlerList.unregisterAll(PlayerFreezer)
        }
    }
    
    override fun disable() {
        PlayerFreezer.clearPlayers()
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
    
    internal fun updateModelDataLookup(modelDataLookup: Map<String, Pair<ItemModelData?, BlockModelData?>>) {
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
    
    fun getModelDataOrNull(id: NamespacedId): Pair<ItemModelData?, BlockModelData?>? {
        return modelDataLookup[id.toString()]
    }
    
    fun getModelData(id: NamespacedId): Pair<ItemModelData?, BlockModelData?> {
        return modelDataLookup[id.toString()]!!
    }
    
    fun getGUIDataOrNull(id: NamespacedId): GUIData? {
        return guiDataLookup[id.toString()]
    }
    
    fun getGUIData(id: NamespacedId): GUIData {
        return guiDataLookup[id.toString()]!!
    }
    
    fun getModelDataOrNull(id: String): Pair<ItemModelData?, BlockModelData?>? {
        return modelDataLookup[id]
    }
    
    fun getModelData(id: String): Pair<ItemModelData?, BlockModelData?> {
        return modelDataLookup[id]!!
    }
    
    fun getGUIDataOrNull(id: String): GUIData? {
        return guiDataLookup[id]
    }
    
    fun getGUIData(id: String): GUIData {
        return guiDataLookup[id]!!
    }
    
}