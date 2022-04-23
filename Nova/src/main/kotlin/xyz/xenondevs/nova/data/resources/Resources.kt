package xyz.xenondevs.nova.data.resources

import net.lingala.zip4j.ZipFile
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsLoader
import xyz.xenondevs.nova.addon.assets.AssetPack
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.builder.GUIData
import xyz.xenondevs.nova.data.resources.builder.PNGMetadataRemover
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.material.ModelData
import xyz.xenondevs.nova.util.data.write
import java.io.File

internal object Resources : Initializable() {
    
    override val inMainThread = false
    override val dependsOn = setOf(AddonsLoader)
    
    private lateinit var modelDataLookup: Map<String, Pair<ModelData?, ModelData?>>
    private lateinit var guiDataLookup: Map<String, GUIData>
    internal lateinit var languageLookup: Map<String, Map<String, String>>
    
    override fun init() {
        LOGGER.info("Loading resources")
        // TODO: Build resource pack automatically when addon changes are detected
        if (PermanentStorage.has("modelDataLookup") && PermanentStorage.has("guiDataLookup") && PermanentStorage.has("languageLookup")) {
            // Load from PermanentStorage
            modelDataLookup = PermanentStorage.retrieveOrNull<HashMap<String, Pair<ModelData?, ModelData?>>>("modelDataLookup")!!
            languageLookup = PermanentStorage.retrieveOrNull<HashMap<String, HashMap<String, String>>>("languageLookup")!!
            guiDataLookup = PermanentStorage.retrieveOrNull<HashMap<String, GUIData>>("guiDataLookup")!!
        } else {
            // Create ResourcePack
            createResourcePack()
        }
    }
    
    fun createResourcePack(): File {
        val assetPacksDir = File(NOVA.dataFolder, "ResourcePack/AssetPacks/")
        assetPacksDir.deleteRecursively()
        val assetPacks = (AddonManager.loaders.asSequence().map { it.file to it.description.id } + (NOVA.pluginFile to "nova"))
            .mapTo(ArrayList()) { (addonFile, namespace) ->
                val assetPackDir = File(assetPacksDir, namespace)
                
                val zip = ZipFile(addonFile)
                zip.fileHeaders.forEach { header ->
                    if (!header.isDirectory && header.fileName.startsWith("assets/")) {
                        val file = File(assetPackDir, header.fileName.substringAfter("assets/"))
                        val inputStream = zip.getInputStream(header)
                        if (header.fileName.endsWith(".png")) {
                            file.parentFile.mkdirs()
                            PNGMetadataRemover.remove(inputStream, file.outputStream())
                        } else file.write(inputStream)
                    }
                }
                
                return@mapTo AssetPack(assetPackDir, namespace)
            }
        
        return ResourcePackBuilder(assetPacks).create()
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