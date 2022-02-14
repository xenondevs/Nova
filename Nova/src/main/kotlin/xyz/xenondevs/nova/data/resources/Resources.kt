package xyz.xenondevs.nova.data.resources

import org.bukkit.Material
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.assets.AssetsPack
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.Resources.guiCharLookup
import xyz.xenondevs.nova.data.resources.Resources.modelDataLookup
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.material.ModelData
import java.io.File

object Resources : Initializable() {
    
    override val inMainThread = true
    override val dependsOn: Initializable? = null
    
    private lateinit var modelDataLookup: HashMap<String, Pair<ModelData?, ModelData?>>
    private lateinit var guiCharLookup: HashMap<String, Triple<Char, Int, Int>>
    
    override fun init() {
        LOGGER.info("Loading resources...")
//        if (PermanentStorage.has("modelDataLookup") && PermanentStorage.has("guiCharLookup")) {
//            // Load from PermanentStorage
//            modelDataLookup = PermanentStorage.retrieveOrNull("modelDataLookup")!!
//            guiCharLookup = PermanentStorage.retrieveOrNull("guiCharLookup")!!
//        } else {
            // Create ResourcePack
//            val coreAssets = AssetsPack(File(""))
//            val pack = ResourcePackBuilder(listOf(coreAssets)).create()
            
            // TODO
//        }
    }
    
    fun isInitialized() = ::modelDataLookup.isInitialized && ::guiCharLookup.isInitialized
    
    internal fun updateLookupMaps(
        modelDataLookup: HashMap<String, Pair<ModelData?, ModelData?>>,
        guiCharLookup: HashMap<String, Triple<Char, Int, Int>>
    ) {
        this.modelDataLookup = modelDataLookup
        this.guiCharLookup = guiCharLookup
        
        PermanentStorage.store("modelDataLookup", modelDataLookup)
        PermanentStorage.store("guiCharLookup", guiCharLookup)
    }
    
    fun getModelDataOrNull(id: String): Pair<ModelData?, ModelData?>? {
        return modelDataLookup[id]
    }
    
    fun getModelData(id: String): Pair<ModelData?, ModelData?> {
        return modelDataLookup[id]
            // This is a temporary workaround until all items can be found
            ?: (ModelData(Material.DIRT, intArrayOf(0)) to ModelData(Material.DIRT, intArrayOf(0)))
    }
    
    fun getGUIDataOrNull(id: String): Triple<Char, Int, Int>? {
        return guiCharLookup[id]
    }
    
    fun getGUIData(id: String): Triple<Char, Int, Int> {
        return guiCharLookup[id]!!
    }
    
}