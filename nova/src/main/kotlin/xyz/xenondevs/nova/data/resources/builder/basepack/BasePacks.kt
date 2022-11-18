package xyz.xenondevs.nova.data.resources.builder.basepack

import net.lingala.zip4j.ZipFile
import org.bukkit.Material
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.basepack.merger.FileMerger
import xyz.xenondevs.nova.data.resources.model.blockstate.BlockStateConfigType
import xyz.xenondevs.nova.util.StringUtils
import java.io.File
import java.nio.file.Path
import java.util.logging.Level

private val DEFAULT_WHITELISTED_FILE_TYPES: Set<String> = hashSetOf(
    "json", "png", "mcmeta", "ogg", "txt", "bin", "fsh", "vsh", "glsl", // vanilla
    "properties" // optifine
)

private val WHITELISTED_FILE_TYPES: Set<String> by configReloadable {
    DEFAULT_CONFIG.getStringList("resource_pack.whitelisted_file_types").mapTo(HashSet()) { it.lowercase() } + DEFAULT_WHITELISTED_FILE_TYPES
}

private val BASE_PACKS by configReloadable { DEFAULT_CONFIG.getStringList("resource_pack.base_packs").map(::File) }

internal class BasePacks {
    
    private val mergers = FileMerger.createMergers(this)
    private val packs = BASE_PACKS + (ResourcePackBuilder.BASE_PACKS_DIR.listFiles() ?: emptyArray())
    
    val packAmount = packs.size
    val occupiedModelData = HashMap<Material, HashSet<Int>>()
    val occupiedSolidIds = HashMap<BlockStateConfigType<*>, HashSet<Int>>()
    
    fun include() {
        packs.map {
            if (it.isFile && it.extension.equals("zip", true)) {
                val dir = File(ResourcePackBuilder.TEMP_BASE_PACKS_DIR, it.nameWithoutExtension + StringUtils.randomString(10))
                dir.mkdirs()
                ZipFile(it).extractAll(dir.absolutePath)
                
                return@map dir
            }
            
            return@map it
        }.forEach(::mergeBasePack)
    }
    
    private fun mergeBasePack(packDir: File) {
        LOGGER.info("Adding base pack $packDir")
        packDir.walkTopDown()
            .filter(File::isFile)
            .forEach { file ->
                if (file.extension.lowercase() !in WHITELISTED_FILE_TYPES) {
                    LOGGER.info("Skipping file $file as it is not a resource pack file")
                    return@forEach
                }
                
                val relStr = file.toRelativeString(packDir)
                val relPath = Path.of(relStr)
                val packFile = File(ResourcePackBuilder.PACK_DIR, relStr)
                
                packFile.parentFile.mkdirs()
                val fileMerger = mergers.firstOrNull { relPath.startsWith(it.path) }
                if (fileMerger != null) {
                    try {
                        fileMerger.merge(file, packFile)
                    } catch (t: Throwable) {
                        LOGGER.log(Level.SEVERE, "An exception occurred trying to merge base pack file \"$file\" with \"$packFile\"", t)
                    }
                } else if (!packFile.exists()) {
                    file.copyTo(packFile)
                } else {
                    LOGGER.warning("Skipping file $file: File type cannot be merged")
                }
            }
    }
    
}