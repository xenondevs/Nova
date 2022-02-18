package xyz.xenondevs.nova.data.resources

import org.bukkit.Material
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.loader.AddonsInitializer
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.builder.GUIData
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.material.ModelData

internal object Resources : Initializable() {
    
    override val inMainThread = false
    override val dependsOn = AddonsInitializer
    
    private lateinit var modelDataLookup: Map<String, Pair<ModelData?, ModelData?>>
    private lateinit var guiDataLookup: Map<String, GUIData>
    internal lateinit var languageLookup: Map<String, Map<String, String>>
    
    override fun init() {
        LOGGER.info("Loading resources")
//        if (PermanentStorage.has("modelDataLookup") && PermanentStorage.has("guiCharLookup")) {
//            // Load from PermanentStorage
//            modelDataLookup = PermanentStorage.retrieveOrNull("modelDataLookup")!!
//            guiCharLookup = PermanentStorage.retrieveOrNull("guiCharLookup")!!
//        } else {
        // Create ResourcePack
//        val pack = ResourcePackBuilder(listOf(coreAssets)).create()
        
        // TODO
//        }
    }
    
    fun isInitialized() = ::modelDataLookup.isInitialized && ::guiDataLookup.isInitialized
    
    internal fun updateModelDataLookup(modelDataLookup: Map<String, Pair<ModelData?, ModelData?>>) {
        this.modelDataLookup = modelDataLookup
        PermanentStorage.store("modelDataLookup", modelDataLookup)
    }
    
    internal fun updateGuiDataLookup(guiDataLookup: Map<String, GUIData>) {
        this.guiDataLookup = guiDataLookup
        PermanentStorage.store("guiDataLookup", modelDataLookup)
    }
    
    internal fun updateLanguageLookup(languageLookup: Map<String, Map<String, String>>) {
        this.languageLookup = languageLookup
        PermanentStorage.store("languageLookup", this.languageLookup)
    }
    
    fun getModelDataOrNull(id: String): Pair<ModelData?, ModelData?>? {
        return modelDataLookup[id]
    }
    
    fun getModelData(id: String): Pair<ModelData?, ModelData?> {
        return modelDataLookup[id]
        // This is a temporary workaround until all items can be found
            ?: (ModelData(Material.DIRT, intArrayOf(0), id, false) to ModelData(Material.DIRT, intArrayOf(0), id, true))
    }
    
    fun getGUIDataOrNull(id: String): GUIData? {
        return guiDataLookup[id]
    }
    
    fun getGUIData(id: String): GUIData {
        return guiDataLookup[id]!!
    }
    
}