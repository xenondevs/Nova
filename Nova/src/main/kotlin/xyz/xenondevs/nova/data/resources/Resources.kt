package xyz.xenondevs.nova.data.resources

import org.bukkit.Material
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.assets.AssetPack
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.builder.GUIData
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.material.ModelData
import java.io.File

object Resources : Initializable() {
    
    override val inMainThread = true
    override val dependsOn: Initializable? = null
    
    private lateinit var modelDataLookup: HashMap<String, Pair<ModelData?, ModelData?>>
    private lateinit var guiDataLookup: HashMap<String, GUIData>
    
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
    
    internal fun updateModelDataLookup(modelDataLookup: HashMap<String, Pair<ModelData?, ModelData?>>) {
        this.modelDataLookup = modelDataLookup
        PermanentStorage.store("modelDataLookup", modelDataLookup)
    }
    
    internal fun updateGuiDataLookup(guiDataLookup: HashMap<String, GUIData>) {
        this.guiDataLookup = guiDataLookup
        PermanentStorage.store("guiDataLookup", modelDataLookup)
    }
    
    fun getModelDataOrNull(id: String): Pair<ModelData?, ModelData?>? {
        return modelDataLookup[id]
    }
    
    fun getModelData(id: String): Pair<ModelData?, ModelData?> {
        return modelDataLookup[id]
        // This is a temporary workaround until all items can be found
            ?: (ModelData(Material.DIRT, intArrayOf(0)) to ModelData(Material.DIRT, intArrayOf(0)))
    }
    
    fun getGUIDataOrNull(id: String): GUIData? {
        return guiDataLookup[id]
    }
    
    fun getGUIData(id: String): GUIData {
        return guiDataLookup[id]!!
    }
    
}