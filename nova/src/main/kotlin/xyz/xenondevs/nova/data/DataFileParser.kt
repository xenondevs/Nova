package xyz.xenondevs.nova.data

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.addon.loader.AddonLoader
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.util.data.HashUtils
import xyz.xenondevs.nova.util.data.getResourceAsStream
import xyz.xenondevs.nova.util.data.getResources
import xyz.xenondevs.nova.util.insertAfter
import java.io.File
import java.io.FileFilter

@InternalInit(
    stage = InitializationStage.PRE_WORLD,
    dependsOn = [AddonsInitializer::class]
)
internal object DataFileParser {
    
    private val FILE_PATTERN = Regex("""^[a-z][a-z\d_]*.json$""")
    private val DATA_DIR = File(NOVA.dataFolder, "data")
    
    fun init() {
        val existingPaths = ObjectArrayList<String>()
        
        // Extract data files
        existingPaths += getResources("data/").mapNotNull(::extractFile)
        
        // Extract data files from addons
        AddonManager.loaders.values.forEach { loader ->
            existingPaths += getResources(loader.file, "data/").mapNotNull { extractFile(it, loader) }
        }
        
        // find unedited data files that are no longer default and remove them
        DATA_DIR.walkTopDown().forEach { file ->
            if (file.isDirectory || file.extension != "json") return@forEach
            
            val relativePath = NOVA.dataFolder.toURI().relativize(file.toURI()).path
            
            if (!existingPaths.contains(relativePath)
                && HashUtils.getFileHash(file, "MD5").contentEquals(UpdatableFile.getStoredHash(file))) {
                
                UpdatableFile.removeStoredHash(file)
                file.delete()
            }
        }
    }
    
    
    private fun extractFile(path: String, addon: AddonLoader? = null): String? {
        val namespace = addon?.description?.id ?: "nova"
        val file = File(NOVA.dataFolder, path.insertAfter('/', "$namespace/")).let { File(it.parent, it.name) }
        if (file.name.matches(FILE_PATTERN)) {
            UpdatableFile.load(file) { if (addon != null) getResourceAsStream(addon.file, path)!! else getResourceAsStream(path)!! }
            return NOVA.dataFolder.toURI().relativize(file.toURI()).path
        }
        
        LOGGER.severe("Could not load data file $path: Invalid file name")
        return null
    }
    
    fun processFiles(
        dirName: String,
        filter: (File) -> Boolean = { it.isFile && it.extension == "json" },
        fileProcessor: (NamespacedId, File) -> Unit
    ) {
        DATA_DIR.listFiles(FileFilter(File::isDirectory))?.forEach { namespaceDir ->
            val namespace = namespaceDir.name
            val dir = File(DATA_DIR, "$namespace/$dirName")
            if (!dir.exists() || dir.isFile) return@forEach
            dir.walkTopDown().filter(filter).forEach { file ->
                val id = NamespacedId(namespace, file.nameWithoutExtension)
                fileProcessor.invoke(id, file)
            }
        }
    }
    
}