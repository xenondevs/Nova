package xyz.xenondevs.nova.data

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.file
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.util.data.HashUtils
import xyz.xenondevs.nova.util.data.getResourceAsStream
import xyz.xenondevs.nova.util.data.getResources
import xyz.xenondevs.nova.util.insertAfter
import java.io.File
import java.io.FileFilter

@InternalInit(
    stage = InternalInitStage.PRE_WORLD
)
internal object DataFileParser {
    
    private val FILE_PATTERN = Regex("""^[a-z][a-z\d_]*.json$""")
    private val DATA_DIR = File(NOVA.dataFolder, "data")
    
    @InitFun
    private fun init() {
        val existingPaths = ObjectArrayList<String>()
        
        // Extract data files
        existingPaths += getResources("data/").mapNotNull(::extractFile)
        
        // Extract data files from addons
        AddonBootstrapper.addons.forEach { addon -> 
            existingPaths += getResources(addon.file, "data/").mapNotNull { extractFile(it, addon) }
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
    
    
    private fun extractFile(path: String, addon: Addon? = null): String? {
        val namespace = addon?.id ?: "nova"
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
        fileProcessor: (ResourceLocation, File) -> Unit
    ) {
        DATA_DIR.listFiles(FileFilter(File::isDirectory))?.forEach { namespaceDir ->
            val namespace = namespaceDir.name
            val dir = File(DATA_DIR, "$namespace/$dirName")
            if (!dir.exists() || dir.isFile) return@forEach
            dir.walkTopDown().filter(filter).forEach { file ->
                val id = ResourceLocation.fromNamespaceAndPath(namespace, file.nameWithoutExtension)
                fileProcessor.invoke(id, file)
            }
        }
    }
    
}